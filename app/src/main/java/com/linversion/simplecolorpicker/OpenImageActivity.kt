package com.linversion.simplecolorpicker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
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
import com.linversion.simplecolorpicker.ui.widget.PaletteSheetContent
import kotlinx.coroutines.launch

private fun Intent.parcelableUri(key: String): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}

private fun Uri.toBitmap(context: Context): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, this)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, this)
        }
    } catch (_: Exception) {
        null
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
                    ImagePickScreen(uri = firstUri)
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ImagePickScreen(
    viewModel: OpenImageViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    uri: Uri?
) {
    val controller = rememberColorPickerController()
    val colorState = viewModel.colorState.collectAsState().value
    val palette = viewModel.palette.collectAsState().value
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetBackgroundColor = Color.White,
        sheetContent = { PaletteSheetContent(swatches = palette) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ImagePreview(viewModel = viewModel, controller = controller, firstUri = uri)
            ColorResult(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(100.dp),
                colorState = colorState,
                onUriResult = {
                    it.toBitmap(context)?.let { bitmap ->
                        controller.setPaletteImageBitmap(bitmap)
                        viewModel.extractPalette(bitmap)
                    }
                },
                onOpenPalette = {
                    scope.launch { sheetState.show() }
                }
            )
        }
    }
}

@Composable
fun ImagePreview(
    viewModel: OpenImageViewModel,
    controller: ColorPickerController,
    firstUri: Uri?
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray)
    ) {
        firstUri?.let { uri ->
            val bitmap = uri.toBitmap(context)
            if (bitmap != null) {
                LaunchedEffect(bitmap) {
                    viewModel.extractPalette(bitmap)
                }
                ImageColorPicker(
                    modifier = Modifier.fillMaxSize(),
                    controller = controller,
                    bitmap = bitmap,
                    onColorChanged = { colorEnvelope: ColorEnvelope ->
                        viewModel.updateColor(colorEnvelope)
                    }
                )
            }
        }
    }
}
