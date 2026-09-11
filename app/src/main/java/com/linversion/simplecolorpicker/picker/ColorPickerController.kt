package com.linversion.simplecolorpicker.picker

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun rememberColorPickerController(): ColorPickerController {
    return remember { ColorPickerController() }
}

class ColorPickerController {
    internal var paletteBitmap: ImageBitmap? = null
    internal val canvasSize: MutableState<IntSize> = mutableStateOf(IntSize(0, 0))
    internal val imageBitmapMatrix: MutableState<Matrix> = mutableStateOf(Matrix())
    internal var reviseTick = mutableStateOf(0)
    internal var colorChangedTick = MutableStateFlow<ColorEnvelope?>(null)
    /** 图片在 Canvas 上的绘制原点，用于触摸坐标映射。 */
    internal var imageOffset: Offset = Offset.Zero

    private val debounceHandler = Handler(Looper.getMainLooper())
    private var debounceDuration: Long = 0L
    private val _selectedPoint: MutableState<PointF> = mutableStateOf(PointF(0f, 0f))
    val selectedPoint: State<PointF> = _selectedPoint
    private val _selectedColor: MutableState<Color> = mutableStateOf(Color.Transparent)
    val selectedColor: State<Color> = _selectedColor
    internal var pureSelectedColor: MutableState<Color> = mutableStateOf(Color.Transparent)
    internal var wheelRadius: Dp = 30.dp
        private set
    internal var wheelPaint: Paint = Paint().apply { color = Color.White }
        private set

    fun setPaletteImageBitmap(imageBitmap: Bitmap) {
        val targetSize = canvasSize.value.takeIf { it.width != 0 && it.height != 0 }
            ?: throw IllegalAccessException("Can't set an ImageBitmap before initializing the canvas")
        val copiedBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, false)
        val resized = BitmapCalculator.inside(copiedBitmap, targetSize)
        paletteBitmap = resized.asImageBitmap()
        if (resized != copiedBitmap) {
            copiedBitmap.recycle()
        }
        selectCenter(fromUser = false)
        reviseTick.value++
    }

    fun selectByCoordinate(x: Float, y: Float, fromUser: Boolean) {
        val palette = paletteBitmap ?: return
        val bitmap = palette.asAndroidBitmap()
        val bx = (x - imageOffset.x).toInt()
        val by = (y - imageOffset.y).toInt()
        if (bx !in 0 until bitmap.width || by !in 0 until bitmap.height) return

        val pixel = sampleBitmapMedian(bitmap, bx, by) ?: return
        applySelection(pixel, x, y, fromUser)
    }

    private fun selectCenter(fromUser: Boolean) {
        val palette = paletteBitmap ?: return
        val bitmap = palette.asAndroidBitmap()
        val bx = bitmap.width / 2
        val by = bitmap.height / 2
        val pixel = sampleBitmapMedian(bitmap, bx, by) ?: return
        val canvasX = imageOffset.x + bx
        val canvasY = imageOffset.y + by
        applySelection(pixel, canvasX, canvasY, fromUser)
    }

    private fun applySelection(pixel: Int, canvasX: Float, canvasY: Float, fromUser: Boolean) {
        val extractedColor = Color(pixel)
        if (extractedColor == Color.Transparent) return
        pureSelectedColor.value = extractedColor
        _selectedPoint.value = PointF(canvasX, canvasY)
        _selectedColor.value = extractedColor
        notifyColorChanged(fromUser, isColorLight(pixel))
    }

    private fun notifyColorChanged(fromUser: Boolean, isLight: Boolean) {
        val color = _selectedColor.value
        colorChangedTick.value = ColorEnvelope(color, color.hexCode, fromUser, isLight = isLight)
    }

    private fun isColorLight(color: Int): Boolean {
        val grayLevel = android.graphics.Color.red(color) * 0.299 +
            android.graphics.Color.green(color) * 0.587 +
            android.graphics.Color.blue(color) * 0.114
        return grayLevel >= 192
    }

    internal fun extractPixelColor(x: Int, y: Int): Color {
        val palette = paletteBitmap ?: return Color.Transparent
        val bitmap = palette.asAndroidBitmap()
        if (x !in 0 until bitmap.width || y !in 0 until bitmap.height) return Color.Transparent
        return Color(bitmap.getPixel(x, y))
    }

    internal fun releaseBitmap() {
        paletteBitmap?.asAndroidBitmap()?.recycle()
        paletteBitmap = null
    }
}
