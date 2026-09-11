package com.linversion.simplecolorpicker.picker

import android.graphics.Bitmap
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

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
            controller.releaseBitmap()
        }
    }

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
                        controller.selectByCoordinate(event.x, event.y, true)
                        true
                    }
                    else -> false
                }
            }
    ) {
        drawIntoCanvas { canvas ->
            controller.paletteBitmap?.let { imageBitmap ->
                val left = (size.width - imageBitmap.width) / 2f
                val top = (size.height - imageBitmap.height) / 2f
                controller.imageOffset = Offset(left, top)
                drawImage(image = imageBitmap, topLeft = controller.imageOffset, alpha = 1f, style = Fill)
            }
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
