# Pull-Up Biomechanics & Kinematics Engine
## Research & Technical Documentation

**Date:** September 2026
**Subject:** Mathematical and Algorithmic Foundations for Pull-Up Pose Analysis

---

## 1. Introduction
This document outlines the rigorous biomechanical models, kinematic calculations, and logical state machines powering the pull-up analysis engine. The system leverages real-time pose estimation (via coordinates mapping the wrists, shoulders, elbows, and hips) to deduce highly precise physical metrics, including mechanical power, metabolic energy expenditure, and localized muscle fatigue.

---

## 2. Anthropometric Setup & Calibration

### 2.1 Mass Distribution
The system operates on a predefined baseline user mass of $79.0 \text{ kg}$. To accurately estimate the mass being displaced during a pull-up, the weight of the forearms and hands (which remain static relative to the bar) is subtracted using standard anthropometric ratios:
$$ M_{\text{lifted}} = M_{\text{total}} \times 0.956 $$
Where standard gravitational acceleration $G = 9.81 \text{ m/s}^2$.

### 2.2 Spatial Calibration (Pixel-to-Meter Mapping)
To translate dimensionless pixel coordinates into real-world physical metrics (meters), a dynamic calibration sequence executes once the user achieves a hanging position:
1. The **Bar Height (Y)** is anchored at the midpoint of the left and right wrists.
2. The user's expected arm length is modeled using human stature proportions: $Arm_{\text{expected}} = Height (1.88\text{m}) \times 0.332$.
3. The spatial scalar `pxPerM` (pixels per meter) is derived by comparing the vertical pixel distance between the shoulders and wrists against the expected physiological arm length:
   $$ \text{pxPerM} = \frac{| Y_{\text{shoulder\_mid}} - Y_{\text{wrist\_mid}} |}{Arm_{\text{expected}}} $$

---

## 3. Kinematic Engine (Velocity, Acceleration, and Power)

### 3.1 Angle Computation
Joint angles (e.g., elbows) are calculated using the dot product equivalent via 2D Cartesian inverse tangents (`atan2`). For a joint sequence $\text{Shoulder} (A)$, $\text{Elbow} (B)$, and $\text{Wrist} (C)$:
$$ \theta_{\text{rad}} = \arctan2(C_y - B_y, C_x - B_x) - \arctan2(A_y - B_y, A_x - B_x) $$
The current elbow angle is strictly tracked as the average of the left and right arms.

### 3.2 Velocity & Acceleration Estimation
To mitigate pose jitter, the vertical shoulder trajectory ($Y_{\text{sh}}$) is buffered using a rolling window of the last 30 frames.
1. Instantaneous positional change ($Y_{\text{curr}}$ and $Y_{\text{prev}}$) is computed using 3-frame rolling averages.
2. Vertical Velocity ($v$) in m/s:
   $$ v = \frac{Y_{\text{prev}} - Y_{\text{curr}}}{\text{pxPerM} \times \Delta t} $$
3. Vertical Acceleration ($a$) in m/s²:
   $$ a = \frac{v_{\text{new}} - v_{\text{old}}}{\Delta t} $$

### 3.3 Mechanical Power output
Using Newtonian mechanics, the instantaneous force applied by the user accounts for both static body weight and dynamic acceleration.
$$ F_{\text{applied}} = M_{\text{lifted}} \times (G + \max(0, a)) $$
$$ P_{\text{instantaneous}} (W) = F_{\text{applied}} \times \max(0, v) $$

---

## 4. Logical State Machine (Phase Detection)

A robust finite state machine tracks the pull-up lifecycle, enforcing strict threshold rules:

*   **HANG Phase:**
    *   *Condition:* Arms are extended ($<165^\circ$) and the user initiates a strong upward velocity ($v > 0.08 \text{ m/s}$).
    *   *Action:* Transitions to **PULL**.
*   **PULL Phase (Concentric):**
    *   *Condition:* Velocity drops ($v \le 0.05 \text{ m/s}$) and elbows are deeply flexed ($< 100^\circ$).
    *   *Action:* Transitions to **HOLD**.
*   **HOLD Phase (Isometric):**
    *   *Condition:* Downward velocity exceeds bounds ($v < -0.06 \text{ m/s}$) or elbow extends ($> 110^\circ$).
    *   *Action:* Transitions to **LOWER**.
*   **LOWER Phase (Eccentric):**
    *   *Condition:* Full lockout achieved ($Elbow \ge 150^\circ$) OR controlled bottom out ($v \ge -0.04 \text{ m/s}$ & $Elbow \ge 140^\circ$).
    *   *Action:* Repetition complete. Data is flushed to the rep matrix, and the state resets to **HANG**.

---

## 5. Form Evaluation & Penalty Heuristics

The `ExerciseFormEvaluator` runs parallel to the kinematics engine, analyzing the skeletal geometry for biomechanical inefficiencies:

1.  **Chin Clearance:**
    *   *Logic:* A predicted chin coordinate is established at $Y_{\text{sh}} - 0.15\text{m}$.
    *   *Verdict:* Computes the delta against the Bar Y. $>3\text{cm}$ yields "Above Bar", $\pm 3\text{cm}$ yields "At Bar", and $<-3\text{cm}$ triggers a "Short Rep" warning.
2.  **Arm Asymmetry:**
    *   *Logic:* Evaluates $| \theta_{\text{ElbowLeft}} - \theta_{\text{ElbowRight}} |$.
    *   *Verdict:* Deltas exceeding $25^\circ$ trigger an "Uneven Pull" penalty.
3.  **Kipping / Sway Detection:**
    *   *Logic:* Measures the horizontal planar displacement between the hip midpoint and shoulder midpoint.
    *   *Verdict:* Sway exceeding $14\text{cm}$ (0.14 normalized ratio) triggers a "Strict Form / Kipping" penalty.

---

## 6. Metabolic & Fatigue Modeling

### 6.1 Neuromuscular State Model
A 3-state differential compartment model simulates motor unit fatigue across four muscle groups (Lats, Biceps, Forearms, Legs):
*   $m_R$: Resting (Available) motor units.
*   $m_A$: Active motor units.
*   $m_F$: Fatigued motor units.

Each muscle group targets a specific activation ratio based on the phase (e.g., Lats target 1.2 during `PULL`, 1.0 during `HOLD`, and 0.6 during `LOWER`).
State transitions ($\Delta m_A$ and $\Delta m_F$) are solved using first-order ordinary differential equations (ODEs), mapping the transition from Rest $\rightarrow$ Active $\rightarrow$ Fatigued based on time under tension.

### 6.2 Thermal Energy & Kcal Expenditure
Heat dissipation is calculated dynamically, correlating with localized muscle mass and instantaneous power:
$$ Heat_{\text{Power}} = (\text{Activation} \times \text{Mass} \times 45.0) + \left( P_{\text{inst}} \times \left(\frac{1}{0.22} - 1\right) \times \frac{\text{MVIC}}{400} \right) $$
*Note: A biological mechanical efficiency quotient of 22% ($0.22$) is assumed.*

The total metabolic cost (Kcal) aggregates the mechanical work accomplished (Joules) and the thermal heat generated:
$$ \text{Total Kcal} = \frac{\sum (W_{\text{mech}}) + \sum (Heat_{\text{Joules}})}{4184} $$

### 6.3 Speed Loss (Velocity-Based Training)
The system employs Velocity-Based Training (VBT) metrics. The peak concentric velocity ($v_{\text{peak}}$) of the first successful repetition ($v_{\text{ref1}}$) is cached. Subsequent repetitions calculate the central nervous system (CNS) fatigue index via:
$$ \text{Speed Loss \%} = \max\left(0, \left(1 - \frac{v_{\text{curr\_peak}}}{v_{\text{ref1}}}\right) \times 100\right) $$
