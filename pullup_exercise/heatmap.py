"""
pullup_exercise.heatmap
=======================
Calculates frame-by-frame muscle temperature/fatigue values and generates
dynamic thermal color gradient overlays mapped to the body silhouette.

Target muscles:
  - Latissimus dorsi (upper back / flank / armpit to ribcage)
  - Biceps brachii & brachialis (anterior upper arms, shoulder -> elbow)
  - Forearm flexors / extensors (elbow -> wrist)
  - Trapezius / Neck (upper back / shoulders)
  - Postural legs (isometric lower body, cold end of spectrum)
"""

import math
from typing import Dict, List, Optional, Tuple, Union
from .biomechanics import BlazePoseLandmarks, MuscleState


class ColorGradient:
    """
    Continuous thermal color gradient lookup table.
    Maps effort/temperature index E in [0.0, 1.0] to smooth (R, G, B) colors:
      0.00 -> Deep Blue   (25, 45, 180)   [Rest / Passive]
      0.20 -> Cyan        (0, 190, 220)   [Low activation]
      0.45 -> Green       (40, 210, 60)   [Moderate effort]
      0.65 -> Yellow      (245, 220, 20)  [High strain]
      0.82 -> Orange      (255, 125, 0)   [Near failure]
      1.00 -> Bright Red  (240, 30, 20)   [Max effort & heat]
    """

    STOPS: List[Tuple[float, Tuple[int, int, int]]] = [
        (0.00, (25, 45, 180)),
        (0.20, (0, 190, 220)),
        (0.45, (40, 210, 60)),
        (0.65, (245, 220, 20)),
        (0.82, (255, 125, 0)),
        (1.00, (240, 30, 20)),
    ]

    @classmethod
    def get_rgb(cls, t: float) -> Tuple[int, int, int]:
        """Interpolates RGB color tuple for scalar in [0.0, 1.0]."""
        val = max(0.0, min(1.0, float(t)))
        for i in range(len(cls.STOPS) - 1):
            t0, c0 = cls.STOPS[i]
            t1, c1 = cls.STOPS[i + 1]
            if val <= t1:
                ratio = (val - t0) / (t1 - t0) if (t1 - t0) > 1e-6 else 0.0
                r = int(round(c0[0] + ratio * (c1[0] - c0[0])))
                g = int(round(c0[1] + ratio * (c1[1] - c0[1])))
                b = int(round(c0[2] + ratio * (c1[2] - c0[2])))
                return (r, g, b)
        return cls.STOPS[-1][1]

    @classmethod
    def get_bgr(cls, t: float) -> Tuple[int, int, int]:
        """Returns BGR tuple for OpenCV rendering."""
        r, g, b = cls.get_rgb(t)
        return (b, g, r)

    @classmethod
    def get_hex(cls, t: float) -> str:
        """Returns hex color string (e.g. '#FF7D00')."""
        r, g, b = cls.get_rgb(t)
        return f"#{r:02X}{g:02X}{b:02X}"


class MuscleHeatmapRenderer:
    """
    Maps muscle effort/temperature to body anatomy polygons derived from BlazePose landmarks.
    Renders semi-transparent thermal gradients over silhouettes or generates polygon geometries.
    """

    def __init__(self, alpha: float = 0.55):
        self.alpha = alpha

    def compute_muscle_regions(self,
                               landmarks: List[Tuple[float, float, float, float]],
                               muscle_states: Dict[str, MuscleState],
                               width: int,
                               height: int) -> List[Dict]:
        """
        Calculates 2D pixel polygons and corresponding thermal colors for each muscle group.
        """
        def to_px(idx: int) -> Tuple[int, int]:
            pt = landmarks[idx]
            return (int(round(pt[0] * width)), int(round(pt[1] * height)))

        regions = []

        sh_l = to_px(BlazePoseLandmarks.LEFT_SHOULDER)
        sh_r = to_px(BlazePoseLandmarks.RIGHT_SHOULDER)
        el_l = to_px(BlazePoseLandmarks.LEFT_ELBOW)
        el_r = to_px(BlazePoseLandmarks.RIGHT_ELBOW)
        wr_l = to_px(BlazePoseLandmarks.LEFT_WRIST)
        wr_r = to_px(BlazePoseLandmarks.RIGHT_WRIST)
        hip_l = to_px(BlazePoseLandmarks.LEFT_HIP)
        hip_r = to_px(BlazePoseLandmarks.RIGHT_HIP)
        kn_l = to_px(BlazePoseLandmarks.LEFT_KNEE)
        kn_r = to_px(BlazePoseLandmarks.RIGHT_KNEE)

        # Midpoints and anatomical anchors
        sh_mid = ((sh_l[0] + sh_r[0]) // 2, (sh_l[1] + sh_r[1]) // 2)
        hip_mid = ((hip_l[0] + hip_r[0]) // 2, (hip_l[1] + hip_r[1]) // 2)
        spine_mid = ((sh_mid[0] + hip_mid[0]) // 2, (sh_mid[1] + hip_mid[1]) // 2)

        # Vector thickness helpers
        torso_w = max(20, int(abs(sh_r[0] - sh_l[0]) * 0.35))
        arm_w = max(12, int(torso_w * 0.40))
        forearm_w = max(10, int(arm_w * 0.80))

        # 1. Latissimus Dorsi & Back / Ribcage
        lat_effort = muscle_states.get("latissimus_dorsi", MuscleState()).effort_index
        lat_rgb = ColorGradient.get_rgb(lat_effort)
        lat_bgr = ColorGradient.get_bgr(lat_effort)

        # Left Lat polygon (Torso flank left)
        poly_lat_l = [
            sh_l,
            (sh_l[0] - torso_w // 2, (sh_l[1] + hip_l[1]) // 2),
            hip_l,
            hip_mid,
            spine_mid,
        ]
        # Right Lat polygon (Torso flank right)
        poly_lat_r = [
            sh_r,
            (sh_r[0] + torso_w // 2, (sh_r[1] + hip_r[1]) // 2),
            hip_r,
            hip_mid,
            spine_mid,
        ]
        regions.append({
            "name": "latissimus_dorsi_left",
            "muscle": "latissimus_dorsi",
            "polygon": poly_lat_l,
            "color_rgb": lat_rgb,
            "color_bgr": lat_bgr,
            "effort": lat_effort,
        })
        regions.append({
            "name": "latissimus_dorsi_right",
            "muscle": "latissimus_dorsi",
            "polygon": poly_lat_r,
            "color_rgb": lat_rgb,
            "color_bgr": lat_bgr,
            "effort": lat_effort,
        })

        # 2. Biceps Brachii & Upper Arms
        bicep_effort = muscle_states.get("biceps_brachii", MuscleState()).effort_index
        bicep_rgb = ColorGradient.get_rgb(bicep_effort)
        bicep_bgr = ColorGradient.get_bgr(bicep_effort)

        poly_bicep_l = self._build_capsule_polygon(sh_l, el_l, arm_w)
        poly_bicep_r = self._build_capsule_polygon(sh_r, el_r, arm_w)
        regions.append({
            "name": "biceps_left",
            "muscle": "biceps_brachii",
            "polygon": poly_bicep_l,
            "color_rgb": bicep_rgb,
            "color_bgr": bicep_bgr,
            "effort": bicep_effort,
        })
        regions.append({
            "name": "biceps_right",
            "muscle": "biceps_brachii",
            "polygon": poly_bicep_r,
            "color_rgb": bicep_rgb,
            "color_bgr": bicep_bgr,
            "effort": bicep_effort,
        })

        # 3. Forearm Flexors & Grip
        grip_effort = muscle_states.get("forearm_flexors", MuscleState()).effort_index
        grip_rgb = ColorGradient.get_rgb(grip_effort)
        grip_bgr = ColorGradient.get_bgr(grip_effort)

        poly_forearm_l = self._build_capsule_polygon(el_l, wr_l, forearm_w)
        poly_forearm_r = self._build_capsule_polygon(el_r, wr_r, forearm_w)
        regions.append({
            "name": "forearms_left",
            "muscle": "forearm_flexors",
            "polygon": poly_forearm_l,
            "color_rgb": grip_rgb,
            "color_bgr": grip_bgr,
            "effort": grip_effort,
        })
        regions.append({
            "name": "forearms_right",
            "muscle": "forearm_flexors",
            "polygon": poly_forearm_r,
            "color_rgb": grip_rgb,
            "color_bgr": grip_bgr,
            "effort": grip_effort,
        })

        # 4. Trapezius / Upper Back
        trap_effort = muscle_states.get("trapezius", MuscleState()).effort_index
        trap_rgb = ColorGradient.get_rgb(trap_effort)
        trap_bgr = ColorGradient.get_bgr(trap_effort)
        poly_trap = [
            sh_l,
            (sh_mid[0], sh_mid[1] - int(torso_w * 0.6)),
            sh_r,
            spine_mid,
        ]
        regions.append({
            "name": "trapezius",
            "muscle": "trapezius",
            "polygon": poly_trap,
            "color_rgb": trap_rgb,
            "color_bgr": trap_bgr,
            "effort": trap_effort,
        })

        # 5. Postural Legs (Isometric tuck, stays cold blue)
        leg_effort = muscle_states.get("postural_legs", MuscleState()).effort_index
        leg_rgb = ColorGradient.get_rgb(leg_effort)
        leg_bgr = ColorGradient.get_bgr(leg_effort)
        poly_leg_l = self._build_capsule_polygon(hip_l, kn_l, int(arm_w * 1.2))
        poly_leg_r = self._build_capsule_polygon(hip_r, kn_r, int(arm_w * 1.2))
        regions.append({
            "name": "legs_left",
            "muscle": "postural_legs",
            "polygon": poly_leg_l,
            "color_rgb": leg_rgb,
            "color_bgr": leg_bgr,
            "effort": leg_effort,
        })
        regions.append({
            "name": "legs_right",
            "muscle": "postural_legs",
            "polygon": poly_leg_r,
            "color_rgb": leg_rgb,
            "color_bgr": leg_bgr,
            "effort": leg_effort,
        })

        return regions

    def _build_capsule_polygon(self,
                               p1: Tuple[int, int],
                               p2: Tuple[int, int],
                               radius: int) -> List[Tuple[int, int]]:
        """Constructs a capsule polygon around line segment (p1, p2) with radius r."""
        dx = p2[0] - p1[0]
        dy = p2[1] - p1[1]
        dist = math.hypot(dx, dy) + 1e-6
        nx = -dy / dist * radius
        ny = dx / dist * radius

        return [
            (int(round(p1[0] + nx)), int(round(p1[1] + ny))),
            (int(round(p2[0] + nx)), int(round(p2[1] + ny))),
            (int(round(p2[0] - nx)), int(round(p2[1] - ny))),
            (int(round(p1[0] - nx)), int(round(p1[1] - ny))),
        ]

    def render_overlay(self,
                       frame,
                       landmarks: List[Tuple[float, float, float, float]],
                       muscle_states: Dict[str, MuscleState]):
        """
        Renders semi-transparent thermal polygons directly onto a numpy BGR image frame (OpenCV).
        """
        try:
            import cv2
            import numpy as np

            h, w = frame.shape[:2]
            regions = self.compute_muscle_regions(landmarks, muscle_states, w, h)

            overlay = frame.copy()
            for r in regions:
                pts = np.array(r["polygon"], dtype=np.int32)
                cv2.fillPoly(overlay, [pts], r["color_bgr"])

            # Gaussian blur the overlay slightly for smooth organic muscle transitions
            cv2.GaussianBlur(overlay, (21, 21), 0, dst=overlay)

            # Alpha blend with original frame
            cv2.addWeighted(overlay, self.alpha, frame, 1.0 - self.alpha, 0, dst=frame)
            return frame
        except ImportError:
            # OpenCV not present in current environment; return unmodified frame
            return frame
