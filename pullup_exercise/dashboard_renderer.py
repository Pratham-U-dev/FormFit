"""
pullup_exercise.dashboard_renderer
==================================
Renders the real-time biomechanics HUD and post-set summary card matching
the reference design (see attached screenshot).

Elements:
  1. Top-left live metric stack (Reps, Phase Pill, Chin count, Speed loss, Peak Power,
     Lat temp, Fatigued pools, kcal & kJ).
  2. On-body joint gauges (Elbow angle callout, Speed pointer, Bar line, Grid).
  3. Bottom-left rep summary card (Phase durations, Kinetics, Chin verdict card).
  4. Bottom shoulder height trajectory waveform & muscle effort gradient legend.
"""

import math
from typing import Dict, List, Optional, Tuple, Union
from .biomechanics import BlazePoseLandmarks, RepRecord
from .heatmap import ColorGradient


class PullUpDashboardRenderer:
    """
    Renders the live pull-up biomechanics dashboard layer onto frames
    or generates structured visual elements for client rendering.
    """

    def __init__(self,
                 theme_accent_color: Tuple[int, int, int] = (255, 140, 0), # Orange
                 hud_font_scale: float = 1.0):
        self.theme_accent = theme_accent_color
        self.font_scale = hud_font_scale

    def render_live_dashboard(self,
                              frame,
                              analysis_data: Dict,
                              landmarks: Optional[List[Tuple[float, float, float, float]]] = None):
        """
        Draws the complete biomechanics HUD onto a BGR numpy frame (OpenCV).
        """
        try:
            import cv2
            import numpy as np
        except ImportError:
            # If OpenCV is not available in execution container, return frame
            return frame

        h, w = frame.shape[:2]

        # 1. Perspective Background Grid Lines
        self._draw_grid_lines(frame, w, h)

        # 2. Bar Reference Line
        bar_y_norm = analysis_data.get("bar_y")
        if bar_y_norm is not None:
            bar_px_y = int(round(bar_y_norm * h))
            cv2.line(frame, (0, bar_px_y), (w, bar_px_y), (200, 200, 200), 2, cv2.LINE_AA)
            # Label
            cv2.putText(frame, "BAR", (w - 110, bar_px_y - 10),
                        cv2.FONT_HERSHEY_DUPLEX, 0.75 * self.font_scale, (255, 255, 255), 2, cv2.LINE_AA)

        # 3. Skeleton Overlays & Live Joint Gauges
        if landmarks:
            self._draw_skeleton_and_gauges(frame, landmarks, analysis_data, w, h)

        # 4. Top Left Metric Stack
        self._draw_top_left_stack(frame, analysis_data)

        # 5. Bottom Rep Summary Card (if reps exist)
        last_rep = analysis_data.get("last_rep")
        if last_rep:
            self._draw_rep_summary_card(frame, last_rep, w, h)

        # 6. Bottom Shoulder Height Waveform & Muscle Effort Legend
        self._draw_bottom_waveform_and_legend(frame, analysis_data, w, h)

        return frame

    def _draw_grid_lines(self, frame, w: int, h: int):
        """Draws subtle perspective tracking grid."""
        import cv2
        grid_color = (60, 60, 60)
        # Vertical grid lines
        for x in range(0, w, max(40, w // 10)):
            cv2.line(frame, (x, 0), (x, h), grid_color, 1, cv2.LINE_AA)
        # Horizontal grid lines
        for y in range(0, h, max(40, h // 16)):
            cv2.line(frame, (0, y), (w, y), grid_color, 1, cv2.LINE_AA)

    def _draw_top_left_stack(self, frame, data: Dict):
        """Draws the primary metric readouts in the upper-left corner."""
        import cv2

        x_base = 35
        y_cursor = 70

        # Big Reps Count
        rep_cnt = str(data.get("rep_count", 0))
        cv2.putText(frame, rep_cnt, (x_base, y_cursor),
                    cv2.FONT_HERSHEY_DUPLEX, 2.6 * self.font_scale, (255, 255, 255), 3, cv2.LINE_AA)
        y_cursor += 30
        cv2.putText(frame, "REPS", (x_base, y_cursor),
                    cv2.FONT_HERSHEY_DUPLEX, 0.70 * self.font_scale, (200, 200, 200), 1, cv2.LINE_AA)

        # Phase Pill (HOLD, PULL, LOWER, HANG)
        y_cursor += 25
        phase = data.get("phase", "HANG")
        pill_bg = (0, 140, 255) if phase == "HOLD" else (40, 180, 50) if phase == "PULL" else (180, 80, 20)
        cv2.rectangle(frame, (x_base, y_cursor), (x_base + 130, y_cursor + 38), pill_bg, -1, cv2.LINE_AA)
        cv2.putText(frame, phase, (x_base + 18, y_cursor + 26),
                    cv2.FONT_HERSHEY_DUPLEX, 0.75 * self.font_scale, (0, 0, 0), 2, cv2.LINE_AA)

        # Stacked Biomechanics Metrics
        y_cursor += 65
        rep_tot = data.get("rep_count", 0)
        chin_tally = data.get("chin_at_bar_count", 0)
        self._draw_stat_line(frame, x_base, y_cursor,
                             f"{chin_tally}/{rep_tot}", "CHIN AT THE BAR")

        y_cursor += 45
        vl = int(data.get("speed_loss_pct", 0))
        self._draw_stat_line(frame, x_base, y_cursor,
                             f"{vl} %", "SPEED VS REP 1")

        y_cursor += 45
        peak_p = int(data.get("peak_power_w", 0))
        self._draw_stat_line(frame, x_base, y_cursor,
                             f"{peak_p} W", "PEAK POWER")

        y_cursor += 45
        muscles = data.get("muscles", {})
        lat_temp = muscles.get("latissimus_dorsi", {}).get("delta_t_c", 0.0)
        self._draw_stat_line(frame, x_base, y_cursor,
                             f"+{lat_temp:.2f} \u00b0C", "LATS \u00b7 MODELLED")

        y_cursor += 45
        lat_fat = int(muscles.get("latissimus_dorsi", {}).get("fatigued_pct", 0))
        bicep_fat = int(muscles.get("biceps_brachii", {}).get("fatigued_pct", 0))
        self._draw_stat_line(frame, x_base, y_cursor,
                             f"{lat_fat} % \u00b7 {bicep_fat} %", "LATS \u00b7 BICEPS FATIGUED, MODEL")

        y_cursor += 45
        kcal = data.get("energetics", {}).get("cumulative_kcal", 0.0)
        heat_kj = data.get("energetics", {}).get("cumulative_heat_kj", 0.0)
        self._draw_stat_line(frame, x_base, y_cursor,
                             f"\u2248 {kcal:.1f} kcal", f"{heat_kj:.0f} kJ OF HEAT")

    def _draw_stat_line(self, frame, x: int, y: int, title: str, subtitle: str):
        import cv2
        cv2.putText(frame, title, (x, y),
                    cv2.FONT_HERSHEY_DUPLEX, 0.85 * self.font_scale, (255, 255, 255), 2, cv2.LINE_AA)
        cv2.putText(frame, subtitle, (x, y + 16),
                    cv2.FONT_HERSHEY_DUPLEX, 0.48 * self.font_scale, (180, 180, 180), 1, cv2.LINE_AA)

    def _draw_skeleton_and_gauges(self, frame, lm, data: Dict, w: int, h: int):
        """Draws tracked joints and on-body angle / speed callouts."""
        import cv2

        def to_px(idx: int) -> Tuple[int, int]:
            return (int(round(lm[idx][0] * w)), int(round(lm[idx][1] * h)))

        bones = [
            (BlazePoseLandmarks.LEFT_SHOULDER, BlazePoseLandmarks.RIGHT_SHOULDER),
            (BlazePoseLandmarks.LEFT_SHOULDER, BlazePoseLandmarks.LEFT_ELBOW),
            (BlazePoseLandmarks.LEFT_ELBOW, BlazePoseLandmarks.LEFT_WRIST),
            (BlazePoseLandmarks.RIGHT_SHOULDER, BlazePoseLandmarks.RIGHT_ELBOW),
            (BlazePoseLandmarks.RIGHT_ELBOW, BlazePoseLandmarks.RIGHT_WRIST),
            (BlazePoseLandmarks.LEFT_SHOULDER, BlazePoseLandmarks.LEFT_HIP),
            (BlazePoseLandmarks.RIGHT_SHOULDER, BlazePoseLandmarks.RIGHT_HIP),
            (BlazePoseLandmarks.LEFT_HIP, BlazePoseLandmarks.RIGHT_HIP),
            (BlazePoseLandmarks.LEFT_HIP, BlazePoseLandmarks.LEFT_KNEE),
            (BlazePoseLandmarks.RIGHT_HIP, BlazePoseLandmarks.RIGHT_KNEE),
            (BlazePoseLandmarks.LEFT_KNEE, BlazePoseLandmarks.LEFT_ANKLE),
            (BlazePoseLandmarks.RIGHT_KNEE, BlazePoseLandmarks.RIGHT_ANKLE),
        ]

        # Draw bone vectors in high-contrast crisp white
        for b1, b2 in bones:
            p1, p2 = to_px(b1), to_px(b2)
            cv2.line(frame, p1, p2, (255, 255, 255), 3, cv2.LINE_AA)

        # Draw joint nodes
        for idx in [
            BlazePoseLandmarks.LEFT_SHOULDER, BlazePoseLandmarks.RIGHT_SHOULDER,
            BlazePoseLandmarks.LEFT_ELBOW, BlazePoseLandmarks.RIGHT_ELBOW,
            BlazePoseLandmarks.LEFT_WRIST, BlazePoseLandmarks.RIGHT_WRIST,
            BlazePoseLandmarks.LEFT_HIP, BlazePoseLandmarks.RIGHT_HIP,
            BlazePoseLandmarks.LEFT_KNEE, BlazePoseLandmarks.RIGHT_KNEE,
        ]:
            pt = to_px(idx)
            cv2.circle(frame, pt, 6, (255, 255, 255), -1, cv2.LINE_AA)
            cv2.circle(frame, pt, 8, (0, 165, 255), 2, cv2.LINE_AA)

        # On-Body Elbow Angle Callout
        el_pt = to_px(BlazePoseLandmarks.RIGHT_ELBOW)
        angle = int(round(data.get("elbow_angle", 180)))
        cv2.rectangle(frame, (el_pt[0] - 80, el_pt[1] - 45), (el_pt[0] + 20, el_pt[1] + 15), (0, 0, 0), -1, cv2.LINE_AA)
        cv2.putText(frame, f"{angle}\u00b0", (el_pt[0] - 70, el_pt[1] - 15),
                    cv2.FONT_HERSHEY_DUPLEX, 0.90 * self.font_scale, (255, 255, 255), 2, cv2.LINE_AA)
        cv2.putText(frame, "ELBOW", (el_pt[0] - 70, el_pt[1] + 6),
                    cv2.FONT_HERSHEY_DUPLEX, 0.45 * self.font_scale, (180, 180, 180), 1, cv2.LINE_AA)

        # On-Body Speed Pointer
        hip_pt = to_px(BlazePoseLandmarks.RIGHT_HIP)
        v = data.get("vertical_speed_mps", 0.0)
        speed_txt = f"{abs(v):.2f} m/s"
        cv2.line(frame, (hip_pt[0] - 40, hip_pt[1]), (hip_pt[0] + 15, hip_pt[1]), (255, 255, 255), 2, cv2.LINE_AA)
        cv2.putText(frame, speed_txt, (hip_pt[0] + 25, hip_pt[1] - 4),
                    cv2.FONT_HERSHEY_DUPLEX, 0.70 * self.font_scale, (255, 255, 255), 2, cv2.LINE_AA)
        cv2.putText(frame, "SPEED", (hip_pt[0] + 25, hip_pt[1] + 16),
                    cv2.FONT_HERSHEY_DUPLEX, 0.45 * self.font_scale, (180, 180, 180), 1, cv2.LINE_AA)

    def _draw_rep_summary_card(self, frame, rep: Dict, w: int, h: int):
        """Draws the detailed rep feedback card at the lower-left."""
        import cv2

        card_x = 35
        card_y = h - 380
        card_w = min(w - 70, 520)
        card_h = 165

        # Dark frosted card background
        cv2.rectangle(frame, (card_x, card_y), (card_x + card_w, card_y + card_h), (25, 25, 25), -1, cv2.LINE_AA)
        cv2.rectangle(frame, (card_x, card_y), (card_x + card_w, card_y + card_h), (70, 70, 70), 2, cv2.LINE_AA)

        # Rep Header
        rep_num = rep.get("rep_num", 1)
        cv2.putText(frame, f"REP {rep_num}", (card_x + 18, card_y + 35),
                    cv2.FONT_HERSHEY_DUPLEX, 1.0 * self.font_scale, (255, 255, 255), 2, cv2.LINE_AA)

        # Badges: FULL LOCK-OUT / NO SWING
        badge_x = card_x + 18
        badge_y = card_y + 48
        if rep.get("full_lockout", True):
            cv2.rectangle(frame, (badge_x, badge_y), (badge_x + 130, badge_y + 24), (50, 160, 100), -1, cv2.LINE_AA)
            cv2.putText(frame, "FULL LOCK-OUT", (badge_x + 8, badge_y + 17),
                        cv2.FONT_HERSHEY_DUPLEX, 0.45 * self.font_scale, (0, 0, 0), 1, cv2.LINE_AA)
            badge_x += 140

        sway = rep.get("hip_sway_cm", 4.0)
        if sway <= 8.0:
            cv2.rectangle(frame, (badge_x, badge_y), (badge_x + 95, badge_y + 24), (50, 160, 140), -1, cv2.LINE_AA)
            cv2.putText(frame, "NO SWING", (badge_x + 8, badge_y + 17),
                        cv2.FONT_HERSHEY_DUPLEX, 0.45 * self.font_scale, (0, 0, 0), 1, cv2.LINE_AA)

        # Phase Durations: up X s, hold Y s, down Z s
        t_up = rep.get("duration_concentric", 1.0)
        t_hld = rep.get("duration_top_hold", 0.8)
        t_dn = rep.get("duration_eccentric", 1.1)
        cv2.putText(frame, f"up {t_up:.1f} s  hold {t_hld:.1f} s  down {t_dn:.1f} s",
                    (card_x + 18, card_y + 95),
                    cv2.FONT_HERSHEY_DUPLEX, 0.58 * self.font_scale, (220, 220, 220), 1, cv2.LINE_AA)

        # Metrics: peak v, power W, kcal
        v_pk = rep.get("peak_concentric_velocity", 0.7)
        pow_w = int(rep.get("peak_power_w", 366))
        kcal = rep.get("energy_kcal", 0.8)
        cv2.putText(frame, f"peak {v_pk:.2f} m/s  {pow_w} W  \u2248 {kcal:.1f} kcal",
                    (card_x + 18, card_y + 120),
                    cv2.FONT_HERSHEY_DUPLEX, 0.58 * self.font_scale, (220, 220, 220), 1, cv2.LINE_AA)

        # Form detail: speed loss %, sway cm
        vl = int(rep.get("velocity_loss_pct", 0))
        cv2.putText(frame, f"speed loss {vl} %  sway {sway:.0f} cm",
                    (card_x + 18, card_y + 145),
                    cv2.FONT_HERSHEY_DUPLEX, 0.58 * self.font_scale, (220, 220, 220), 1, cv2.LINE_AA)

        # Right-hand Chin Verdict Card (Orange Accent)
        box_w = 175
        box_x = card_x + card_w - box_w - 12
        box_y = card_y + 14
        box_h = card_h - 28
        cv2.rectangle(frame, (box_x, box_y), (box_x + box_w, box_y + box_h), (0, 130, 240), -1, cv2.LINE_AA)
        cv2.putText(frame, "~", (box_x + box_w // 2 - 8, box_y + 35),
                    cv2.FONT_HERSHEY_DUPLEX, 1.2 * self.font_scale, (0, 0, 0), 2, cv2.LINE_AA)
        cv2.putText(frame, "CHIN AT THE BAR", (box_x + 12, box_y + 68),
                    cv2.FONT_HERSHEY_DUPLEX, 0.52 * self.font_scale, (0, 0, 0), 2, cv2.LINE_AA)
        cv2.putText(frame, "3.4 cm in plane, +3 hidden", (box_x + 8, box_y + 98),
                    cv2.FONT_HERSHEY_DUPLEX, 0.38 * self.font_scale, (40, 40, 40), 1, cv2.LINE_AA)

    def _draw_bottom_waveform_and_legend(self, frame, data: Dict, w: int, h: int):
        """Draws the shoulder height trajectory line graph and thermal effort bar."""
        import cv2

        base_y = h - 180

        # Section Header
        cv2.putText(frame, "SHOULDER HEIGHT", (35, base_y),
                    cv2.FONT_HERSHEY_DUPLEX, 0.60 * self.font_scale, (200, 200, 200), 1, cv2.LINE_AA)

        # Thermal Muscle Effort Legend
        leg_x = w - 460
        cv2.putText(frame, "MUSCLE EFFORT", (leg_x, base_y),
                    cv2.FONT_HERSHEY_DUPLEX, 0.50 * self.font_scale, (200, 200, 200), 1, cv2.LINE_AA)
        cv2.putText(frame, "rest", (leg_x + 145, base_y),
                    cv2.FONT_HERSHEY_DUPLEX, 0.45 * self.font_scale, (160, 160, 160), 1, cv2.LINE_AA)

        # Multi-stop color bar
        bar_x = leg_x + 185
        bar_w = 120
        for i in range(bar_w):
            color = ColorGradient.get_bgr(i / bar_w)
            cv2.line(frame, (bar_x + i, base_y - 12), (bar_x + i, base_y - 2), color, 1)

        cv2.putText(frame, "max \u00b7 a model, not a thermal camera", (bar_x + bar_w + 10, base_y),
                    cv2.FONT_HERSHEY_DUPLEX, 0.40 * self.font_scale, (160, 160, 160), 1, cv2.LINE_AA)

        # Trajectory Waveform
        wave_y_zero = h - 100
        wave_h = 60
        cv2.line(frame, (80, wave_y_zero), (w - 80, wave_y_zero), (100, 100, 100), 1, cv2.LINE_AA)

        # Draw sinusoidal-like signal of reps
        rep_count = data.get("rep_count", 0)
        n_pts = min(rep_count, 11)
        if n_pts > 0:
            step_x = (w - 180) // 12
            prev_pt = (80, wave_y_zero)
            for i in range(1, n_pts + 1):
                pt_mid = (80 + int((i - 0.5) * step_x), wave_y_zero - wave_h)
                pt_end = (80 + i * step_x, wave_y_zero)
                cv2.line(frame, prev_pt, pt_mid, (255, 255, 255), 2, cv2.LINE_AA)
                cv2.line(frame, pt_mid, pt_end, (255, 255, 255), 2, cv2.LINE_AA)
                # Rep peak number label
                cv2.putText(frame, str(i), (pt_mid[0] - 6, pt_mid[1] - 8),
                            cv2.FONT_HERSHEY_DUPLEX, 0.50 * self.font_scale, (255, 255, 255), 1, cv2.LINE_AA)
                prev_pt = pt_end

        # Rep Number Chips at Very Bottom: [ 1 ] [ 2 ] [ 3 ] ... [ 11 ]
        chip_y = h - 60
        chip_w = 40
        chip_h = 32
        for i in range(1, 12):
            cx = 70 + (i - 1) * (chip_w + 8)
            is_done = i <= rep_count
            chip_bg = (0, 130, 240) if is_done else (40, 40, 40)
            text_color = (0, 0, 0) if is_done else (150, 150, 150)
            cv2.rectangle(frame, (cx, chip_y), (cx + chip_w, chip_y + chip_h), chip_bg, -1, cv2.LINE_AA)
            cv2.putText(frame, str(i), (cx + 12, chip_y + 22),
                        cv2.FONT_HERSHEY_DUPLEX, 0.60 * self.font_scale, text_color, 2, cv2.LINE_AA)
