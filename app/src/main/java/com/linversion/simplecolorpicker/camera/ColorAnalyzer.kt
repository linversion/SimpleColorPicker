package com.linversion.simplecolorpicker.camera

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * 对准星附近做空间平均，避免单像素噪声导致颜色乱跳。
 */
class ColorAnalyzer(
    private val listener: (alpha: Int, red: Int, green: Int, blue: Int, isLight: Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalyzeTime = 0L
    private val analyzeIntervalMs = 80L
    private val sampleRadius = 7

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.uptimeMillis()
        if (lastAnalyzeTime != 0L && now - lastAnalyzeTime < analyzeIntervalMs) {
            image.close()
            return
        }
        lastAnalyzeTime = now

        val (r, g, b, yAvg) = sampleCenterBlock(image, sampleRadius)
        listener.invoke(255, r, g, b, yAvg >= 160)
        image.close()
    }

    private fun sampleCenterBlock(image: ImageProxy, radius: Int): IntArray {
        val width = image.width
        val height = image.height
        val cx = width / 2
        val cy = height / 2

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer
        val yRow = yPlane.rowStride
        val yPix = yPlane.pixelStride
        val uRow = uPlane.rowStride
        val uPix = uPlane.pixelStride
        val vRow = vPlane.rowStride
        val vPix = vPlane.pixelStride

        var rSum = 0
        var gSum = 0
        var bSum = 0
        var ySum = 0
        var count = 0

        for (dy in -radius..radius) {
            val py = (cy + dy).coerceIn(0, height - 1)
            for (dx in -radius..radius) {
                val px = (cx + dx).coerceIn(0, width - 1)
                val yVal = yBuf.get(py * yRow + px * yPix).toInt() and 0xFF
                val ux = px / 2
                val uy = py / 2
                val uVal = (uBuf.get(uy * uRow + ux * uPix).toInt() and 0xFF) - 128
                val vVal = (vBuf.get(uy * vRow + ux * vPix).toInt() and 0xFF) - 128

                // BT.601 YUV -> RGB
                val r = (yVal + 1.370705 * vVal).toInt().coerceIn(0, 255)
                val g = (yVal - 0.698001 * vVal - 0.337633 * uVal).toInt().coerceIn(0, 255)
                val b = (yVal + 1.732446 * uVal).toInt().coerceIn(0, 255)
                rSum += r
                gSum += g
                bSum += b
                ySum += yVal
                count++
            }
        }

        if (count == 0) return intArrayOf(0, 0, 0, 0)
        return intArrayOf(rSum / count, gSum / count, bSum / count, ySum / count)
    }
}
