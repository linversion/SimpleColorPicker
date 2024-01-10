package com.linversion.simplecolorpicker.camera

import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import kotlin.experimental.and

/**
 * @author linversion
 * on 2022/5/14
 */
class ColorAnalyzer(private val listener: (alpha: Int, red: Int, green: Int, blue: Int, isLight: Boolean) -> Unit) : ImageAnalysis.Analyzer {
    private val TAG = "ColorAnalyzer"
    private var yArr: ByteArray? = null
    private var uArr: ByteArray? = null
    private var vArr: ByteArray? = null
    private var lastAnalyzeTime = 0L
    private val analyzeInterval = 3000

    override fun analyze(image: ImageProxy) {
        //mi6 1920*1080
        // format YUV_420_888
        // rowStride=640,
        // pixelStride=1
        val now = SystemClock.uptimeMillis()
        if (lastAnalyzeTime == 0L) {
            lastAnalyzeTime = now
        }
        if (now - lastAnalyzeTime < analyzeInterval) {
            image.close()
            return
        }

        val width = image.width
        val height = image.height
//        Log.d(
//            TAG,
//            "analyze: size=$width*$height"
//        )

        //y
        val yBuffer = image.planes[0].buffer
        if (yArr == null) {
            yArr = ByteArray(yBuffer.remaining())
        }
        yBuffer.rewind()
        yBuffer.get(yArr!!)
        val yPixelStride = image.planes[0].pixelStride
        val yRowStride = image.planes[0].rowStride

        //u
        val uBuffer = image.planes[1].buffer
        if (uArr == null) {
            uArr = ByteArray(uBuffer.remaining())
        }
        uBuffer.rewind()
        uBuffer.get(uArr!!)
        val uPixelStride = image.planes[1].pixelStride
        val uRowStride = image.planes[1].rowStride

        //v
        val vBuffer = image.planes[2].buffer
        if (vArr == null) {
            vArr = ByteArray(vBuffer.remaining())
        }
        vBuffer.rewind()
        vBuffer.get(vArr!!)

        val vPixelStride = image.planes[2].pixelStride
        val vRowStride = image.planes[2].rowStride

        val y = (yArr!![(height * yRowStride + width * yPixelStride) / 2]).toInt() and 255
        val u = ((uArr!![(height * uRowStride + width * uPixelStride) / 4]).toInt() and 255) - 128
        val v = ((vArr!![(height * vRowStride + width * vPixelStride) / 4]).toInt() and 255) - 128

        val r = (y + (1.370705 * v)).toInt()
        val g = (y - (0.698001 * v) - (0.337633 * u)).toInt()
        val b = (y + (1.732446 * u)).toInt()

        listener.invoke(255, r, g, b, y >= 192)
        lastAnalyzeTime = now
        image.close()
    }
}