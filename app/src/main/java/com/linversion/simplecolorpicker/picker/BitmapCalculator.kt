/*
 * Copyright (C) 2022 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linversion.simplecolorpicker.picker

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import androidx.compose.ui.unit.IntSize

/**
 * A bitmap calculator to scaling and cropping to a target size.
 */
internal object BitmapCalculator {

    /**
     * Scale the source with maintaining the source's aspect ratio
     * so that both dimensions (width and height) of the source will be equal to or less than the
     * corresponding dimension of the target size.
     */
    internal fun scaleBitmap(bitmap: Bitmap, targetSize: IntSize): Bitmap {
        return Bitmap.createScaledBitmap(
            bitmap,
            targetSize.width,
            targetSize.height,
            false
        )
    }

    /**
     * Crop ths source the corresponding dimension of the target size.
     * so that if the dimensions (width and height) source is bigger than the target size,
     * it will be cut off from the center.
     */
    internal fun cropBitmap(bitmap: Bitmap, targetSize: IntSize): Bitmap {
        return ThumbnailUtils.extractThumbnail(bitmap, targetSize.width, targetSize.height)
    }

    /**
     * Scale the source with maintaining the source's aspect ratio
     * so that if both dimensions (width and height) of the source is smaller than the target size,
     * it will not be scaled.
     */
    internal fun inside(bitmap: Bitmap, targetSize: IntSize): Bitmap {
        // 在这里算一个合适的比例

        //A1
        val picRatio = bitmap.width / bitmap.height.toFloat()
        //A
        val canvasRatio = targetSize.width / targetSize.height.toFloat()
        var scaleW = bitmap.width
        var scaleH = bitmap.height

        if (picRatio > canvasRatio) {
            //加载的图片相对容器更宽、更矮
            scaleW = targetSize.width
            scaleH = (targetSize.width / picRatio).toInt()
        } else if (picRatio < canvasRatio) {
            //加载的图片相对容器更高、更窄
            scaleH = targetSize.height
            scaleW = (targetSize.height * picRatio).toInt()
        } else if (picRatio == canvasRatio) {
            //加载的图片宽高与容器相当
            scaleW = targetSize.width
            scaleH = targetSize.height
        }

        return scaleBitmap(bitmap, IntSize(scaleW, scaleH))
    }
}
