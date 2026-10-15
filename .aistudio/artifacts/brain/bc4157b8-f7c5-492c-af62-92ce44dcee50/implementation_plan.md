# Stamina Heart Redesign, Camera HUD Decluttering & Voice Throttle Plan

Comprehensive architectural specification for redesigning the heart mechanic into a positive, anti-burnout energy system (25 hearts max with passive 5-minute regeneration per heart), resolving HUD visual clutter and bottom-card clipping, introducing collapsible biomechanics telemetry, transparent alert styling, and an 8–10 second voice coach cooldown.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Choices from Clarification Phase**:
> - **Heart Economy**: Initial maximum capacity of **25 hearts** (replacing the old 5-heart punishment model). Hearts are no longer lost on form mistakes or false detections.
> - **Heart Consumption**: **1 heart deducted per completed exercise set / target goal**, functioning as an energy meter to prevent physical overtraining.
> - **Passive Regeneration**: **1 heart automatically regenerated every 5 minutes** (timestamp-anchored so it regenerates even while the app is closed or in background).
> - **HUD Visual Fixes**:
>   - Advanced biomechanics telemetry (peak power, velocity loss, muscle temperature rise, and fatigue) is **collapsible/minimized by default** with a toggle to expand.
>   - Warning banners are styled with **semi-transparent glassmorphism** (`Color(0xBBCC3333)`) so the user's camera feed and form remain visible.
>   - HUD layout changed to a container-based `Box` layout ensuring the bottom **"REP 5" report card is permanently pinned** and never pushed out of frame.
> - **Voice Coach Cooldown**: Non-urgent voice cues are throttled with an **8–10 second cooldown timer**, while urgent posture/rejection alerts can override immediately.

---

## 1. Overview & Core Concept

### 1. Duolingo-Style Anti-Burnout Energy System
Currently, the heart system penalizes mistakes, which frustrates users when computer vision has detection noise. In this new design:
- Hearts represent **Energy / Stamina** with a maximum cap of **25**.
- Completing a set of target repetitions consumes 1 heart.
- A background ticker and timestamp-delta engine automatically refills 1 heart every 5 minutes until reaching 25.
- Zero hearts lost on posture mistakes or rep rejections.

### 2. Camera View Decluttering & Layout Resilience
Currently, the HUD stacks 12+ vertical text and badge elements in a single `Arrangement.SpaceBetween` column:
- When an alert pops up (e.g. `STATE: AWAITING_OVERHEAD` + `⚠️ Hands must be overhead`), the column overflows and pushes the bottom `REP 5` summary card off-screen.
- The solid opaque red banners block the camera view.
- By converting the telemetry lines into a compact, expandable drawer and anchoring the bottom report card to `Alignment.BottomCenter`, the camera screen remains clean and readable.

---

## 2. User Experience & Visual Design

### HUD Layout Architecture

```
┌──────────────────────────────────────────────────────────┐
│ [🔥 1]           [💎 450]                       [❤️ 25] │ <── 25 Hearts Capacity
│ (Next heart in 3:42)                                     │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  [FRONT CAM]                                      [🔄]   │
│                                                          │
│  5 / 10 REPS                                            │
│  ═══════════──────────── (Progress Bar)                  │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │ ⚠️ [Translucent Banner: Hands overhead on bar]     │  │ <── 75% Translucent
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  [📊 Telemetry (123W, -0% Spd) ▾] (Tap to Expand)        │ <── Collapsed by default
│                                                          │
│                                                          │
│                  (Camera Feed Area)                      │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │ REP 5                    [PERFECT FORM]            │  │ <── Pinned to bottom
│  │ peak 0.35 m/s · 145 W    Full Lock-out · No swing  │  │     (Never pushed off)
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

1. **Top Heart Pill**:
   - Displays current hearts out of 25 (e.g., `❤️ 24/25`).
   - Tapping or viewing the streak dialog shows a subtle countdown timer: *"Next heart in 3:20"*.

2. **Semi-Transparent Warning Overlays**:
   - Translucent rounded chip with subtle glass border (`Color.Black.copy(alpha = 0.55f)` with `Color(0xFFFF5252)` accent text).
   - Eliminates the heavy solid red rectangle that was blinding the camera view.

3. **Collapsible Telemetry Drawer**:
   - Single clean pill: `📊 123W · 0% loss ▾`.
   - Tapping it expands the full biomechanics breakdown (muscle fatigue, temperature rise, mechanical work) in a lightweight floating glass overlay.

---

## 3. Key Technical Decisions & Trade-Offs

### Decision 1: Timestamp-Delta Regeneration vs. In-Memory Timer
- **Chosen Approach**: Store `lastHeartRegenTimestamp` in Room DB / SharedPreferences and calculate `(currentTime - lastHeartRegenTimestamp) / 300_000L` on every query, paired with an active in-app 1-second countdown ticker.
- **Why**: Ensures hearts accurately regenerate even if the user closes the app, locks their phone, or returns hours later.

### Decision 2: Deduct Heart on Set Completion
- **Chosen Approach**: Deduct 1 heart when the target repetition goal is achieved or when the user finishes and logs their workout set.
- **Why**: Directly matches user instruction and eliminates user frustration from computer vision misclassifications.

### Decision 3: Pinning the Bottom Report Card with Box Constraints
- **Chosen Approach**: Use a `Box(Modifier.fillMaxSize())` with `Alignment.TopStart` for counter/alerts and `Alignment.BottomCenter` for the last-rep report card, with `verticalScroll` or strict `heightIn` on the top container.
- **Why**: Completely isolates the bottom card from vertical height expansion of alert messages, preventing the clipping bug seen in the user's screenshot.

---

## 4. Technical Architecture & Implementation Steps

```
┌────────────────────────────────────────────────────────────────────────┐
│                        HEART & HUD ARCHITECTURE                        │
└────────────────────────────────────────────────────────────────────────┘

  [Room Database: UserStats]
    - hearts: Int (default: 25, max: 25)
    - lastHeartRegenTime: Long (epoch millis)
               │
               ▼
  [Repository / WorkoutViewModel]
    - checkPassiveHeartRegeneration(): refills 1 heart per 300s elapsed
    - onSetCompleted(): consumes 1 heart
    - (deductHeartOnMistake removed from scoring/CV pipeline)
               │
               ▼
  [PracticeScreen HUD]
    - Box layout: Top HUD + Bottom HUD (pinned)
    - Collapsible Telemetry State (default: collapsed)
    - Translucent glassmorphism warning badges
               │
               ▼
  [TtsCoach]
    - Throttled queue: 8.5s cooldown for repeated cues
    - Urgent override for critical rejections
```

### File Changes Checklist

1. **`app/src/main/java/com/example/data/Database.kt` & `Repository.kt`**:
   - Update default `hearts` from 5 to 25 and `maxHearts` to 25.
   - Add `lastHeartRegenTime: Long` to `UserStats` entity.
   - Implement `checkAndApplyPassiveRegen(regenIntervalMs = 300_000L)` to calculate and credit regenerated hearts based on elapsed time.
   - Implement `consumeHeartForCompletedSet()`.

2. **`app/src/main/java/com/example/audio/TtsCoach.kt`**:
   - Update non-urgent speech throttling from `2800L` to `8500L` (8.5 seconds cooldown).
   - Maintain `isUrgent` path for high-priority rep rejections.

3. **`app/src/main/java/com/example/viewmodel/WorkoutViewModel.kt`**:
   - Remove heart deduction calls from `processCameraFrameAnalysis` and form mistake callbacks.
   - Trigger 1 heart deduction when `targetReps` is achieved or when `stopAndSaveWorkout()` is called after completing a set.
   - Expose `nextHeartRegenSecondsRemaining` for the UI timer.

4. **`app/src/main/java/com/example/ui/screens/PracticeScreen.kt`**:
   - Redesign `PullUpDashboardHUD` and `PushUpDashboardHUD`:
     - Restructure container from single expanding Column to `Box` with explicit Top and Bottom alignments.
     - Add `isTelemetryExpanded` toggle state (defaulting to collapsed).
     - Style warning alerts with semi-transparent glass background (`Color(0x992B1010)` with translucent border).
