package com.nyxiaglow.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nyxiaglow.app.ui.theme.Coral
import com.nyxiaglow.app.ui.theme.CoralDeep
import com.nyxiaglow.app.ui.theme.CoralSoft
import com.nyxiaglow.app.ui.theme.Ink
import com.nyxiaglow.app.ui.theme.SurfaceDark
import com.nyxiaglow.app.ui.theme.SurfaceRaised
import com.nyxiaglow.app.ui.theme.TextMuted

@Composable
@Suppress("UNUSED_PARAMETER")
fun RetouchScreen(
    preserveTexture: Boolean,
    smoothingIntensity: Float,
    selectedTool: String,
    selectedPreset: String,
    onTextureToggle: () -> Unit,
    onSmoothingChange: (Float) -> Unit,
    onToolSelected: (String) -> Unit,
    onPresetSelected: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Brush.verticalGradient(listOf(SurfaceDark, Ink)))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("LOOKS", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = .6.sp)
                    Text("Live preview", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Surface(color = Coral.copy(alpha = .16f), shape = RoundedCornerShape(50)) {
                    Text("On camera", color = CoralSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
            Column(Modifier.fillMaxWidth()) {
                Text("LOOK", color = TextMuted, fontSize = 12.sp, letterSpacing = .6.sp)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GlowLooks.all) { item ->
                        val selected = item.id == selectedPreset
                        Surface(
                            color = if (selected) Coral else SurfaceRaised,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .sizeIn(minWidth = 64.dp, minHeight = 48.dp)
                                .clickable(role = Role.Button) { onPresetSelected(item.id) }
                                .semantics {
                                    this.selected = selected
                                    stateDescription = if (selected) "Selected" else "Not selected"
                                }
                        ) {
                            Column(
                                Modifier.width(68.dp).padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(lookSwatch(item))
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    item.title,
                                    color = if (selected) CoralDeep else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SMOOTHING", color = TextMuted, fontSize = 12.sp, letterSpacing = .5.sp)
                Text("${(smoothingIntensity * 100).toInt()}%", color = CoralSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                value = smoothingIntensity,
                onValueChange = onSmoothingChange,
                valueRange = 0f..1f,
                modifier = Modifier.semantics {
                    stateDescription = "Smoothing intensity ${(smoothingIntensity * 100).toInt()} percent"
                },
                colors = SliderDefaults.colors(thumbColor = Coral, activeTrackColor = Coral, inactiveTrackColor = SurfaceRaised)
            )
            Surface(
                color = SurfaceRaised.copy(alpha = .9f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 48.dp)
                    .clickable(role = Role.Switch, onClick = onTextureToggle)
                    .semantics {
                        stateDescription = if (preserveTexture) "On" else "Off"
                    }
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Keep natural texture", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Uses lighter smoothing", color = TextMuted, fontSize = 12.sp)
                    }
                    Text(if (preserveTexture) "ON" else "OFF", color = if (preserveTexture) CoralSoft else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = Color.White),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("RESET", fontSize = 12.sp) }
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = CoralDeep),
                    modifier = Modifier.weight(2f).height(48.dp)
                ) { Text("APPLY TO PREVIEW", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private fun lookSwatch(look: GlowLook): Color {
    val lip = GlowLooks.hexToRgb(look.lipHex)
    val blush = GlowLooks.hexToRgb(look.blushHex)
    return Color(
        red = (lip[0] * 0.65f + blush[0] * 0.35f).coerceIn(0f, 1f),
        green = (lip[1] * 0.65f + blush[1] * 0.35f).coerceIn(0f, 1f),
        blue = (lip[2] * 0.65f + blush[2] * 0.35f).coerceIn(0f, 1f)
    )
}
