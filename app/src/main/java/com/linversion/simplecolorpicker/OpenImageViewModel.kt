package com.linversion.simplecolorpicker

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linversion.simplecolorpicker.picker.ColorEnvelope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * @author linversion
 * on 2022/5/16
 */
class OpenImageViewModel : ViewModel() {
    private var _uriState = MutableStateFlow<Uri?>(null)
    val uriState: StateFlow<Uri?> = _uriState

    private var _colorState = MutableStateFlow<ColorState>(ColorState(0, 0, 0, 1, false))
    val colorState: StateFlow<ColorState> = _colorState

    fun updateUri(uri: Uri?) {
        viewModelScope.launch {
            Log.d("test", "updateUri: ")
            _uriState.emit(uri)
        }
    }

    fun updateColor(colorEnvelope: ColorEnvelope) {
        viewModelScope.launch {
            val color = ColorState(0, 0, 0, 0, colorEnvelope.isLight, colorEnvelope = colorEnvelope)
            _colorState.emit(color)
        }
    }
}