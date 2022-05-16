package com.linversion.simplecolorpicker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.linversion.simplecolorpicker.ui.theme.SimpleColorPickerTheme
import com.linversion.simplecolorpicker.ui.widget.Ring

/**
 * @author linversion
 * on 2022/5/16
 */
class OpenImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firstUri = intent.getParcelableExtra<Uri>(key_uri)

        setContent {
            ProvideWindowInsets {
                SimpleColorPickerTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(color = MaterialTheme.colors.background) {
                        MainContent(uri = firstUri)
                    }
                }
            }
        }
        //Hide the status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    companion object {
        private const val key_uri = "key_uri"
        fun startActivity(context: Context, uri: Uri) {
            Intent(context, OpenImageActivity::class.java).let {
                it.putExtra(key_uri, uri)
                context.startActivity(it)
            }
        }
    }
}

@Composable
fun MainContent(
    viewModel: OpenImageViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    uri: Uri?
) {
    viewModel.updateUri(uri)

    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colors.isLight
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ImagePreview(viewModel = viewModel)
        ImageResult(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(100.dp), viewModel
        )
    }
}

@Composable
fun ImagePreview(viewModel: OpenImageViewModel) {
    val context = LocalContext.current
    val targetUri = viewModel.uriState.collectAsState().value
    val bitmap = remember {
        mutableStateOf<Bitmap?>(null)
    }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        val calScale = scale * zoomChange
        Log.d("test", "ImagePreview: $calScale")
        scale = if (calScale < 1f) 1f else calScale
        offset += offsetChange
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
    ) {
        targetUri?.let { uri ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                bitmap.value = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                bitmap.value = ImageDecoder.decodeBitmap(source)
            }

            bitmap.value?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Current image",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            translationX = offset.x
                            translationY = offset.y
                            scaleX = scale
                            scaleY = scale
                        }
                        .transformable(state),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.Center)) {
            val currentColorState = viewModel.colorState.collectAsState().value
            Ring(color = if (currentColorState.isLight) Color.Black else Color.White)
        }
    }
}

@Composable
fun ImageResult(
    modifier: Modifier,
    viewModel: OpenImageViewModel
) {
    val launcher = rememberLauncherForActivityResult(
        contract =
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateUri(uri)
        }
    }

    val colorState = viewModel.colorState.collectAsState().value

    Box(
        modifier = modifier
            .background(colorState.toColor())
            .statusBarsPadding()
    ) {
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