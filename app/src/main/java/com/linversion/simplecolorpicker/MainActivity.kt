package com.linversion.simplecolorpicker

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.linversion.simplecolorpicker.camera.CameraPreview
import com.linversion.simplecolorpicker.ui.theme.SimpleColorPickerTheme
import com.linversion.simplecolorpicker.ui.widget.Ring
import com.linversion.simplecolorpicker.util.Permission

@ExperimentalPermissionsApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProvideWindowInsets {
                SimpleColorPickerTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(color = MaterialTheme.colors.background) {
                        MainContent(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        //Hide the status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}

@ExperimentalPermissionsApi
@Composable
fun MainContent(modifier: Modifier, mainViewModel: MainViewModel = viewModel()) {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colors.isLight
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
    }

    val context = LocalContext.current
    Permission(
        permission = Manifest.permission.CAMERA,
        rationale = "You said you wanted a picture, so I'm going to have to ask for permission",
        permissionNotAvailableContent = {
            Column(modifier) {
                Text("O noes! No Camera!")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }) {
                    Text("Open Settings")
                }
            }
        }
    ) {
        Box(modifier = modifier) {
            val currentColorState = mainViewModel.colorState.collectAsState().value
            CameraPreview(modifier = Modifier.fillMaxSize(), viewModel = mainViewModel)
            Box(modifier = Modifier.align(Alignment.Center)) {
                Ring(color = if (currentColorState.isLight) Color.Black else Color.White)
            }
            Result(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(100.dp),
                colorState = currentColorState
            )
        }
    }
}

@Composable
fun Result(
    modifier: Modifier,
    colorState: ColorState
) {

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(contract =
    ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            OpenImageActivity.startActivity(context, it)
        }
    }

    Box(modifier = modifier
        .background(colorState.toColor())
        .statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = colorState.toHexString(),
                color = if (colorState.isLight) Color.Black else Color.White,
                modifier = Modifier.padding(start = 12.dp)
            )

            IconButton(onClick = {
                launcher.launch("image/*")
            }) {
                Icon(imageVector = Icons.Default.Image, contentDescription = "Choose an image")
            }
        }
    }
}
