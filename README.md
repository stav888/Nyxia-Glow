# Nyxia Glow

Nyxia Glow is a native Android camera studio built with Kotlin, Jetpack Compose, CameraX, and MediaPipe Tasks Vision. It provides a live camera preview with front/rear switching, zoom, hardware-aware torch control, ambient-light guidance, beauty preset state, texture-preservation state, gallery picking, and MediaStore photo capture.

## Current scope

- CameraX binds `Preview`, `ImageCapture`, and one `ImageAnalysis` use case.
- Ambient luminance is sampled from each analysis frame and mapped to lighting guidance.
- MediaPipe Face Landmarker runs in live-stream mode, with GPU initialization and CPU fallback.
- CameraX YUV frames are converted to RGBA bitmaps because the tested device rejects direct YUV MediaImages.
- Android 10+ uses scoped MediaStore storage. Android 8/9 requests legacy storage only when the shutter is pressed.
- The main Compose surface follows the downloaded AuraSync Stitch direction with a dark viewfinder, coral controls, and a functional Retouch dashboard.
- The live preview runs through a real GPU pipeline: CameraX `Preview` feeds a
  `BeautyCameraRenderer` (GLSurfaceView) that applies `beauty_shader.glsl`
  (`uGlowStrength`, `uSmoothStrength`, `uLipColor`, `uBlushColor`,
  `uLipStrength`, and `uBlushStrength`). `GlowLooks` drives those live lip and
  blush uniforms, while the MediaPipe-driven makeup mask (`MakeupMaskGenerator` →
  `uMakeupMask`: lip layer in R, blush layer in G).
- Photo capture currently uses a separate CameraX `ImageCapture` use case, so the
  saved JPEG is the raw camera frame WITHOUT the live shader/makeup effect
  (known product bug, see `app_edit.md`).
- Shape tool has no geometry effect yet. Product catalog search is not implemented.

## Requirements

- JDK 17
- Android SDK platform and build tools for API 35
- Android 8.0+ device or emulator with a camera for the full flow
- USB debugging enabled for ADB installation

Create `local.properties` with the Android SDK path if Android Studio has not created it. On Windows:

```properties
sdk.dir=C\:\Users\<user>\AppData\Local\Android\Sdk
```

## Build and test

From the repository root:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Install with ADB

```powershell
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.nyxiaglow.app/.MainActivity
```

To inspect launch and camera errors:

```powershell
adb logcat -c
adb shell am force-stop com.nyxiaglow.app
adb shell am start -n com.nyxiaglow.app/.MainActivity
adb logcat -d | Select-String "FATAL EXCEPTION|AndroidRuntime|com.nyxiaglow.app"
```

## Repository guide

- `app/src/main/java/com/nyxiaglow/app/MainActivity.kt`: Compose UI, permissions, CameraX binding, capture, and controls.
- `app/src/main/java/com/nyxiaglow/app/camera/FaceLandmarkAnalyzer.kt`: ambient-light sampling, YUV conversion, and MediaPipe face landmarks.
- `app/src/main/java/com/nyxiaglow/app/camera/LightingState.kt`: pure ambient-light label mapping.
- `app/src/main/java/com/nyxiaglow/app/camera/BeautyCameraRenderer.kt`: live GL renderer (glow + smoothing + makeup mask).
- `app/src/main/res/raw/beauty_shader.glsl`: fragment shader for the live preview.
- `app/src/main/java/com/nyxiaglow/app/camera/MakeupMaskGenerator.kt`: lip/blush mask from landmarks.
- `app/src/test/java/com/nyxiaglow/app/camera/LightingStateTest.kt`: lighting boundary tests.
- `app/src/main/assets/face_landmarker.task`: MediaPipe Face Landmarker model.
- `app_edit.md`: detailed implementation and UI reference.

GitHub Actions runs unit tests, lint, and the debug build for pushes to `main` and pull requests.
