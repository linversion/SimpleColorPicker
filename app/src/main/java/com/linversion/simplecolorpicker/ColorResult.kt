package com.linversion.simplecolorpicker

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.linversion.simplecolorpicker.settings.SettingsActivity

@Composable
fun ColorResult(
    modifier: Modifier,
    colorState: ColorState,
    onUriResult: (uri: Uri) -> Unit,
    onToggleLock: (() -> Unit)? = null,
    onOpenPalette: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .background(if (colorState.colorEnvelope != null) colorState.colorEnvelope.color else colorState.toColor())
            .statusBarsPadding()
    ) {
        val text = if (colorState.colorEnvelope != null) {
            "#${colorState.colorEnvelope.hexCode}"
        } else {
            colorState.toHexString()
        }
        Content(
            text = text,
            isLight = colorState.isLight,
            locked = colorState.locked,
            onUriResult = onUriResult,
            onToggleLock = onToggleLock,
            onOpenPalette = onOpenPalette
        )
    }
}

@Composable
fun Content(
    text: String,
    isLight: Boolean,
    locked: Boolean,
    onUriResult: (uri: Uri) -> Unit,
    onToggleLock: (() -> Unit)?,
    onOpenPalette: (() -> Unit)? = null
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onUriResult(uri) }
    }
    val context = LocalContext.current
    val tint = if (isLight) Color.Black else Color.White
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (locked) "$text 锁定" else text,
            color = tint,
            modifier = Modifier.padding(start = 12.dp)
        )
        Row {
            if (onOpenPalette != null) {
                IconButton(onClick = onOpenPalette) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Image palette",
                        tint = tint
                    )
                }
            }
            if (onToggleLock != null) {
                IconButton(onClick = onToggleLock) {
                    Icon(
                        imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (locked) "Unlock color" else "Lock color",
                        tint = tint
                    )
                }
            }
            IconButton(onClick = { launcher.launch("image/*") }) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Choose an image",
                    tint = tint
                )
            }
            IconButton(onClick = { SettingsActivity.startActivity(context) }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open Setting",
                    tint = tint
                )
            }
        }
    }
}
