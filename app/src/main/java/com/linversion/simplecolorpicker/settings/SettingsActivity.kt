package com.linversion.simplecolorpicker.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linversion.simplecolorpicker.BuildConfig

private val ScreenBg = Color(0xFF121212)
private val CardBg = Color(0xFF1E1E1E)
private val TextPrimary = Color(0xFFF2F2F2)
private val TextSecondary = Color(0xFF9A9A9A)
private val Divider = Color(0xFF2C2C2C)

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen(onBack = { finish() })
        }
    }

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextPrimary
                )
            }
            Text(
                text = "设置",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    listOf(
                        Color(0xFFE05A5A),
                        Color(0xFFE0B25A),
                        Color(0xFF5AC8A0),
                        Color(0xFF5A9BE0),
                        Color(0xFFB07AE0)
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
                Text(
                    text = "SimpleColorPicker",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            Text(
                text = "关于",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
            ) {
                SettingsRow(
                    title = "隐私政策",
                    subtitle = "查看数据如何使用",
                    showDivider = true
                ) {
                    uriHandler.openUri("https://sites.google.com/view/linversion-privacy-policy")
                }
                SettingsRow(
                    title = "版本",
                    subtitle = BuildConfig.VERSION_NAME,
                    trailing = null,
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    },
    showDivider: Boolean,
    onClick: (() -> Unit)? = null
) {
    val rowMod = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 16.dp, vertical = 16.dp)
    Column {
        Row(
            modifier = rowMod,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = TextPrimary, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, color = TextSecondary, fontSize = 13.sp)
            }
            trailing?.invoke()
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .height(1.dp)
                    .background(Divider)
            )
        }
    }
}
