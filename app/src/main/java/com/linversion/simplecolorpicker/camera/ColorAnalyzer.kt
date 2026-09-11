package com.linversion.simplecolorpicker.camera

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * RGBA 圆形孔径中值取样。取景为居中裁剪，准星即分析缓冲区中心。
 */
class ColorAnalyzer(
    private val listener: (red: Int, green: Int, blue: Int) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalyzeTime = 0L
    private val analyzeIntervalMs = 50L
    private val radiusPx = 8
    private val rs = IntArray((2 * radiusPx + 1) * (2 * radiusPx + 1))
    private val gs = IntArray(rs.size)
    private val bs = IntArray(rs.size)

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.uptimeMillis()
        if (lastAnalyzeTime != 0L && now - lastAnalyzeTime < analyzeIntervalMs) {
            image.close()
            return
        }
        lastAnalyzeTime = now

        val color = sampleMedian(image, image.width / 2, image.height / 2)
        if (color != null) {
            listener(color[0], color[1], color[2])
        }
        image.close()
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
