package com.nyxiaglow.app

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.nyxiaglow.app.BuildConfig
import com.nyxiaglow.app.camera.FaceLandmarkAnalyzer
import com.nyxiaglow.app.camera.BeautyCameraRenderer
import com.nyxiaglow.app.camera.BeautyControls
import com.nyxiaglow.app.camera.LandmarkChangeDetector
import com.nyxiaglow.app.camera.LookLut
import com.nyxiaglow.app.camera.MakeupMaskGenerator
import com.nyxiaglow.app.camera.lightingState
import com.nyxiaglow.app.ui.RetouchScreen
import com.nyxiaglow.app.ui.RetouchState
import com.nyxiaglow.app.ui.GlowLooks
import com.nyxiaglow.app.ui.retouchApplyMessage
import com.nyxiaglow.app.ui.theme.NyxiaGlowTheme
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Coral = Color(0xFFFF9A8B)
private val CoralDeep = Color(0xFF96463B)
private val Mist = Color(0xFFF5F1F0)
private val Ink = Color(0xFF111111)
private val SurfaceDark = Color(0xFF171515)
private val SurfaceRaised = Color(0xFF242020)

private fun lookIdForPreset(value: String): String = when (value) {
    "Smooth" -> "natural"
    "Freckles" -> "nude"
    "Matte" -> "matte"
    "Dewy" -> "dewy"
    "Refine" -> "glam"
    else -> value
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NyxiaGlowTheme { NyxiaGlowApp() } }
    }
}

@Composable
private fun NyxiaGlowApp() {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    var cameraGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var permissionAsked by remember { mutableStateOf(false) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    LaunchedEffect(lifecycleState) {
        if (lifecycleState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionAsked = true
        cameraGranted = granted || ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    if (cameraGranted) {
        GlowStudio()
    } else {
        val permanentlyDenied = permissionAsked && !activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        PermissionPrompt(
            permanentlyDenied = permanentlyDenied,
            onEnable = {
                if (permanentlyDenied) {
                    activity.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onLater = { activity.finish() }
        )
    }
}

@Composable
private fun GlowStudio() {
    val context = LocalContext.current
    var glow by remember { mutableFloatStateOf(0.68f) }
    var ambient by remember { mutableFloatStateOf(0.58f) }
    var preset by remember { mutableStateOf("Soft") }
    var zoom by remember { mutableStateOf("1x") }
    var facing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var flashOn by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("Camera") }
    var retouchState by remember { mutableStateOf(RetouchState()) }
    var selectedLookId by remember { mutableStateOf("natural") }
    fun applyLook(id: String) {
        val look = GlowLooks.byId(id)
        selectedLookId = look.id
        retouchState = retouchState.copy(
            smoothingIntensity = look.smooth,
            lipIntensity = look.lipStrength,
            blushIntensity = look.blushStrength,
            selectedPreset = look.id
        )
    }
    val selectedLook = GlowLooks.byId(selectedLookId)
    val beautyControls = selectedLook.toBeautyControls(
        texturePreservation = if (retouchState.preserveTexture) 1f else 0.2f
    ).copy(
        skinSmooth = retouchState.smoothingIntensity,
        lipIntensity = retouchState.lipIntensity,
        blushIntensity = retouchState.blushIntensity
    ).clamped()
    var reticleVisible by remember { mutableStateOf(true) }
    var captureRequest by remember { mutableStateOf(0) }
    val captureLock = remember { AtomicBoolean(false) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var stillUri by remember { mutableStateOf<Uri?>(null) }
    var statusMessage by remember { mutableStateOf("Ready") }
    var showSettings by remember { mutableStateOf(false) }
    var flashAvailable by remember { mutableStateOf(false) }
    val legacyStorageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            captureRequest++
        } else {
            captureLock.set(false)
            statusMessage = "Storage permission is required to save photos"
        }
    }
    fun requestCapture() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            legacyStorageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            captureRequest++
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            capturedUri = uri
            stillUri = uri
            statusMessage = "Photo selected"
        }
    }

    LaunchedEffect(preset, zoom, flashOn, facing) {
        reticleVisible = true
        delay(2_000)
        reticleVisible = false
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        if (activeTab == "Retouch") {
            RetouchScreen(
                preserveTexture = retouchState.preserveTexture,
                smoothingIntensity = retouchState.smoothingIntensity,
                selectedTool = retouchState.selectedTool,
                selectedPreset = selectedLookId,
                onTextureToggle = { retouchState = retouchState.copy(preserveTexture = !retouchState.preserveTexture) },
                onSmoothingChange = { retouchState = retouchState.copy(smoothingIntensity = it.coerceIn(0f, 1f)) },
                onToolSelected = { retouchState = retouchState.copy(selectedTool = it) },
                onPresetSelected = { applyLook(lookIdForPreset(it)) },
                onReset = {
                    retouchState = retouchState.reset()
                    selectedLookId = GlowLooks.natural.id
                    statusMessage = "Retouch reset"
                },
                onApply = { statusMessage = retouchApplyMessage(capturedUri != null) },
                lipIntensity = retouchState.lipIntensity,
                blushIntensity = retouchState.blushIntensity,
                onLipIntensityChange = { retouchState = retouchState.copy(lipIntensity = it.coerceIn(0f, 1f)) },
                onBlushIntensityChange = { retouchState = retouchState.copy(blushIntensity = it.coerceIn(0f, 1f)) }
            )
        } else {
            CameraPreview(Modifier.fillMaxSize(), facing, zoom, flashOn, captureRequest, beautyControls, selectedLookId, stillUri,
                onAmbient = { ambient = it },
                onLandmarksDetected = { result, _ ->
                    if (result.faceLandmarks().isNotEmpty()) statusMessage = "Face detected"
                },
                onCapture = { uri ->
                    captureLock.set(false)
                    capturedUri = uri
                    statusMessage = "Photo captured"
                },
                onCaptureCancelled = { captureLock.set(false) },
                onCameraError = {
                    captureLock.set(false)
                    statusMessage = it
                },
                onFlashAvailabilityChanged = {
                    flashAvailable = it
                    if (!it) flashOn = false
                }
            )
            if (stillUri == null) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(alpha = .78f), Color.Transparent, Ink.copy(alpha = .96f)))))
                Column(Modifier.fillMaxSize().padding(WindowInsets.navigationBars.asPaddingValues()), verticalArrangement = Arrangement.SpaceBetween) {
                    TopBar(flashOn, flashAvailable, { flashOn = !flashOn }, { showSettings = true }, activeTab)
                    Column(Modifier.fillMaxWidth().padding(bottom = 126.dp)) {
                        CameraOverlay(glow, ambient, retouchState.preserveTexture, reticleVisible) { retouchState = retouchState.copy(preserveTexture = !retouchState.preserveTexture) }
                        CameraDeck(
                            selectedLookId = selectedLookId,
                            zoom = zoom,
                            capturing = captureLock.get(),
                            onLook = { applyLook(it) },
                            onZoom = { zoom = it },
                            onFlip = { facing = if (facing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT },
                            onGallery = { galleryLauncher.launch("image/*") },
                            onCapture = { if (captureLock.compareAndSet(false, true)) requestCapture() }
                        )
                    }
                }
            } else {
                StillTryOn(
                    uri = stillUri!!,
                    lookId = selectedLookId,
                    intensity = retouchState.smoothingIntensity,
                    onBack = { stillUri = null; statusMessage = "Ready" },
                    onSaved = { uri ->
                        capturedUri = uri
                        statusMessage = "Photo saved"
                    },
                    onStatus = { statusMessage = it }
                )
            }
        }
        Box(Modifier.align(Alignment.BottomCenter)) {
            BottomNavigation(activeTab) { tab ->
                activeTab = tab
                when (tab) {
                    "Gallery" -> galleryLauncher.launch("image/*")
                    "Looks" -> activeTab = "Retouch"
                    "Profile" -> showSettings = true
                }
            }
        }
        if (statusMessage != "Ready") {
            Surface(color = Ink.copy(alpha = .82f), shape = RoundedCornerShape(50), modifier = Modifier.align(Alignment.TopCenter).padding(top = 70.dp)) {
                Text(statusMessage, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
        if (showSettings) {
            Dialog(onDismissRequest = { showSettings = false }) {
                Surface(color = Mist, shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(22.dp)) {
                        Text("Camera settings", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        Text("Texture preservation is ${if (retouchState.preserveTexture) "on" else "off"}.", color = Color(0xFF595F65), fontSize = 14.sp)
                        Text("Flash and zoom follow the connected camera hardware.", color = Color(0xFF595F65), fontSize = 14.sp)
                        TextButton(onClick = { showSettings = false }) { Text("Done", color = CoralDeep) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(flashOn: Boolean, flashAvailable: Boolean, onFlash: () -> Unit, onSettings: () -> Unit, activeTab: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AuraLogo(Modifier.size(34.dp))
            Spacer(Modifier.width(9.dp))
            Text("NYXIA-GLOW", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (activeTab == "Retouch") "Retouch Looks" else "Camera", color = Color.White.copy(alpha = .82f), fontSize = 11.sp)
            IconButton(onClick = onFlash, enabled = flashAvailable) { Icon(Icons.Default.FlashOn, "Toggle flash", tint = if (flashOn) Coral else Color.White.copy(alpha = if (flashAvailable) .75f else .3f)) }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Camera settings", tint = Color.White.copy(alpha = .75f)) }
        }
    }
}

@Composable
private fun CameraDeck(
    selectedLookId: String,
    zoom: String,
    capturing: Boolean,
    onLook: (String) -> Unit,
    onZoom: (String) -> Unit,
    onFlip: () -> Unit,
    onGallery: () -> Unit,
    onCapture: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GlowLooks.all) { look ->
                val selected = look.id == selectedLookId
                Button(
                    onClick = { onLook(look.id) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Coral else Ink.copy(alpha = .44f),
                        contentColor = if (selected) CoralDeep else Color.White
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(48.dp).semantics {
                        role = Role.Button
                        stateDescription = if (selected) "Selected" else "Not selected"
                    }
                ) {
                    if (selected) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(look.title, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("0.5x", "1x", "2x", "3x").forEach { item ->
                val selected = item == zoom
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Coral else Ink.copy(alpha = .4f))
                        .clickable { onZoom(item) }
                        .semantics {
                            role = Role.Button
                            stateDescription = if (selected) "Selected" else "Not selected"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        item,
                        color = if (selected) CoralDeep else Color.White.copy(alpha = .82f),
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onGallery, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.PhotoLibrary, "Open gallery", tint = Color.White.copy(alpha = .86f), modifier = Modifier.size(26.dp))
            }
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(84.dp).clip(CircleShape).background(Coral.copy(alpha = .22f)))
                Surface(color = Color.White.copy(alpha = .94f), shape = CircleShape, modifier = Modifier.size(70.dp)) {
                    IconButton(onClick = onCapture, enabled = !capturing) {
                        if (capturing) {
                            CircularProgressIndicator(color = CoralDeep, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Camera, "Capture photo", tint = CoralDeep, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
            IconButton(onClick = onFlip, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.FlipCameraAndroid, "Flip camera", tint = Color.White.copy(alpha = .86f), modifier = Modifier.size(27.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BottomNavigation(active: String, onChange: (String) -> Unit) {
    Surface(color = SurfaceDark.copy(alpha = .98f), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            NavItem("Gallery", Icons.Default.PhotoLibrary, active, onChange)
            NavItem("Looks", Icons.Default.AutoAwesome, active, onChange)
            Surface(color = Coral, shape = CircleShape, modifier = Modifier.size(54.dp)) { IconButton(onClick = { onChange("Camera") }) { Icon(Icons.Default.Camera, "Camera", tint = CoralDeep, modifier = Modifier.size(25.dp)) } }
            NavItem("Profile", Icons.Default.Person, active, onChange)
        }
    }
}

@Composable
private fun NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: String, onChange: (String) -> Unit) {
    val selected = label == active
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onChange(label) }.padding(horizontal = 7.dp, vertical = 3.dp)) {
        Icon(icon, label, tint = if (selected) Coral else Color.White.copy(alpha = .58f), modifier = Modifier.size(21.dp))
        Text(label, color = if (selected) Coral else Color.White.copy(alpha = .58f), fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun AuraLogo(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clip(CircleShape).border(3.dp, Coral, CircleShape))
        Box(Modifier.fillMaxSize(.7f).clip(CircleShape).border(1.5.dp, Coral, CircleShape))
        Box(Modifier.size(12.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Coral, CoralDeep))))
    }
}

@Composable
private fun PermissionPrompt(
    permanentlyDenied: Boolean = false,
    onEnable: (() -> Unit)? = null,
    onLater: (() -> Unit)? = null,
    onRequest: (() -> Unit)? = null
) {
    Box(Modifier.fillMaxSize().background(Mist), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            AuraLogo(Modifier.size(72.dp))
            Spacer(Modifier.height(20.dp))
            Text("Camera access brings the glow to life", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Nyxia Glow needs your camera for the live beauty preview.", color = Color(0xFF595F65), fontSize = 14.sp)
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onEnable ?: onRequest ?: {},
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = CoralDeep)
            ) { Text(if (permanentlyDenied) "Open settings" else "Enable camera") }
            if (onLater != null) {
                TextButton(onClick = onLater) { Text("Maybe later", color = Color(0xFF736E6D)) }
            }
        }
    }
}

@Composable
private fun CameraOverlay(
    glow: Float,
    ambient: Float,
    preserveTexture: Boolean,
    reticleVisible: Boolean,
    onTextureToggle: () -> Unit
) {
    Box(Modifier.fillMaxWidth().height(350.dp)) {
        Surface(
            color = Ink.copy(alpha = .68f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Coral))
                Spacer(Modifier.width(7.dp))
                Text("AI ACTIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                Text("LIVE", color = Coral, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (reticleVisible) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(220.dp)
                    .border(1.dp, Coral.copy(alpha = (.22f + glow * .28f).coerceIn(0f, 1f)), RoundedCornerShape(110.dp))
            )
        }
        Surface(
            color = Ink.copy(alpha = .72f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)
        ) {
            Row(
                Modifier.clickable(onClick = onTextureToggle).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = Coral, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Preserve texture", color = Color.White, fontSize = 12.sp)
                Spacer(Modifier.width(7.dp))
                Text(if (preserveTexture) "On" else "Off", color = Coral, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier,
    lensFacing: Int,
    zoom: String,
    flashOn: Boolean,
    captureRequest: Int,
    beautyControls: BeautyControls,
    makeupPresetName: String,
    stillUri: Uri?,
    onAmbient: (Float) -> Unit,
    onLandmarksDetected: (FaceLandmarkerResult, Int) -> Unit,
    onCapture: (Uri) -> Unit,
    onCaptureCancelled: () -> Unit,
    onCameraError: (String) -> Unit,
    onFlashAvailabilityChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val renderer = remember {
        BeautyCameraRenderer(
            context = context,
            callbackExecutor = ContextCompat.getMainExecutor(context),
            onSurfaceError = onCameraError
        )
    }
    val glView = remember {
        android.opengl.GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }
    val maskGenerator = remember { MakeupMaskGenerator() }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(glView.display?.rotation ?: android.view.Surface.ROTATION_0)
            .build()
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    DisposableEffect(lensFacing, lifecycleOwner, stillUri) {
        val disposed = AtomicBoolean(false)
        var localAnalyzer: FaceLandmarkAnalyzer? = null
        var localAnalysis: ImageAnalysis? = null
        var localProvider: ProcessCameraProvider? = null
        var displayListener: DisplayManager.DisplayListener? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (disposed.get()) return@addListener
            if (stillUri != null) return@addListener
            val provider = try {
                future.get()
            } catch (exception: Exception) {
                onCameraError("Camera initialization failed: ${exception.message ?: exception.javaClass.simpleName}")
                return@addListener
            }
            localProvider = provider
            val analysisResolution = Size(640, 480)
            val targetRotation = glView.display?.rotation ?: android.view.Surface.ROTATION_0
            var previousLandmarks: FloatArray? = null
            var hadFace = false
            var lastDebugFaceState: Boolean? = null
            val preview = Preview.Builder()
                .setTargetResolution(analysisResolution)
                .setTargetRotation(targetRotation)
                .build()
                .also { it.setSurfaceProvider(renderer::provideSurfaceRequest) }
            val analyzer = FaceLandmarkAnalyzer(
                context = context,
                onLandmarksDetected = { result, rotationDegrees ->
                    val landmarks = result.faceLandmarks().firstOrNull()
                    if (BuildConfig.DEBUG && lastDebugFaceState != (landmarks != null)) {
                        lastDebugFaceState = landmarks != null
                        Log.d("CameraPreview", "Face Landmarker result: face=${landmarks != null}")
                    }
                    if (landmarks == null) {
                        previousLandmarks = null
                        if (hadFace) {
                            hadFace = false
                            glView.queueEvent { renderer.clearMakeupMask() }
                            glView.requestRender()
                        }
                    } else {
                        val currentLandmarks = FloatArray(landmarks.size * 2) { index ->
                            if (index % 2 == 0) landmarks[index / 2].x() else landmarks[index / 2].y()
                        }
                        val changed = previousLandmarks == null || LandmarkChangeDetector.changed(previousLandmarks!!, currentLandmarks)
                        previousLandmarks = currentLandmarks
                        if (changed) {
                            val previewSize = renderer.currentTextureSize()
                            val mask = maskGenerator.generateMask(
                                result,
                                rotationDegrees = rotationDegrees,
                                mirrorX = lensFacing == CameraSelector.LENS_FACING_FRONT,
                                width = previewSize.width,
                                height = previewSize.height
                            )
                            if (!MakeupMaskGenerator.hasVisiblePixels(mask)) {
                                if (BuildConfig.DEBUG) Log.d("CameraPreview", "Generated makeup mask is empty")
                                mask.recycle()
                                glView.queueEvent { renderer.clearMakeupMask() }
                                glView.requestRender()
                            } else if (disposed.get()) {
                                mask.recycle()
                            } else {
                                if (BuildConfig.DEBUG) Log.d("CameraPreview", "Generated makeup mask: ${previewSize.width}x${previewSize.height}")
                                glView.queueEvent { renderer.updateMakeupMask(mask) }
                                glView.requestRender()
                            }
                        }
                        hadFace = true
                    }
                    onLandmarksDetected(result, rotationDegrees)
                },
                onLightChanged = { if (!disposed.get()) onAmbient(it) },
                onError = { if (!disposed.get()) onCameraError(it) }
            )
            localAnalyzer = analyzer
            if (disposed.get()) {
                analyzer.close()
                localAnalyzer = null
                return@addListener
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(analysisResolution)
                .setTargetRotation(targetRotation)
                .build()
                .also { it.setAnalyzer(executor, analyzer) }
            localAnalysis = analysis
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            provider.unbindAll()
            camera = try {
                val boundCamera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture, analysis).also {
                    onFlashAvailabilityChanged(it.cameraInfo.hasFlashUnit())
                }
                val listener = object : DisplayManager.DisplayListener {
                    override fun onDisplayChanged(displayId: Int) {
                        if (displayId == glView.display?.displayId) {
                            val rotation = glView.display?.rotation ?: android.view.Surface.ROTATION_0
                            preview.targetRotation = rotation
                            analysis.targetRotation = rotation
                            imageCapture.targetRotation = rotation
                        }
                    }

                    override fun onDisplayAdded(displayId: Int) = Unit
                    override fun onDisplayRemoved(displayId: Int) = Unit
                }
                displayListener = listener
                (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
                    ?.registerDisplayListener(listener, null)
                boundCamera
            } catch (exception: Exception) {
                onCameraError("Camera unavailable: ${exception.javaClass.simpleName}")
                analyzer.close()
                localAnalyzer = null
                localAnalysis = null
                null
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed.set(true)
            displayListener?.let { listener ->
                (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
                    ?.unregisterDisplayListener(listener)
            }
            camera = null
            onFlashAvailabilityChanged(false)
            glView.queueEvent { renderer.clearMakeupMask() }
            glView.requestRender()
            localAnalysis?.clearAnalyzer()
            localProvider?.unbindAll()
            localAnalyzer?.let { analyzer ->
                localAnalyzer = null
                analyzer.close()
            }
        }
    }

    DisposableEffect(Unit) {
        renderer.setRenderRequest { glView.requestRender() }
        onDispose {
            renderer.setRenderRequest(null)
            renderer.release()
            glView.queueEvent {
                renderer.releaseGlResources()
                glView.onPause()
            }
            onCaptureCancelled()
            executor.shutdown()
        }
    }

    val look = GlowLooks.byId(makeupPresetName)
    LaunchedEffect(look.id, beautyControls) {
        val lutPixels = withContext(Dispatchers.Default) {
            LookLut.buildPixels(LookLut.gradeFor(look.id))
        }
        glView.queueEvent {
            renderer.setBeautyControls(beautyControls)
            renderer.setColorLut(lutPixels, look.lutIntensity)
        }
        glView.requestRender()
    }

    LaunchedEffect(camera, zoom, flashOn) {
        val currentCamera = camera ?: return@LaunchedEffect
        val requestedZoom = zoom.removeSuffix("x").toFloatOrNull() ?: 1f
        val zoomState = currentCamera.cameraInfo.zoomState.value
        if (zoomState != null) {
            currentCamera.cameraControl.setZoomRatio(requestedZoom.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio))
        }
        if (currentCamera.cameraInfo.hasFlashUnit()) {
            currentCamera.cameraControl.enableTorch(flashOn)
        }
    }

    LaunchedEffect(captureRequest) {
        if (captureRequest == 0) return@LaunchedEffect
        if (executor.isShutdown) {
            onCameraError("Camera session ended")
            return@LaunchedEffect
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "NyxiaGlow_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Nyxia Glow")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            onCameraError("Storage permission is required to save photos")
            return@LaunchedEffect
        }
        val outputUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (outputUri == null) {
            onCameraError("Could not prepare photo storage")
            return@LaunchedEffect
        }
        glView.queueEvent {
            val bitmap = renderer.captureFrame()
            if (bitmap == null) {
                context.contentResolver.delete(outputUri, null, null)
                onCameraError("Capture failed")
                return@queueEvent
            }
            try {
                executor.execute {
                    try {
                        context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) { "JPEG encoding failed" }
                        } ?: error("Could not open photo storage")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            context.contentResolver.update(outputUri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
                        }
                        bitmap.recycle()
                        onCapture(outputUri)
                    } catch (exception: Exception) {
                        bitmap.recycle()
                        context.contentResolver.delete(outputUri, null, null)
                        onCameraError("Capture failed")
                    }
                }
            } catch (_: RejectedExecutionException) {
                bitmap.recycle()
                context.contentResolver.delete(outputUri, null, null)
                onCameraError("Camera session ended")
            }
        }
    }

    AndroidView(factory = { glView }, modifier = modifier)
}

@Composable
private fun GalleryEmpty(onImport: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ink.copy(alpha = .55f))
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Coral, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("No photo yet", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Capture from Camera or import a photo to try a look.",
            color = Color.White.copy(alpha = .72f),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onImport,
            modifier = Modifier.height(48.dp).fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = CoralDeep)
        ) { Text("Import photo", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    }
}

private data class StillRenderResult(
    val bitmap: Bitmap?,
    val status: String
)

@Composable
private fun StillTryOn(
    uri: Uri,
    lookId: String,
    intensity: Float,
    onBack: () -> Unit,
    onSaved: (Uri) -> Unit,
    onStatus: (String) -> Unit
) {
    val context = LocalContext.current
    val landmarker = remember {
        runCatching {
            FaceLandmarker.createFromOptions(
                context,
                FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(
                        BaseOptions.builder()
                            .setModelAssetPath("face_landmarker.task")
                            .setDelegate(Delegate.CPU)
                            .build()
                    )
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumFaces(1)
                    .setMinFaceDetectionConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .build()
            )
        }.getOrNull()
    }
    var rendered by remember(uri, lookId, intensity) { mutableStateOf<Bitmap?>(null) }
    var status by remember(uri) { mutableStateOf("Loading photo") }

    DisposableEffect(landmarker) {
        onDispose { landmarker?.close() }
    }

    LaunchedEffect(uri, lookId, intensity, landmarker) {
        val result = withContext(Dispatchers.IO) {
            val source = decodeScaledBitmap(context, uri)
            if (source == null || landmarker == null) {
                return@withContext StillRenderResult(null, "Could not open photo")
            }
            val detection = runCatching {
                landmarker.detect(BitmapImageBuilder(source).build())
            }.getOrNull()
            if (detection == null) {
                source.recycle()
                return@withContext StillRenderResult(null, "Still face analysis failed")
            }
            val mask = MakeupMaskGenerator().generateMask(
                detection,
                width = source.width,
                height = source.height
            )
            val look = GlowLooks.byId(lookId)
            val output = applyStillLook(
                source,
                mask,
                GlowLooks.hexToRgb(look.lipHex),
                GlowLooks.hexToRgb(look.blushHex),
                look.lipStrength * intensity,
                look.blushStrength * intensity
            )
            source.recycle()
            mask.recycle()
            StillRenderResult(
                bitmap = output,
                status = if (detection.faceLandmarks().isEmpty()) "No face detected" else "Still look applied"
            )
        }
        status = result.status
        rendered = result.bitmap
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        rendered?.let { bitmap ->
            Image(bitmap.asImageBitmap(), contentDescription = "Retouched photo", modifier = Modifier.fillMaxSize())
        }
        if (rendered == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Coral)
                Spacer(Modifier.height(12.dp))
                Text(status, color = Color.White, fontSize = 14.sp)
            }
        }
        Row(
            Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).clip(CircleShape).background(Ink.copy(alpha = .7f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to camera", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(GlowLooks.byId(lookId).title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 88.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val bitmap = rendered
                    if (bitmap == null) {
                        onStatus("Photo is still loading")
                    } else {
                        val saved = saveJpegToGallery(context, bitmap)
                        if (saved != null) onSaved(saved) else onStatus("Could not save photo")
                    }
                },
                enabled = rendered != null,
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = CoralDeep),
                modifier = Modifier.weight(1f).height(48.dp)
            ) { Text("Save look", fontWeight = FontWeight.SemiBold) }
            Button(
                onClick = {
                    val bitmap = rendered
                    if (bitmap == null) {
                        onStatus("Photo is still loading")
                    } else {
                        val saved = saveJpegToGallery(context, bitmap)
                        if (saved == null) {
                            onStatus("Could not share photo")
                        } else {
                            onSaved(saved)
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "image/jpeg"
                                        putExtra(Intent.EXTRA_STREAM, saved)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    },
                                    "Share look"
                                )
                            )
                        }
                    }
                },
                enabled = rendered != null,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = Color.White),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share")
            }
        }
    }
}

private fun saveJpegToGallery(context: Context, bitmap: Bitmap): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "NyxiaGlow_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Nyxia Glow")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val outputUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    return try {
        context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) { "JPEG encoding failed" }
        } ?: error("Could not open photo storage")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.update(outputUri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        }
        outputUri
    } catch (_: Exception) {
        context.contentResolver.delete(outputUri, null, null)
        null
    }
}

private fun decodeScaledBitmap(context: Context, uri: Uri, maxEdge: Int = 1024): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
    if (bounds.outWidth < 1 || bounds.outHeight < 1) return null
    var sample = 1
    while (bounds.outWidth / sample > maxEdge || bounds.outHeight / sample > maxEdge) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inMutable = false
    }
    val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    if (decoded.config == Bitmap.Config.ARGB_8888 && !decoded.isMutable) return decoded
    return decoded.copy(Bitmap.Config.ARGB_8888, false).also { decoded.recycle() }
}

private fun applyStillLook(
    source: Bitmap,
    mask: Bitmap,
    lipColor: FloatArray,
    blushColor: FloatArray,
    lipStrength: Float,
    blushStrength: Float
): Bitmap {
    val sourcePixels = IntArray(source.width * source.height)
    val maskPixels = IntArray(mask.width * mask.height)
    source.getPixels(sourcePixels, 0, source.width, 0, 0, source.width, source.height)
    mask.getPixels(maskPixels, 0, mask.width, 0, 0, mask.width, mask.height)
    val output = IntArray(sourcePixels.size)
    for (index in sourcePixels.indices) {
        val sourceColor = sourcePixels[index]
        val maskColor = maskPixels[index]
        var red = sourceColor shr 16 and 0xFF
        var green = sourceColor shr 8 and 0xFF
        var blue = sourceColor and 0xFF
        val lipAmount = ((maskColor shr 16 and 0xFF) / 255f * lipStrength).coerceIn(0f, 1f)
        val blushAmount = ((maskColor shr 8 and 0xFF) / 255f * blushStrength).coerceIn(0f, 1f)
        val targetRed = (lipColor[0] * 255f * lipAmount + blushColor[0] * 255f * blushAmount) / (lipAmount + blushAmount).coerceAtLeast(1f)
        val targetGreen = (lipColor[1] * 255f * lipAmount + blushColor[1] * 255f * blushAmount) / (lipAmount + blushAmount).coerceAtLeast(1f)
        val targetBlue = (lipColor[2] * 255f * lipAmount + blushColor[2] * 255f * blushAmount) / (lipAmount + blushAmount).coerceAtLeast(1f)
        val amount = (lipAmount + blushAmount).coerceIn(0f, 1f)
        red = (red * (1f - amount) + targetRed * amount).toInt().coerceIn(0, 255)
        green = (green * (1f - amount) + targetGreen * amount).toInt().coerceIn(0, 255)
        blue = (blue * (1f - amount) + targetBlue * amount).toInt().coerceIn(0, 255)
        output[index] = (sourceColor and -0x1000000) or (red shl 16) or (green shl 8) or blue
    }
    return Bitmap.createBitmap(output, source.width, source.height, Bitmap.Config.ARGB_8888)
}
