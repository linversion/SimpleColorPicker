package com.linversion.simplecolorpicker.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * @author linversion
 * on 2022/5/14
 */
@Composable
fun Ring(color: Color) {

    Canvas(modifier = Modifier.size(24.dp).background(color = Color.Transparent)
    ) {
        drawCircle(
            color,
            radius = 12.dp.toPx(),
            style = Stroke(2.dp.toPx())
        )
    }
}