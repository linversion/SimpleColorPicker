package com.linversion.simplecolorpicker.camera

import android.util.Log
import android.util.Size
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.linversion.simplecolorpicker.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.TimeUnit

private const val TAG = "CameraPreview"

@Composable
fun CameraPreview(
    modifier: Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    viewModel: MainViewModel,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Preview use case 产出的 SurfaceRequest 交给 CameraXViewfinder 直接渲染
    val surfaceRequests = remember { MutableStateFlow<SurfaceRequest?>(null) }

    val imageAnalyzer = remember {
        val analysisSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(640, 480),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()
        ImageAnalysis.Builder()
            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setResolutionSelector(analysisSelector)
            .build()
            .also {
                it.setAnalyzer(
                    context.executor,
                    ColorAnalyzer { red, green, blue ->
                        viewModel.onSample(red, green, blue)
                    }
                )
            }
    }

    LaunchedEffect(cameraSelector) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider { request -> surfaceRequests.value = request }
        }
        try {
            val cameraProvider = context.getCameraProvider()
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            // 对准星测光对焦，但不锁死 AE/AWB。
            // 取景为居中裁剪，归一化中心点 (0.5, 0.5) 与分辨率、旋转无关
            val action = FocusMeteringAction.Builder(
                SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(0.5f, 0.5f),
                FocusMeteringAction.FLAG_AF or
                    FocusMeteringAction.FLAG_AE or
                    FocusMeteringAction.FLAG_AWB
            ).setAutoCancelDuration(4, TimeUnit.SECONDS).build()
            camera.cameraControl.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    val surfaceRequest = surfaceRequests.collectAsState().value
    if (surfaceRequest != null) {
        CameraXViewfinder(
            surfaceRequest = surfaceRequest,
            modifier = modifier,
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
        )
    }
}
