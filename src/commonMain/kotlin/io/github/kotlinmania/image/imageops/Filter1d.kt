// port-lint: source imageops/filter_1d.rs
package io.github.kotlinmania.image.imageops

import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.LimitError
import io.github.kotlinmania.image.LimitErrorKind

internal fun safeMul(a: Int, b: Int): Int {
    val res = a.toLong() * b.toLong()
    if (res > Int.MAX_VALUE || res < Int.MIN_VALUE) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }
    return res.toInt()
}

internal fun safeAdd(a: Int, b: Int): Int {
    val res = a.toLong() + b.toLong()
    if (res > Int.MAX_VALUE || res < Int.MIN_VALUE) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }
    return res.toInt()
}

public data class FilterImageSize(
    public val width: Int,
    public val height: Int,
)

/**
 * 1D filter kernel configuration.
 */
public data class KernelShape(
    public val width: Int,
    public val height: Int,
)

/**
 * Applies horizontal 1D convolution on byte array buffer.
 */
public fun filter1dHorizontal(
    src: ByteArray,
    dst: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    kernel: FloatArray,
) {
    val kRadius = kernel.size / 2
    for (y in 0 until height) {
        val rowOffset = y * width * channels
        for (x in 0 until width) {
            for (c in 0 until channels) {
                var sum = 0f
                for (k in kernel.indices) {
                    val kx = (x + k - kRadius).coerceIn(0, width - 1)
                    val px = (src[rowOffset + kx * channels + c].toInt() and 0xFF).toFloat()
                    sum += px * kernel[k]
                }
                dst[rowOffset + x * channels + c] = sum.coerceIn(0f, 255f).toInt().toByte()
            }
        }
    }
}

/**
 * Applies vertical 1D convolution on byte array buffer.
 */
public fun filter1dVertical(
    src: ByteArray,
    dst: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    kernel: FloatArray,
) {
    val kRadius = kernel.size / 2
    for (y in 0 until height) {
        for (x in 0 until width) {
            for (c in 0 until channels) {
                var sum = 0f
                for (k in kernel.indices) {
                    val ky = (y + k - kRadius).coerceIn(0, height - 1)
                    val px = (src[ky * width * channels + x * channels + c].toInt() and 0xFF).toFloat()
                    sum += px * kernel[k]
                }
                dst[y * width * channels + x * channels + c] = sum.coerceIn(0f, 255f).toInt().toByte()
            }
        }
    }
}
