package com.linversion.simplecolorpicker.camera

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.linversion.simplecolorpicker.MainViewModel
import kotlinx.coroutines.launch

/**
 * @author linversion
 * on 2022/5/14
 */
@Composable
fun CameraPreview(
    modifier: Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    viewModel: MainViewModel,
) {
    // this coroutine scope is bound to the current composable context.[Side-effect]
    val coroutineScope = rememberCoroutineScope()
    // The compositionLocal containing the current lifecycleOwner
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { context ->

            val previewView = PreviewView(context).apply {
                this.scaleType = scaleType
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            // CameraX Preview UseCase
            // an object that encapsulates the purpose for which you are using the camera
            val previewViewUseCase = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
//                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888) // YUV颜色空间转RGBA_8888，
                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST) // 非阻塞模式
                .build()
                .also {
                    it.setAnalyzer(
                        context.executor,
                        ColorAnalyzer {alpha, red, green, blue, isLight ->
                            viewModel.updateColor(alpha, red, green, blue, isLight)
                        }
                    )
                }
            coroutineScope.launch {
                val cameraProvider = context.getCameraProvider()
                try {
                    // must unbind the use-cases before rebinding them.
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        previewViewUseCase,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    Log.e("CameraView", "Use case binding failed", e)
                }
            }

            previewView
        }
    )
}