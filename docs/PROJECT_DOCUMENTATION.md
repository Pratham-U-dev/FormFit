# FormFit - Project Documentation

## 1. Overview
**FormFit** is an AI-powered personal fitness and nutrition tracking Android application built using Jetpack Compose, Kotlin Coroutines & Flow, Material 3, Room, Firebase (Authentication & Firestore), and Gemini 2.5 Flash Vision API.

The app features a Gamified "Duolingo-style" design language with playful sound effects, vibrant M3 cards, thick tactile buttons, real-time pose coaching, meal scanning, and a live online leaderboard.

---

## 2. Key Features

### 🏋️ FormFit AI Workout Coach
- **Real-Time Camera & Pose Analysis**: Uses Google ML Kit Pose Detection to evaluate exercise execution (e.g., Squats, Pushups, Jumping Jacks) and count valid repetitions.
- **Form Quality Scoring**: Analyzes key joint angles in real time to calculate form accuracy percentages and give instantaneous audio/visual coaching feedback.
- **Session Summaries**: Logs calories burned, average form score, duration, and rep count upon completing a workout session.

### 🥗 Gemini AI Nutrition & Calorie Tracker
- **Snap Plate Vision AI**: Take or upload a photo of any food plate or meal. Gemini 2.5 Flash analyzes the image to detect meal items, total calories, protein, carbs, and fat.
- **Multi-Source Image Capture**: Presents an interactive selection modal with options to **Take Photo with Camera**, **Choose from Gallery**, or **Try Sample Meal (AI Test)** with robust camera runtime permission checking and try-catch safety guards.
- **Manual Log & Preset Item Calculator**: Offers direct calorie entry and pre-calculated common food items (e.g., Steak & Eggs, Chicken Breast, Rice, Avocado Toast).
- **Daily Progress Bar**: Displays remaining calories against a daily target (2000 kcal default) with visual macro pill breakdowns (P/C/F).
- **Interactive Date Strip & History**: Select past dates (last 30 days) to review historical meal logs and calorie totals.

### 🏆 Live Online Leaderboard (Firebase Integration)
- **Anonymous Authentication**: On first launch, users are anonymously authenticated via Firebase Authentication.
- **Custom Display Name**: Prompts first-time users for a display name stored permanently in Firebase Firestore (`users` collection).
- **Real-Time Sync**: Firestore snapshot listener keeps the online leaderboard automatically updated and ordered by total XP.
- **Offline First & Local Cache**: Firestore offline persistence ensures cached entries remain visible when internet connectivity is lost, synchronizing automatically when reconnected.
- **User Row Highlight**: Highlights the current authenticated user's row with a green border and custom badge.

### 🥇 Achievements, Streaks & Gamification
- **XP & Level Progression**: Earn XP with every completed workout based on form accuracy and total reps.
- **Duolingo-Inspired UI & Audio**: Custom Duolingo-styled M3 components, progress bars, and synthesized audio sound effects (`DuoSoundPlayer`).
- **Badges & Achievements**: Dynamic achievement unlocks (e.g., *First Workout*, *Streak Master*, *Form Master*).

---

## 3. Architecture & Tech Stack

- **UI Framework**: Jetpack Compose with Material Design 3.
- **Architecture Pattern**: MVVM (Model-View-ViewModel) + Clean Architecture principles.
- **State Management**: Kotlin `StateFlow` and `collectAsStateWithLifecycle()`.
- **Database (Local)**: Room Database v2 with KSP (`FormFitDatabase` storing `WorkoutSession`, `UserStats`, and `NutritionLog`).
- **Database & Auth (Cloud)**: Firebase Authentication (Anonymous) and Cloud Firestore for online leaderboard profile synchronization.
- **AI & Vision**:
  - Google ML Kit Pose Detection for real-time rep counting and angle measurement.
  - Gemini 2.5 Flash via REST (`GeminiFoodAnalyzer`) for image-based nutrition estimation.
- **Networking**: OkHttp for REST endpoints.
- **Audio Engine**: Android `AudioTrack` PCM tone generator for click, correct, and level-up audio cues.

---

## 4. Data Models & Database Schema

### Room Local Database (`FormFitDatabase`)

#### `WorkoutSession`
- `id`: Int (Auto-generated Primary Key)
- `exerciseName`: String
- `reps`: Int
- `averageScore`: Int
- `caloriesBurned`: Int
- `durationSeconds`: Int
- `timestamp`: Long

#### `UserStats`
- `id`: Int (Primary Key = 1)
- `currentXp`: Int
- `level`: Int
- `streakDays`: Int
- `lastWorkoutDate`: String (YYYY-MM-DD)
- `unlockedBadgesCsv`: String

#### `NutritionLog`
- `id`: Int (Auto-generated Primary Key)
- `date`: String (YYYY-MM-DD)
- `mealName`: String
- `calories`: Int
- `proteinGrams`: Int
- `carbsGrams`: Int
- `fatGrams`: Int
- `photoPath`: String?
- `timestamp`: Long

---

### Cloud Firestore Schema (`users` collection)

Document ID: `uid` (Firebase Auth Anonymous UID)

```json
{
  "uid": "string",
  "displayName": "string",
  "level": 1,
  "xp": 1450,
  "workoutCount": 12,
  "totalReps": 350,
  "averageFormScore": 92,
  "streak": 5,
  "lastWorkoutDate": "2026-07-23",
  "achievements": ["first_workout", "form_master"],
  "avatarIcon": "💪"
}
```

---

## 5. Directory & File Structure

```
app/src/main/java/com/example/
├── api/
│   └── GeminiFoodAnalyzer.kt             # Gemini Vision AI food analyzer
├── audio/
│   └── DuoSoundPlayer.kt                 # Synthesized sound effects
├── data/
│   ├── Database.kt                       # Room DB, Entities & DAOs
│   ├── FirestoreUser.kt                  # Firestore User data model
│   ├── FirebaseLeaderboardRepository.kt # Firebase Auth & Firestore sync
│   └── Repository.kt                     # FormFit Room Repository
├── ui/
│   ├── components/
│   │   ├── DisplayNameDialog.kt          # First-launch display name dialog
│   │   └── DuoComponents.kt              # Duolingo-styled Compose widgets
│   ├── screens/
│   │   ├── CoachScreen.kt                # Camera & Pose analysis view
│   │   ├── LeaderboardScreen.kt          # Live online leaderboard view
│   │   ├── NutritionScreen.kt            # Calorie & macro tracking view
│   │   └── ProfileScreen.kt              # User stats, badges & history
│   └── theme/                            # Color, Type, and Theme setups
├── viewmodel/
│   └── WorkoutViewModel.kt               # Central ViewModel managing state
└── MainActivity.kt                       # Navigation & bottom bar shell
```
