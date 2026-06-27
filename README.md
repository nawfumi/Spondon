# 🩸 Spondon · স্পন্দন

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white"/>
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black"/>
  <img src="https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-C0152A?style=for-the-badge"/>
</p>

<p align="center">
  <strong>রক্ত দিন, জীবন বাঁচান</strong><br/>
  <em>Give Blood, Save Lives</em>
</p>

<p align="center">
  A community-driven blood donation platform built for Bangladesh, connecting donors with those in need through intelligent community-scoped matching.
</p>

---

## 📋 Overview

| Item | Detail |
|------|--------|
| **App Name** | Spondon · স্পন্দন |
| **Platform** | Android (Jetpack Compose) |
| **Backend** | Firebase (Firestore, Auth, FCM, Storage) |
| **Architecture** | MVVM + Clean Architecture |
| **Min SDK** | API 26 (Android 8.0) |
| **Target SDK** | API 35 |

---

## ✨ Key Features

- 🔴 **Community-Scoped Blood Requests** — Post and browse urgent/moderate/normal blood requests filtered by your communities
- 🔍 **Smart Donor Search** — Filter by blood group, location, availability, and community with map view
- 👥 **Community System** — Public & private communities with role-based access (Super Admin / Community Admin / Moderator / Member)
- 🔢 **Custom Member Serial IDs** — SuperAdmin enabled serial numbers for private communities
- 🔔 **Real-time Push Notifications** — FCM alerts for CRITICAL requests, join approvals, donor acceptance (even works when app is closed)
- 📞 **Direct Call Integration** — One-tap phone dialer for accepted donor↔requester contact
- 🛡️ **Privacy First** — Donor contact information remains hidden until the donor explicitly accepts a blood request.
- 🏅 **Donation Badges & History** — Gamified achievement system with a verifiable donation log
- 🌙 **Adaptive Theming** — Full dark and light mode support with modern UI aesthetics
- 🔒 **Secure Auth** — Biometric Login & OTP Phone Verification
- 📄 **PDF Export** — Export Community Member list & Donation Certificates
- 🗺️ **Offline Caching** — Built with Room for resilience on low connectivity

---

## 🏗️ Architecture & Tech Stack

Spondon follows the **MVVM + Clean Architecture** principles, ensuring a robust separation of concerns, scalability, and testability.

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **UI** | Jetpack Compose | Declarative UI |
| **Architecture** | MVVM + Clean | Separation of concerns |
| **DI** | Hilt | Dependency injection |
| **Navigation** | Compose Navigation | Screen routing |
| **Auth** | Firebase Auth | Phone / Email / Google |
| **Database** | Cloud Firestore | Real-time sync |
| **Storage** | Firebase Storage | Image compression & saving |
| **Push** | FCM + Cloud Functions| Background Push Notifications |
| **Async** | Kotlin Coroutines + Flow | Reactive data streams |
| **Local DB** | Room | Offline caching |
| **Images** | Coil | Fast image loading |
| **Animation** | Lottie | Onboarding animations |

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Koala or later
- JDK 17
- Minimum SDK 26 (Android 8.0 Oreo)

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/ashgorhythm/Spondon.git
   cd Spondon
   ```

2. **Add Firebase config**  
   Download your `google-services.json` from the Firebase Console and place it in the `app/` directory:
   ```text
   app/google-services.json
   ```

3. **Enable Firebase Plugin**  
   Ensure the Google Services plugin is applied in `app/build.gradle.kts`:
   ```kotlin
   alias(libs.plugins.google.services)
   ```

4. **Build & Run**
   Open the project in Android Studio, sync Gradle, and press **Run ▶**.

---

## 🔮 Future Roadmap

- 💬 **In-app Chat** — Secure messaging between donor and requester before sharing numbers
- 🏥 **Blood Bank Directory** — Live inventory updates from partner hospitals
- 🚨 **Emergency SOS** — 1-tap broadcast to all joined communities
- 🏆 **Community Leaderboards** — Gamified donor recognition
- 🤖 **AI Donor Matching** — Intelligent matchmaking algorithms for urgent requests
- 🍏 **iOS Client** — Rebuilding with Kotlin Multiplatform (KMP)

---

## 🤝 Contributing

We welcome pull requests! Spondon is an open-source initiative aimed at saving lives. 
For major changes, please open an issue first to discuss what you would like to change.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📜 License

```
No License — Copyright (c) 2026 ashgorhythm
```

---

<p align="center">
  <em>Spondon · স্পন্দন</em><br/>
  <strong>Every heartbeat counts. Every community matters.</strong><br/>
  Built with ❤️ in Kotlin
</p>
