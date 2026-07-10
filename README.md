<div align="center">

<img src="https://raw.githubusercontent.com/nawfumi/spondon/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp" width="96" height="96" alt="Spondon Logo"/>

# 🩸 Spondon · স্পন্দন

**রক্ত দিন, জীবন বাঁচান**
*Give Blood, Save Lives*

A community-driven blood donation platform built for Bangladesh —
connecting donors with those in need through intelligent, community-scoped matching.

<br/>

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-C0152A?style=for-the-badge)](#architecture--tech-stack)
[![License](https://img.shields.io/badge/License-Copyright%20%C2%A9%202026-gray?style=for-the-badge)](./LICENSE)
[![Free Forever](https://img.shields.io/badge/Free-Forever-brightgreen?style=for-the-badge)](#-free-forever)

<br/>

[**⬇️ Download APK**](#-download) · [**🌐 Website**](#) · [**📋 Changelog**](#) · [**🐛 Report a Bug**](../../issues)

</div>

---

## 🌟 Why Spondon?

Every day in Bangladesh, people facing medical emergencies struggle to find the right blood type at the right time. Families make frantic calls, post on Facebook, and wait — uncertain if help will arrive. There was no dedicated, trustworthy space built for this singular purpose.

**Spondon** was built to change that.

Not just an app — a living community where strangers become lifesavers. Spondon connects donors and recipients instantly, within trusted local communities, with privacy and dignity at the core.

> *"Every heartbeat counts. Every community matters."*

---

## ✨ Features

### 🩸 Blood Requests & Donor Search
- **Community-Scoped Blood Requests** — Post and browse urgent, moderate, or normal blood requests filtered by your joined communities
- **Smart Donor Search** — Filter donors by blood group, location, availability, and community membership with map view support
- **Direct Call Integration** — One-tap phone dialer available only after a donor explicitly accepts a request

### 👥 Community System
- **Public & Private Communities** — Open communities anyone can join, or gated communities with approval-based access
- **Role-Based Access Control** — Super Admin / Community Admin / Moderator / Member hierarchy
- **Custom Member Serial IDs** — SuperAdmin-enabled unique serial numbers for private community members

### 🔔 Notifications & Alerts
- **Real-Time Push Notifications** — FCM-powered alerts for critical blood requests, join approvals, and donor acceptance — delivered even when the app is closed
- **In-App Notification Inbox** — All alerts organized in one place, nothing slips through

### 🛡️ Privacy & Security
- **Privacy-First Design** — Donor contact information stays hidden until the donor explicitly accepts a request
- **Privacy Mode** — Fine-grained control over who can view your personal details
- **Secure Authentication** — Biometric Login, OTP Phone Verification, and Google Sign-In

### 🏅 Donor Recognition
- **Donation Badges & Achievement System** — Gamified milestones that celebrate and reward consistent donors
- **Verifiable Donation History** — A complete, tamper-evident log of every donation
- **PDF Export** — Download donation certificates and community member lists

### 🎨 Experience & Performance
- **Adaptive Theming** — Polished dark and light mode with modern Material 3 aesthetics
- **Offline Caching** — Room-powered local database for resilience on low connectivity
- **Smooth Animations** — Lottie-powered onboarding and micro-interactions

---

## 📋 App Overview

| Item | Detail |
|---|---|
| **App Name** | Spondon · স্পন্দন |
| **Platform** | Android (Jetpack Compose) |
| **Backend** | Firebase (Firestore, Auth, FCM, Storage) |
| **Architecture** | MVVM + Clean Architecture |
| **Min SDK** | API 26 (Android 8.0 Oreo) |
| **Target SDK** | API 35 |
| **Language** | Kotlin |
| **Status** | ✅ Production — v1.0.0 |

---

## 🏗️ Architecture & Tech Stack

Spondon is built on **MVVM + Clean Architecture**, ensuring a clear separation of concerns, long-term scalability, and maintainability.

| Layer | Technology | Purpose |
|---|---|---|
| **UI** | Jetpack Compose | Declarative, reactive UI |
| **Architecture** | MVVM + Clean | Separation of concerns |
| **DI** | Hilt | Dependency injection |
| **Navigation** | Compose Navigation | Screen routing & deep links |
| **Auth** | Firebase Auth | Phone / Email / Google Sign-In |
| **Database** | Cloud Firestore | Real-time sync & cloud storage |
| **Storage** | Firebase Storage | Image compression & uploads |
| **Push** | FCM + Cloud Functions | Background push notifications |
| **Async** | Kotlin Coroutines + Flow | Reactive data streams |
| **Local DB** | Room | Offline caching |
| **Images** | Coil | Fast, memory-efficient image loading |
| **Animation** | Lottie | Onboarding & UI animations |
| **PDF** | Android PdfDocument | Certificate & member list export |

---

## ⬇️ Download

Spondon is distributed as a sideloaded APK via GitHub Releases — no Play Store, no gatekeeping.

> **[⬇️ Download Latest APK →](../../releases/latest)**

**Installation steps:**
1. Download the `.apk` from the link above
2. On your Android device, go to **Settings → Install Unknown Apps** and allow your browser or file manager
3. Open the downloaded `.apk` and tap **Install**
4. Launch Spondon and sign up — it takes under a minute

> Minimum Android version: **8.0 Oreo (API 26)**

---

## 🚀 Building from Source

### Prerequisites

- Android Studio Koala or later
- JDK 17
- Android SDK with API 26–35

### Setup

```bash
# 1. Clone the repository
git clone https://github.com/nawfumi/Spondon.git
cd Spondon

# 2. Add your Firebase config
# Download google-services.json from Firebase Console → Project Settings
# Place it at:
cp /path/to/google-services.json app/google-services.json

# 3. Open in Android Studio, sync Gradle, and press Run ▶
```

> **Note:** The app requires a Firebase project with Firestore, Auth, FCM, and Storage enabled. Cloud Functions are required for push notifications.

---

## 💚 Free Forever

Spondon is **completely free** — and will always be.

There are no ads, no subscription tiers, no premium features, and no hidden costs. This app was designed, built, and is maintained entirely by one developer, with one purpose: to make blood donation faster and more accessible for everyone in Bangladesh.

It was free on day one. It will be free on day one thousand.

---

## 🙏 Support the Project

Building and maintaining a solo app has real costs — Firebase infrastructure, domain, ongoing development time, and future features. If Spondon has been useful to you or someone you know, here are two ways you can help:

**⭐ Star this repo** — It helps others discover the project and motivates continued development.

**🔗 Share it** — Tell a friend, post about it, or share the APK with someone who might need it. One share could save a life.

**☕ Support the developer** — If you'd like to contribute financially toward keeping this project alive and growing, any support is deeply appreciated and goes directly back into Spondon.

> [**→ Support on [Platform]**](#)

---

## 🔮 Roadmap

| Feature | Status |
|---|---|
| In-app Chat (donor ↔ requester secure messaging) | 🔜 Planned |
| Blood Bank Directory with live inventory | 🔜 Planned |
| Emergency SOS — 1-tap broadcast to all communities | 🔜 Planned |
| Community Leaderboards & gamified recognition | 🔜 Planned |
| AI Donor Matching for urgent requests | 🔜 Planned |
| iOS Client via Kotlin Multiplatform (KMP) | 🔜 Planned |

---

## 🤝 Contributing

Spondon is open source and welcomes contributions. For major changes, please open an issue first to discuss what you'd like to change.

```bash
# Fork → Branch → Commit → Push → Pull Request
git checkout -b feature/your-feature-name
git commit -m "feat: add your feature"
git push origin feature/your-feature-name
```

Please follow the existing code style (Kotlin, Compose, MVVM) and write meaningful commit messages.

---

## 📜 License

```
Copyright (c) 2026 nawfumi (ashgorhythm)
All rights reserved.

This source code is made available for transparency and community contribution.
Redistribution, commercial use, or derivative works require explicit permission
from the original author.
```

---

## 📬 Contact

Built and maintained by **[@nawfumi](https://github.com/nawfumi)**

Have a question, found a bug, or want to suggest a feature?
→ [Open an Issue](../../issues) · [GitHub Profile](https://github.com/nawfumi)

---

<div align="center">

*Spondon · স্পন্দন*
**Every heartbeat counts. Every community matters.**

Built with ❤️ in Kotlin · Made in Bangladesh 🇧🇩

</div>