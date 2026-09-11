package com.linversion.simplecolorpicker.camera

import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.linversion.simplecolorpicker.MainViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraPreview(
    modifier: Modifier,
    scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    viewModel: MainViewModel,
) {
    val coroutineScope = rememberCoroutineScope()
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

            val previewUseCase = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysisSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(640, 480),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(analysisSelector)
                .build()
                .also {
                    it.setAnalyzer(
                        context.executor,
                        ColorAnalyzer { alpha, red, green, blue, isLight ->
                            viewModel.updateColor(alpha, red, green, blue, isLight)
                        }
                    )
                }

            coroutineScope.launch {
                val cameraProvider = context.getCameraProvider()
                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        previewUseCase,
                        imageAnalyzer
                    )

                    previewView.post {
                        if (previewView.width == 0 || previewView.height == 0) return@post
                        val point = previewView.meteringPointFactory.createPoint(
                            previewView.width / 2f,
                            previewView.height / 2f
                        )
                        val action = FocusMeteringAction.Builder(
                            point,
                            FocusMeteringAction.FLAG_AF or
                                FocusMeteringAction.FLAG_AE or
                                FocusMeteringAction.FLAG_AWB
                        ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                        camera.cameraControl.startFocusAndMetering(action)
                    }

                    val camera2Control = Camera2CameraControl.from(camera.cameraControl)
                    val options = CaptureRequestOptions.Builder()
                        .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
                        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
                        .build()
                    camera2Control.setCaptureRequestOptions(options)
                } catch (e: Exception) {
                    Log.e("CameraView", "Use case binding failed", e)
                }
            }

            previewView
        }
    )
}
