package com.linversion.simplecolorpicker

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.linversion.simplecolorpicker.camera.ColorScience
import com.linversion.simplecolorpicker.picker.ColorEnvelope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(ColorState(0, 0, 0, 255, false))
    val colorState: StateFlow<ColorState> = _state

    private var ema: ColorScience.Lab? = null
    private var published: ColorScience.Lab? = null
    private val smooth = 0.28f
    private val holdDeltaE = 1.6f

    fun onSample(red: Int, green: Int, blue: Int) {
        val current = _state.value
        if (current.locked) return

        val sample = ColorScience.rgbToLab(red, green, blue)
        val prev = ema
        val next = if (prev == null) {
            sample
        } else {
            ColorScience.Lab(
                prev.l + (sample.l - prev.l) * smooth,
                prev.a + (sample.a - prev.a) * smooth,
                prev.b + (sample.b - prev.b) * smooth
            )
        }
        ema = next

        val last = published
        if (last != null && ColorScience.deltaE(last, next) < holdDeltaE) return

        published = next
        val rgb = ColorScience.labToRgb(next)
        _state.value = current.copy(
            red = rgb[0],
            green = rgb[1],
            blue = rgb[2],
            alpha = 255,
            isLight = next.l >= 55f
        )
    }

    fun toggleLock() {
        _state.value = _state.value.copy(locked = !_state.value.locked)
    }
}

data class ColorState(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int,
    val isLight: Boolean,
    val colorEnvelope: ColorEnvelope? = null,
    val locked: Boolean = false
)

fun ColorState.toColor(): Color = Color(red, green, blue)
fun ColorState.toHexString(): String =
    "#%02X%02X%02X".format(red.coerceIn(0, 255), green.coerceIn(0, 255), blue.coerceIn(0, 255))
