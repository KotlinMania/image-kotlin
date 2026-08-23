// port-lint: source imageops/fast_blur.rs
package io.github.kotlinmania.image.imageops

import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Calculates box sizes for Gaussian blur approximation.
 */
public fun boxesForGauss(sigma: Float, n: Int): IntArray {
    val wIdeal = sqrt((12.0f * sigma * sigma / n.toFloat()) + 1.0f)
    var wl = floor(wIdeal)
    if (wl % 2.0f == 0.0f) {
        wl -= 1.0f
    }
    val wu = wl + 2.0f

    val mIdeal = 0.25f * n.toFloat() * (wl + 3.0f) - 3.0f * sigma * sigma / (wl + 1.0f)
    val m = round(mIdeal).toInt()

    val result = IntArray(n)
    for (i in 0 until n) {
        val size = if (i < m) wl.toInt() else wu.toInt()
        val odd = ceilToOdd((size - 1).coerceAtLeast(0) / 2)
        result[i] = odd
    }
    return result
}

private fun ceilToOdd(x: Int): Int = if (x % 2 == 0) x + 1 else x

private fun boxBlurHorizontal(
    src: ByteArray,
    dst: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    radius: Int,
) {
    val scale = 1.0f / (radius * 2 + 1).toFloat()
    for (y in 0 until height) {
        val rowOffset = y * width * channels
        for (c in 0 until channels) {
            var sum = 0f
            // Initialize sum with clamped border
            for (i in -radius..radius) {
                val x = i.coerceIn(0, width - 1)
                sum += src[rowOffset + x * channels + c].toInt() and 0xFF
            }

            for (x in 0 until width) {
                dst[rowOffset + x * channels + c] = (sum * scale).coerceIn(0f, 255f).toInt().toByte()
                // Shift window
                val nextRight = (x + radius + 1).coerceIn(0, width - 1)
                val prevLeft = (x - radius).coerceIn(0, width - 1)
                sum += (src[rowOffset + nextRight * channels + c].toInt() and 0xFF) -
                    (src[rowOffset + prevLeft * channels + c].toInt() and 0xFF)
            }
        }
    }
}

private fun boxBlurVertical(
    src: ByteArray,
    dst: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    radius: Int,
) {
    val scale = 1.0f / (radius * 2 + 1).toFloat()
    for (x in 0 until width) {
        for (c in 0 until channels) {
            var sum = 0f
            // Initialize sum with clamped border
            for (i in -radius..radius) {
                val y = i.coerceIn(0, height - 1)
                sum += src[y * width * channels + x * channels + c].toInt() and 0xFF
            }

            for (y in 0 until height) {
                dst[y * width * channels + x * channels + c] = (sum * scale).coerceIn(0f, 255f).toInt().toByte()
                // Shift window
                val nextBottom = (y + radius + 1).coerceIn(0, height - 1)
                val prevTop = (y - radius).coerceIn(0, height - 1)
                sum += (src[nextBottom * width * channels + x * channels + c].toInt() and 0xFF) -
                    (src[prevTop * width * channels + x * channels + c].toInt() and 0xFF)
            }
        }
    }
}

/**
 * Fast Gaussian blur approximation using 3 passes of box blur.
 */
public fun fastBlur(
    inputBuffer: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    sigma: Float,
): ByteArray {
    if (width == 0 || height == 0 || inputBuffer.isEmpty()) {
        return inputBuffer.copyOf()
    }

    val numPasses = 3
    val boxes = boxesForGauss(sigma, numPasses)
    if (boxes.isEmpty()) {
        return inputBuffer.copyOf()
    }

    val size = width * height * channels
    var current = inputBuffer.copyOf()
    var temp = ByteArray(size)

    for (radius in boxes) {
        boxBlurHorizontal(current, temp, width, height, channels, radius)
        boxBlurVertical(temp, current, width, height, channels, radius)
    }

    return current
}
