# Quick Flip Actions – Development Roadmap (Kotlin / Android)

This roadmap guides you from zero to a working, publishable version of **Quick Flip Actions**, your gesture-based automation Android app.

---

## 0. Prerequisites & Project Setup ✅ **COMPLETED**

- **Install tools** ✅
  - Android Studio (latest stable) with Kotlin support.
  - Java 17 (or version recommended by latest Android Studio).
- **Create project** ✅
  - New Project → Empty Activity.
  - Language: Kotlin.
  - Min SDK: 26 or 28 (balance between market share and features).
  - Package name consistent with app name (`com.praise.quickflipactions`).
- **Configure version control** ✅
  - Initialize Git repo (if not already).
  - Commit initial Android Studio project.
- **High-level modules (conceptual)** ✅
  - `core-sensors`: sensor reading and gesture detection.
  - `core-actions`: definition and execution of actions (notes, audio, camera, etc.).
  - `app-ui`: configuration screens, onboarding, permissions.

**Additional Completed:**
- ✅ Custom app icon designed and implemented
- ✅ App successfully running on Pixel 7 device
- ✅ Basic project structure with MainActivity

---

## 1. Requirements & Architecture Design

- **Clarify functional scope (MVP)**
  - Support gestures:
    - Face-up → Face-down.
    - Face-down → Face-up.
    - (Optional MVP) A basic double-tap on screen to deactivate.
  - Support actions:
    - Silent/normal mode toggle.
    - Flashlight on/off.
    - Launch a specific app.
  - Basic mapping UI: user selects gesture → selects one action.
- **Non-functional requirements**
  - Run efficiently in background.
  - Minimize battery usage.
  - Avoid false triggers (pocket, random motion).
- **High-level architecture**
  - Use **Service** or **Foreground Service** to listen to sensors when enabled.
  - `SensorManager` + `SensorEventListener` for accelerometer & gyroscope.
  - Local storage for user configuration (e.g., `DataStore` or Room later).
  - Use MVVM or clean-ish architecture for separation of concerns.
- **Data models (draft)**
  - `GestureType` (enum): `FACE_UP_TO_DOWN`, `FACE_DOWN_TO_UP`, `DOUBLE_TAP`.
  - `ActionType` (enum): `TOGGLE_SILENT_MODE`, `TOGGLE_FLASHLIGHT`, `LAUNCH_APP`, `VOICE_MEMO`, `SOS_MESSAGE`, etc.
  - `GestureActionMapping`: gesture → selected action (+ any parameters like target app, SOS contacts).

Create a simple diagram (even on paper) of data flow: **Sensors → GestureDetector → MappingResolver → ActionExecutor**.

---

## 2. Permissions & Basic App Skeleton

- **Identify required permissions**
  - Sensors: usually no special runtime permission, but check docs.
  - Flashlight: `android.permission.CAMERA` or `android.permission.FLASHLIGHT` depending on API.
  - Audio recording: `RECORD_AUDIO`.
  - Location (for live location / SOS): `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`.
  - SMS or sending messages (optional, careful for Play Store policies): `SEND_SMS`.
- **Implement permission handling**
  - Centralize runtime permission requests in Activity / separate helper.
  - Show rationale and simple onboarding screen explaining why each permission is needed.
- **Create navigation structure**
  - Main Activity with bottom navigation or simple single-screen + settings.
  - Screens (at least):
    - Home/Status screen: enable/disable gesture service.
    - Gesture mapping screen.
    - Settings screen (battery optimization hints, sensitivity options later).

---

## 3. Sensor Reading & Orientation Detection

- **Implement sensor layer**
  - Create a `SensorController` or similar class:
    - Registers for accelerometer and gyroscope events.
    - Normalizes sensor values.
  - Handle lifecycle properly:
    - Start listening when service is started.
    - Stop listening when service is stopped.
- **Compute orientation (face-up / face-down)**
  - Use accelerometer Z-axis relative to gravity to classify orientation.
  - Define thresholds and debounce logic:
    - Example: consider face-up if `z > thresholdUp` for a steady duration.
    - Face-down if `z < thresholdDown` for a steady duration.
  - Store last stable orientation and trigger events when orientation changes.
- **Add basic logging & debugging UI**
  - Temporary debug screen or logcat logs showing:
    - Raw accelerometer values.
    - Detected orientation changes.

Goal of this phase: **reliable detection of face-up vs face-down** with minimal noise.

---

## 4. Flip Gesture Detection Logic

- **Define what a “flip” is**
  - Face-up → Face-down transition within a certain time window.
  - Face-down → Face-up transition within a certain time window.
- **Implement flip detection**
  - Use state machine:
    - `IDLE` → `FACE_UP` / `FACE_DOWN`.
    - If orientation changes and time difference < maxFlipDuration → emit flip event.
  - Use gyroscope to filter out slow rotations vs intentional flips.
- **Prevent accidental triggers**
  - Require motion intensity threshold (from gyroscope or accelerometer magnitude).
  - Add minimum time between flips.
  - Optional: disable when proximity sensor indicates pocket (if available).
- **Expose clean API**
  - `GestureDetector` emits `onGestureDetected(GestureType)` to higher layers.

Test flip detection thoroughly with emulator & real device.

---

## 5. Background Service & App Lifecycle

- **Choose background mechanism**
  - Likely a **Foreground Service** with persistent notification (for reliability on modern Android).
- **Implement service**
  - Start/stop from UI (toggle on home screen).
  - Service holds `SensorController` + `GestureDetector`.
  - On gesture detected → delegate to `ActionExecutor`.
- **Handle battery optimization**
  - Detect if app is battery-optimized and show instructions to user to exclude app if needed.
- **Test lifecycle scenarios**
  - App in foreground, background, killed.
  - Device reboot (optional later: use `BOOT_COMPLETED` to restart service if user enabled).

---

## 6. Action System (MVP)

- **Design action execution layer**
  - `ActionExecutor` that takes `ActionType` + params and performs the action.
- **Implement system actions first (low friction)**
  - Toggle silent/normal mode.
  - Flashlight on/off.
  - Launch specific app by package name.
- **Add simple configuration UI**
  - Screen where user:
    - Picks a gesture from list (face-up→face-down, face-down→face-up).
    - Chooses an action from list.
    - For `LAUNCH_APP`, opens an app picker.
- **Persist mappings**
  - Use `DataStore` or SharedPreferences as a first step.
  - Store `GestureActionMapping` list and load at service startup.

Goal: **You can flip the phone and see a real action (e.g., flashlight) happening.**

---

## 7. Extended Actions (Notes, Audio, Camera, Location)

Once MVP is stable, extend actions one by one.

- **Voice memo recording**
  - Implement a lightweight audio recorder using `MediaRecorder` or preferred API.
  - Define storage path and file naming.
  - Decide behavior: start recording on flip, stop on next flip or from notification.
- **Task timer**
  - Simple `CountDownTimer` or chronometer that starts on gesture.
  - Basic UI to show active timers & history.
- **Camera actions**
  - Integrate CameraX or system intent for photo/video.
  - For quick photo: open camera in background or start camera activity.
  - For video: toggle record start/stop.
- **Location & safety actions**
  - Live location share: get last known location or request single update.
  - SOS message: send SMS or share via intent to messaging apps with current location link.
  - Save current location: store in local DB for history.

Add each capability incrementally and test permissions, error handling, and UX.

---

## 8. UI/UX Polish & Accessibility

- **Refine configuration UI**
  - Clear descriptions of gestures and default mappings.
  - Indicate when service is active/inactive.
- **Onboarding flow**
  - First-time launch screens:
    - Explain app concept.
    - Request critical permissions with explanations.
    - Quick setup: choose one or two default mappings.
- **Accessibility considerations**
  - Large touch targets, clear labels.
  - Dark mode support.
  - Consider vibration or sound feedback on successful gesture detection.

---

## 9. Reliability, Testing & Optimization

- **Testing**
  - Unit tests for gesture detection logic (state machine, thresholds).
  - Instrumentation tests for Service + ActionExecutor if possible.
  - Manual tests on real device in different contexts:
    - Walking, sitting, in pocket, in bag.
- **False positive reduction**
  - Tune thresholds and time windows.
  - Optionally expose “sensitivity” slider in settings.
- **Performance & battery**
  - Use Profiler to monitor CPU and battery usage.
  - Reduce sensor sampling rate if possible.
  - Unregister listeners when not needed.

---

## 10. Security, Privacy & Data Handling

- **Data storage**
  - Define what user data you store:
    - Voice memos.
    - Locations.
    - Mappings.
  - Provide clear settings to clear data.
- **Permissions transparency**
  - In settings, list all permissions with explanation and ability to open system settings.
- **Backup & restore (optional)**
  - Consider exporting/importing mappings and basic settings.

---

## 11. Pre-Release & Play Store Preparation

- **Branding**
  - Choose final app name, icon, and color theme.
- **Build variants**
  - Create `debug` and `release` build types with proper signing config.
- **Internal testing**
  - Generate signed APK/AAB.
  - Test on multiple devices / emulators.
- **Play Store assets**
  - Write app description (short & full).
  - Take screenshots and record a short demo.
  - Prepare privacy policy (especially for location/audio/SMS).

---

## 12. Release, Feedback & Iteration

- **Initial rollout**
  - Publish to Play Store with staged rollout (e.g., 20–50%).
  - Monitor crash reports (Firebase Crashlytics or Play Console).
- **Feedback loop**
  - Add in-app feedback link (email or form).
  - Collect which gestures/actions are used most.
- **Post-release roadmap (future ideas)**
  - Context-aware gestures (time of day, location, connected device).
  - Cloud backup of settings.
  - Community presets (share gesture/action profiles).

---

## How to Use This Roadmap

- Work through sections **in order**, but you can overlap small tasks (e.g., UI polish while running tests).
- At the start of each week or session, pick a subsection (e.g., *4. Flip Gesture Detection Logic*) and focus only on completing that.
- Update this file with notes, decisions, and TODOs as you learn more from testing.
