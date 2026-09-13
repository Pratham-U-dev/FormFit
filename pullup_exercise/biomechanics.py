"""
pullup_exercise.biomechanics
============================
Core biomechanics signal processing, rep boundary segmentation, kinematics,
force-velocity dynamics, 3-state motor unit fatigue modeling, and form efficiency scoring.

References:
  - Xia & Frey-Law 2008 (J Biomech): 3-state motor unit model (M_R, M_A, M_F).
  - Frey-Law, Looft & Heitsman 2012 (J Biomech): Joint fatigue (F) and recovery (R) rates.
  - Looft, Herkert & Frey-Law 2018 (J Biomech): 3CC-r intermittent fatigue and blood flow gating.
  - Youdas et al. 2010 (J Strength Cond Res): Muscle activation (%MVIC) during pull-ups.
  - Sanchez-Moreno et al. 2020: Velocity loss thresholds in pull-up training (25% & 50%).
  - Beckham et al. 2018: Rep 1 velocity reference anchoring.
  - Crowninshield & Brand 1981 (J Biomech): Static optimization muscle force sharing.
  - Gonzalez-Alonso et al. 2000 (J Physiol) & Kenny et al. 2003: Muscle thermal models.
"""

import math
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple, Union


# BlazePose Standard 33 Landmark Indices
class BlazePoseLandmarks:
    NOSE = 0
    LEFT_EYE_INNER = 1
    LEFT_EYE = 2
    LEFT_EYE_OUTER = 3
    RIGHT_EYE_INNER = 4
    RIGHT_EYE = 5
    RIGHT_EYE_OUTER = 6
    LEFT_EAR = 7
    RIGHT_EAR = 8
    MOUTH_LEFT = 9
    MOUTH_RIGHT = 10
    LEFT_SHOULDER = 11
    RIGHT_SHOULDER = 12
    LEFT_ELBOW = 13
    RIGHT_ELBOW = 14
    LEFT_WRIST = 15
    RIGHT_WRIST = 16
    LEFT_PINKY = 17
    RIGHT_PINKY = 18
    LEFT_INDEX = 19
    RIGHT_INDEX = 20
    LEFT_THUMB = 21
    RIGHT_THUMB = 22
    LEFT_HIP = 23
    RIGHT_HIP = 24
    LEFT_KNEE = 25
    RIGHT_KNEE = 26
    LEFT_ANKLE = 27
    RIGHT_ANKLE = 28
    LEFT_HEEL = 29
    RIGHT_HEEL = 30
    LEFT_FOOT_INDEX = 31
    RIGHT_FOOT_INDEX = 32


# Biomechanics and Anthropometric Constants
G = 9.80665              # m/s^2, gravitational acceleration
C_MUSCLE = 3600.0        # J/(kg*K), specific heat of skeletal muscle
K_BLOOD = 42.0           # W/(K*kg), fully perfused blood heat dissipation rate
TAU_PERFUSION = 90.0     # s, perfusion ramp time constant
ETA_CONC = 0.22          # metabolic efficiency of concentric work
ECC_COST = 0.35          # eccentric metabolic cost relative to concentric
HANG_MET = 3.5           # isometric bar hang metabolic equivalent
KCAL_PER_J = 1.0 / 4184.0
LIFTED_FRAC = 0.956      # body mass minus hands + forearms (de Leva 1996)
ARM_FRAC = 0.332         # shoulder joint -> wrist / stature (Drillis & Contini 1966)
CHIN_DEPTH_CM = 8.0      # assumed depth of chin behind bar plane at peak
VL_LANDMARKS = (25.0, 50.0)  # Sanchez-Moreno 2020 velocity loss thresholds

# 3-State Fatigue Model Parameters (Frey-Law 2012 Table 1 & Looft 2018 Table 4)
# (Fatigue rate F [1/s], Recovery rate R [1/s], Rest recovery multiplier r)
FATIGUE_RATES = {
    "latissimus_dorsi": (0.01820, 0.00168, 15.0), # shoulder extensors
    "biceps_brachii":   (0.00912, 0.00094, 15.0), # elbow flexors
    "brachialis":       (0.00912, 0.00094, 15.0),
    "brachioradialis":  (0.00912, 0.00094, 15.0),
    "forearm_flexors":  (0.00980, 0.00064, 30.0), # grip flexors
    "trapezius":        (0.01820, 0.00168, 15.0),
    "pectoralis_major": (0.01820, 0.00168, 15.0),
    "postural_legs":    (0.01500, 0.00149, 15.0),
}

# Muscle Parameters: (F_max [N], moment_arm [m], mass [kg], mvic_ref [%])
# Holzbaur 2005/2007 scaled to 1.88m, 79kg athlete
MUSCLE_PROPERTIES = {
    "latissimus_dorsi": {"f_max": 1253.0 * 1.2, "arm": 0.045, "mass": 0.734, "mvic": 124},
    "biceps_brachii":   {"f_max": 1060.0 * 1.2, "arm": 0.047 * 0.75, "mass": 0.402, "mvic": 78}, # pronated grip
    "brachialis":       {"f_max": 987.0 * 1.2,  "arm": 0.026, "mass": 0.402, "mvic": 78},
    "brachioradialis":  {"f_max": 261.0 * 1.2,  "arm": 0.077, "mass": 0.182, "mvic": 62},
    "forearm_flexors":  {"f_max": 650.0 * 1.2,  "arm": 0.020, "mass": 0.666, "mvic": 60},
    "trapezius":        {"f_max": 800.0 * 1.2,  "arm": 0.035, "mass": 0.532, "mvic": 52},
    "pectoralis_major": {"f_max": 450.0 * 1.2,  "arm": 0.015, "mass": 0.812, "mvic": 44},
    "postural_legs":    {"f_max": 1200.0,       "arm": 0.040, "mass": 5.500, "mvic": 10},
}


@dataclass
class MuscleState:
    """Represents real-time 3-compartment fatigue & thermal state for a muscle."""
    m_r: float = 1.0     # Resting motor units pool (0..1)
    m_a: float = 0.0     # Active motor units pool (0..1)
    m_f: float = 0.0     # Fatigued motor units pool (0..1)
    activation: float = 0.0   # Current neuromuscular activation (0..1)
    delta_t_c: float = 0.0    # Temperature rise in degrees Celsius
    effort_index: float = 0.0 # Composite effort 0..1 (drives color)
    heat_power_w: float = 0.0 # Heat dissipation rate (Watts)


@dataclass
class RepRecord:
    """Detailed biomechanical summary for a single completed repetition."""
    rep_num: int
    t_start: float
    t_top: float
    t_end: float
    duration_concentric: float
    duration_top_hold: float
    duration_eccentric: float
    duration_total: float
    rom_cm: float
    chin_clearance_cm: float
    chin_verdict: str         # "above", "at", "short"
    elbow_angle_top: float    # degrees at peak flexion
    elbow_angle_lockout: float# degrees at bottom extension
    full_lockout: bool
    peak_concentric_velocity: float # m/s
    mean_concentric_velocity: float # m/s
    velocity_loss_pct: float  # % relative to Rep 1
    peak_power_w: float       # mechanical peak power (W)
    work_j: float             # mechanical work (J)
    energy_kcal: float        # total metabolic energy (kcal)
    hip_sway_cm: float        # horizontal sway / kipping metric
    arm_asymmetry_deg: float  # difference between left and right elbow angle
    form_score: float         # 0..100
    score_breakdown: Dict[str, float] = field(default_factory=dict)
    letter_grade: str = "A"


def calculate_angle_3d(a: Tuple[float, float, float],
                       b: Tuple[float, float, float],
                       c: Tuple[float, float, float]) -> float:
    """Calculates 3D angle at joint b (degrees) given 3D coordinates."""
    v1 = (a[0] - b[0], a[1] - b[1], a[2] - b[2])
    v2 = (c[0] - b[0], c[1] - b[1], c[2] - b[2])
    dot = v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2]
    norm1 = math.sqrt(v1[0]**2 + v1[1]**2 + v1[2]**2) + 1e-9
    norm2 = math.sqrt(v2[0]**2 + v2[1]**2 + v2[2]**2) + 1e-9
    cos_angle = max(-1.0, min(1.0, dot / (norm1 * norm2)))
    return math.degrees(math.acos(cos_angle))


def calculate_angle_2d(a: Tuple[float, float],
                       b: Tuple[float, float],
                       c: Tuple[float, float]) -> float:
    """Calculates 2D angle at joint b (degrees) in image plane."""
    angle_rad = math.atan2(c[1] - b[1], c[0] - b[0]) - math.atan2(a[1] - b[1], a[0] - b[0])
    angle_deg = abs(math.degrees(angle_rad))
    if angle_deg > 180.0:
        angle_deg = 360.0 - angle_deg
    return angle_deg


class PullUpBiomechanicsAnalyzer:
    """
    Deterministic real-time Pull-Up Biomechanics and Form Evaluator.
    Processes frame-by-frame BlazePose coordinates and updates kinematic,
    kinetic, fatigue, and form efficiency states.
    """

    def __init__(self,
                 height_m: float = 1.88,
                 mass_kg: float = 79.0,
                 fps: float = 30.0,
                 custom_bar_y: Optional[float] = None):
        self.height_m = height_m
        self.mass_kg = mass_kg
        self.fps = fps
        self.dt = 1.0 / fps
        self.lifted_mass_kg = mass_kg * LIFTED_FRAC

        # Calibration & Geometry
        self.custom_bar_y = custom_bar_y
        self.bar_y: Optional[float] = custom_bar_y
        self.px_per_m: float = 1000.0 # Default scale until calibrated
        self.calibrated = False

        # State Machine Tracking
        self.phase: str = "SETUP" # SETUP, HANDS_ON_BAR, LOADING, HANG, PULL, HOLD, LOWER, DONE
        self.frame_index: int = 0
        self.current_time_s: float = 0.0
        self.set_start_time_s: Optional[float] = None
        self.hands_on_bar_time_s: Optional[float] = None

        # Kinematic traces (rolling buffer)
        self.shoulder_y_history: List[float] = []
        self.velocity_y_history: List[float] = []
        self.timestamps: List[float] = []

        # Current Frame Kinematic Metrics
        self.current_shoulder_y: float = 0.0
        self.current_shoulder_height_pct: float = 0.0
        self.current_velocity_y: float = 0.0 # + = upward (m/s)
        self.current_acceleration_y: float = 0.0 # m/s^2
        self.current_elbow_angle: float = 180.0
        self.current_elbow_angle_left: float = 180.0
        self.current_elbow_angle_right: float = 180.0
        self.current_chin_clearance_cm: float = 0.0
        self.current_chin_verdict: str = "short"
        self.current_power_w: float = 0.0
        self.current_force_n: float = 0.0

        # Rep Detection Working State
        self.reps: List[RepRecord] = []
        self.rep_count: int = 0
        self.rep_in_progress: bool = False
        self.rep_start_frame: int = 0
        self.rep_start_time: float = 0.0
        self.rep_top_frame: int = 0
        self.rep_top_time: float = 0.0
        self.rep_conc_end_time: float = 0.0
        self.rep_ecc_start_time: float = 0.0
        self.rep_min_elbow_angle: float = 180.0
        self.rep_peak_conc_velocity: float = 0.0
        self.rep_peak_acceleration: float = 0.0
        self.rep_min_shoulder_y: float = 1.0 # Normalized Y (lower is higher physically)
        self.rep_base_shoulder_y: float = 0.0
        self.rep_max_sway_cm: float = 0.0
        self.rep_hip_x_history: List[float] = []

        # Reference Velocities for Velocity Loss
        self.v_ref_rep1: Optional[float] = None
        self.v_ref_fastest: Optional[float] = None
        self.peak_power_session_w: float = 0.0

        # Cumulative Energetics
        self.cumulative_work_j: float = 0.0
        self.cumulative_heat_j: float = 0.0
        self.cumulative_kcal: float = 0.0

        # Muscle Model States
        self.muscle_states: Dict[str, MuscleState] = {
            m: MuscleState() for m in MUSCLE_PROPERTIES.keys()
        }

    def process_frame(self,
                      landmarks: Union[List, Dict, 'np.ndarray'],
                      timestamp_s: Optional[float] = None) -> Dict:
        """
        Ingest a single frame of 33 BlazePose landmarks (x, y, z, [visibility]).
        Returns structured analysis for the current frame.
        """
        self.frame_index += 1
        if timestamp_s is not None:
            self.current_time_s = timestamp_s
        else:
            self.current_time_s += self.dt

        # Parse landmark tuples: (x, y, z, vis)
        lm = self._extract_landmarks(landmarks)
        if not lm:
            return self._empty_frame_result()

        # Step 1: Geometry & Scale Calibration
        self._calibrate_scale_and_bar(lm)

        # Step 2: Joint Angles
        self._calculate_joint_angles(lm)

        # Step 3: Kinematics (Trajectory, Velocity, Acceleration)
        self._update_kinematics(lm)

        # Step 4: Inverse Dynamics & Muscle Fatigue / Heat Integration
        self._integrate_muscle_model()

        # Step 5: Rep State Machine & Segmentation
        self._update_rep_state_machine(lm)

        # Build output structure
        return self._build_frame_result()

    def _extract_landmarks(self, landmarks) -> Optional[List[Tuple[float, float, float, float]]]:
        """Converts arbitrary landmark containers into indexed list of (x, y, z, vis)."""
        try:
            # Check if landmark list has len >= 33
            if hasattr(landmarks, "landmark"):
                # MediaPipe NormalizedLandmarkList
                raw = landmarks.landmark
                return [(p.x, p.y, p.z, getattr(p, "visibility", 1.0)) for p in raw]
            elif isinstance(landmarks, (list, tuple)):
                parsed = []
                for pt in landmarks:
                    if hasattr(pt, "x") and hasattr(pt, "y"):
                        z = getattr(pt, "z", 0.0)
                        vis = getattr(pt, "visibility", 1.0)
                        parsed.append((float(pt.x), float(pt.y), float(z), float(vis)))
                    elif isinstance(pt, (list, tuple)) and len(pt) >= 2:
                        z = float(pt[2]) if len(pt) > 2 else 0.0
                        vis = float(pt[3]) if len(pt) > 3 else 1.0
                        parsed.append((float(pt[0]), float(pt[1]), z, vis))
                    elif isinstance(pt, dict):
                        parsed.append((float(pt.get("x", 0)), float(pt.get("y", 0)),
                                       float(pt.get("z", 0)), float(pt.get("visibility", 1.0))))
                if len(parsed) >= 25:
                    return parsed
            elif hasattr(landmarks, "shape"):
                # NumPy array shape (33, 3) or (33, 4)
                parsed = []
                for i in range(landmarks.shape[0]):
                    row = landmarks[i]
                    x, y = float(row[0]), float(row[1])
                    z = float(row[2]) if row.shape[0] > 2 else 0.0
                    vis = float(row[3]) if row.shape[0] > 3 else 1.0
                    parsed.append((x, y, z, vis))
                if len(parsed) >= 25:
                    return parsed
        except Exception:
            pass
        return None

    def _calibrate_scale_and_bar(self, lm):
        """Calibrates scale (pixels/meters) and detects bar plane from overhead wrists."""
        sh_l, sh_r = lm[BlazePoseLandmarks.LEFT_SHOULDER], lm[BlazePoseLandmarks.RIGHT_SHOULDER]
        wr_l, wr_r = lm[BlazePoseLandmarks.LEFT_WRIST], lm[BlazePoseLandmarks.RIGHT_WRIST]

        sh_mid_y = (sh_l[1] + sh_r[1]) / 2.0
        wr_mid_y = (wr_l[1] + wr_r[1]) / 2.0

        # Bar detection: when wrists are overhead (wr_mid_y < sh_mid_y)
        if wr_mid_y < sh_mid_y and self.bar_y is None:
            self.bar_y = wr_mid_y
        elif self.custom_bar_y is not None:
            self.bar_y = self.custom_bar_y

        # Scale estimation from arm length when hanging
        if not self.calibrated and self.bar_y is not None:
            # Expected arm segment: shoulder to wrist ~ 0.332 * height
            expected_arm_m = self.height_m * ARM_FRAC
            measured_arm_y = abs(sh_mid_y - wr_mid_y)
            if measured_arm_y > 0.10: # Reasonable arm extension
                self.px_per_m = measured_arm_y / expected_arm_m
                self.calibrated = True

    def _calculate_joint_angles(self, lm):
        """Calculates 3D & 2D elbow, shoulder, and knee angles."""
        sh_l, sh_r = lm[BlazePoseLandmarks.LEFT_SHOULDER][:3], lm[BlazePoseLandmarks.RIGHT_SHOULDER][:3]
        el_l, el_r = lm[BlazePoseLandmarks.LEFT_ELBOW][:3], lm[BlazePoseLandmarks.RIGHT_ELBOW][:3]
        wr_l, wr_r = lm[BlazePoseLandmarks.LEFT_WRIST][:3], lm[BlazePoseLandmarks.RIGHT_WRIST][:3]

        self.current_elbow_angle_left = calculate_angle_3d(sh_l, el_l, wr_l)
        self.current_elbow_angle_right = calculate_angle_3d(sh_r, el_r, wr_r)
        self.current_elbow_angle = (self.current_elbow_angle_left + self.current_elbow_angle_right) / 2.0

        # Chin position estimation: mouth + 0.60 * (mouth - eye_mid)
        mouth_l, mouth_r = lm[BlazePoseLandmarks.MOUTH_LEFT][:2], lm[BlazePoseLandmarks.MOUTH_RIGHT][:2]
        eye_l, eye_r = lm[BlazePoseLandmarks.LEFT_EYE][:2], lm[BlazePoseLandmarks.RIGHT_EYE][:2]
        mouth_mid_y = (mouth_l[1] + mouth_r[1]) / 2.0
        eye_mid_y = (eye_l[1] + eye_r[1]) / 2.0
        chin_y = mouth_mid_y + 0.60 * (mouth_mid_y - eye_mid_y)

        # Chin clearance relative to bar (in centimeters)
        # Note: In normalized coordinates, smaller Y means higher on screen.
        if self.bar_y is not None:
            delta_y = self.bar_y - chin_y # Positive when chin is higher than bar
            self.current_chin_clearance_cm = (delta_y / self.px_per_m) * 100.0 + (CHIN_DEPTH_CM * 0.35)
            if self.current_chin_clearance_cm > 3.0:
                self.current_chin_verdict = "above"
            elif self.current_chin_clearance_cm >= -3.0:
                self.current_chin_verdict = "at"
            else:
                self.current_chin_verdict = "short"
        else:
            self.current_chin_clearance_cm = 0.0
            self.current_chin_verdict = "short"

    def _update_kinematics(self, lm):
        """Tracks vertical shoulder height, velocity (m/s), and acceleration (m/s^2)."""
        sh_l, sh_r = lm[BlazePoseLandmarks.LEFT_SHOULDER], lm[BlazePoseLandmarks.RIGHT_SHOULDER]
        self.current_shoulder_y = (sh_l[1] + sh_r[1]) / 2.0

        self.shoulder_y_history.append(self.current_shoulder_y)
        self.timestamps.append(self.current_time_s)

        # Rolling window numerical differentiation for velocity
        if len(self.shoulder_y_history) >= 3:
            # Moving average smoothing over last 3 frames
            y_curr = sum(self.shoulder_y_history[-3:]) / 3.0
            y_prev = sum(self.shoulder_y_history[-6:-3]) / 3.0 if len(self.shoulder_y_history) >= 6 else self.shoulder_y_history[0]
            dt_step = (self.timestamps[-1] - self.timestamps[-4]) if len(self.timestamps) >= 4 else (3.0 * self.dt)
            if dt_step < 1e-4:
                dt_step = self.dt

            # Velocity in m/s (upward is negative Y in coordinates, so negate)
            delta_m = (y_prev - y_curr) / self.px_per_m
            self.current_velocity_y = delta_m / dt_step

            # Acceleration in m/s^2
            if len(self.velocity_y_history) >= 2:
                v_prev = self.velocity_y_history[-1]
                self.current_acceleration_y = (self.current_velocity_y - v_prev) / self.dt
            else:
                self.current_acceleration_y = 0.0
        else:
            self.current_velocity_y = 0.0
            self.current_acceleration_y = 0.0

        self.velocity_y_history.append(self.current_velocity_y)

        # Keep rolling histories manageable (last 300 frames ~ 10 seconds)
        if len(self.shoulder_y_history) > 300:
            self.shoulder_y_history.pop(0)
            self.velocity_y_history.pop(0)
            self.timestamps.pop(0)

        # Mechanical Power & Force
        accel_term = max(0.0, self.current_acceleration_y)
        self.current_force_n = self.lifted_mass_kg * (G + accel_term)
        self.current_power_w = self.current_force_n * max(0.0, self.current_velocity_y)

        if self.current_power_w > self.peak_power_session_w:
            self.peak_power_session_w = self.current_power_w

    def _integrate_muscle_model(self):
        """
        Integrates the 3-state muscle motor unit fatigue equations (Xia & Frey-Law 2008)
        and thermal dissipation budget (Gonzalez-Alonso 2000).
        """
        # Forearm length in meters from arm fraction
        l_fore = (self.height_m * ARM_FRAC) * 0.48
        l_up = (self.height_m * ARM_FRAC) * 0.52

        # Elbow moment arm via inverse dynamics (half body weight per arm)
        # d_elbow = sqrt(max(L_fore^2 - delta_y^2, 0))
        elbow_angle_rad = math.radians(self.current_elbow_angle)
        d_elbow = max(0.04, l_fore * math.sin(max(0.1, elbow_angle_rad)))
        m_elbow = (self.current_force_n / 2.0) * d_elbow

        # Shoulder moment arm via law of cosines for shoulder depth behind the bar
        # L_hs^2 = L_fore^2 + L_up^2 - 2*L_fore*L_up*cos(180 - theta)
        l_hs_sq = l_fore**2 + l_up**2 - 2.0 * l_fore * l_up * math.cos(math.pi - elbow_angle_rad)
        d_shoulder = math.sqrt(max(0.04**2, l_hs_sq))
        m_shoulder = (self.current_force_n / 2.0) * d_shoulder

        # Crowninshield & Brand Static Optimization Force Sharing
        # a_i = M * sqrt(r_i * F_max,i) / sum((r_k * F_max,k)^1.5)
        denom_elbow = sum(
            (MUSCLE_PROPERTIES[m]["arm"] * MUSCLE_PROPERTIES[m]["f_max"])**1.5
            for m in ["biceps_brachii", "brachialis", "brachioradialis"]
        )
        denom_shoulder = sum(
            (MUSCLE_PROPERTIES[m]["arm"] * MUSCLE_PROPERTIES[m]["f_max"])**1.5
            for m in ["latissimus_dorsi", "trapezius", "pectoralis_major"]
        )

        on_bar = self.phase in ("HANG", "PULL", "HOLD", "LOWER")

        # Perfusion ramp time constant (90s)
        time_on = max(0.0, self.current_time_s - (self.set_start_time_s or 0.0))
        perf_factor = 1.0 - math.exp(-time_on / TAU_PERFUSION)

        for muscle_name, prop in MUSCLE_PROPERTIES.items():
            state = self.muscle_states[muscle_name]
            f_rate, r_rate, rest_multiplier = FATIGUE_RATES[muscle_name]

            # 1. Target Required Activation a(t)
            if not on_bar:
                u_target = 0.05
            elif muscle_name == "forearm_flexors":
                # Grip holds the body weight continuously on the bar
                u_target = 0.58 + 0.15 * min(1.0, max(0.0, self.current_velocity_y))
            elif muscle_name in ("biceps_brachii", "brachialis", "brachioradialis"):
                num = math.sqrt(prop["arm"] * prop["f_max"])
                u_target = min(1.2, (m_elbow * num) / denom_elbow)
                # Force-velocity scaling (Hill shortening factor)
                if self.phase == "PULL":
                    u_target *= 1.15
                elif self.phase == "LOWER":
                    u_target *= 0.70
            elif muscle_name in ("latissimus_dorsi", "trapezius", "pectoralis_major"):
                num = math.sqrt(prop["arm"] * prop["f_max"])
                u_target = min(1.4, (m_shoulder * num) / denom_shoulder)
                if self.phase == "PULL":
                    u_target *= 1.20
                elif self.phase == "HOLD":
                    u_target *= 1.10
                elif self.phase == "LOWER":
                    u_target *= 0.85
            else:
                # Postural legs
                u_target = 0.08

            # Thelen 2003 Activation Dynamics
            tau = 0.015 if u_target > state.activation else 0.050
            state.activation += (u_target - state.activation) * (self.dt / tau)
            state.activation = max(0.0, min(1.5, state.activation))

            # 2. Xia & Frey-Law 3-State Fatigue Compartment Differential Equations
            # dM_A/dt = C(t) - F * M_A
            # dM_F/dt = F * M_A - R * r(t) * M_F
            # dM_R/dt = -C(t) + R * r(t) * M_F
            r_effective = rest_multiplier if (not on_bar or state.activation < 0.05) else 1.0
            c_controller = 10.0 * (min(1.0, state.activation) - state.m_a)
            # Controller can only draw from available resting units M_R
            if c_controller > 0:
                c_controller = min(c_controller, state.m_r / self.dt)
            else:
                c_controller = max(c_controller, -state.m_a / self.dt)

            d_ma = (c_controller - f_rate * state.m_a) * self.dt
            d_mf = (f_rate * state.m_a - r_rate * r_effective * state.m_f) * self.dt

            state.m_a = max(0.0, min(1.0, state.m_a + d_ma))
            state.m_f = max(0.0, min(1.0, state.m_f + d_mf))
            state.m_r = max(0.0, min(1.0, 1.0 - state.m_a - state.m_f))

            # 3. Energetics & Temperature Rise (Gonzalez-Alonso 2000)
            # C_m * dT/dt = q_heat - k_blood * delta_T
            heat_gen_w = state.activation * prop["mass"] * 45.0 # baseline ~45 W/kg at 100%
            if self.phase == "PULL":
                heat_gen_w += self.current_power_w * ((1.0 / ETA_CONC) - 1.0) * (prop["mvic"] / 400.0)
            elif self.phase == "LOWER":
                heat_gen_w += abs(self.current_power_w) * (ECC_COST / ETA_CONC) * (prop["mvic"] / 400.0)

            state.heat_power_w = max(0.0, heat_gen_w)
            k_removal = K_BLOOD * prop["mass"] * perf_factor
            d_temp = (state.heat_power_w - k_removal * state.delta_t_c) / (C_MUSCLE * prop["mass"]) * self.dt
            state.delta_t_c = max(0.0, state.delta_t_c + d_temp)

            # 4. Composite Effort Index (v4.3 formulation)
            # E_i = min(1.0, 0.9 * (M_A + M_F) + 0.2 * delta_T / 1.5 C)
            non_resting = state.m_a + state.m_f
            state.effort_index = min(1.0, 0.90 * non_resting + 0.20 * (state.delta_t_c / 1.5))

            # Accumulate session metrics
            self.cumulative_heat_j += state.heat_power_w * self.dt
        
        self.cumulative_work_j += max(0.0, self.current_power_w) * self.dt
        self.cumulative_kcal = (self.cumulative_work_j + self.cumulative_heat_j) * KCAL_PER_J

    def _update_rep_state_machine(self, lm):
        """
        Discrete finite state machine for pull-up repetition detection:
        SETUP -> HANDS_ON_BAR -> HANG -> PULL (concentric) -> HOLD -> LOWER (eccentric) -> HANG.
        """
        wr_l, wr_r = lm[BlazePoseLandmarks.LEFT_WRIST], lm[BlazePoseLandmarks.RIGHT_WRIST]
        sh_l, sh_r = lm[BlazePoseLandmarks.LEFT_SHOULDER], lm[BlazePoseLandmarks.RIGHT_SHOULDER]
        hip_l, hip_r = lm[BlazePoseLandmarks.LEFT_HIP], lm[BlazePoseLandmarks.RIGHT_HIP]

        hands_overhead = (wr_l[1] < sh_l[1]) and (wr_r[1] < sh_r[1])
        hip_mid_x = (hip_l[0] + hip_r[0]) / 2.0

        if not hands_overhead:
            if self.phase != "SETUP" and self.phase != "DONE":
                self.phase = "DONE"
            return

        if self.phase == "SETUP" and hands_overhead:
            self.phase = "HANG"
            if self.set_start_time_s is None:
                self.set_start_time_s = self.current_time_s

        # Track horizontal hip position for kipping / sway
        if self.rep_in_progress:
            self.rep_hip_x_history.append(hip_mid_x)

        # STATE TRANSITIONS
        if self.phase == "HANG":
            # Initiate pull when velocity is upward (> 0.08 m/s) and elbows begin flexing (< 145 deg)
            if self.current_velocity_y > 0.08 and self.current_elbow_angle < 145.0:
                self.phase = "PULL"
                self.rep_in_progress = True
                self.rep_start_frame = self.frame_index
                self.rep_start_time = self.current_time_s
                self.rep_min_elbow_angle = self.current_elbow_angle
                self.rep_peak_conc_velocity = self.current_velocity_y
                self.rep_base_shoulder_y = self.current_shoulder_y
                self.rep_min_shoulder_y = self.current_shoulder_y
                self.rep_hip_x_history = [hip_mid_x]

        elif self.phase == "PULL":
            # Track peak concentric velocity and minimum elbow angle
            if self.current_velocity_y > self.rep_peak_conc_velocity:
                self.rep_peak_conc_velocity = self.current_velocity_y
            if self.current_elbow_angle < self.rep_min_elbow_angle:
                self.rep_min_elbow_angle = self.current_elbow_angle
            if self.current_shoulder_y < self.rep_min_shoulder_y:
                self.rep_min_shoulder_y = self.current_shoulder_y

            # Transition to HOLD or LOWER when upward velocity diminishes (< 0.05 m/s) and elbow < 90 deg
            if self.current_velocity_y <= 0.05 and self.current_elbow_angle < 95.0:
                self.phase = "HOLD"
                self.rep_top_frame = self.frame_index
                self.rep_top_time = self.current_time_s
                self.rep_conc_end_time = self.current_time_s
            elif self.current_velocity_y < -0.10: # Immediately began descending
                self.phase = "LOWER"
                self.rep_top_frame = self.frame_index
                self.rep_top_time = self.current_time_s
                self.rep_conc_end_time = self.current_time_s
                self.rep_ecc_start_time = self.current_time_s

        elif self.phase == "HOLD":
            if self.current_elbow_angle < self.rep_min_elbow_angle:
                self.rep_min_elbow_angle = self.current_elbow_angle
            if self.current_shoulder_y < self.rep_min_shoulder_y:
                self.rep_min_shoulder_y = self.current_shoulder_y

            # Descent starts (velocity negative < -0.06 m/s or elbows opening > 100 deg)
            if self.current_velocity_y < -0.06 or self.current_elbow_angle > 100.0:
                self.phase = "LOWER"
                self.rep_ecc_start_time = self.current_time_s

        elif self.phase == "LOWER":
            # Rep finishes when arms return to full extension (elbow >= 150 deg or velocity settles near 0)
            if self.current_elbow_angle >= 148.0 or (self.current_velocity_y >= -0.04 and self.current_elbow_angle >= 140.0):
                self._record_completed_rep()
                self.phase = "HANG"
                self.rep_in_progress = False

    def _record_completed_rep(self):
        """Finalizes metrics, efficiency scoring, and records for the completed rep."""
        self.rep_count += 1
        t_end = self.current_time_s

        duration_conc = max(0.2, self.rep_conc_end_time - self.rep_start_time)
        duration_hold = max(0.0, (self.rep_ecc_start_time or self.rep_conc_end_time) - self.rep_conc_end_time)
        duration_ecc = max(0.2, t_end - (self.rep_ecc_start_time or self.rep_conc_end_time))
        duration_total = t_end - self.rep_start_time

        # Range of Motion (ROM in cm)
        delta_shoulder_px = abs(self.rep_base_shoulder_y - self.rep_min_shoulder_y)
        rom_cm = (delta_shoulder_px / self.px_per_m) * 100.0

        # Full Lockout Check (>= 150 degrees at bottom)
        lockout = self.current_elbow_angle >= 148.0

        # Velocity references (Rep 1 Beckham 2018 anchoring)
        if self.v_ref_rep1 is None:
            self.v_ref_rep1 = self.rep_peak_conc_velocity
        if self.v_ref_fastest is None or self.rep_peak_conc_velocity > self.v_ref_fastest:
            self.v_ref_fastest = self.rep_peak_conc_velocity

        vl_pct = max(0.0, (1.0 - (self.rep_peak_conc_velocity / self.v_ref_rep1)) * 100.0)
        mean_conc_v = (rom_cm / 100.0) / duration_conc

        # Horizontal hip sway
        if self.rep_hip_x_history:
            sway_px = max(self.rep_hip_x_history) - min(self.rep_hip_x_history)
            sway_cm = (sway_px / self.px_per_m) * 100.0
        else:
            sway_cm = 4.0

        # Symmetry
        asymmetry_deg = abs(self.current_elbow_angle_left - self.current_elbow_angle_right)

        # Work and Power
        work_j = self.lifted_mass_kg * G * (rom_cm / 100.0)
        peak_pow = self.lifted_mass_kg * (G + max(0.0, self.rep_peak_acceleration)) * self.rep_peak_conc_velocity
        rep_kcal = (work_j / ETA_CONC + work_j * ECC_COST / ETA_CONC) * KCAL_PER_J

        # Form Efficiency Scoring (/100)
        # ROM (25 pts) + Lockout (15 pts) + Control (20 pts) + Sway (15 pts) + Symmetry (10 pts) + Power (15 pts)
        score_rom = 25.0 * max(0.0, min(1.0, 1.0 + min(self.current_chin_clearance_cm, 0.0) / 10.0))
        score_lock = 15.0 * max(0.0, min(1.0, (self.current_elbow_angle - 120.0) / 35.0))
        score_ctrl = 20.0 * max(0.0, min(1.0, duration_ecc / 1.5))
        score_sway = 15.0 * max(0.0, min(1.0, 1.0 - (sway_cm - 4.0) / 16.0))
        score_symm = 10.0 * max(0.0, min(1.0, 1.0 - (asymmetry_deg - 2.0) / 15.0))
        score_power = 15.0 * max(0.0, min(1.0, self.rep_peak_conc_velocity / (self.v_ref_fastest or 0.8)))

        total_score = round(score_rom + score_lock + score_ctrl + score_sway + score_symm + score_power, 1)

        grade = "A" if total_score >= 85 else "B" if total_score >= 70 else "C" if total_score >= 55 else "D"

        rep_obj = RepRecord(
            rep_num=self.rep_count,
            t_start=round(self.rep_start_time, 2),
            t_top=round(self.rep_top_time, 2),
            t_end=round(t_end, 2),
            duration_concentric=round(duration_conc, 2),
            duration_top_hold=round(duration_hold, 2),
            duration_eccentric=round(duration_ecc, 2),
            duration_total=round(duration_total, 2),
            rom_cm=round(rom_cm, 1),
            chin_clearance_cm=round(self.current_chin_clearance_cm, 1),
            chin_verdict=self.current_chin_verdict,
            elbow_angle_top=round(self.rep_min_elbow_angle, 1),
            elbow_angle_lockout=round(self.current_elbow_angle, 1),
            full_lockout=lockout,
            peak_concentric_velocity=round(self.rep_peak_conc_velocity, 2),
            mean_concentric_velocity=round(mean_conc_v, 2),
            velocity_loss_pct=round(vl_pct, 1),
            peak_power_w=round(peak_pow, 1),
            work_j=round(work_j, 1),
            energy_kcal=round(rep_kcal, 2),
            hip_sway_cm=round(sway_cm, 1),
            arm_asymmetry_deg=round(asymmetry_deg, 1),
            form_score=total_score,
            score_breakdown={
                "rom": round(score_rom, 1),
                "lockout": round(score_lock, 1),
                "control": round(score_ctrl, 1),
                "sway": round(score_sway, 1),
                "symmetry": round(score_symm, 1),
                "power": round(score_power, 1),
            },
            letter_grade=grade,
        )
        self.reps.append(rep_obj)

    def _build_frame_result(self) -> Dict:
        """Constructs rich output metrics dictionary for real-time visualization."""
        last_rep = self.reps[-1] if self.reps else None
        current_vl = 0.0
        if self.v_ref_rep1 and self.v_ref_rep1 > 0:
            current_vl = max(0.0, (1.0 - (self.current_velocity_y / self.v_ref_rep1)) * 100.0)

        # Chin over bar tally
        chin_at_bar_count = sum(1 for r in self.reps if r.chin_verdict in ("above", "at"))

        lat_state = self.muscle_states["latissimus_dorsi"]
        bicep_state = self.muscle_states["biceps_brachii"]
        grip_state = self.muscle_states["forearm_flexors"]

        return {
            "frame_index": self.frame_index,
            "timestamp_s": round(self.current_time_s, 2),
            "phase": self.phase,
            "rep_count": self.rep_count,
            "chin_at_bar_count": chin_at_bar_count,
            "elbow_angle": round(self.current_elbow_angle, 1),
            "elbow_angle_left": round(self.current_elbow_angle_left, 1),
            "elbow_angle_right": round(self.current_elbow_angle_right, 1),
            "vertical_speed_mps": round(self.current_velocity_y, 2),
            "peak_power_w": round(self.peak_power_session_w, 0),
            "current_power_w": round(self.current_power_w, 0),
            "speed_loss_pct": round(last_rep.velocity_loss_pct if last_rep else 0.0, 0),
            "chin_clearance_cm": round(self.current_chin_clearance_cm, 1),
            "chin_verdict": self.current_chin_verdict,
            "energetics": {
                "cumulative_kcal": round(self.cumulative_kcal, 1),
                "cumulative_heat_kj": round(self.cumulative_heat_j / 1000.0, 1),
                "cumulative_work_kj": round(self.cumulative_work_j / 1000.0, 1),
            },
            "muscles": {
                "latissimus_dorsi": {
                    "delta_t_c": round(lat_state.delta_t_c, 2),
                    "fatigued_pct": round(lat_state.m_f * 100.0, 1),
                    "active_pct": round(lat_state.m_a * 100.0, 1),
                    "effort_index": round(lat_state.effort_index, 2),
                },
                "biceps_brachii": {
                    "delta_t_c": round(bicep_state.delta_t_c, 2),
                    "fatigued_pct": round(bicep_state.m_f * 100.0, 1),
                    "active_pct": round(bicep_state.m_a * 100.0, 1),
                    "effort_index": round(bicep_state.effort_index, 2),
                },
                "forearm_flexors": {
                    "delta_t_c": round(grip_state.delta_t_c, 2),
                    "fatigued_pct": round(grip_state.m_f * 100.0, 1),
                    "active_pct": round(grip_state.m_a * 100.0, 1),
                    "effort_index": round(grip_state.effort_index, 2),
                },
            },
            "last_rep": last_rep.__dict__ if last_rep else None,
            "bar_y": self.bar_y,
            "px_per_m": self.px_per_m,
        }

    def _empty_frame_result(self) -> Dict:
        return {
            "frame_index": self.frame_index,
            "timestamp_s": round(self.current_time_s, 2),
            "phase": self.phase,
            "rep_count": self.rep_count,
            "chin_at_bar_count": 0,
            "elbow_angle": 180.0,
            "vertical_speed_mps": 0.0,
            "peak_power_w": 0.0,
            "speed_loss_pct": 0.0,
            "chin_clearance_cm": 0.0,
            "chin_verdict": "short",
            "energetics": {"cumulative_kcal": 0.0, "cumulative_heat_kj": 0.0, "cumulative_work_kj": 0.0},
            "muscles": {},
            "last_rep": None,
            "bar_y": None,
            "px_per_m": self.px_per_m,
        }

    def get_session_summary(self) -> Dict:
        """Returns comprehensive final set summary statistics."""
        avg_score = (sum(r.form_score for r in self.reps) / len(self.reps)) if self.reps else 0.0
        avg_rom = (sum(r.rom_cm for r in self.reps) / len(self.reps)) if self.reps else 0.0
        lockouts_count = sum(1 for r in self.reps if r.full_lockout)
        chin_above_count = sum(1 for r in self.reps if r.chin_verdict in ("above", "at"))

        return {
            "total_reps": self.rep_count,
            "chin_above_bar_reps": chin_above_count,
            "full_lockouts": lockouts_count,
            "average_form_score": round(avg_score, 1),
            "average_rom_cm": round(avg_rom, 1),
            "peak_concentric_velocity_first_rep": round(self.v_ref_rep1 or 0.0, 2),
            "peak_concentric_velocity_last_rep": round(self.reps[-1].peak_concentric_velocity if self.reps else 0.0, 2),
            "final_velocity_loss_pct": round(self.reps[-1].velocity_loss_pct if self.reps else 0.0, 1),
            "peak_power_session_w": round(self.peak_power_session_w, 0),
            "total_work_kj": round(self.cumulative_work_j / 1000.0, 2),
            "total_heat_kj": round(self.cumulative_heat_j / 1000.0, 2),
            "total_kcal": round(self.cumulative_kcal, 2),
            "lat_final_temp_rise_c": round(self.muscle_states["latissimus_dorsi"].delta_t_c, 2),
            "lat_final_fatigue_pct": round(self.muscle_states["latissimus_dorsi"].m_f * 100.0, 1),
            "bicep_final_fatigue_pct": round(self.muscle_states["biceps_brachii"].m_f * 100.0, 1),
            "grip_final_fatigue_pct": round(self.muscle_states["forearm_flexors"].m_f * 100.0, 1),
            "reps_detail": [r.__dict__ for r in self.reps],
        }
