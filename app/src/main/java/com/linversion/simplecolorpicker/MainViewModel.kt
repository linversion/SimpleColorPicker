package com.linversion.simplecolorpicker

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * @author linversion
 * on 2022/5/15
 */
class MainViewModel : ViewModel() {
    private var _state = MutableStateFlow(ColorState(0, 0, 0, 0, false))
    val colorState: StateFlow<ColorState> = _state

    fun updateColor(alpha: Int, red: Int, green: Int, blue: Int, isLight: Boolean) {
        viewModelScope.launch {
            val color = ColorState(red, green, blue, alpha, isLight)
            _state.emit(color)
        }
    }
}

data class ColorState(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int,
    val isLight: Boolean
)

fun ColorState.toColor(): Color = Color(this.red, this.green, this.blue)
fun ColorState.toHexString(): String =
    "#${this.red.toString(16)}${this.green.toString(16)}${this.blue.toString(16)}"