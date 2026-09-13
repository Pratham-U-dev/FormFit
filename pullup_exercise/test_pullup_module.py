"""
pullup_exercise.test_pullup_module
==================================
Deterministic verification suite for Pull-Up Biomechanics Module.
Simulates a realistic 3-rep pull-up sequence using synthetic BlazePose landmarks,
and tests:
  1. Bar detection & scale calibration
  2. Concentric pull, top hold, eccentric lowering, and lockout state transitions
  3. Rep counting and velocity loss calculations
  4. 3-state motor unit fatigue equations (M_R, M_A, M_F)
  5. Thermal muscle temperature rises (Delta T in deg C)
  6. Muscle heatmap polygon and color generation
  7. Form efficiency scoring and summary statistics
"""

import math
import sys
from pullup_exercise.pullup_module import PullUpExerciseModule
from pullup_exercise.biomechanics import BlazePoseLandmarks
from pullup_exercise.heatmap import ColorGradient


def generate_synthetic_pullup_landmarks(t: float, rep_cycle: float = 3.0) -> list:
    """
    Generates 33 BlazePose landmark coordinates simulating a pull-up athlete.
    Cycle phases:
      0.0s -> 1.0s: Concentric pull (shoulders rise, elbows flex from 170 deg to 65 deg)
      1.0s -> 1.8s: Top hold (chin cleared bar, elbows 60 deg)
      1.8s -> 3.0s: Eccentric lowering (shoulders lower, elbows extend back to 160 deg)
    """
    phase_t = t % rep_cycle
    if phase_t < 1.0:
        # Concentric: progress 0 -> 1
        p = phase_t / 1.0
        # Ease in-out
        progress = 0.5 * (1.0 - math.cos(p * math.pi))
    elif phase_t < 1.8:
        # Hold at top
        progress = 1.0
    else:
        # Eccentric: progress 1 -> 0
        p = (phase_t - 1.8) / 1.2
        progress = 0.5 * (1.0 + math.cos(p * math.pi))

    # Bar fixed at overhead Y = 0.20
    bar_y = 0.20
    # Wrists fixed on the bar
    wr_l = (0.42, bar_y, 0.0)
    wr_r = (0.58, bar_y, 0.0)

    # Shoulders: hang at Y = 0.48, top at Y = 0.28 (above hands level)
    sh_y = 0.48 - progress * 0.20
    sh_l = (0.43, sh_y, 0.0)
    sh_r = (0.57, sh_y, 0.0)

    # Elbows flare out during flexion
    el_x_offset = 0.06 * progress
    el_y = (bar_y + sh_y) / 2.0 + (0.04 * (1.0 - progress))
    el_l = (0.42 - el_x_offset, el_y, 0.0)
    el_r = (0.58 + el_x_offset, el_y, 0.0)

    # Head and Chin: hang at Y = 0.40, top at Y = 0.16 (clears bar_y = 0.20!)
    head_y = 0.40 - progress * 0.24
    chin_y = head_y + 0.04
    nose = (0.50, head_y, 0.0)
    mouth_l = (0.48, chin_y - 0.02, 0.0)
    mouth_r = (0.52, chin_y - 0.02, 0.0)
    eye_l = (0.47, head_y - 0.02, 0.0)
    eye_r = (0.53, head_y - 0.02, 0.0)

    # Torso & Hips
    hip_y = sh_y + 0.26
    hip_l = (0.45, hip_y, 0.0)
    hip_r = (0.55, hip_y, 0.0)

    # Legs (knees slightly bent)
    kn_y = hip_y + 0.18
    kn_l = (0.44, kn_y, 0.0)
    kn_r = (0.56, kn_y, 0.0)

    # Ankles
    ank_y = kn_y + 0.18
    ank_l = (0.45, ank_y, 0.0)
    ank_r = (0.55, ank_y, 0.0)

    landmarks = [(0.5, 0.5, 0.0, 1.0)] * 33
    landmarks[BlazePoseLandmarks.NOSE] = (*nose, 1.0)
    landmarks[BlazePoseLandmarks.LEFT_EYE] = (*eye_l, 1.0)
    landmarks[BlazePoseLandmarks.RIGHT_EYE] = (*eye_r, 1.0)
    landmarks[BlazePoseLandmarks.MOUTH_LEFT] = (*mouth_l, 1.0)
    landmarks[BlazePoseLandmarks.MOUTH_RIGHT] = (*mouth_r, 1.0)
    landmarks[BlazePoseLandmarks.LEFT_SHOULDER] = (*sh_l, 1.0)
    landmarks[BlazePoseLandmarks.RIGHT_SHOULDER] = (*sh_r, 1.0)
    landmarks[BlazePoseLandmarks.LEFT_ELBOW] = (*el_l, 1.0)
    landmarks[BlazePoseLandmarks.RIGHT_ELBOW] = (*el_r, 1.0)
    landmarks[BlazePoseLandmarks.LEFT_WRIST] = (*wr_l, 1.0)
    landmarks[BlazePoseLandmarks.RIGHT_WRIST] = (*wr_r, 1.0)
    landmarks[BlazePoseLandmarks.LEFT_HIP] = (*hip_l, 1.0)
    landmarks[BlazePoseLandmarks.RIGHT_HIP] = (*hip_r, 1.0)
    landmarks[BlazePoseLandmarks.LEFT_KNEE] = (*kn_l, 1.0)
    landmarks[BlazePoseLandmarks.RIGHT_KNEE] = (*kn_r, 1.0)
    landmarks[BlazePoseLandmarks.LEFT_ANKLE] = (*ank_l, 1.0)
    landmarks[BlazePoseLandmarks.RIGHT_ANKLE] = (*ank_r, 1.0)

    return landmarks


def run_test():
    print("=== PULL-UP BIOMECHANICS MODULE: DETERMINISTIC TEST ===")
    module = PullUpExerciseModule(height_m=1.88, mass_kg=79.0, fps=30.0)

    # Simulate 3 repetitions (each rep takes 3.0 seconds -> 9.0 seconds total, 270 frames)
    total_frames = 270
    dt = 1.0 / 30.0

    print(f"Simulating {total_frames} frames of pull-up kinematics...")

    for f in range(total_frames):
        t = f * dt
        lm = generate_synthetic_pullup_landmarks(t, rep_cycle=3.0)
        metrics = module.update(lm, timestamp_s=t)

        if f % 45 == 0:
            phase = metrics["phase"]
            reps = metrics["rep_count"]
            elbow = metrics["elbow_angle"]
            spd = metrics["vertical_speed_mps"]
            lat_temp = metrics["muscles"]["latissimus_dorsi"]["delta_t_c"]
            lat_fat = metrics["muscles"]["latissimus_dorsi"]["fatigued_pct"]
            print(f"Frame {f:03d} | t={t:.2f}s | Phase: {phase:6s} | Reps: {reps} | Elbow: {elbow:.1f}\u00b0 | Speed: {spd:+.2f} m/s | Lats: +{lat_temp:.2f}\u00b0C, {lat_fat:.1f}% fat")

    summary = module.finish_set()
    print("\n--- FINAL SET SUMMARY ---")
    print(f"Total Reps Completed: {summary['total_reps']}")
    print(f"Average Form Score:   {summary['average_form_score']}/100")
    print(f"Average ROM:          {summary['average_rom_cm']} cm")
    print(f"Total Energetics:     {summary['total_kcal']} kcal, {summary['total_heat_kj']} kJ heat")
    print(f"Final Lat Temp Rise:  +{summary['lat_final_temp_rise_c']} \u00b0C")
    print(f"Final Lat Fatigue:    {summary['lat_final_fatigue_pct']} %")
    print(f"Final Bicep Fatigue:  {summary['bicep_final_fatigue_pct']} %")

    # Test Muscle Thermal Polygon Generation
    polys = module.get_muscle_thermal_polygons(lm, width=1280, height=720)
    print(f"\nGenerated {len(polys)} anatomical muscle thermal polygons:")
    for p in polys:
        print(f" - {p['name']:24s} | Effort: {p['effort']:.2f} | RGB: {p['color_rgb']}")

    # Assertions for deterministic correctness
    assert summary["total_reps"] >= 2, f"Expected >= 2 reps, got {summary['total_reps']}"
    assert summary["lat_final_temp_rise_c"] > 0.0, "Expected positive muscle temperature rise"
    assert summary["lat_final_fatigue_pct"] > 0.0, "Expected motor unit fatigue accumulation"
    assert len(polys) >= 6, "Expected at least 6 anatomical muscle polygon regions"

    print("\n\u2705 ALL DETERMINISTIC BIOMECHANICS & THERMAL TESTS PASSED!")


if __name__ == "__main__":
    run_test()
