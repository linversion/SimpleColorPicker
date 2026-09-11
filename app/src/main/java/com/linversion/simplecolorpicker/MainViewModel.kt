package com.linversion.simplecolorpicker

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.linversion.simplecolorpicker.picker.ColorEnvelope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {
    private var _state = MutableStateFlow(ColorState(0, 0, 0, 0, false))
    val colorState: StateFlow<ColorState> = _state

    private var emaR = -1f
    private var emaG = -1f
    private var emaB = -1f
    private val smooth = 0.25f

    fun updateColor(alpha: Int, red: Int, green: Int, blue: Int, isLight: Boolean) {
        if (emaR < 0f) {
            emaR = red.toFloat()
            emaG = green.toFloat()
            emaB = blue.toFloat()
        } else {
            emaR += (red - emaR) * smooth
            emaG += (green - emaG) * smooth
            emaB += (blue - emaB) * smooth
        }
        val r = emaR.toInt().coerceIn(0, 255)
        val g = emaG.toInt().coerceIn(0, 255)
        val b = emaB.toInt().coerceIn(0, 255)
        _state.value = ColorState(r, g, b, alpha, isLight)
    }
}

data class ColorState(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int,
    val isLight: Boolean,
    val colorEnvelope: ColorEnvelope? = null
)

fun ColorState.toColor(): Color = Color(red, green, blue)
fun ColorState.toHexString(): String =
    "#%02X%02X%02X".format(red.coerceIn(0, 255), green.coerceIn(0, 255), blue.coerceIn(0, 255))
