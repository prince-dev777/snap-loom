# AGENTS.md — Snap Loom System & Engineering Guide

> **Context for AI Agents & Developers:**  
> This file is the single source of truth for **Snap Loom** (Screen Reaction Studio Android App) and its integration with the **Prince Master Hub** multi-project cloud licensing and update ecosystem. Read this file first before making changes, debugging, or releasing new versions.

---

## 1. Project Overview & Architecture

* **App Name:** Snap Loom (Screen Reaction Studio)
* **Application ID:** `com.reactionstudio.screenrecorder`
* **Target Platforms:** Android 8.0 (API 26) through Android 15 (API 35/36)
* **Technology Stack:**
  * 100% Kotlin + Jetpack Compose + Material 3
  * Android CameraX (Facecam Preview & Lifecycle)
  * Android MediaProjection + MediaRecorder (Screen & Audio Capture)
  * Android 14+ Compliant Foreground Service (`mediaProjection`, `camera`, `microphone`)
  * Floating WindowManager Overlays (Draggable Facecam, Live Brush Tool, Quick Controls Pill)
  * Firebase Firestore REST API (Cloud Licensing & 1-Click Device Auto-Unlock)
  * Semantic OTA Auto-Updater (GitHub Releases API)

### Key Directory Structure
```
screen_reco/
├── app/
│   ├── build.gradle.kts                   <-- Versioning (versionCode, versionName) & dependencies
│   └── src/main/
│       ├── AndroidManifest.xml            <-- Permissions, Services, and FileProvider
│       └── kotlin/com/reactionstudio/screenrecorder/
│           ├── MainActivity.kt            <-- App Entry & Access Gatekeeper
│           ├── service/
│           │   └── ScreenRecordService.kt <-- Core Foreground Recording & Notification Engine
│           ├── overlay/
│           │   ├── FacecamOverlayManager.kt   <-- Floating Draggable Facecam
│           │   ├── BrushOverlayManager.kt     <-- Live Screen Pen/Brush Drawing
│           │   └── FloatingControlsManager.kt <-- Movable Quick Controls Pill
│           ├── ui/
│           │   ├── ActivationScreen.kt    <-- Zero-Fear Friendly Upgrade Screen
│           │   ├── SettingsScreen.kt      <-- VIP Status & App Config
│           │   └── main/
│           │       ├── MainScreen.kt          <-- Tabs: Studio, Recordings, Settings
│           │       └── MainScreenViewModel.kt <-- State management & auto-update check
│           └── utils/
│               ├── LicenseManager.kt      <-- 3-Day Trial & Silent Device Registration
│               └── AppUpdateManager.kt    <-- OTA Auto-Update Checker & Downloader
├── AGENTS.md                              <-- This definitive guide
└── README.md                              <-- User-facing feature highlights
```

---

## 2. Cloud Licensing & Multi-Project Ecosystem

Snap Loom is connected to Prince's central master hub:
* **Admin Dashboard:** `https://prince-portfolio-jade.vercel.app/admin`
* **Admin Password:** `prince777` (backup: `admin123`)
* **Portfolio Repo:** `c:\Users\sawar\MyProjects\Prince-portfolio` (`prince-dev777/Prince-portfolio`)
* **Firebase Project ID:** `prince-master-hub`
* **Firebase Account:** `saware.tj@gmail.com`
* **Telegram Contact:** `@atheist_09`

### Firestore Data Model
1. **`devices/{deviceId}` (1-Click Auto-Unlock Collection):**
   * `deviceId` (string): Unique SHA-256 hash of device hardware (`android.os.Build` + `ANDROID_ID`).
   * `deviceModel` (string): Friendly model name (e.g. `Samsung SM-S918B`).
   * `status` (string): `"PENDING"` | `"APPROVED"` | `"REVOKED"`.
   * `plan` (string): `"30 Days Pro"`, `"90 Days Pro"`, `"1 Year Pro"`, or `"Lifetime Pro"`.
   * `requestedAt` (ISO Timestamp): Time of click on "Unlock Pro via Telegram".
   * `approvedAt` (ISO Timestamp): Time Prince approved it in `/admin`.
   * `subscriptionExpiresAt` (ISO Timestamp | null): Expiry timestamp (null for Lifetime).

2. **`licenses/{licenseKey}` (Optional Manual Key Collection):**
   * `key` (string): e.g. `SNAP-XXXX-XXXX`.
   * `projectId` (string): `"snaploom"`.
   * `status` (string): `"ACTIVE"`, `"EXPIRED"`, `"REVOKED"`.

---

## 3. Core Business & UX Rules (MANDATORY)

1. **Zero Technical Friction / No Raw Device ID Visible:**
   * **NEVER** expose raw hardware strings (e.g. `DEV-9F8A12`) to the end-user in the app UI.
   * Users should only see friendly copy like *"Your 3-Day Free VIP Trial"*, *"Snap Loom Pro"*, and *"Unlock Pro via Telegram (@atheist_09)"*.
   * The device ID is sent **silently** in the background to the Firebase `devices` collection.

2. **3-Day Silent Free VIP Trial:**
   * For the first 72 hours (`3 * 24 * 60 * 60 * 1000L`) from the first launch, the app **directly opens without any key, registration, or paywall**.
   * All pro features (60 FPS, 1080p, Facecam, Brush, Trimmer) are 100% accessible.

3. **Magic 1-Click Auto-Unlock:**
   * When trial ends, user taps "Unlock Pro via Telegram (@atheist_09)".
   * The phone silently registers in Firestore and begins background polling.
   * Prince opens `/admin` -> "1-Click Device Approvals" -> selects validity (30 Days, 90 Days, 1 Year, Lifetime) -> clicks **"1-Click Approve"**.
   * The user's phone detects approval in real-time and **automatically unlocks the app** without typing or pasting any key!

4. **Zero Half-Work & 360° Propagation:**
   * Any change to licensing or updates MUST be kept in sync across `screen_reco` (Android Kotlin), `Prince-portfolio` (React/Vite admin dashboard), and GitHub releases.

---

## 4. In-App Auto-Update (OTA) Mechanism

* **How it works:**
  1. `MainScreenViewModel.kt` invokes `AppUpdateManager.checkForUpdates(silent = true)` on app launch.
  2. Queries `https://api.github.com/repos/prince-dev777/snap-loom/releases/latest`.
  3. Compares semantic version: if `remoteTag > currentVersion`, pops up:
     `"Update Available! (Snap Loom vX.Y.Z)"` with *"What's New"* notes.
  4. User taps **"Update Now"**:
     * Android `DownloadManager` downloads `SnapLoom-vX.Y.Z.apk` to Downloads folder.
     * System notification alerts user when ready; tapping prompts system package installer (`REQUEST_INSTALL_PACKAGES` + `FileProvider`).
     * Fallback opens browser download URL.

---

## 5. Development, Build & Release Playbook

### Daily Build Commands (PowerShell)
```powershell
# Test compilation quickly (fast)
.\gradlew.bat compileDebugKotlin

# Assemble Debug APK
.\gradlew.bat assembleDebug

# Clean cache when Gradle gets stuck
.\gradlew.bat clean
```

### Complete Release Workflow (Publishing a New Version)
To release an update (e.g., `v1.2.0`):
1. **Bump Version in `app/build.gradle.kts`:**
   ```kotlin
   versionCode = 3
   versionName = "1.2.0"
   ```
2. **Build Debug APK:**
   ```powershell
   .\gradlew.bat assembleDebug
   ```
3. **Copy APK to Root for Artifact Distribution:**
   ```powershell
   Copy-Item "app\build\outputs\apk\debug\app-debug.apk" -Destination "SnapLoom.apk" -Force
   ```
4. **Create GitHub Release (Attaching APK):**
   ```powershell
   gh release create v1.2.0 SnapLoom.apk --repo prince-dev777/snap-loom --title "Snap Loom v1.2.0 - [Release Title]" --notes "[Release Notes]"
   ```
5. **Git Commit & Push:**
   ```powershell
   git add .
   git commit -m "Release Snap Loom v1.2.0"
   git push origin main
   ```
*(Note: Portfolio download link is pinned to `https://github.com/prince-dev777/snap-loom/releases/latest/download/SnapLoom.apk`, so it automatically serves the newest release without manual edits).*

---

## 6. Common Gotchas & Troubleshooting

* **PowerShell Command Separator:** Windows PowerShell does NOT support `&&`. Always use `;` to chain commands (e.g., `cmd1 ; cmd2`).
* **Duplicate Icon Resources:** Android AAPT2 fails if both `.png` and `.webp` of the same name exist in `res/mipmap-*`. Always remove conflicting `.webp` files when replacing icons.
* **Vercel Browser Cache:** If `/admin` shows "Incorrect password" after updating credentials, the browser is serving stale cached CRA bundles. Tell user to hard-refresh with `Ctrl + Shift + R`.
* **Firestore REST API:** Raw Android REST calls to Firestore require `?key=$API_KEY` query parameter for unauthenticated client endpoints matching public read/write rules.
