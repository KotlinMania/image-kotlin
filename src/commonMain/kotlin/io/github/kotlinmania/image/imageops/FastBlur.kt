// port-lint: source image/src/imageops/fast_blur.rs
package io.github.kotlinmania.image.imageops

import io.github.kotlinmania.image.images.ImageBuffer
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Approximation of Gaussian blur.
 *
 * Source: Kovesi, P.: Fast Almost-Gaussian Filtering The Australian Pattern
 * Recognition Society Conference: DICTA 2010. December 2010. Sydney.
 */
public fun <P, Container> fastBlur(
    inputBuffer: ImageBuffer<P, Container>,
    sigma: Float,
): ImageBuffer<P, ByteArray> = inputBuffer.fastBlur(sigma)

/**
 * Bounds and radius validation helper.
 */
public fun testRadiusSize(bound: Int, radius: Int) {
    val sum = bound.toLong() + radius.toLong()
    require(sum <= Int.MAX_VALUE.toLong()) { "Radius overflowed maximum possible size" }
}

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

/**
 * Ceil an integer to the nearest odd integer.
 */
public fun ceilToOdd(x: Int): Int = if (x % 2 == 0) x + 1 else x

/**
 * Rounding saturating multiplication for sample accumulation.
 */
public fun roundingSaturatingMul(v: Float, w: Float): Byte {
    val r = round(v * w)
    return r.coerceIn(0.0f, 255.0f).toInt().toByte()
}

/**
 * Strategy dispatcher for horizontal box blur pass.
 */
public fun boxBlurHorizontalPassStrategy(
    src: ByteArray,
    srcStride: Int,
    dst: ByteArray,
    dstStride: Int,
    width: Int,
    channels: Int,
    radius: Int,
) {
    when (channels) {
        1, 2, 3, 4 -> boxBlurHorizontalPassImpl(src, srcStride, dst, dstStride, width, channels, radius)
        else -> throw UnsupportedOperationException("Channels count greater than 4 is unsupported")
    }
}

/**
 * Strategy dispatcher for vertical box blur pass.
 */
public fun boxBlurVerticalPassStrategy(
    src: ByteArray,
    srcStride: Int,
    dst: ByteArray,
    dstStride: Int,
    width: Int,
    height: Int,
    channels: Int,
    radius: Int,
) {
    when (channels) {
        1, 2, 3, 4 -> boxBlurVerticalPassImpl(src, srcStride, dst, dstStride, width, height, channels, radius)
        else -> throw UnsupportedOperationException("Channels count greater than 4 is unsupported")
    }
}

/**
 * Implementation of horizontal box blur with 4-phase sliding window.
 */
public fun boxBlurHorizontalPassImpl(
    src: ByteArray,
    srcStride: Int,
    dst: ByteArray,
    dstStride: Int,
    width: Int,
    cn: Int,
    radius: Int,
) {
    require(width > 0) { "Width must be sanitized before this method" }
    testRadiusSize(width, radius)

    val kernelSize = radius * 2 + 1
    val edgeCount = (kernelSize / 2 + 1).toFloat()
    val halfKernel = kernelSize / 2
    val weight = 1.0f / kernelSize.toFloat()
    val widthBound = width - 1

    val height = if (srcStride > 0) src.size / srcStride else 0

    for (y in 0 until height) {
        val srcRowOffset = y * srcStride
        val dstRowOffset = y * dstStride

        var weight0 = (src[srcRowOffset].toInt() and 0xFF).toFloat() * edgeCount
        var weight1 = if (cn > 1) (src[srcRowOffset + 1].toInt() and 0xFF).toFloat() * edgeCount else 0f
        var weight2 = if (cn > 2) (src[srcRowOffset + 2].toInt() and 0xFF).toFloat() * edgeCount else 0f
        var weight3 = if (cn == 4) (src[srcRowOffset + 3].toInt() and 0xFF).toFloat() * edgeCount else 0f

        for (x in 1..halfKernel) {
            val px = minOf(x, widthBound) * cn
            weight0 += (src[srcRowOffset + px].toInt() and 0xFF).toFloat()
            if (cn > 1) weight1 += (src[srcRowOffset + px + 1].toInt() and 0xFF).toFloat()
            if (cn > 2) weight2 += (src[srcRowOffset + px + 2].toInt() and 0xFF).toFloat()
            if (cn == 4) weight3 += (src[srcRowOffset + px + 3].toInt() and 0xFF).toFloat()
        }

        val leadingLimit = minOf(halfKernel, width)
        for (x in 0 until leadingLimit) {
            val next = minOf(x + halfKernel + 1, widthBound) * cn
            val previous = maxOf(x - halfKernel, 0) * cn

            val dstPos = dstRowOffset + x * cn
            dst[dstPos] = roundingSaturatingMul(weight0, weight)
            if (cn > 1) dst[dstPos + 1] = roundingSaturatingMul(weight1, weight)
            if (cn > 2) dst[dstPos + 2] = roundingSaturatingMul(weight2, weight)
            if (cn == 4) dst[dstPos + 3] = roundingSaturatingMul(weight3, weight)

            val srcNextPos = srcRowOffset + next
            val srcPrevPos = srcRowOffset + previous

            weight0 += (src[srcNextPos].toInt() and 0xFF).toFloat()
            if (cn > 1) weight1 += (src[srcNextPos + 1].toInt() and 0xFF).toFloat()
            if (cn > 2) weight2 += (src[srcNextPos + 2].toInt() and 0xFF).toFloat()
            if (cn == 4) weight3 += (src[srcNextPos + 3].toInt() and 0xFF).toFloat()

            weight0 -= (src[srcPrevPos].toInt() and 0xFF).toFloat()
            if (cn > 1) weight1 -= (src[srcPrevPos + 1].toInt() and 0xFF).toFloat()
            if (cn > 2) weight2 -= (src[srcPrevPos + 2].toInt() and 0xFF).toFloat()
            if (cn == 4) weight3 -= (src[srcPrevPos + 3].toInt() and 0xFF).toFloat()
        }

        val maxXBeforeClamping = (widthBound - (halfKernel + 1)).coerceAtLeast(0)
        var lastProcessedItem = halfKernel

        val rowLength = srcStride
        if ((halfKernel * 2 + 1) * cn < rowLength && maxXBeforeClamping * cn < rowLength && maxXBeforeClamping > halfKernel) {
            for (x in halfKernel until maxXBeforeClamping) {
                val dstPos = dstRowOffset + x * cn
                dst[dstPos] = roundingSaturatingMul(weight0, weight)
                if (cn > 1) dst[dstPos + 1] = roundingSaturatingMul(weight1, weight)
                if (cn > 2) dst[dstPos + 2] = roundingSaturatingMul(weight2, weight)
                if (cn == 4) dst[dstPos + 3] = roundingSaturatingMul(weight3, weight)

                val nextPos = srcRowOffset + (x + halfKernel + 1) * cn
                val prevPos = srcRowOffset + (x - halfKernel) * cn

                weight0 += (src[nextPos].toInt() and 0xFF).toFloat()
                if (cn > 1) weight1 += (src[nextPos + 1].toInt() and 0xFF).toFloat()
                if (cn > 2) weight2 += (src[nextPos + 2].toInt() and 0xFF).toFloat()
                if (cn == 4) weight3 += (src[nextPos + 3].toInt() and 0xFF).toFloat()

                weight0 -= (src[prevPos].toInt() and 0xFF).toFloat()
                if (cn > 1) weight1 -= (src[prevPos + 1].toInt() and 0xFF).toFloat()
                if (cn > 2) weight2 -= (src[prevPos + 2].toInt() and 0xFF).toFloat()
                if (cn == 4) weight3 -= (src[prevPos + 3].toInt() and 0xFF).toFloat()
            }
            lastProcessedItem = maxXBeforeClamping
        }

        for (x in lastProcessedItem until width) {
            val next = minOf(x + halfKernel + 1, widthBound) * cn
            val previous = maxOf(x - halfKernel, 0) * cn

            val dstPos = dstRowOffset + x * cn
            dst[dstPos] = roundingSaturatingMul(weight0, weight)
            if (cn > 1) dst[dstPos + 1] = roundingSaturatingMul(weight1, weight)
            if (cn > 2) dst[dstPos + 2] = roundingSaturatingMul(weight2, weight)
            if (cn == 4) dst[dstPos + 3] = roundingSaturatingMul(weight3, weight)

            val srcNextPos = srcRowOffset + next
            val srcPrevPos = srcRowOffset + previous

            weight0 += (src[srcNextPos].toInt() and 0xFF).toFloat()
            if (cn > 1) weight1 += (src[srcNextPos + 1].toInt() and 0xFF).toFloat()
            if (cn > 2) weight2 += (src[srcNextPos + 2].toInt() and 0xFF).toFloat()
            if (cn == 4) weight3 += (src[srcNextPos + 3].toInt() and 0xFF).toFloat()

            weight0 -= (src[srcPrevPos].toInt() and 0xFF).toFloat()
            if (cn > 1) weight1 -= (src[srcPrevPos + 1].toInt() and 0xFF).toFloat()
            if (cn > 2) weight2 -= (src[srcPrevPos + 2].toInt() and 0xFF).toFloat()
            if (cn == 4) weight3 -= (src[srcPrevPos + 3].toInt() and 0xFF).toFloat()
        }
    }
}

/**
 * Implementation of vertical box blur with column accumulator.
 */
public fun boxBlurVerticalPassImpl(
    src: ByteArray,
    srcStride: Int,
    dst: ByteArray,
    dstStride: Int,
    width: Int,
    height: Int,
    cn: Int,
    radius: Int,
) {
    require(width > 0) { "Width must be sanitized before this method" }
    require(height > 0) { "Height must be sanitized before this method" }
    testRadiusSize(width, radius)

    val kernelSize = radius * 2 + 1
    val edgeCount = (kernelSize / 2 + 1).toFloat()
    val halfKernel = kernelSize / 2
    val weight = 1.0f / kernelSize.toFloat()

    val bufSize = width * cn
    val heightBound = height - 1

    val buffer = FloatArray(bufSize)

    for (x in 0 until bufSize) {
        var w = (src[x].toInt() and 0xFF).toFloat() * edgeCount
        for (y in 1..halfKernel) {
            val ySrcShift = minOf(y, heightBound) * srcStride
            w += (src[ySrcShift + x].toInt() and 0xFF).toFloat()
        }
        buffer[x] = w
    }

    for (y in 0 until height) {
        val next = minOf(y + halfKernel + 1, heightBound) * srcStride
        val previous = maxOf(y - halfKernel, 0) * srcStride
        val dstRowOffset = y * dstStride

        for (x in 0 until bufSize) {
            var weight0 = buffer[x]
            dst[dstRowOffset + x] = roundingSaturatingMul(weight0, weight)
            val srcNext = (src[next + x].toInt() and 0xFF).toFloat()
            val srcPrevious = (src[previous + x].toInt() and 0xFF).toFloat()
            weight0 += srcNext
            weight0 -= srcPrevious
            buffer[x] = weight0
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

    val stride = width * channels
    val destinationSize = width * height * channels
    val transient = ByteArray(destinationSize)
    val dst = ByteArray(destinationSize)

    val firstBox = boxes[0]
    testRadiusSize(width, firstBox)
    testRadiusSize(height, firstBox)

    boxBlurHorizontalPassStrategy(inputBuffer, stride, transient, stride, width, channels, firstBox)
    boxBlurVerticalPassStrategy(transient, stride, dst, stride, width, height, channels, firstBox)

    for (i in 1 until boxes.size) {
        val boxContainer = boxes[i]
        testRadiusSize(width, boxContainer)
        testRadiusSize(height, boxContainer)

        boxBlurHorizontalPassStrategy(dst, stride, transient, stride, width, channels, boxContainer)
        boxBlurVerticalPassStrategy(transient, stride, dst, stride, width, height, channels, boxContainer)
    }

    return dst
}
