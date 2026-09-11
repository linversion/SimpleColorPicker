package com.linversion.simplecolorpicker.camera

import kotlin.math.pow

/** sRGB / CIE Lab 转换，平均和比较都在 Lab 里做。 */
internal object ColorScience {

    data class Lab(val l: Float, val a: Float, val b: Float)

    fun rgbToLab(r: Int, g: Int, b: Int): Lab {
        val rl = srgbToLinear(r / 255f)
        val gl = srgbToLinear(g / 255f)
        val bl = srgbToLinear(b / 255f)
        val x = 0.4124564f * rl + 0.3575761f * gl + 0.1804375f * bl
        val y = 0.2126729f * rl + 0.7151522f * gl + 0.0721750f * bl
        val z = 0.0193339f * rl + 0.1191920f * gl + 0.9503041f * bl
        val fx = labF(x / 0.95047f)
        val fy = labF(y / 1.00000f)
        val fz = labF(z / 1.08883f)
        return Lab(116f * fy - 16f, 500f * (fx - fy), 200f * (fy - fz))
    }

    fun labToRgb(lab: Lab): IntArray {
        val fy = (lab.l + 16f) / 116f
        val fx = lab.a / 500f + fy
        val fz = fy - lab.b / 200f
        val x = 0.95047f * labFInv(fx)
        val y = 1.00000f * labFInv(fy)
        val z = 1.08883f * labFInv(fz)
        val rl = 3.2404542f * x - 1.5371385f * y - 0.4985314f * z
        val gl = -0.9692660f * x + 1.8760108f * y + 0.0415560f * z
        val bl = 0.0556434f * x - 0.2040259f * y + 1.0572252f * z
        return intArrayOf(
            linearToSrgb(rl),
            linearToSrgb(gl),
            linearToSrgb(bl)
        )
    }

    fun deltaE(a: Lab, b: Lab): Float {
        val dl = a.l - b.l
        val da = a.a - b.a
        val db = a.b - b.b
        return kotlin.math.sqrt(dl * dl + da * da + db * db)
    }

    private fun srgbToLinear(c: Float): Float =
        if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

    private fun linearToSrgb(c: Float): Int {
        val v = c.coerceIn(0f, 1f)
        val s = if (v <= 0.0031308f) 12.92f * v else 1.055f * v.pow(1f / 2.4f) - 0.055f
        return (s * 255f + 0.5f).toInt().coerceIn(0, 255)
    }

    private fun labF(t: Float): Float =
        if (t > 0.008856f) t.pow(1f / 3f) else (7.787f * t + 16f / 116f)

    private fun labFInv(t: Float): Float {
        val t3 = t * t * t
        return if (t3 > 0.008856f) t3 else (t - 16f / 116f) / 7.787f
    }
}
