package com.linversion.simplecolorpicker.camera

import android.os.SystemClock
import android.view.View
import androidx.annotation.OptIn
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.CoordinateTransform
import androidx.camera.view.transform.ImageProxyTransformFactory
import java.lang.ref.WeakReference

/**
 * RGBA 圆形孔径中值取样。准星从 PreviewView 映射到 analysis buffer。
 */
class ColorAnalyzer(
    previewView: PreviewView,
    private val listener: (red: Int, green: Int, blue: Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val previewRef = WeakReference(previewView)
    private val transformFactory = ImageProxyTransformFactory().apply {
        isUsingCropRect = true
        isUsingRotationDegrees = true
    }
    private var lastAnalyzeTime = 0L
    private val analyzeIntervalMs = 50L
    private val radiusPx = 8
    private val rs = IntArray((2 * radiusPx + 1) * (2 * radiusPx + 1))
    private val gs = IntArray(rs.size)
    private val bs = IntArray(rs.size)

    @OptIn(TransformExperimental::class)
    override fun analyze(image: ImageProxy) {
        val now = SystemClock.uptimeMillis()
        if (lastAnalyzeTime != 0L && now - lastAnalyzeTime < analyzeIntervalMs) {
            image.close()
            return
        }
        lastAnalyzeTime = now

        val preview = previewRef.get()
        if (preview == null || preview.width == 0 || preview.height == 0) {
            image.close()
            return
        }

        val (cx, cy) = mapPreviewCenter(preview, image)
        val color = sampleMedian(image, cx, cy)
        if (color != null) {
            listener(color[0], color[1], color[2])
        }
        image.close()
    }

    @OptIn(TransformExperimental::class)
    private fun mapPreviewCenter(preview: PreviewView, image: ImageProxy): Pair<Int, Int> {
        val source = preview.outputTransform ?: return image.width / 2 to image.height / 2
        return try {
            val target = transformFactory.getOutputTransform(image)
            val transform = CoordinateTransform(source, target)
            val pts = floatArrayOf(preview.width / 2f, preview.height / 2f)
            transform.mapPoints(pts)
            pts[0].toInt().coerceIn(0, image.width - 1) to
                pts[1].toInt().coerceIn(0, image.height - 1)
        } catch (_: Exception) {
            image.width / 2 to image.height / 2
        }
    }

    private fun sampleMedian(image: ImageProxy, cx: Int, cy: Int): IntArray? {
        val plane = image.planes[0]
        val buf = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val width = image.width
        val height = image.height
        val r2 = radiusPx * radiusPx
        var n = 0

        for (dy in -radiusPx..radiusPx) {
            val y = cy + dy
            if (y !in 0 until height) continue
            for (dx in -radiusPx..radiusPx) {
                if (dx * dx + dy * dy > r2) continue
                val x = cx + dx
                if (x !in 0 until width) continue
                val i = y * rowStride + x * pixelStride
                if (i + 2 >= buf.capacity()) continue
                rs[n] = buf.get(i).toInt() and 0xFF
                gs[n] = buf.get(i + 1).toInt() and 0xFF
                bs[n] = buf.get(i + 2).toInt() and 0xFF
                n++
            }
        }
        if (n == 0) return null
        return intArrayOf(median(rs, n), median(gs, n), median(bs, n))
    }

    private fun median(values: IntArray, n: Int): Int {
        values.sort(0, n)
        return values[n / 2]
    }
}
