# Nyxia Glow App Reference

## 1. Overview

Nyxia Glow is a native Android beauty-camera studio built with Kotlin, Jetpack Compose, CameraX, and MediaPipe Tasks Vision. The app opens directly into a portrait camera experience after the required permissions are granted.

The current implementation provides:

- Live CameraX preview.
- Front and rear camera switching.
- Ambient-light sampling and semantic lighting guidance.
- Camera zoom selection with hardware-range clamping.
- Hardware-aware torch control.
- Beauty preset state and visual glow intensity.
- Natural-texture toggle state.
- Photo capture into the Android MediaStore.
- Android photo picker access.
- MediaPipe Face Landmarker initialization and live landmark callbacks.
- Settings/status feedback surfaces.
- Stitch-inspired dark AuraSync viewfinder styling with AI Active and 4K RAW status capsules.
- Stitch-inspired Retouch dashboard with tool categories, preset strip, smoothing control, micro-texture toggle, reset, and apply actions.

The live preview runs through a real GPU pipeline: CameraX `Preview` feeds
`BeautyCameraRenderer` (GLSurfaceView), which applies `beauty_shader.glsl` to
every frame — glow (`uGlowStrength`), smoothing (`uSmoothStrength`), and a
MediaPipe-driven makeup mask (`MakeupMaskGenerator` uploads lip coverage in the
R channel and blush in the G channel to `uMakeupMask`).

Still UI-only (no pixel effect): the Shape tool, color differences between
Retouch presets (Smooth/Freckles/Matte/Dewy/Refine currently change state
only — lip/blush colors are still hardcoded in GLSL), preset names without a
color palette, and Looks without a product catalog. Photo capture uses a
separate `ImageCapture` use case, so the saved JPEG is the raw camera frame
without the live effect (known product bug).

## 2. Repository Structure

```text
Nyxia-Glow/
  app/
    src/main/
      AndroidManifest.xml
      assets/
        face_landmarker.task
      java/com/nyxiaglow/app/
        MainActivity.kt
        camera/
          FaceLandmarkAnalyzer.kt
          LightingState.kt
      res/values/
        styles.xml
    src/test/java/com/nyxiaglow/app/camera/
      LightingStateTest.kt
    build.gradle.kts
  .github/workflows/android.yml
  build.gradle.kts
  gradle.properties
  gradlew
  gradlew.bat
  settings.gradle.kts
  README.md
  app_edit.md
```

`local.properties` is intentionally local-only and must contain the Android SDK path on a developer machine. It is ignored by Git.

## 3. Android Configuration

The module is an Android application with:

- Namespace: `com.nyxiaglow.app`
- Application ID: `com.nyxiaglow.app`
- Minimum SDK: 26, Android 8.0
- Target SDK: 35
- Compile SDK: 35
- Java source and target: 17
- Kotlin JVM target: 17
- Version code: 2
- Version name: 1.1
- Portrait-only main activity
- Jetpack Compose enabled

The manifest declares:

- `android.permission.CAMERA`
- `android.permission.WRITE_EXTERNAL_STORAGE` only through API 28
- Optional camera hardware, so the app can still install on devices without a camera and report the unavailable state at runtime

## 4. Dependencies

The app uses these main dependency groups:

- CameraX 1.4.1: camera2, lifecycle binding, preview view, image capture, and image analysis.
- MediaPipe Tasks Vision 0.10.26: Face Landmarker.
- AndroidX Core KTX 1.15.0.
- Activity Compose 1.10.0.
- Lifecycle Runtime Compose 2.8.7.
- Compose BOM 2024.12.01.
- Compose UI, graphics, tooling preview, Material 3, and extended Material icons.
- Kotlin test for JVM unit tests.

The official MediaPipe model is stored at:

```text
app/src/main/assets/face_landmarker.task
```

## 5. Build and Run

### Requirements

- Android Studio or a command-line Android SDK installation.
- JDK 17.
- Android SDK platform and build tools for API 35.
- An Android 8.0+ device or emulator with a camera for the full camera flow.
- USB debugging enabled for ADB installation.

### Configure the SDK

Create `local.properties` in the repository root with the local SDK path. Windows example:

```properties
sdk.dir=C\\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
```

### Gradle commands

From the repository root:

```powershell
.\\gradlew.bat testDebugUnitTest
.\\gradlew.bat lintDebug
.\\gradlew.bat assembleDebug
.\\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

The APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Install with ADB

```powershell
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.nyxiaglow.app/.MainActivity
```

To inspect runtime errors:

```powershell
adb logcat -c
adb shell am force-stop com.nyxiaglow.app
adb shell am start -n com.nyxiaglow.app/.MainActivity
adb logcat -d | Select-String "FATAL EXCEPTION|AndroidRuntime|com.nyxiaglow.app"
```

## 6. Automated Validation

GitHub Actions is defined in `.github/workflows/android.yml`. It runs on pushes to `main` and pull requests.

The workflow:

1. Checks out the repository.
2. Installs JDK 17.
3. Installs/configures the Android SDK.
4. Runs `testDebugUnitTest`.
5. Runs `lintDebug`.
6. Builds `assembleDebug`.

The current JVM test covers the boundary behavior of semantic lighting labels in `LightingStateTest`.

## 7. Application Entry Flow

`MainActivity` creates the Compose content and applies `NyxiaGlowTheme`.

`NyxiaGlowApp` owns permission state:

1. It checks camera permission.
2. It shows `PermissionPrompt` until camera permission is granted.
3. The permission dialog is opened only after the user presses `Enable camera`.
4. After permission is granted it renders `GlowStudio`.

On Android 8 or 9, `WRITE_EXTERNAL_STORAGE` is requested only when the user presses the shutter. Android 10 and newer use MediaStore scoped storage and do not request that legacy permission.

## 8. Main UI

### Camera studio layout

The live studio is a full-screen layered Compose layout:

1. CameraX preview fills the screen.
2. A vertical dark gradient improves contrast for controls.
3. The top bar contains the Aura logo, app name, screen label, flash, and settings.
4. The center overlay contains the Glow Engine status and temporary neural-focus reticle.
5. The texture control appears below the reticle.
6. Preset and zoom controls appear above the capture row.
7. Gallery, shutter, and flip controls form the capture row.
8. A dark bottom navigation surface contains Gallery, Looks, Camera, and Profile.

### Visual design system

- Coral: `#FF9A8B`
- Deep coral: `#96463B`
- Mist: `#F5F1F0`
- Ink: `#111111`
- Muted gray: `#595F65`
- White: `#FFFFFF`

The UI uses dark translucent camera overlays, coral selected states, rounded pills, a circular shutter action, and a dark bottom navigation panel. The live feed remains CameraX-backed; downloaded Stitch screenshots are design references, not the camera background.

### Icon alignment and touch targets

- Top-bar icons are placed in a dedicated horizontal row.
- Gallery, shutter, and flip controls use one centered horizontal row with stable circular dimensions.
- Bottom navigation uses evenly distributed columns and a fixed elevated camera button.
- Presets use a horizontally scrollable row so labels do not force the layout off-screen.
- Icon buttons include content descriptions for TalkBack.
- Selected controls expose semantic selected state descriptions.

## 9. State Owned by GlowStudio

`GlowStudio` currently owns:

- `glow`: selected preset intensity used by the reticle arc.
- `ambient`: normalized luminance from the camera analyzer.
- `preset`: selected look name.
- `zoom`: selected zoom label.
- `facing`: front or rear CameraX lens.
- `flashOn`: requested torch state.
- `flashAvailable`: whether the active camera has a flash unit.
- `activeTab`: selected bottom-navigation label.
- `preserveTexture`: texture toggle state.
- `reticleVisible`: two-second interaction indicator.
- `captureRequest`: capture event counter.
- `captureLock`: atomic protection against double-tap capture requests.
- `capturedUri`: most recent imported or captured image URI.
- `statusMessage`: user-facing capture, selection, or error feedback.
- `showSettings`: settings dialog visibility.

State is currently composable-local. It is not persisted across process death and is not yet extracted into a ViewModel.

## 10. CameraX Pipeline

`CameraPreview` creates and owns:

- A remembered `PreviewView`.
- A remembered `ImageCapture` use case.
- A single `ImageAnalysis` use case.
- A single-thread analysis executor.
- The active CameraX `Camera` reference.
- A `FaceLandmarkAnalyzer` used by the analysis use case.

The bound use cases are:

```text
Preview + ImageCapture + ImageAnalysis
```

The single analysis use case performs both ambient-light sampling and face landmark analysis. Keeping one analysis use case avoids exceeding device-specific camera stream limits.

When the lens changes:

1. The previous effect marks itself disposed.
2. A stale asynchronous provider callback exits without binding.
3. The active camera is cleared.
4. The previous Face Landmarker is closed.
5. The new lens is bound to the lifecycle.
6. Flash capability is reported to the UI.

The executor remains active across lens changes and is shut down only when the composable leaves composition.

## 11. Ambient Light Analysis

The first Y plane of each CameraX frame is sampled from a duplicated buffer. The analyzer:

- Samples no more than 120 positions.
- Normalizes the average value to `0.0..1.0`.
- Updates Compose state through `onLightChanged`.
- Closes every `ImageProxy`.
- Runs alongside the Face Landmarker inside the same analyzer.

The semantic labels are:

| Ambient range | Label |
|---|---|
| `< 0.20` | Low light - glow boosted |
| `0.20..0.59` | Balanced light |
| `0.60..0.84` | Bright - highlights softened |
| `>= 0.85` | Harsh light - smoothing adjusted |

The pure mapping is implemented in `camera/LightingState.kt` and tested by `LightingStateTest`.

## 12. Face Landmarker Pipeline

`FaceLandmarkAnalyzer`:

1. Loads `face_landmarker.task` from app assets.
2. Tries the MediaPipe GPU delegate first and falls back to the CPU delegate if GPU initialization fails.
3. Runs in `LIVE_STREAM` mode.
4. Tracks one face.
5. Converts CameraX YUV planes to an NV21 image and decodes an RGBA bitmap because the tested device rejects direct YUV MediaImages.
6. Throttles face inference to approximately 10 frames per second.
7. Passes CameraX rotation through `ImageProcessingOptions`.
8. Calls `onLandmarksDetected` when MediaPipe returns a result.
9. Calls `onError` for initialization, frame conversion, or MediaPipe errors.
10. Closes each `ImageProxy` exactly once through an outer `finally` block.
11. Keeps the bitmap alive for asynchronous MediaPipe processing instead of recycling it immediately after `detectAsync`.
12. Closes the landmarker when the camera effect is disposed.

Landmark points drive the live makeup mask: `CameraPreview` passes each
`FaceLandmarkerResult` to `MakeupMaskGenerator.generateMask()`, uploads the
bitmap via `renderer.updateMakeupMask()`, and the shader composites lip color
(mask R) and blush (mask G) over the camera texture. When no face is present
the mask is cleared. Smoothing intensity reaches `uSmoothStrength`
(scaled ×0.5 while texture preservation is on).

## 13. User Functionality

### Permission

`Enable camera` requests camera permission only. On Android 8 or 9, storage permission is requested at capture time, after the user presses the shutter. Denial leaves the camera available and reports that the photo could not be saved.

### Flash

The flash icon requests torch state only when the active camera exposes a flash unit. Unsupported hardware disables the control. The result is sent through CameraX camera control.

### Settings

The settings icon opens a dialog showing current texture-preservation state and the hardware-linked camera behavior.

### Presets

The four presets are:

- Soft: 68%
- Radiant: 86%
- Velvet: 34%
- Defined: 57%

Selection updates the reticle arc, selected styling, and the live `uGlowStrength`
uniform (Soft 68%, Radiant 86%, Velvet 34%, Defined 57%).

### Zoom

The available choices are `0.5x`, `1x`, `2x`, and `3x`. The selected value is clamped to the active camera's actual `ZoomState` range before calling `setZoomRatio`.

### Texture preservation

The control scales the live `uSmoothStrength` uniform (×0.5 while On, ×1.0 while
Off). There is no TFLite model behind it — smoothing is the GL blur in
`beauty_shader.glsl`.

### Gallery picker

Gallery actions launch Android's `GetContent` picker for `image/*`. The selected URI is stored in Compose state and a status message confirms selection. There is not yet a dedicated gallery grid screen.

### Capture

The shutter uses CameraX `ImageCapture` and MediaStore:

- Android 10+: stores under `Pictures/Nyxia Glow` using `RELATIVE_PATH` and `IS_PENDING`.
- Android 8/9: uses MediaStore with the legacy storage permission.
- Android 8/9 requests the legacy permission only for the capture action.
- A unique timestamped JPEG name is generated.
- Failed captures delete the pending URI and report an error.
- An atomic capture lock prevents overlapping double-tap requests.

### Retouch dashboard

The Retouch tab is a functional Compose surface reachable from the Looks item in bottom navigation. It exposes Skin, Shape, Light, and Makeup tool categories, Smooth/Freckles/Matte/Dewy/Refine preset choices, a smoothing-intensity control, the micro-texture toggle, Reset, and Apply & Save feedback. The smoothing slider and texture toggle drive the live shader (see above).
Preset choices, tool tabs (Skin/Shape/Light/Makeup), Reset, and Apply & Save
currently update UI state and status only; the Shape tab has no effect at all.

### Flip camera

The flip icon swaps front and rear lens selectors. CameraX is unbound and rebound through the guarded lifecycle effect.

### Bottom navigation

- Gallery opens the image picker.
- Looks opens the Retouch dashboard.
- Camera returns to the camera tab state.
- Profile opens the settings dialog.

These are useful placeholder flows, not separate navigation destinations yet.

## 14. Accessibility

- Icon buttons have content descriptions.
- Presets and zoom values expose selected/not-selected state descriptions.
- Texture preservation exposes switch role and On/Off state.
- Primary controls have larger touch targets.
- Selected state is represented with both color and text/shape changes.
- Bottom navigation labels remain visible below icons.

A complete TalkBack traversal audit and large-font layout audit remain future work.

## 15. Error and Runtime States

Currently surfaced states include:

- Camera unavailable with the exception type.
- Camera provider initialization failure.
- Face model initialization failure.
- Face frame conversion or inference failure.
- Storage permission missing on Android 8/9.
- MediaStore insertion failure.
- Capture failure.
- Photo selected.
- Photo captured.

Still recommended for a production release:

- Dedicated full-screen camera unavailable state.
- Processing indicator around the shutter.
- Low-storage detection.
- Low-battery and thermal throttling.
- Offline banner for future network features.
- Retry and recovery actions for model failures.

## 16. Known Limitations

The following are intentionally not complete yet:

- No TFLite texture-preservation model (smoothing is GL blur, not ML).
- Retouch presets (Smooth/Freckles/Matte/Dewy/Refine) have no per-preset colors;
  lip/blush colors are still hardcoded in `beauty_shader.glsl`.
- Capture saves the raw CameraX frame, not the shaded preview.
- Face landmarks drive a coarse mask (single lip polygon, two fixed blush
  circles) — no dense mesh, no landmark stabilization, no mirror/rotation
  verification on a real device.
- Shape tool has no effect.
- Gallery is the system picker, not a saved-photo grid.
- Looks and Profile are lightweight placeholder destinations.
- Settings are not persisted.
- Camera and UI state are not ViewModel-backed.
- No export, share, account, subscription, product catalog, or shade-search flow.

## 17. Recommended Next Development Steps

1. Move camera and studio state into a ViewModel.
2. Store selected preset, texture preference, and camera preference with DataStore.
3. Use landmark results to create a face-aware processing region.
4. Add and validate a real TFLite texture-preservation model.
5. Implement Adaptive Glow and texture processing with a GPU-compatible pipeline.
6. Add a captured-photo gallery grid and a retouch editor.
7. Add instrumentation tests for permission denial, lens switching, capture, and picker flows.
8. Add device-matrix testing for front/rear cameras, torch support, zoom limits, API 26-35, and devices with no camera hardware.
9. Complete accessibility, thermal, battery, and low-storage audits.

## 18. Release Checklist

Before a production release:

- Run `testDebugUnitTest`, `lintDebug`, and `assembleDebug`.
- Install on at least one API 26-28 device and one API 29+ device.
- Verify permission grant and denial flows.
- Verify front/rear switching repeatedly.
- Verify supported and unsupported torch behavior.
- Verify all zoom choices on cameras with different zoom ranges.
- Capture multiple photos and confirm they appear in the system gallery.
- Test rapid shutter taps.
- Test gallery picker cancellation and selection.
- Test MediaPipe model loading and camera rotation.
- Check logcat for crashes and repeated analyzer errors.
- Run the GitHub Actions workflow successfully.
- Confirm the APK is signed appropriately for the target distribution channel.
