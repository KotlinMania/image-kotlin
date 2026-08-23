// port-lint: source imageops/sample.rs
package io.github.kotlinmania.image.imageops

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Available Sampling Filters for image resizing.
 */
public enum class FilterType {
    /** Nearest Neighbor */
    Nearest,

    /** Linear Filter (Triangle) */
    Triangle,

    /** Cubic Filter (Catmull-Rom) */
    CatmullRom,

    /** Gaussian Filter */
    Gaussian,

    /** Lanczos with window 3 */
    Lanczos3,
}

private fun sinc(t: Float): Float {
    val a = t * PI.toFloat()
    return if (t == 0.0f) 1.0f else sin(a) / a
}

private fun lanczos(x: Float, t: Float): Float =
    if (abs(x) < t) sinc(x) * sinc(x / t) else 0.0f

private fun gaussianKernel(x: Float, r: Float): Float =
    (1.0f / (sqrt(2.0f * PI.toFloat()) * r)) * exp(-x * x / (2.0f * r * r))

private fun filterKernel(filter: FilterType, x: Float): Float =
    when (filter) {
        FilterType.Nearest -> if (abs(x) <= 0.5f) 1.0f else 0.0f
        FilterType.Triangle -> (1.0f - abs(x)).coerceAtLeast(0.0f)
        FilterType.CatmullRom -> {
            val a = abs(x)
            if (a < 1.0f) {
                0.5f * (3.0f * a * a * a - 5.0f * a * a + 2.0f)
            } else if (a < 2.0f) {
                0.5f * (-a * a * a + 5.0f * a * a - 8.0f * a + 4.0f)
            } else {
                0.0f
            }
        }
        FilterType.Gaussian -> gaussianKernel(x, 0.5f)
        FilterType.Lanczos3 -> lanczos(x, 3.0f)
    }

/**
 * Resizes an image from (srcW x srcH) to (dstW x dstH) with the specified filter.
 */
public fun resize(
    image: ByteArray,
    srcW: Int,
    srcH: Int,
    dstW: Int,
    dstH: Int,
    channels: Int,
    filter: FilterType = FilterType.Triangle,
): ByteArray {
    if (dstW <= 0 || dstH <= 0 || srcW <= 0 || srcH <= 0) return ByteArray(0)
    val out = ByteArray(dstW * dstH * channels)

    if (filter == FilterType.Nearest) {
        for (y in 0 until dstH) {
            val srcY = (y.toLong() * srcH / dstH).toInt().coerceIn(0, srcH - 1)
            for (x in 0 until dstW) {
                val srcX = (x.toLong() * srcW / dstW).toInt().coerceIn(0, srcW - 1)
                val srcIdx = (srcY * srcW + srcX) * channels
                val dstIdx = (y * dstW + x) * channels
                image.copyInto(out, destinationOffset = dstIdx, startIndex = srcIdx, endIndex = srcIdx + channels)
            }
        }
        return out
    }

    val xRatio = srcW.toFloat() / dstW.toFloat()
    val yRatio = srcH.toFloat() / dstH.toFloat()

    for (y in 0 until dstH) {
        val srcYCenter = (y + 0.5f) * yRatio - 0.5f
        val yMin = max(0, (srcYCenter - 1.5f).toInt())
        val yMax = minOf(srcH - 1, (srcYCenter + 1.5f).toInt())

        for (x in 0 until dstW) {
            val srcXCenter = (x + 0.5f) * xRatio - 0.5f
            val xMin = max(0, (srcXCenter - 1.5f).toInt())
            val xMax = minOf(srcW - 1, (srcXCenter + 1.5f).toInt())

            for (c in 0 until channels) {
                var weightSum = 0.0f
                var valSum = 0.0f

                for (sy in yMin..yMax) {
                    val wy = filterKernel(filter, sy - srcYCenter)
                    for (sx in xMin..xMax) {
                        val wx = filterKernel(filter, sx - srcXCenter)
                        val w = wx * wy
                        if (w > 0.0f) {
                            val pixelVal = image[(sy * srcW + sx) * channels + c].toInt() and 0xFF
                            valSum += pixelVal * w
                            weightSum += w
                        }
                    }
                }

                val finalVal = if (weightSum > 0.0f) (valSum / weightSum) else 0.0f
                val dstIdx = (y * dstW + x) * channels + c
                out[dstIdx] = finalVal.coerceIn(0.0f, 255.0f).toInt().toByte()
            }
        }
    }

    return out
}

/**
 * Creates a thumbnail of the given image fitting inside maxW x maxH maintaining aspect ratio.
 */
public fun thumbnail(
    image: ByteArray,
    srcW: Int,
    srcH: Int,
    maxW: Int,
    maxH: Int,
    channels: Int,
): ByteArray {
    val ratio = minOf(maxW.toFloat() / srcW.toFloat(), maxH.toFloat() / srcH.toFloat())
    val dstW = max(1, (srcW * ratio).toInt())
    val dstH = max(1, (srcH * ratio).toInt())
    return resize(image, srcW, srcH, dstW, dstH, channels, FilterType.Triangle)
}
