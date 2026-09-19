# Nyxia Glow Advanced Beauty Engine Plan

## 1. Objective

Upgrade Nyxia Glow from a basic live makeup preview into a modular real-time beauty camera system inspired by modern beauty camera applications.

The implementation must be original and adapted to the existing Nyxia Glow architecture. It must not copy Persona source code, proprietary native libraries, shaders, models, UI, branding, or assets.

The system should support:

- Real-time face beautification
- Skin smoothing and glow
- Lip and blush makeup
- Eye enhancement
- Teeth whitening
- Face refinement
- Configurable GlowLooks presets
- Gallery photo try-on
- Processed photo capture
- Future hair, body, and video effects

The first version should prioritize stability, visual quality, low latency, and compatibility with the existing OpenGL and MediaPipe pipeline.

## 2. Existing Project Foundation

Nyxia Glow already contains the core components required for the first implementation:

- Kotlin
- Jetpack Compose
- CameraX
- MediaPipe Tasks Vision
- OpenGL ES
- GLSurfaceView
- BeautyCameraRenderer
- FaceLandmarkAnalyzer
- MakeupMaskGenerator
- beauty_shader.glsl
- GlowLooks
- Processed framebuffer capture
- Gallery still-image try-on
- Permission and camera flows

The existing architecture should be extended instead of replaced.

Important implementation files:

```text
app/src/main/java/com/nyxiaglow/app/MainActivity.kt
app/src/main/java/com/nyxiaglow/app/camera/BeautyCameraRenderer.kt
app/src/main/java/com/nyxiaglow/app/camera/FaceLandmarkAnalyzer.kt
app/src/main/java/com/nyxiaglow/app/ui/Looks.kt
app/src/main/java/com/nyxiaglow/app/ui/RetouchScreen.kt
app/src/main/res/raw/beauty_shader.glsl
```

## 3. Recommended Dependencies

The current project already contains the most important dependencies:

```kotlin
implementation("androidx.camera:camera-camera2:1.4.1")
implementation("androidx.camera:camera-lifecycle:1.4.1")
implementation("androidx.camera:camera-view:1.4.1")
implementation("com.google.mediapipe:tasks-vision:0.10.26")
```

These libraries are sufficient for:

- Camera preview
- Face detection
- Face landmarks
- Image segmentation
- Pose landmarks
- Still-image analysis
- Live mask generation

No additional dependency is required for the first beauty-engine version. OpenGL ES is part of Android and does not require a third-party dependency.

For future video export, Media3 may be evaluated:

```kotlin
implementation("androidx.media3:media3-transformer:<compatible-version>")
implementation("androidx.media3:media3-effect:<compatible-version>")
```

Media3 should only be added when video processing is implemented. FFmpeg, OpenCV, TensorFlow Lite, and custom native libraries should not be added initially because they increase APK size, memory usage, build complexity, and maintenance cost.

## 4. High-Level Architecture

```text
CameraX
   |
   v
SurfaceTexture
   |
   v
BeautyCameraRenderer
   |
   +-- Camera texture
   +-- Face landmarks
   +-- Makeup masks
   +-- Skin segmentation mask
   +-- Beauty controls
   +-- Look preset
   |
   v
OpenGL beauty shader
   |
   +-- Skin processing
   +-- Makeup processing
   +-- Face effects
   +-- Color grading
   |
   v
Live preview
   |
   +-- Framebuffer capture
   +-- MediaStore image export
   +-- Gallery still-image processing
```

The renderer remains the central image-processing component. MediaPipe is responsible for geometry and segmentation. OpenGL is responsible for real-time visual effects. Compose is responsible for controls, presets, sliders, and user interaction.

## 5. Core Data Model

Create a central model for individual retouch controls:

```kotlin
data class BeautyControls(
    val skinSmooth: Float = 0f,
    val skinGlow: Float = 0f,
    val skinWhitening: Float = 0f,
    val skinTexturePreservation: Float = 1f,
    val lipIntensity: Float = 0f,
    val blushIntensity: Float = 0f,
    val eyeEnhancement: Float = 0f,
    val teethWhitening: Float = 0f,
    val faceSlim: Float = 0f,
    val jawRefinement: Float = 0f,
    val noseRefinement: Float = 0f,
    val sharpness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f
)
```

Create a look model that combines these controls:

```kotlin
data class GlowLook(
    val id: String,
    val name: String,
    val lipColor: FloatArray,
    val blushColor: FloatArray,
    val controls: BeautyControls,
    val colorTemperature: Float = 0f,
    val exposure: Float = 0f
)
```

A `GlowLook` should be a data configuration, not a separate shader. This allows the same renderer to support many looks without duplicating rendering code.

## 6. Renderer Responsibilities

`BeautyCameraRenderer` should be responsible for:

1. Receiving the camera frame.
2. Receiving current face masks.
3. Receiving current beauty controls.
4. Uploading masks to OpenGL textures.
5. Uploading control values as shader uniforms.
6. Applying the beauty shader.
7. Rendering the final result.
8. Capturing the processed framebuffer.

Add a method similar to:

```kotlin
fun setBeautyControls(controls: BeautyControls)
```

The renderer should keep the latest values in memory and apply them during the next draw call. All OpenGL uniform updates must happen on the GL thread. The UI thread should only update state.

## 7. Shader Uniforms

The shader should expose explicit uniforms instead of hardcoded effect values:

```glsl
uniform float uSkinSmooth;
uniform float uSkinGlow;
uniform float uSkinWhitening;
uniform float uTexturePreservation;

uniform float uLipIntensity;
uniform vec3 uLipColor;

uniform float uBlushIntensity;
uniform vec3 uBlushColor;

uniform float uEyeEnhancement;
uniform float uTeethWhitening;

uniform float uSharpness;
uniform float uContrast;
uniform float uSaturation;
```

The shader should apply effects in a controlled order:

1. Read camera texture.
2. Read face and skin masks.
3. Preserve important facial details.
4. Apply skin smoothing.
5. Apply glow and whitening.
6. Apply lips and blush.
7. Apply eye and teeth adjustments.
8. Apply color correction.
9. Apply sharpness.
10. Write the final pixel.

## 8. Skin Processing

Skin smoothing must be mask-based. The shader must not blur the whole frame.

Recommended flow:

```text
Camera frame
   |
Skin mask
   |
Blurred skin sample
   |
Texture preservation
   |
Blend only inside skin region
```

The skin effect should preserve eyes, eyebrows, lips, nostrils, hair, face contours, and natural highlights.

The first implementation can use a lightweight blur approximation in the shader. A more advanced version can use a separable blur pass. The goal is natural retouching rather than an artificial plastic effect.

## 9. Face Masks

The current `MakeupMaskGenerator` should be extended or supplemented with masks for:

- Lips
- Blush zones
- Eyes
- Eyebrows
- Teeth
- Skin
- Face contour

The masks should be stored as GPU textures where possible.

The system should support both live camera frames and still images from the gallery. The same mask coordinate system must be used for preview and capture so the saved image matches the live preview.

## 10. Face Refinement

Face refinement requires geometric warping, not only color changes.

Initial features:

- Face slimming
- Jaw refinement
- Nose width
- Nose height
- Eye size
- Lip fullness

Recommended first implementation:

- Use face landmarks to define local regions.
- Apply displacement inside the shader.
- Use smooth radial falloff.
- Limit the deformation to the face mask.
- Clamp values to avoid unnatural distortion.

Conceptual flow:

```text
Face landmarks
   |
Region center and radius
   |
Displacement vector
   |
Smooth falloff
   |
Warp camera texture
```

Face refinement should be added after skin and makeup effects are stable.

## 11. UI Design

The Compose UI should expose the controls in a simple retouch panel.

### Skin

- Smooth
- Glow
- Brightness
- Texture preservation

### Makeup

- Lip color
- Lip intensity
- Blush color
- Blush intensity
- Eye enhancement
- Teeth whitening

### Face

- Face slim
- Jaw
- Nose
- Eyes
- Lips

### Color

- Exposure
- Contrast
- Saturation
- Temperature
- Sharpness

Each control should use a slider with a range from `0f` to `1f`, except temperature and exposure, which may use a centered range from `-1.0` to `1.0`.

The UI should update the renderer in real time. Existing GlowLooks chips should remain available as quick presets.

## 12. GlowLooks Presets

Each GlowLook should combine multiple effects:

```kotlin
GlowLook(
    id = "ruby",
    name = "Ruby",
    lipColor = hexToRgb("#B9475D"),
    blushColor = hexToRgb("#D97978"),
    controls = BeautyControls(
        skinSmooth = 0.25f,
        skinGlow = 0.35f,
        lipIntensity = 0.75f,
        blushIntensity = 0.42f,
        eyeEnhancement = 0.15f,
        sharpness = 0.08f
    )
)
```

Presets should remain editable after selection. The renderer should not know the preset name; it should only receive the resolved `BeautyControls`.

## 13. Gallery Try-On

The gallery pipeline should follow the same processing stages as the live camera:

```text
Selected image
   |
EXIF orientation correction
   |
FaceLandmarker IMAGE mode
   |
Mask generation
   |
OpenGL or bitmap processing
   |
Processed preview
   |
Save/export
```

The gallery and camera pipelines should share:

- BeautyControls
- GlowLook
- Mask definitions
- Color processing logic
- Shader uniform values

This prevents differences between the live preview and the final still image.

## 14. Hair Segmentation

Hair effects should be implemented after face retouching.

Required components:

- MediaPipe image segmentation
- Hair segmentation model
- Hair mask texture
- Color blending shader

Possible effects:

- Hair tint
- Hair brightness
- Hair saturation
- Hair glow
- Background separation

Hair coloring must use a low-opacity blend and preserve luminance variation. A flat color overlay will look artificial.

## 15. Background Blur

Background blur requires a foreground segmentation mask.

The system should:

1. Detect the person.
2. Create a foreground mask.
3. Blur the background.
4. Composite the sharp person over the blurred background.

This should be implemented as a separate rendering pass rather than mixed into the face shader.

## 16. Body Retouch

Body retouch should be postponed until pose tracking is stable.

Required components:

- PoseLandmarker
- Body segmentation
- Landmark-based deformation
- Distortion limits
- Preview/capture consistency

Possible controls:

- Waist
- Hips
- Shoulders
- Legs
- Body smoothing

This feature has the highest risk of visible artifacts and should not be added before the face system is reliable.

## 17. Performance Requirements

The first live version should target:

- Stable 30 FPS on mid-range devices
- No more than one face-analysis frame in flight
- No blocking work on the UI thread
- Reuse of bitmap buffers
- Reuse of GPU textures
- No JPEG conversion for every live frame
- No unnecessary allocations inside `onDrawFrame`
- Graceful degradation when analysis is unavailable

Recommended behavior:

```text
If a new analysis result is available:
    upload the latest mask

If no new result is available:
    reuse the previous mask

If analysis is busy:
    drop the frame instead of queuing it
```

The renderer should continue displaying the camera preview even if MediaPipe temporarily fails.

## 18. Device Compatibility

The current app supports Android API 26 and above. MediaPipe models and advanced segmentation should be tested on Android 10, Android 12, Android 14, and Android 15.

The POCO Android 15 device should be the primary acceptance device.

The implementation must not assume that every device supports the same GPU precision or texture format. Use conservative OpenGL ES 2.0-compatible shader code unless a higher version is explicitly detected.

## 19. Testing Plan

### Unit tests

Test:

- BeautyControls defaults
- Look preset mapping
- Hex color conversion
- Value clamping
- Mask coordinate conversion
- EXIF rotation handling

### Renderer tests

Verify:

- Each uniform is uploaded correctly.
- Zero strength produces the original image.
- Maximum strength does not produce invalid colors.
- Missing masks do not crash rendering.
- Captured frames include the active controls.

### Device tests

Verify:

- Camera permission flow
- Front camera preview
- Correct preview orientation
- Live mask alignment
- Look switching
- Slider updates
- Processed still capture
- Gallery try-on
- App background/foreground recovery
- Rotation changes
- Long-running camera stability

### Performance tests

Measure:

- Frame rate
- Camera analysis latency
- GPU render time
- Memory usage
- Number of dropped analysis frames
- Capture latency

## 20. Acceptance Criteria

The first milestone is complete when:

- The camera preview remains stable for at least five minutes.
- Skin smoothing only affects the skin mask.
- Eyes and lips remain sharp.
- Lip and blush masks align with the face.
- Presets change multiple effects together.
- Sliders update the live preview immediately.
- The saved image matches the live processed preview.
- Gallery try-on produces the same effect as the camera pipeline.
- No native MediaPipe crashes occur.
- No UI freezing occurs during camera use.
- The feature works on the connected POCO Android 15 device.

## 21. Recommended Implementation Order

### Milestone 1: Beauty controls

- Add `BeautyControls`.
- Add renderer state.
- Add shader uniforms.
- Add Compose sliders.
- Connect controls to the live preview.

### Milestone 2: Skin enhancement

- Add skin segmentation.
- Add skin mask texture.
- Implement smoothing.
- Implement glow.
- Implement texture preservation.

### Milestone 3: Makeup expansion

- Improve lips.
- Improve blush.
- Add eye enhancement.
- Add teeth whitening.
- Update GlowLooks presets.

### Milestone 4: Face refinement

- Add landmark-based face warping.
- Add face slim.
- Add jaw refinement.
- Add nose and eye controls.

### Milestone 5: Gallery parity

- Reuse the same controls and masks for still images.
- Ensure camera and gallery output match.
- Add regression tests.

### Milestone 6: Hair and background

- Add hair segmentation.
- Add hair tint.
- Add background blur.

### Milestone 7: Body and video

- Add pose segmentation.
- Add body retouch.
- Add processed video export.

## 22. Security and Licensing Notes

The Persona APK may be used as a behavior and architecture reference only.

The implementation must not reuse:

- Persona native libraries
- Persona shaders
- Persona models
- Persona assets
- Persona UI layouts
- Persona branding
- Decompiled source code
- Proprietary configuration files

Nyxia Glow should use:

- Original Kotlin code
- Original Compose UI
- Original shaders
- Properly licensed MediaPipe models
- Original GlowLooks
- Original app branding

## Final Recommendation

Start with Milestones 1 through 3:

1. BeautyControls
2. Skin segmentation and smoothing
3. Expanded lips, blush, eyes, and teeth effects

These features fit directly into the existing Nyxia Glow architecture and provide the largest visual improvement with the lowest implementation risk. Hair, body retouch, and video processing should follow only after the face and still-image pipelines are stable.
