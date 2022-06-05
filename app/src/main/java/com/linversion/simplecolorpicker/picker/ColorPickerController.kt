package com.linversion.simplecolorpicker.picker

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow

/** Creates and remembers a [ColorPickerController] on the current composer. */
@Composable
public fun rememberColorPickerController(): ColorPickerController {
    return remember { ColorPickerController() }
}

/**
 * @author linversion
 * on 2022/5/18
 */
class ColorPickerController {
    /** An [ImageBitmap] to be drawn on the canvas as a palette. */
    internal var paletteBitmap: ImageBitmap? = null

    /** Size of the measured canvas dimensions (width and height). */
    internal val canvasSize: MutableState<IntSize> = mutableStateOf(IntSize(0, 0))

    /** Matrix of the [paletteBitmap], which is used to calculate pixel positions. */
    internal val imageBitmapMatrix: MutableState<Matrix> = mutableStateOf(Matrix())
    internal var reviseTick = mutableStateOf(0)

    internal var colorChangedTick = MutableStateFlow<ColorEnvelope?>(null)

    private val debounceHandler = Handler(Looper.getMainLooper())

    private var debounceDuration: Long = 0L

    private val _selectedPoint: MutableState<PointF> = mutableStateOf(PointF(0f, 0f))

    /** State of [PointF], which represents the currently selected coordinate. */
    public val selectedPoint: State<PointF> = _selectedPoint

    private val _selectedColor: MutableState<Color> = mutableStateOf(Color.Transparent)

    /** State of [Color], which represents the currently selected color value with alpha and brightness. */
    public val selectedColor: State<Color> = _selectedColor

    /** State of [Color], which represents the currently selected color value without alpha and brightness. */
    internal var pureSelectedColor: MutableState<Color> = mutableStateOf(Color.Transparent)

    /** Radius to draw default wheel. */
    internal var wheelRadius: Dp = 30.dp
        private set

    /** Paint to draw default wheel. */
    internal var wheelPaint: Paint = Paint().apply { color = Color.White }
        private set

    /** Set an [ImageBitmap] to draw on the canvas as a palette. */
    fun setPaletteImageBitmap(imageBitmap: Bitmap) {
        val targetSize = canvasSize.value.takeIf { it.width != 0 && it.height != 0 }
            ?: throw IllegalAccessException("Can't set an ImageBitmap before initializing the canvas")
        val copiedBitmap =
            imageBitmap.copy(Bitmap.Config.ARGB_8888, false)

        val resized = BitmapCalculator.inside(copiedBitmap, targetSize)
        paletteBitmap = resized.asImageBitmap()
//        imageBitmap.recycle()
        if(resized != copiedBitmap) {
            copiedBitmap.recycle()
        }
        selectCenter(fromUser = false)
        reviseTick.value++
    }

    /**
     * Select a specific point by coordinates and update a selected color.
     *
     * @param x x-coordinate to extract a pixel color.
     * @param y y-coordinate to extract a pixel color.
     * @param fromUser Represents this event is triggered by user or not.
     */
    fun selectByCoordinate(x: Float, y: Float, fromUser: Boolean, topLeft: Offset) {
        Log.d("selectByCoordinate", "selectByCoordinate: $x, $y")
//        val snapPoint = PointMapper.getColorPoint(this, PointF(x, y))
//        val extractedColor = if (isHsvColorPalette) {
//            extractPixelHsvColor(snapPoint.x, snapPoint.y)
//        } else {
//            extractPixelColor(snapPoint.x, snapPoint.y)
//        }

        val realY = y - topLeft.y
        val realX = x

        if (realY <= 0 || y >= topLeft.y + paletteBitmap!!.height) {
            //超出邊界
            return
        }
        val pixel = paletteBitmap!!.asAndroidBitmap().getPixel(realX.toInt(), realY.toInt())

        val extractedColor = Color(pixel)

        if (extractedColor != Color.Transparent) {
            // set the extracted color.
            pureSelectedColor.value = extractedColor
            _selectedPoint.value = PointF(realX, y)
            _selectedColor.value = applyHSVFactors(extractedColor)

            // notify color changes to the listeners.
            if (fromUser && debounceDuration != 0L) {
                notifyColorChangedWithDebounce(fromUser)
            } else {
                notifyColorChanged(fromUser, isColorLight(pixel))
            }
        }
    }

    /**
     * Select center point of the palette.
     *
     * @param fromUser Represents this event is triggered by user or not.
     */
    private fun selectCenter(fromUser: Boolean) {
        val x = paletteBitmap!!.width / 2
        val y = paletteBitmap!!.height / 2
        val pixel = paletteBitmap!!.asAndroidBitmap().getPixel(x, y)

        val extractedColor = Color(pixel)

        if (extractedColor != Color.Transparent) {
            // set the extracted color.
            pureSelectedColor.value = extractedColor
            _selectedPoint.value = PointF(x.toFloat(), (canvasSize.value.height / 2).toFloat())
            _selectedColor.value = applyHSVFactors(extractedColor)

            // notify color changes to the listeners.
            if (fromUser && debounceDuration != 0L) {
                notifyColorChangedWithDebounce(fromUser)
            } else {
                notifyColorChanged(fromUser, isColorLight(pixel))
            }
        }
    }

    /** Notify color changes to the color picker and other subcomponents. */
    private fun notifyColorChanged(fromUser: Boolean, isLight: Boolean) {
        val color = _selectedColor.value
        colorChangedTick.value = ColorEnvelope(color, color.hexCode, fromUser, isLight = isLight)
    }

    private fun isColorLight(color: Int) : Boolean{
        val grayLevel = android.graphics.Color.red(color) * 0.299 + android.graphics.Color.green(color) * 0.587 + android.graphics.Color.blue(color) * 0.114
        return grayLevel >= 192
    }

    /** Notify color changes to the color picker and other subcomponents with debounce duration. */
    private fun notifyColorChangedWithDebounce(fromUser: Boolean) {
        val runnable = { notifyColorChanged(fromUser, false)}
        debounceHandler.removeCallbacksAndMessages(null)
        debounceHandler.postDelayed(runnable, debounceDuration)
    }

    /**
     * Extract a pixel color from the [paletteBitmap].
     *
     * @param x x-coordinate to extract a pixel color.
     * @param y y-coordinate to extract a pixel color.
     *
     * @return An extracted [Color] from the desired coordinates.
     * if fail to extract a pixel value, it will returns [Color.Transparent].
     */
    internal fun extractPixelColor(x: Int, y: Int): Color {
//        val invertMatrix = Matrix()
//        imageBitmapMatrix.value.invert(invertMatrix)
//
//        val mappedPoints = floatArrayOf(x, y)
//        invertMatrix.mapPoints(mappedPoints)
//
//        val palette = paletteBitmap
//        if (palette != null &&
//            mappedPoints[0] >= 0 &&
//            mappedPoints[1] >= 0 &&
//            mappedPoints[0] < palette.width &&
//            mappedPoints[1] < palette.height
//        ) {
//            val scaleX = mappedPoints[0] / palette.width
//            val x1 = scaleX * palette.width
//            val scaleY = mappedPoints[1] / palette.height
//            val y1 = scaleY * palette.height
//            val pixelColor = palette.asAndroidBitmap().getPixel(x1.toInt(), y1.toInt())
//            return Color(pixelColor)
//        }
        val pixel = paletteBitmap!!.asAndroidBitmap().getPixel(x, y)
        return Color(pixel)
    }

    /** Return a [Color] that is applied with HSV color factors to the [color]. */
    private fun applyHSVFactors(color: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)

        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    internal fun releaseBitmap() {
        paletteBitmap?.asAndroidBitmap()?.recycle()
        paletteBitmap = null
    }
}