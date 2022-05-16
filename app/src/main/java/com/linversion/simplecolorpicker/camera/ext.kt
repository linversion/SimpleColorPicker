package com.linversion.simplecolorpicker.camera

import android.content.Context
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * @author linversion
 * on 2022/5/14
 */
/**
 * use a suspendCoroutine to wrap the CameraX call which returns a Future
 * to which we attach a callback function that cause our suspend function to return
 * the expected result
 */
suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { future ->
        future.addListener({
            continuation.resume(future.get())
        }, executor)
    }
}

val Context.executor: Executor get() = ContextCompat.getMainExecutor(this)