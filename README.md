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
| **Total Phases** | 6 |
| **Min SDK** | API 26 (Android 8.0) |
| **Target SDK** | API 35 |

---

## ✨ Features at a Glance

- 🔴 **Community-Scoped Blood Requests** — Post and browse urgent/moderate/normal blood requests filtered by your communities
- 🔍 **Smart Donor Search** — Filter by blood group, location, availability, and community with map view
- 👥 **Community System** — Public & private communities with role-based access (Super Admin / Community Admin / Moderator / Member)
- 🔢 **Custom Member Serial IDs** — SuperAdmin enabled serial numbers for private communities
- 🔔 **Real-time Push Notifications** — FCM alerts for CRITICAL requests, join approvals, donor acceptance
- 📞 **Direct Call Integration** — One-tap phone dialer for accepted donor↔requester contact
- 🏅 **Donation Badges & History** — Gamified achievement system with a verifiable donation log
- 🌙 **Dark / Light Mode** — Full adaptive theming with Bangla + English language support
- 🔒 **Biometric Login** & OTP Phone Verification
- 📄 **PDF Export** — Export Community Member list & Donation Certificates
- 🗺️ **Offline Caching** with Room for resilience on low connectivity

---

## 🏗️ Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| UI | Jetpack Compose | Declarative UI |
| Architecture | MVVM + Clean | Separation of concerns |
| DI | Hilt | Dependency injection |
| Navigation | Compose Navigation | Screen routing |
| Auth | Firebase Auth | Phone / Email / Google |
| Database | Cloud Firestore | Real-time sync |
| Storage | Firebase Storage | Images & files |
| Push | FCM | Notifications |
| Async | Kotlin Coroutines + Flow | Reactive data streams |
| Local DB | Room | Offline caching |
| Images | Coil | Image loading |
| Animation | Lottie | Onboarding animations |

---

## 📁 Module Structure

```
app/src/main/java/com/spondon/app/
│
├── core/
│   ├── ui/           → Design system: Theme, Typography, Color, Shape
│   │   └── components/  → Reusable composables (BloodGroupBadge, UrgencyTag, etc.)
│   ├── data/         → Repositories, Room DB, Firebase data sources
│   └── domain/       → Use-cases, domain models (User, Community, BloodRequest, Donation)
│
├── feature/
│   ├── auth/         → Splash, Onboarding, Login, SignUp (3-step), OTP, Forgot Password
│   ├── community/    → Community List/Detail, Create, Join Request, Admin Dashboard
│   ├── request/      → Home Dashboard, Create/Detail/Feed Blood Request
│   ├── donor/        → Find Donor, Donor Profile, Donation History, Achievements
│   ├── profile/      → My Profile, Edit Profile
│   ├── notification/ → Notification Center, FCM Service
│   └── settings/     → Settings (appearance, language, privacy, security, account)
│
├── di/               → Hilt modules (AppModule, RepositoryModule)
└── navigation/       → Routes, SpondonNavGraph
```

---

## 🎨 Design System

**Color Palette:**
- Primary: Deep Blood Red `#C0152A`
- Accent: Soft Rose `#E63950`
- Background (Dark): `#0F0F0F` / `#161616`
- Background (Light): `#FAFAFA`
- Surface Cards: `#1E1E1E` (dark) / `#FFFFFF` (light)

**Typography:**
- Display: Playfair Display (Bengali + English headings)
- Body: DM Sans (readable, clean)
- Mono / Labels: JetBrains Mono (codes, routes)

---

## 📐 Roadmap

### ✅ Phase 1 — Architecture, Design System & Project Setup
> Lay the technical foundation before writing any feature code.

- [x] MVVM + Clean Architecture scaffold
- [x] Hilt dependency injection wiring
- [x] Compose Navigation graph
- [x] Design system (Color, Typography, Shape, Theme)
- [x] Reusable component library (BloodGroupBadge, UrgencyTag, AvailabilityIndicator, RoleBadge, AnimatedBloodDropLoader, SpondonBottomBar, etc.)
- [x] Domain models (User, Community, BloodRequest, Donation, Enums)
- [x] Use-cases (Auth, Community, Donor, Request)
- [x] Hilt modules (AppModule, RepositoryModule)

---

### ✅ Phase 2 — Onboarding, Auth & User Registration
> First-impression flows — animated onboarding, multi-step sign-up, OTP verification, session management.

| Screen | Route | Status |
|--------|-------|--------|
| Splash Screen | `SplashScreen` | Completed |
| Onboarding (3 slides) | `OnboardingScreen` | Completed |
| Sign Up — Step 1 (Basic Info) | `SignUpScreen` | Completed |
| Sign Up — Step 2 (Health Profile) | `DonorProfileSetup` | Completed |
| Sign Up — Step 3 (Location) | `LocationSetupScreen` | Completed |
| OTP Verification | `OtpScreen` | Completed |
| Login | `LoginScreen` | Completed |
| Forgot Password | `ForgotPasswordScreen` | Completed |

**Auth Flow:**
```
Splash → Auth Check → Onboarding (first launch) → Sign Up / Login → Profile Setup → Home
```

---

### ✅ Phase 3 — Community System
> The core differentiator. Public & private communities with admin governance, membership requests, and RBAC.

**User Roles:**

| Role | Capabilities |
|------|-------------|
| **Super Admin** | Platform-wide moderation, verify/ban communities, global announcements, enable custom serial numbers |
| **Community Admin** | Accept/reject joins, remove members, promote, pin requests, override donor availability, assign serial IDs, export member list to PDF |
| **Moderator** | Accept/reject joins, flag/remove requests |
| **Member (Donor)** | View feed, post requests, respond, search donors |

| Screen | Route | Status |
|--------|-------|--------|
| Community Feed / List | `CommunityListScreen` | Completed |
| Community Detail | `CommunityDetailScreen` | Completed |
| Create Community | `CreateCommunityScreen` | Completed |
| Join Request | `JoinRequestScreen` | Completed |
| Admin Panel | `AdminDashboardScreen` | Completed |

**Community Firestore Model:**
```
communities/{communityId}
  id, name, description, coverUrl, type (PUBLIC|PRIVATE),
  adminIds, moderatorIds, memberIds, pendingIds,
  area (GeoPoint + String), bloodGroups, isSerialEnabled,
  memberCount, donationCount, isVerified, createdAt
```

---

### ✅ Phase 4 — Home Dashboard & Blood Request System
> The heart of the app — community-scoped request feed, urgent request creation, donor matching, real-time status.

| Screen | Route | Status |
|--------|-------|--------|
| Home Dashboard | `HomeScreen` | Completed |
| Create Blood Request | `CreateRequestScreen` | Completed |
| Request Detail | `RequestDetailScreen` | Completed |
| Request Feed / My Requests | `RequestFeedScreen` / `MyRequestsScreen` | Completed |

**Key Business Logic:**
- Community-scoped feed (only show requests from joined communities)
- **120-day donor cooldown** — donor unavailable for 120 days after `lastDonationDate`
- **Admin override at ≥ 90 days** — admin can manually mark a member available
- Auto-expire requests after donation time passes
- Real-time respondent count via Firestore listeners
- FCM push to all community members on new CRITICAL request
- **Call intent**: accepted donor → requester taps 📞 to open phone dialer with `tel:` intent

**Blood Request Firestore Model:**
```
requests/{requestId}
  id, communityIds, requesterId, bloodGroup,
  urgency (CRITICAL|MODERATE|NORMAL), unitsNeeded,
  patientName?, hospital, hospitalGeo (GeoPoint),
  donationDateTime, contactNumber, respondents,
  status (ACTIVE|FULFILLED|EXPIRED|CANCELLED),
  isPinned, createdAt, expiresAt
```

---

### ✅ Phase 5 — Donor Search, Profile & Donation History
> Smart, community-scoped donor discovery with advanced filtering, public donor profiles, and a complete donation history log.

| Screen | Route | Status |
|--------|-------|--------|
| Find Donor | `FindDonorScreen` | Completed |
| Donor Public Profile | `DonorProfileScreen` | Completed |
| My Donation History | `DonationHistoryScreen` | Completed |
| Donor Badges & Achievements | `AchievementsScreen` | Completed |

**Badges:**
| Badge | Criteria |
|-------|---------|
| 🩸 First Drop | 1st donation |
| 🧡 Life Saver | 5 donations |
| ❤️ Hero Donor | 10 donations |
| 👑 Community Champion | Top donor in a community |

**User / Donor Firestore Model:**
```
users/{userId}
  uid, name, phone, email, avatarUrl, bloodGroup,
  dob, weight, isDonor, lastDonationDate?,
  donationInterval (default 120), availabilityOverride,
  totalDonations, communityIds, location (GeoPoint),
  district, isPhoneVisible, badges, fcmToken, createdAt
```

---

### ✅ Phase 6 — Profile, Settings & Notification Center
> Personal profile management, granular notification preferences, privacy controls, and in-app notification inbox.

| Screen | Route | Status |
|--------|-------|--------|
| My Profile | `ProfileScreen` | Completed |
| Edit Profile | `EditProfileScreen` | Completed |
| Settings | `SettingsScreen` | Completed |
| Notification Center | `NotificationScreen` | Completed |

**Notification Triggers:**

| Trigger | Recipients |
|---------|-----------|
| New CRITICAL blood request | All members of broadcast communities + matching blood group donors |
| Join request submitted | Community admin(s) |
| Join approved / rejected | The applicant |
| Donor accepted your request | The request creator (with call button) |
| Donation Reminder | 24 hours before committed donation date/time |
| Cooldown Lifted (120 days) | The donor — "You are now eligible to donate again!" |
| Admin availability override | The member whose availability was manually changed |

---

## 🚀 Getting Started

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/ashgorhythm/Spondon.git
   cd Spondon
   ```

2. **Add Firebase config**  
   Download `google-services.json` from your Firebase Console and place it in `app/`:
   ```
   app/google-services.json
   ```

3. **Enable Firebase plugin**  
   In `app/build.gradle.kts`, uncomment:
   ```kotlin
   alias(libs.plugins.google.services)
   ```

4. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   # or open in Android Studio and press Run ▶
   ```

---

## 🚀 Launch Checklist

- [ ] Play Store listing: screenshots (dark + light), description in Bangla + English
- [ ] Privacy policy page (required for health apps)
- [ ] Content policy compliance review
- [ ] App signing with release keystore
- [ ] ProGuard / R8 minification + obfuscation
- [ ] Baseline Profiles for startup speed
- [ ] App Bundle (.aab) upload
- [ ] Staged rollout: 10% → 50% → 100%
- [ ] Firestore security rules audit
- [ ] Firebase App Check enabled
- [ ] Firebase Crashlytics for production error tracking
- [ ] Firebase Analytics for user behaviour and funnel analysis

---

## 🔮 v1.1 Post-Launch Features

- In-app chat between donor and requester (pre-call messaging)
- Blood bank directory with real-time stock
- Emergency SOS broadcast (1-tap to all communities)
- Leaderboard per community — top donors of the month
- Hospital partnership integrations
- iOS version (Kotlin Multiplatform or React Native)
- Web admin portal for super admins
- AI-based donor-request auto matching
- Digital donation certificate with QR code
- Blood inventory wishlist for rare blood groups

---

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.

---

## 📜 License

```
MIT License — Copyright (c) 2026 ashgorhythm
```

---

<p align="center">
  <em>Spondon · স্পন্দন</em><br/>
  <strong>Every heartbeat counts. Every community matters.</strong><br/>
  Built with Kotlin · Jetpack Compose · Firebase
</p>
