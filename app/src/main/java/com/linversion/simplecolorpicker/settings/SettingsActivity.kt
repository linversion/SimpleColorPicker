package com.linversion.simplecolorpicker.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.linversion.simplecolorpicker.BuildConfig
import com.linversion.simplecolorpicker.ui.theme.SimpleColorPickerTheme

/**
 * @author linversion
 * on 2022/6/5
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleColorPickerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    SettingsContent() {
                        finish()
                    }
                }
            }
        }
    }

    companion object {

        fun startActivity(context: Context) {
            Intent(context, SettingsActivity::class.java).let {
                context.startActivity(it)
            }
        }
    }
}

@Composable
fun SettingsContent(onClickBack: () -> Unit) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        onClickBack.invoke()
                    }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val uriHandler = LocalUriHandler.current

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clickable {
                                uriHandler.openUri("https://sites.google.com/view/linversion-privacy-policy")
                            }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PrivacyTip,
                            contentDescription = "Privacy Policy",
                            tint = Color.Gray
                        )

                        Text(text = "Privacy Policy", color = Color.Gray)
                    }
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Apps,
                            contentDescription = "Version Name",
                            tint = Color.Gray
                        )

                        Text(text = "v${BuildConfig.VERSION_NAME}", color = Color.Gray)
                    }
                }
            }
        }
    }
}