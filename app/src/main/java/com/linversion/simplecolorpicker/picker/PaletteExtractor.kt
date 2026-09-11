package com.linversion.simplecolorpicker.picker

import android.graphics.Bitmap
import android.graphics.Color
import androidx.palette.graphics.Palette
import com.linversion.simplecolorpicker.camera.ColorScience

/**
 * 图片主色：Android Palette（median-cut）+ Lab ΔE 合并相近色。
 */
data class PaletteSwatch(
    val rgb: Int,
    val hex: String,
    val population: Int
)

object PaletteExtractor {

    fun extract(bitmap: Bitmap, maxColors: Int = 8): List<PaletteSwatch> {
        val readable = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return emptyList()
        } else {
            bitmap
        }
        val palette = Palette.from(readable)
            .maximumColorCount(16)
            .resizeBitmapArea(96 * 96)
            .clearFilters()
            .generate()
        val merged = mutableListOf<Palette.Swatch>()
        palette.swatches.sortedByDescending { it.population }.forEach { swatch ->
            val lab = rgbToLab(swatch.rgb)
            val same = merged.indexOfFirst { ColorScience.deltaE(rgbToLab(it.rgb), lab) < 14f }
            if (same < 0) {
                merged.add(swatch)
            } else if (swatch.population > merged[same].population) {
                merged[same] = swatch
            }
        }
        return merged.take(maxColors).map { swatch ->
            PaletteSwatch(
                rgb = swatch.rgb,
                hex = "#%06X".format(swatch.rgb and 0xFFFFFF),
                population = swatch.population
            )
        }
    }

    private fun rgbToLab(rgb: Int): ColorScience.Lab =
        ColorScience.rgbToLab(Color.red(rgb), Color.green(rgb), Color.blue(rgb))
}

/** 图片点选：圆形孔径中值，避免单像素噪声。 */
fun sampleBitmapMedian(bitmap: Bitmap, cx: Int, cy: Int, radius: Int = 5): Int? {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return null
    val x0 = cx.coerceIn(0, width - 1)
    val y0 = cy.coerceIn(0, height - 1)
    val r2 = radius * radius
    val rs = IntArray((2 * radius + 1) * (2 * radius + 1))
    val gs = IntArray(rs.size)
    val bs = IntArray(rs.size)
    var n = 0
    for (dy in -radius..radius) {
        val y = y0 + dy
        if (y !in 0 until height) continue
        for (dx in -radius..radius) {
            if (dx * dx + dy * dy > r2) continue
            val x = x0 + dx
            if (x !in 0 until width) continue
            val pixel = bitmap.getPixel(x, y)
            if (Color.alpha(pixel) < 16) continue
            rs[n] = Color.red(pixel)
            gs[n] = Color.green(pixel)
            bs[n] = Color.blue(pixel)
            n++
        }
    }
    if (n == 0) return null
    rs.sort(0, n)
    gs.sort(0, n)
    bs.sort(0, n)
    val mid = n / 2
    return Color.rgb(rs[mid], gs[mid], bs[mid])
}
