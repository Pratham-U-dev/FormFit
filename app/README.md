# 🏋️‍♂️ FormFit — AI-Powered Personal Fitness & Nutrition Companion

> **Transform your device into a real-time AI movement coach, intelligent meal scanner, and global fitness arena.**

[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-757575?style=for-the-badge&logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![Gemini AI](https://img.shields.io/badge/AI-Gemini%20Vision-8E24AA?style=for-the-badge&logo=google&logoColor=white)](https://ai.google.dev/)
[![Android Min SDK](https://img.shields.io/badge/Min%20SDK-24+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

---

## 🌟 What is FormFit?

**FormFit** is a next-generation, all-in-one mobile workout and nutrition ecosystem. Designed for athletes, fitness enthusiasts, and anyone striving for a healthier lifestyle, FormFit combines cutting-edge computer vision with Google Gemini AI to analyze your exercise technique in real-time, log your meal nutrition instantly from camera photos, and connect you with a vibrant global leaderboard community.

Whether you're performing push-ups in your living room, tracking daily macronutrients, or competing for the top rank on the global leaderboard, FormFit delivers immediate feedback and gamified motivation right in the palm of your hand.

---

## ✨ Key Features

### 🏋️ 1. Real-Time AI Form Coach & Exercise Counter
* **Live Motion & Pose Tracking**: Detects exercise movements dynamically via your phone's camera.
* **Instant Visual Feedback**: Get live form coaching and automatic rep counting during active workout sessions.
* **Form Accuracy Scoring**: Calculates real-time form percentage scores so you can optimize posture, maximize workout gains, and prevent injuries.

### 🥗 2. Smart AI Food & Nutrition Scanner
* **Multimodal Meal Recognition**: Snap a photo of your meal or upload from gallery to receive an instant breakdown of calories, protein, carbs, and fats.
* **Dual-Engine Architecture**:
  * **Gemini Vision API (Cloud)**: Deep multimodal semantic analysis for detailed ingredient and macro estimations.
  * **Local Histogram Engine (Offline)**: Advanced local color-density and contour analysis that guarantees macro estimation even without internet connection or API keys.
* **Daily Macro Dashboard**: Seamlessly tracks your daily target progress with intuitive visual progress rings.

### 🏆 3. Global Real-Time Leaderboard
* **Centralized Live Arena**: Compete with real athletes across devices and regions in real-time.
* **Automatic Cloud Synchronization**: Workout sessions, earned XP, streaks, and levels sync to a centralized global registry automatically.
* **Universal Access**: Share the app APK with friends and family—their custom display names and scores immediately appear on your global leaderboard!

### 🎖️ 4. Gamification, Badges & Streaks
* **XP & Leveling System**: Earn Experience Points (XP) for every completed exercise rep and logged meal to level up your athlete profile.
* **Achievements Shelf**: Unlock milestone badges for workout streaks, total reps, perfect form scores, and diet consistency.
* **Streak Protection**: Stay motivated with daily streak tracking and milestone rewards.

### 🤖 5. Virtual AI Personal Trainer
* **24/7 AI Coach Chat**: Ask questions regarding workout plans, recovery advice, protein requirements, or exercise form tips.
* **Contextual Insights**: Powered by Gemini LLM to deliver personalized fitness guidance tailored to your current stats.

### 🎨 6. Modern Duolingo-Inspired UI
* **Vibrant Material 3 Aesthetic**: Bold high-contrast typography, playful interactive controls, and cheerful visual feedback.
* **Edge-to-Edge Fluidity**: Optimized for standard mobile screens, foldables, and tablets with adaptive navigation.

---

## 🛠️ Technical Architecture & Stack

FormFit is engineered following modern Android architecture best practices (**MVVM + Clean Architecture**), delivering high performance, minimal memory overhead (~24.8 MB footprint), and smooth 60 FPS Compose rendering.

```
┌──────────────────────────────────────────────────────────────────┐
│                          FormFit App                             │
├────────────────────────────────┬─────────────────────────────────┤
│         UI Layer               │          Data & AI Layer        │
│  • Jetpack Compose & M3        │  • Gemini Vision REST API       │
│  • Navigation Compose          │  • Centralized Cloud Sync       │
│  • StateFlow / ViewModel       │  • SharedPreferences Cache     │
└────────────────────────────────┴─────────────────────────────────┘
```

### 💻 Tech Stack
* **Language**: [Kotlin](https://kotlinlang.org/) (100%)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
* **Async & Reactive**: Kotlin Coroutines & `StateFlow` / `SharedFlow`
* **Networking**: [OkHttp3](https://square.github.io/okhttp/) & [Retrofit](https://square.github.io/retrofit/)
* **AI & Vision**: [Google Gemini 1.5 Flash Vision API](https://ai.google.dev/) + Custom Color-Histogram & Contour Analyzer
* **Data Storage**: Local `SharedPreferences` + REST Cloud Master Registry

---

## 📱 App Screens Overview

| Screen | Description |
| :--- | :--- |
| **🏠 Workout Coach** | Interactive camera session, rep counters, exercise selection, and live form feedback. |
| **🥗 Nutrition** | Instant meal camera scanner, macro breakdown summary, and daily target logs. |
| **🏆 Leaderboard** | Live global rankings displaying top athletes, ranks, XP, and streak indicators. |
| **🤖 AI Coach** | Conversational chat interface for personalized fitness advice and custom workout plans. |
| **👤 Profile** | Personal stats overview, Achievements Shelf (badges), AI Key configuration, and settings. |

---

## 🚀 Getting Started

### Prerequisites
* **Android Device / Emulator**: Running Android 7.0 (API Level 24) or higher.
* **Android Studio**: Ladybug / Jellyfish or newer with Kotlin plugin.
* *(Optional)* **Gemini API Key**: For cloud-powered multimodal meal recognition. Get a free key at [Google AI Studio](https://aistudio.google.com/).

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/formfit-android.git
   cd formfit-android
   ```

2. **Open in Android Studio**:
   Open Android Studio and select **Open an Existing Project**, then select the project root directory.

3. **Build & Run**:
   * Connect an Android device via USB debugging or start an emulator.
   * Press `Shift + F10` or click the **Run** button in Android Studio.

4. **(Optional) Add Gemini Vision API Key**:
   * Navigate to the **Profile** tab in FormFit.
   * Enter your Gemini API key in the **AI Vision Recognition Key** field to activate cloud AI meal analysis.

---

## 📂 Project Structure

```
app/src/main/java/com/example/
├── api/                  # Gemini Vision API & Local Computer Vision Analyzers
├── data/                 # Data Models, Repositories & Cloud Leaderboard Services
├── ui/
│   ├── components/       # Reusable Compose Cards, Dialogs, Buttons & Pulse Indicators
│   ├── navigation/       # Navigation routes and bottom bar setup
│   ├── screens/          # Workout, Nutrition, Leaderboard, AI Coach & Profile screens
│   └── theme/            # Material 3 Color Schemes, Typography & Shapes
├── viewmodel/            # Central WorkoutViewModel & UI State Management
└── MainActivity.kt       # Application Entry Point & Navigation Host
```

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

<p align="center">
  <b>Built with ❤️ using Kotlin, Jetpack Compose, and Google AI Studio</b>
</p>
