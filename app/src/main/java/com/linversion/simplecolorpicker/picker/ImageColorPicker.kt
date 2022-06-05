package com.linversion.simplecolorpicker.picker

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import androidx.core.util.Pools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

/**
 * @author linversion
 * on 2022/5/18
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ImageColorPicker(
    modifier: Modifier,
    bitmap: Bitmap,
    controller: ColorPickerController,
    onColorChanged: (colorEnvelope: ColorEnvelope) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(key1 = controller) {
        coroutineScope.launch(Dispatchers.Main) {
            with(controller) {
                setPaletteImageBitmap(bitmap)
                colorChangedTick.mapNotNull { it }.collect {
                    onColorChanged.invoke(it)
                }
            }
        }

        onDispose {
            Log.d("test", "ImageColorPicker: controller dispose")
            controller.releaseBitmap()
        }
    }
    var topLeft = Offset(0f, 0f)
    Log.d("test", "ImageColorPicker: invoke Canvas")
    Canvas(
        modifier = modifier
            .onSizeChanged { size ->
                if (size.width != 0 && size.height != 0) {
                    controller.canvasSize.value = size
                }
            }
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE,
                    MotionEvent.ACTION_UP -> {
                        controller.selectByCoordinate(event.x, event.y, true, topLeft)
                        true
                    }
                    else -> false
                }
            }
    ) {
        drawIntoCanvas { canvas ->
            controller.paletteBitmap?.let { imageBitmap ->
//                var dx = 0f
//                var dy = 0f
//                val scale: Float
//                val shaderMatrix = android.graphics.Matrix()
//                val shader = ImageShader(imageBitmap, TileMode.Mirror)
//                val brush = ShaderBrush(shader)
//                val paint = paintPool.acquire() ?: Paint()
//                paint.asFrameworkPaint().apply {
//                    isAntiAlias = true
//                    isDither = true
//                    isFilterBitmap = true
//                }

                //cache the paint in the internal stack
//                canvas.saveLayer(size.toRect(), paint)
                val mDrawableRect = RectF(0f, 0f, size.width, size.height)
                val bitmapWidth = imageBitmap.asAndroidBitmap().width
                val bitmapHeight = imageBitmap.asAndroidBitmap().height
                Log.d(
                    "ImageColorPicker",
                    "ImageColorPicker: 绘制区域: w=${mDrawableRect.width()},h=${mDrawableRect.height()} 图片: w=$bitmapWidth,h=$bitmapHeight"
                )

                //apply the scaled matrix to the shader
//                shader.setLocalMatrix(shaderMatrix)
                //set the shader matrix to the controller
//                controller.imageBitmapMatrix.value = shaderMatrix
                topLeft = Offset(0f, center.y - (imageBitmap.height / 2))
                drawImage(image = imageBitmap, topLeft = topLeft, alpha = 1f, style = Fill)
                //restore canvas
//                canvas.restore()
                // resets the paint and release to the pool
//                paint.asFrameworkPaint().reset()
//                paintPool.release(paint)
            }

            // draw wheel bitmap on the canvas
            val point = controller.selectedPoint.value
            canvas.drawCircle(
                Offset(point.x, point.y),
                controller.wheelRadius.value,
                controller.wheelPaint
            )
        }

        controller.reviseTick.value
    }
}

/** paint pool which caching and reusing [Paint] instances. */
private val paintPool = Pools.SimplePool<Paint>(2)