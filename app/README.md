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

**FormFit** is a next-generation, all-in-one mobile workout and nutrition ecosystem. Designed for athletes, fitness enthusiasts, and anyone striving for a healthier lifestyle, FormFit combines advanced multi-point computer vision posture validation with Google Gemini AI to analyze your exercise technique in real-time, log your meal nutrition instantly from camera photos, and connect you with a vibrant global leaderboard community.

Whether you're performing rigorous pull-ups or push-ups with spatial form gates, tracking daily macronutrients, or competing for top rank on the global leaderboard, FormFit delivers immediate voice feedback and gamified motivation right in the palm of your hand.

---

## ✨ Key Features & Recent Enhancements

### 📐 1. Rigorous Multi-Point Geometric Pose Analysis & Form Gates
* **Advanced Spatial Vector Validation**: Replaces simplistic single-joint angle heuristics with multi-point coordinate and spatial vector constraints.
* **Pull-Up & Push-Up Rigor**: Eliminates false-positive rep counts (such as horizontal arm extensions or T-poses) by enforcing overhead wrist positioning, bar suspension anchors, and strict horizontal floor plank alignment.
* **Instant Form Rejection & Audio Coaching**: Automatically rejects incomplete or improper form repetitions with descriptive audio warnings and actionable posture correction.

### ❤️ 2. Duolingo-Style Hearts Stamina System
* **25 Heart Energy Pool**: Start with 25 hearts that represent workout stamina and pacing per exercise set.
* **Non-Punitive Mistake Handling**: Form corrections and false posture detections do not drain hearts, ensuring users stay motivated without feeling penalized by pose estimation margins.
* **Passive Time Regeneration**: Consumed hearts automatically regenerate over time (1 heart every 5 minutes), with real-time countdown tracking.

### 🎥 3. Streamlined Collapsible HUD & Transparent Alerts
* **Collapsible Telemetry & Muscle Details**: Toggle button ("Telemetry & Muscle ▼") lets you expand or minimize peak power, speed loss vs. rep 1, and modeled muscle thermal/fatigue metrics on demand.
* **Semi-Transparent Alert Banners**: Warning and rep-rejection overlays use semi-transparent styling so the background camera feed remains fully visible.
* **Optimized Layout**: Clean HUD hierarchy prevents notifications from pushing rep counters or bottom controls out of the active camera frame.

### 🗣️ 4. Throttled Voice Coach & Audio Guidance
* **Intelligent Rate-Limiting**: 8.5-second cooldown interval between non-urgent voice coach tips prevents auditory clutter and confusion during heavy exertion.
* **Urgent Override Support**: Critical safety and rep rejection alerts instantly flush the audio queue to provide immediate corrective feedback.

### 🥗 5. Smart AI Food & Nutrition Scanner
* **Multimodal Meal Recognition**: Snap a photo of your meal or upload from gallery to receive an instant breakdown of calories, protein, carbs, and fats.
* **Dual-Engine Architecture**: Powered by Gemini Vision API (Cloud) with local fallback color-histogram analysis.

### 🏆 6. Global Real-Time Leaderboard & Gamification
* **Centralized Live Arena**: Compete with real athletes across devices and regions in real-time.
* **XP, Levels & Badges**: Earn experience points for completed reps and logged meals to unlock achievement badges and streak protection.

---

## 🛠️ Technical Architecture & Stack

FormFit is engineered following modern Android architecture best practices (**MVVM + Clean Architecture**), delivering high performance, minimal memory overhead, and smooth 60 FPS Compose rendering.

### 💻 Tech Stack
* **Language**: [Kotlin](https://kotlinlang.org/) (100%)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3
* **Async & Reactive**: Kotlin Coroutines & `StateFlow` / `SharedFlow`
* **Networking**: [OkHttp3](https://square.github.io/okhttp/) & [Retrofit](https://square.github.io/retrofit/)
* **AI & Vision**: [Google Gemini 1.5 Flash Vision API](https://ai.google.dev/) + Multi-Point ML Kit Pose Estimator
* **Data Storage**: Room Database + Cloud Master Registry

---

## 📱 App Screens Overview

| Screen | Description |
| :--- | :--- |
| **🏠 Workout Coach** | Interactive camera session with spatial pose validation, collapsible telemetry HUD, and rep counters. |
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
2. **Open in Android Studio** and select the project root directory.
3. **Build & Run**: Connect an Android device or start an emulator and run the app.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

<p align="center">
  <b>Built with ❤️ using Kotlin, Jetpack Compose, and Google AI Studio</b>
</p>
