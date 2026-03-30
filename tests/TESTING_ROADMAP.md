# Quick Flip Actions – Testing Roadmap

This testing roadmap mirrors the main `ROADMAP.md` and describes **what to test**, **which type of test to use**, and **when** to add those tests as you build the app.

Use it as a checklist alongside the main roadmap.

---

## 0. Testing Prerequisites & Environment Setup

- **[Goal] Testing tooling ready**
  - **Unit tests** using JUnit4/JUnit5 and AndroidX test.
  - **Instrumentation/UI tests** using Espresso (optional for MVP UI).

- **[Actions]**
  - **Configure test dependencies** in `app/build.gradle.kts`:
    - **Unit tests**: `testImplementation("junit:junit:4.13.2")` (already present), later add mocking lib if needed.
    - **Android tests**: `androidTestImplementation("androidx.test.espresso:espresso-core:…")` (already present).
  - **Verify test tasks** run:
    - Run `./gradlew testDebugUnitTest` (or use Android Studio **Run tests** on a sample test).
    - Run `./gradlew connectedDebugAndroidTest` (optional for now).

- **[Smoke test]**
  - Create a **dummy unit test** in `src/test/java/...` that always passes, to confirm setup.

---

## 1. Requirements & Architecture Design – Conceptual Tests

At this stage you define *what* to test, not full implementations.

- **[Goal] Testing strategy defined per module**
  - **core-sensors** (orientation + gesture detection): unit tests with fake sensor input.
  - **core-actions** (actions triggered): unit tests with mocked Android services.
  - **service layer** (GestureService): integration-style tests + manual tests on device.
  - **app-ui**: light instrumentation tests for critical flows (start/stop detection).

- **[Actions]**
  - **Write test design notes** in this file for each future section (see below).
  - Define **naming convention** for tests, e.g.:
    - Unit tests: `GestureDetectorTest`, `SensorControllerTest`, `ActionExecutorTest`.
    - Service tests: `GestureServiceTest` (instrumented or robolectric later).

---

## 2. Permissions & Basic App Skeleton – Tests

- **[Goal] Permissions and basic navigation behave correctly.**

- **Unit / small tests**
  - **[Low priority at start]** No heavy unit tests here; most logic is Android framework–driven.

- **Instrumentation / manual tests**
  - **[Test] App launches without crash**
    - Launch `MainActivity` on device/emulator.
    - Verify status text shows **INACTIVE** state.
  - **[Test] Start/Stop buttons**
    - Tap **Start** → foreground notification appears, Stop enabled.
    - Tap **Stop** → service stops, notification removed, Start enabled.
  - **[Test] Permissions prompts (later when you request runtime ones)**
    - When actions need camera/microphone/location, verify permission dialogs show with correct rationale.

---

## 3. Sensor Reading & Orientation Detection – Tests

Module under test: `SensorController` + orientation logic inside `GestureDetector`.

- **Unit tests (high priority)**
  - **File**: `GestureDetectorOrientationTest` in `src/test/java/...`
  - **Tests:**
    - **[Test] Face-up detection**
      - Given accelerometer Z ≈ +9.8, expect orientation `FACE_UP`.
    - **[Test] Face-down detection**
      - Given accelerometer Z ≈ -9.8, expect orientation `FACE_DOWN`.
    - **[Test] Unknown orientation**
      - Given accelerometer Z between thresholds, expect `UNKNOWN`.
    - **[Test] Orientation change triggers internal state update**
      - Simulate sequence of readings and assert `getCurrentOrientation()`.

- **Instrumentation / manual tests**
  - Add temporary **debug logging** (already present) and:
    - **[Test] Log orientation changes** by slowly rotating phone.
    - Confirm logs like `Orientation changed to: FACE_UP/FACE_DOWN` appear as expected.

---

## 4. Flip Gesture Detection Logic – Tests

Module under test: `GestureDetector` (flip logic, thresholds, timing) and its use by the session state machine.

- **Unit tests (critical)** – `GestureDetectorFlipTest`
  - **[Test] Face-up → Face-down within max duration → emits `FACE_UP_TO_DOWN`**
    - Simulate orientation sequence with timestamps respecting `MAX_FLIP_DURATION_MS`.
  - **[Test] Face-down → Face-up within max duration → emits `FACE_DOWN_TO_UP`**
    - Similar simulation reversed.
  - **[Test] Slow orientation change beyond max duration → no gesture**
  - **[Test] Flip without sufficient gyroscope motion → no gesture**
  - **[Test] Multiple flips with cooldown respected**
    - Ensure second flip inside `MIN_TIME_BETWEEN_FLIPS_MS` is ignored.

- **Unit tests – Session state machine (single active action)** – `GestureSessionStateTest`
  - Model a simple state enum: `IDLE`, `ARMED`, `ACTION_ACTIVE`.
  - **[Test] Arm action**
    - Given `IDLE`, when user selects an action and taps Start → state becomes `ARMED` with that `ActionType` stored.
  - **[Test] Start action on face-down**
    - Given `ARMED` and gesture `FACE_UP_TO_DOWN`:
      - State changes to `ACTION_ACTIVE`.
      - `ActionExecutor.executeAction` is invoked to start the selected action.
  - **[Test] Stop action and disable detection on face-up**
    - Given `ACTION_ACTIVE` and gesture `FACE_DOWN_TO_UP`:
      - State returns to `IDLE`.
      - `ActionExecutor` is invoked to stop/revert the action (or toggle back).
      - Service stop / gesture detection stop is requested.
  - **[Test] Ignore irrelevant gestures in each state**
    - Example: `FACE_DOWN_TO_UP` while in `ARMED` does nothing; `FACE_UP_TO_DOWN` while in `ACTION_ACTIVE` does nothing.

- **Manual tests (on device)**
  - With detection service running:
    - **[Test] Normal flip**: Flip quickly → gesture logged.
    - **[Test] Slow tilt**: Slowly rotate phone → no gesture.
    - **[Test] Random movement in pocket** (later) → check for minimal false detections.

---

## 5. Background Service & App Lifecycle – Tests

Module under test: `GestureService` + interaction with `MainActivity`.

- **Instrumentation tests** – `GestureServiceInstrumentedTest` (later)
  - **[Test] Service starts in foreground** when Start button clicked.
  - **[Test] Notification present** with correct text when active.
  - **[Test] Service stops and notification removed** when Stop clicked.

- **Manual tests**
  - **[Test] App in background** – start detection then switch apps; ensure notification persists and service not killed immediately.
  - **[Test] Screen off** – lock phone; check if gestures still processed (later when stable).

---

## 6. Action System (MVP) – Tests

Module under test: `ActionExecutor`.

- **Unit tests** – `ActionExecutorTest`
  - Use mocking (e.g. Mockito/MockK later) for `AudioManager`, `CameraManager`, `PackageManager`.
  - **[Test] TOGGLE_SILENT_MODE**
    - When ringer is NORMAL → should set to SILENT.
    - When ringer is SILENT/VIBRATE → should set to NORMAL.
  - **[Test] TOGGLE_FLASHLIGHT**
    - With available camera ID → toggles torch, flips internal `flashlightOn` flag.
  - **[Test] LAUNCH_APP**
    - With valid package → calls `startActivity` with proper intent.
    - With invalid package → logs warning, does not crash.
  - **[Test] Disabled mapping**
    - If `isEnabled == false`, `executeAction` returns without calling any underlying method.

- **Integration tests (later)**
  - Simple instrumentation test that calls `ActionExecutor` in a controlled environment.

---

## 7. Extended Actions (Notes, Audio, Camera, Location) – Tests

As each feature is implemented, add tests.

- **Voice memo recording**
  - **Unit tests** for recorder wrapper (no real mic access).
  - **Instrumentation/manual**: verify file created, playback works, permissions handled.

- **Task timer**
  - Unit tests for timer logic (start/stop, elapsed time).

- **Camera actions**
  - Manual tests to ensure intents or CameraX flows work; later UI tests for happy paths.

- **Location & safety**
  - Unit tests for location formatting, message building.
  - Manual tests with real GPS to ensure location is acquired and shared.

---

## 8. UI/UX Polish & Accessibility – Tests

- **Instrumentation/UI tests**
  - **[Test] Start/Stop buttons enabled/disabled states**.
  - **[Test] Status text updates** when toggling detection.

- **Manual accessibility tests**
  - Check font sizes, contrast, button hit areas.
  - Test dark mode support if implemented.

---

## 9. Reliability, Testing & Optimization – Tests

- **Regression test suite**
  - Run **all unit tests** before each major commit.
  - Run **device tests** (manual) on at least one low-end and one high-end device, if possible.

- **Stress tests (manual)**
  - Shake/flip the phone repeatedly; monitor for crashes or ANRs.
  - Long-running test: leave service active for several hours; check battery impact.

---

## 10. Security, Privacy & Data Handling – Tests

- **Unit tests**
  - Verify that data-clearing functions actually delete stored items (memos, locations, mappings).

- **Manual tests**
  - Confirm that revoking permissions in system settings is handled gracefully (no crashes).

---

## 11. Pre-Release & Play Store Preparation – Tests

- **Pre-release test checklist**
  - All critical unit tests passing.
  - Basic instrumentation tests green.
  - Manual smoke tests executed on real device(s):
    - Install from release build.
    - Start detection, test both flip gestures.
    - Try all enabled actions.

---

## 12. Release, Feedback & Iteration – Tests

- **Post-release monitoring**
  - Track crashes via Play Console / Crashlytics.
  - Add regression tests for any bug found in production.

- **Continuous improvement**
  - When adding a new gesture or action, **first** add/extend tests in:
    - `GestureDetector*Test` (for detection logic).
    - `ActionExecutorTest` (for action behavior).

---

## How to Use This Testing Roadmap

- **Per roadmap section**, before coding a feature, skim the matching testing section here.
- **After implementing a feature**, immediately add or update its tests.
- Treat this file as **living documentation**: update it when you change design, add new gestures, or refine actions.
