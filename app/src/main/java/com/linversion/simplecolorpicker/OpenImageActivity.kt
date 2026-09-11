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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.linversion.simplecolorpicker.picker.ColorEnvelope
import com.linversion.simplecolorpicker.picker.ColorPickerController
import com.linversion.simplecolorpicker.picker.ImageColorPicker
import com.linversion.simplecolorpicker.picker.rememberColorPickerController
import com.linversion.simplecolorpicker.ui.theme.SimpleColorPickerTheme

private fun Intent.parcelableUri(key: String): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}

private fun Uri.toBitmap(context: Context): Bitmap? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, this)
        ImageDecoder.decodeBitmap(source)
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, this)
    }
}

class OpenImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val firstUri = intent.parcelableUri(key_uri)

        setContent {
            SimpleColorPickerTheme {
                Surface(color = MaterialTheme.colors.background) {
                    MainContent(uri = firstUri)
                }
            }
        }
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
            it.toBitmap(context)?.let { bitmap ->
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
            uri.toBitmap(context)?.let {
                ImageColorPicker(
                    modifier = Modifier.fillMaxSize(),
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
