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
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.linversion.simplecolorpicker.picker.ColorEnvelope
import com.linversion.simplecolorpicker.picker.ColorPickerController
import com.linversion.simplecolorpicker.picker.ImageColorPicker
import com.linversion.simplecolorpicker.picker.rememberColorPickerController
import com.linversion.simplecolorpicker.ui.theme.SimpleColorPickerTheme

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

    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colors.isLight
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
    }
    val controller = rememberColorPickerController()
    val colorState = viewModel.colorState.collectAsState().value
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        ImagePreview(viewModel = viewModel, controller, uri)
        ColorResult(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(100.dp),
            colorState = colorState
        ) {
            val bitmap: Bitmap? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            bitmap?.let {
                controller.setPaletteImageBitmap(bitmap)
            }
        }
    }
}

@Composable
fun ImagePreview(viewModel: OpenImageViewModel, controller: ColorPickerController, firstUri: Uri?) {
    val context = LocalContext.current

    Log.d("test", "ImagePreview: ")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
    ) {

        firstUri?.let { uri ->
            val bitmap: Bitmap? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }

            bitmap?.let {
                ImageColorPicker(
                    modifier = Modifier
                        .fillMaxSize(),
                    controller = controller,
                    bitmap = it,
                    onColorChanged = { colorEnvelope: ColorEnvelope ->
                        viewModel.updateColor(colorEnvelope)
                    }
                )
            }
        }
    }
}