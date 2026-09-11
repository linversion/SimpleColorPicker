package com.linversion.simplecolorpicker

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linversion.simplecolorpicker.picker.ColorEnvelope
import com.linversion.simplecolorpicker.picker.PaletteExtractor
import com.linversion.simplecolorpicker.picker.PaletteSwatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OpenImageViewModel : ViewModel() {
    private var _uriState = MutableStateFlow<Uri?>(null)
    val uriState: StateFlow<Uri?> = _uriState

    private var _colorState = MutableStateFlow(ColorState(0, 0, 0, 1, false))
    val colorState: StateFlow<ColorState> = _colorState

    private var _palette = MutableStateFlow<List<PaletteSwatch>>(emptyList())
    val palette: StateFlow<List<PaletteSwatch>> = _palette

    fun updateUri(uri: Uri?) {
        viewModelScope.launch {
            Log.d("test", "updateUri: ")
            _uriState.emit(uri)
        }
    }

    fun updateColor(colorEnvelope: ColorEnvelope) {
        _colorState.value = ColorState(
            red = 0,
            green = 0,
            blue = 0,
            alpha = 0,
            isLight = colorEnvelope.isLight,
            colorEnvelope = colorEnvelope
        )
    }

    fun extractPalette(bitmap: Bitmap) {
        viewModelScope.launch {
            val swatches = withContext(Dispatchers.Default) {
                PaletteExtractor.extract(bitmap)
            }
            _palette.value = swatches
        }
    }
}
