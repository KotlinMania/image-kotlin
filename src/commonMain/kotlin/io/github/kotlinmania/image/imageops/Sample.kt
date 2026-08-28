// port-lint: source imageops/sample.rs
package io.github.kotlinmania.image.imageops

import io.github.kotlinmania.image.Luma
import io.github.kotlinmania.image.LumaA
import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.images.DynamicImage
import io.github.kotlinmania.image.images.GenericImageView
import io.github.kotlinmania.image.images.ImageBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.round
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

/**
 * A representation of a separable filter.
 */
public class Filter(
    public val kernel: (Float) -> Float,
    public val support: Float,
)

/**
 * Float wrapper rounding to nearest integer values.
 */
internal class FloatNearest(public val value: Float) {
    fun toI8(): Byte = round(value).toInt().toByte()
    fun toI16(): Short = round(value).toInt().toShort()
    fun toI64(): Long = round(value).toLong()
    fun toU8(): UByte = round(value).toInt().toUByte()
    fun toU16(): UShort = round(value).toInt().toUShort()
    fun toU64(): ULong = round(value).toLong().toULong()
}

/**
 * Local struct for keeping track of pixel sums for fast thumbnail averaging.
 */
internal class ThumbnailSum(
    var c0: Long = 0L,
    var c1: Long = 0L,
    var c2: Long = 0L,
    var c3: Long = 0L,
) {
    fun zeroed() {
        c0 = 0L
        c1 = 0L
        c2 = 0L
        c3 = 0L
    }

    fun addPixel(p0: Long, p1: Long, p2: Long, p3: Long) {
        c0 += p0
        c1 += p1
        c2 += p2
        c3 += p3
    }
}

private fun sinc(t: Float): Float {
    val a = t * PI.toFloat()
    return if (t == 0.0f) 1.0f else sin(a) / a
}

private fun lanczos(x: Float, t: Float): Float =
    if (abs(x) < t) sinc(x) * sinc(x / t) else 0.0f

/**
 * The Gaussian function.
 */
public fun gaussian(x: Float, r: Float): Float =
    (1.0f / (sqrt(2.0f * PI.toFloat()) * r)) * exp(-x * x / (2.0f * r * r))

/**
 * Builtin Gaussian kernel filter with radius 0.5.
 */
public fun gaussianKernel(x: Float): Float = gaussian(x, 0.5f)

/**
 * Builtin Lanczos3 kernel filter.
 */
public fun lanczos3Kernel(x: Float): Float = lanczos(x, 3.0f)

private fun bcCubicSpline(x: Float, b: Float, c: Float): Float {
    val a = abs(x)
    val k =
        if (a < 1.0f) {
            (12.0f - 9.0f * b - 6.0f * c) * a * a * a +
                (-18.0f + 12.0f * b + 6.0f * c) * a * a +
                (6.0f - 2.0f * b)
        } else if (a < 2.0f) {
            (-b - 6.0f * c) * a * a * a +
                (6.0f * b + 30.0f * c) * a * a +
                (-12.0f * b - 48.0f * c) * a +
                (8.0f * b + 24.0f * c)
        } else {
            0.0f
        }
    return k / 6.0f
}

/**
 * Builtin Catmull-Rom kernel filter.
 */
public fun catmullromKernel(x: Float): Float = bcCubicSpline(x, 0.0f, 0.5f)

/**
 * Builtin Triangle (linear) kernel filter.
 */
public fun triangleKernel(x: Float): Float =
    if (abs(x) < 1.0f) 1.0f - abs(x) else 0.0f

/**
 * Built-in box kernel filter.
 */
public fun boxKernel(x: Float): Float = 1.0f

private fun filterKernel(filter: FilterType, x: Float): Float =
    when (filter) {
        FilterType.Nearest -> if (abs(x) <= 0.5f) 1.0f else 0.0f
        FilterType.Triangle -> triangleKernel(x).coerceAtLeast(0.0f)
        FilterType.CatmullRom -> catmullromKernel(x)
        FilterType.Gaussian -> gaussianKernel(x)
        FilterType.Lanczos3 -> lanczos3Kernel(x)
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
 * Creates a thumbnail of the given image.
 */
public fun <P> thumbnail(
    image: GenericImageView<P>,
    nwidth: UInt,
    nheight: UInt,
): ImageBuffer<P, ByteArray> {
    val (width, height) = image.dimensions()
    if (width == 0u || height == 0u || nwidth == 0u || nheight == 0u) {
        return image.bufferWithDimensions(0u, 0u)
    }
    val ratio = minOf(nwidth.toFloat() / width.toFloat(), nheight.toFloat() / height.toFloat())
    val dstW = max(1, (width.toFloat() * ratio).toInt()).toUInt()
    val dstH = max(1, (height.toFloat() * ratio).toInt()).toUInt()
    return resize(image, dstW, dstH, FilterType.Triangle)
}

/**
 * Resizes an image from (srcW x srcH) to (dstW x dstH) with the specified filter.
 */
@Suppress("UNCHECKED_CAST")
public fun <P> resize(
    image: GenericImageView<P>,
    nwidth: UInt,
    nheight: UInt,
    filter: FilterType,
): ImageBuffer<P, ByteArray> {
    val out = image.bufferWithDimensions(nwidth, nheight)
    val (srcW, srcH) = image.dimensions()
    if (nwidth == 0u || nheight == 0u || srcW == 0u || srcH == 0u) {
        return out
    }
    for (y in 0u until nheight) {
        val srcY = (y.toDouble() / nheight.toDouble()).toFloat()
        for (x in 0u until nwidth) {
            val srcX = (x.toDouble() / nwidth.toDouble()).toFloat()
            val pixel = if (filter == FilterType.Nearest) {
                sampleNearest(image, srcX, srcY)
            } else {
                sampleBilinear(image, srcX, srcY)
            }
            if (pixel != null) {
                out.putPixel(x, y, pixel)
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


/**
 * Samples a pixel at normalized coordinates ([u], [v]) with bilinear interpolation.
 */
public fun <P> sampleBilinear(
    img: GenericImageView<P>,
    u: Float,
    v: Float,
): P? {
    if (u !in 0.0f..1.0f || v !in 0.0f..1.0f) {
        return null
    }
    val (w, h) = img.dimensions()
    if (w == 0u || h == 0u) {
        return null
    }
    val ui = (w.toFloat() * u - 0.5f).coerceIn(0.0f, (w - 1u).toFloat())
    val vi = (h.toFloat() * v - 0.5f).coerceIn(0.0f, (h - 1u).toFloat())
    return interpolateBilinear(img, ui, vi)
}

/**
 * Samples a pixel at normalized coordinates ([u], [v]) using nearest-neighbor.
 */
public fun <P> sampleNearest(
    img: GenericImageView<P>,
    u: Float,
    v: Float,
): P? {
    if (u !in 0.0f..1.0f || v !in 0.0f..1.0f) {
        return null
    }
    val (w, h) = img.dimensions()
    if (w == 0u || h == 0u) {
        return null
    }
    val maxW = if (w > 0u) (w - 1u).toFloat() else 0f
    val maxH = if (h > 0u) (h - 1u).toFloat() else 0f
    val ui = (w.toFloat() * u - 0.5f).coerceIn(0.0f, maxW)
    val vi = (h.toFloat() * v - 0.5f).coerceIn(0.0f, maxH)
    return interpolateNearest(img, ui, vi)
}

private fun roundAwayFromZero(x: Float): Float =
    if (x >= 0.0f) kotlin.math.floor(x + 0.5f) else kotlin.math.ceil(x - 0.5f)

/**
 * Interpolates a pixel at pixel coordinates ([x], [y]) using nearest-neighbor.
 */
public fun <P> interpolateNearest(
    img: GenericImageView<P>,
    x: Float,
    y: Float,
): P? {
    val (w, h) = img.dimensions()
    if (w == 0u || h == 0u) {
        return null
    }
    if (x !in 0.0f..(w - 1u).toFloat() || y !in 0.0f..(h - 1u).toFloat()) {
        return null
    }
    val rx = roundAwayFromZero(x).toUInt().coerceIn(0u, w - 1u)
    val ry = roundAwayFromZero(y).toUInt().coerceIn(0u, h - 1u)
    return img.getPixel(rx, ry)
}

/**
 * Interpolates a pixel at pixel coordinates ([x], [y]) using bilinear interpolation.
 */
@Suppress("UNCHECKED_CAST")
public fun <P> interpolateBilinear(
    img: GenericImageView<P>,
    x: Float,
    y: Float,
): P? {
    val (w, h) = img.dimensions()
    if (w == 0u || h == 0u) {
        return null
    }
    if (x !in 0.0f..(w - 1u).toFloat() || y !in 0.0f..(h - 1u).toFloat()) {
        return null
    }

    val uf = x.toInt().toUInt()
    val vf = y.toInt().toUInt()
    val uc = minOf(uf + 1u, w - 1u)
    val vc = minOf(vf + 1u, h - 1u)

    val p00 = img.getPixel(uf, vf)
    val p01 = img.getPixel(uf, vc)
    val p10 = img.getPixel(uc, vf)
    val p11 = img.getPixel(uc, vc)

    val ufw = x - uf.toFloat()
    val vfw = y - vf.toFloat()
    val ucw = (uf + 1u).toFloat() - x
    val vcw = (vf + 1u).toFloat() - y

    val wff = ucw * vcw
    val wfc = ucw * vfw
    val wcf = ufw * vcw
    val wcc = ufw * vfw

    return when (p00) {
        is Rgba<*> -> {
            val c00 = p00 as Rgba<*>
            val c01 = p01 as Rgba<*>
            val c10 = p10 as Rgba<*>
            val c11 = p11 as Rgba<*>
            if (c00.r is Float) {
                val r00 = (c00.r as Float)
                val r01 = (c01.r as Float)
                val r10 = (c10.r as Float)
                val r11 = (c11.r as Float)
                val g00 = (c00.g as Float)
                val g01 = (c01.g as Float)
                val g10 = (c10.g as Float)
                val g11 = (c11.g as Float)
                val b00 = (c00.b as Float)
                val b01 = (c01.b as Float)
                val b10 = (c10.b as Float)
                val b11 = (c11.b as Float)
                val a00 = (c00.a as Float)
                val a01 = (c01.a as Float)
                val a10 = (c10.a as Float)
                val a11 = (c11.a as Float)
                val r = wff * r00 + wfc * r01 + wcf * r10 + wcc * r11
                val g = wff * g00 + wfc * g01 + wcf * g10 + wcc * g11
                val b = wff * b00 + wfc * b01 + wcf * b10 + wcc * b11
                val a = wff * a00 + wfc * a01 + wcf * a10 + wcc * a11
                Rgba(r, g, b, a) as P
            } else {
                val r00 = (c00.r as UByte).toFloat()
                val r01 = (c01.r as UByte).toFloat()
                val r10 = (c10.r as UByte).toFloat()
                val r11 = (c11.r as UByte).toFloat()
                val g00 = (c00.g as UByte).toFloat()
                val g01 = (c01.g as UByte).toFloat()
                val g10 = (c10.g as UByte).toFloat()
                val g11 = (c11.g as UByte).toFloat()
                val b00 = (c00.b as UByte).toFloat()
                val b01 = (c01.b as UByte).toFloat()
                val b10 = (c10.b as UByte).toFloat()
                val b11 = (c11.b as UByte).toFloat()
                val a00 = (c00.a as UByte).toFloat()
                val a01 = (c01.a as UByte).toFloat()
                val a10 = (c10.a as UByte).toFloat()
                val a11 = (c11.a as UByte).toFloat()
                val r = round(wff * r00 + wfc * r01 + wcf * r10 + wcc * r11).toInt().coerceIn(0, 255).toUByte()
                val g = round(wff * g00 + wfc * g01 + wcf * g10 + wcc * g11).toInt().coerceIn(0, 255).toUByte()
                val b = round(wff * b00 + wfc * b01 + wcf * b10 + wcc * b11).toInt().coerceIn(0, 255).toUByte()
                val a = round(wff * a00 + wfc * a01 + wcf * a10 + wcc * a11).toInt().coerceIn(0, 255).toUByte()
                Rgba(r, g, b, a) as P
            }
        }
        is Rgb<*> -> {
            val c00 = p00 as Rgb<*>
            val c01 = p01 as Rgb<*>
            val c10 = p10 as Rgb<*>
            val c11 = p11 as Rgb<*>
            if (c00.r is Float) {
                val r00 = (c00.r as Float)
                val r01 = (c01.r as Float)
                val r10 = (c10.r as Float)
                val r11 = (c11.r as Float)
                val g00 = (c00.g as Float)
                val g01 = (c01.g as Float)
                val g10 = (c10.g as Float)
                val g11 = (c11.g as Float)
                val b00 = (c00.b as Float)
                val b01 = (c01.b as Float)
                val b10 = (c10.b as Float)
                val b11 = (c11.b as Float)
                val r = wff * r00 + wfc * r01 + wcf * r10 + wcc * r11
                val g = wff * g00 + wfc * g01 + wcf * g10 + wcc * g11
                val b = wff * b00 + wfc * b01 + wcf * b10 + wcc * b11
                Rgb(r, g, b) as P
            } else {
                val r00 = (c00.r as UByte).toFloat()
                val r01 = (c01.r as UByte).toFloat()
                val r10 = (c10.r as UByte).toFloat()
                val r11 = (c11.r as UByte).toFloat()
                val g00 = (c00.g as UByte).toFloat()
                val g01 = (c01.g as UByte).toFloat()
                val g10 = (c10.g as UByte).toFloat()
                val g11 = (c11.g as UByte).toFloat()
                val b00 = (c00.b as UByte).toFloat()
                val b01 = (c01.b as UByte).toFloat()
                val b10 = (c10.b as UByte).toFloat()
                val b11 = (c11.b as UByte).toFloat()
                val r = round(wff * r00 + wfc * r01 + wcf * r10 + wcc * r11).toInt().coerceIn(0, 255).toUByte()
                val g = round(wff * g00 + wfc * g01 + wcf * g10 + wcc * g11).toInt().coerceIn(0, 255).toUByte()
                val b = round(wff * b00 + wfc * b01 + wcf * b10 + wcc * b11).toInt().coerceIn(0, 255).toUByte()
                Rgb(r, g, b) as P
            }
        }
        is Luma<*> -> {
            val c00 = p00 as Luma<*>
            val c01 = p01 as Luma<*>
            val c10 = p10 as Luma<*>
            val c11 = p11 as Luma<*>
            if (c00.l is Float) {
                val l00 = (c00.l as Float)
                val l01 = (c01.l as Float)
                val l10 = (c10.l as Float)
                val l11 = (c11.l as Float)
                val l = wff * l00 + wfc * l01 + wcf * l10 + wcc * l11
                Luma(l) as P
            } else {
                val l00 = (c00.l as UByte).toFloat()
                val l01 = (c01.l as UByte).toFloat()
                val l10 = (c10.l as UByte).toFloat()
                val l11 = (c11.l as UByte).toFloat()
                val l = round(wff * l00 + wfc * l01 + wcf * l10 + wcc * l11).toInt().coerceIn(0, 255).toUByte()
                Luma(l) as P
            }
        }
        is LumaA<*> -> {
            val c00 = p00 as LumaA<*>
            val c01 = p01 as LumaA<*>
            val c10 = p10 as LumaA<*>
            val c11 = p11 as LumaA<*>
            if (c00.l is Float) {
                val l00 = (c00.l as Float)
                val l01 = (c01.l as Float)
                val l10 = (c10.l as Float)
                val l11 = (c11.l as Float)
                val a00 = (c00.a as Float)
                val a01 = (c01.a as Float)
                val a10 = (c10.a as Float)
                val a11 = (c11.a as Float)
                val l = wff * l00 + wfc * l01 + wcf * l10 + wcc * l11
                val a = wff * a00 + wfc * a01 + wcf * a10 + wcc * a11
                LumaA(l, a) as P
            } else {
                val l00 = (c00.l as UByte).toFloat()
                val l01 = (c01.l as UByte).toFloat()
                val l10 = (c10.l as UByte).toFloat()
                val l11 = (c11.l as UByte).toFloat()
                val a00 = (c00.a as UByte).toFloat()
                val a01 = (c01.a as UByte).toFloat()
                val a10 = (c10.a as UByte).toFloat()
                val a11 = (c11.a as UByte).toFloat()
                val l = round(wff * l00 + wfc * l01 + wcf * l10 + wcc * l11).toInt().coerceIn(0, 255).toUByte()
                val a = round(wff * a00 + wfc * a01 + wcf * a10 + wcc * a11).toInt().coerceIn(0, 255).toUByte()
                LumaA(l, a) as P
            }
        }
        else -> p00
    }
}

/**
 * Holds analytical Gaussian blur representation parameters.
 */
public data class GaussianBlurParameters(
    /** X-axis kernel size, must be odd */
    public val xAxisKernelSize: UInt,
    /** X-axis sigma, must be > 0 and finite */
    public val xAxisSigma: Float,
    /** Y-axis kernel size, must be odd */
    public val yAxisKernelSize: UInt,
    /** Y-axis sigma, must be > 0 and finite */
    public val yAxisSigma: Float,
) {
    public companion object {
        /** Built-in smoothing kernel with size 3. */
        public val SMOOTHING_3: GaussianBlurParameters = GaussianBlurParameters(3u, 0.8f, 3u, 0.8f)

        /** Built-in smoothing kernel with size 5. */
        public val SMOOTHING_5: GaussianBlurParameters = GaussianBlurParameters(5u, 1.1f, 5u, 1.1f)

        /** Built-in smoothing kernel with size 7. */
        public val SMOOTHING_7: GaussianBlurParameters = GaussianBlurParameters(7u, 1.4f, 7u, 1.4f)

        /** Creates a parameter set from radius. */
        public fun newFromRadius(radius: Float): GaussianBlurParameters {
            require(radius >= 0.0f) { "Radius must be non-negative" }
            return newFromKernelSize(radius * 2.0f + 1.0f)
        }

        /** Creates a parameter set from kernel size. */
        public fun newFromKernelSize(kernelSize: Float): GaussianBlurParameters {
            require(kernelSize > 0.0f && !kernelSize.isNaN() && !kernelSize.isInfinite()) { "Kernel size must be positive and finite" }
            val iKernelSize = roundToNearestOdd(kernelSize)
            val vSigma = sigmaSize(kernelSize)
            return GaussianBlurParameters(iKernelSize, vSigma, iKernelSize, vSigma)
        }

        /** Creates an anisotropic parameter set from kernel sizes. */
        public fun newAnisotropicKernelSize(xAxisKernelSize: Float, yAxisKernelSize: Float): GaussianBlurParameters {
            require(xAxisKernelSize > 0.0f && !xAxisKernelSize.isNaN() && !xAxisKernelSize.isInfinite()) { "Kernel size must be positive and finite" }
            require(yAxisKernelSize > 0.0f && !yAxisKernelSize.isNaN() && !yAxisKernelSize.isInfinite()) { "Kernel size must be positive and finite" }
            val xKernelSize = roundToNearestOdd(xAxisKernelSize)
            val yKernelSize = roundToNearestOdd(yAxisKernelSize)
            val xSigma = sigmaSize(xAxisKernelSize)
            val ySigma = sigmaSize(yAxisKernelSize)
            return GaussianBlurParameters(xKernelSize, xSigma, yKernelSize, ySigma)
        }

        /** Creates a parameter set from sigma. */
        public fun newFromSigma(sigma: Float): GaussianBlurParameters {
            require(sigma > 0.0f && !sigma.isNaN() && !sigma.isInfinite()) { "Sigma must be positive and finite" }
            val kernelSize = kernelSizeFromSigma(sigma)
            return GaussianBlurParameters(kernelSize, sigma, kernelSize, sigma)
        }

        public fun roundToNearestOdd(x: Float): UInt {
            val n = roundAwayFromZero(x).toUInt()
            return if (n % 2u != 0u) {
                n
            } else {
                val lower = if (n > 0u) n - 1u else 1u
                val upper = n + 1u
                val distLower = abs(x - lower.toFloat())
                val distUpper = abs(x - upper.toFloat())
                if (distLower <= distUpper) lower else upper
            }
        }

        public fun sigmaSize(kernelSize: Float): Float {
            val safeKernelSize = if (kernelSize <= 1.0f) 0.8f else kernelSize
            return 0.3f * ((safeKernelSize - 1.0f) * 0.5f - 1.0f) + 0.8f
        }

        public fun kernelSizeFromSigma(sigma: Float): UInt {
            val possibleSize = max(3.0f, (((sigma - 0.8f) / 0.3f + 1.0f) * 2.0f) + 1.0f).toUInt()
            return if (possibleSize % 2u == 0u) possibleSize + 1u else possibleSize
        }
    }
}

/**
 * Computes a 1D normalized Gaussian kernel of specified [width] and [sigma].
 */
public fun getGaussianKernel1d(width: Int, sigma: Float): FloatArray {
    var sumNorm = 0.0f
    val kernel = FloatArray(width)
    val scale = 1.0f / (sqrt(2.0f * PI.toFloat()) * sigma)
    val mean = (width / 2).toFloat()

    for (x in 0 until width) {
        val diff = (x.toFloat() - mean) / sigma
        val newWeight = exp(-0.5f * diff * diff) * scale
        kernel[x] = newWeight
        sumNorm += newWeight
    }

    if (sumNorm != 0.0f) {
        val sumScale = 1.0f / sumNorm
        for (i in kernel.indices) {
            kernel[i] *= sumScale
        }
    }

    return kernel
}

/**
 * Performs a 3x3 convolution filter on the image buffer.
 */
public fun filter3x3(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    kernel: FloatArray,
): ByteArray {
    require(kernel.size == 9) { "Kernel must have 9 elements" }
    val tapsX = intArrayOf(-1, 0, 1, -1, 0, 1, -1, 0, 1)
    val tapsY = intArrayOf(-1, -1, -1, 0, 0, 0, 1, 1, 1)

    val out = image.copyOf()
    var sumKernel = 0.0f
    for (k in kernel) sumKernel += k
    val inverseSum = if (sumKernel == 0.0f) 1.0f else 1.0f / sumKernel

    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            for (c in 0 until channels) {
                var t = 0.0f
                for (k in 0 until 9) {
                    val x0 = x + tapsX[k]
                    val y0 = y + tapsY[k]
                    val px = image[(y0 * width + x0) * channels + c].toInt() and 0xFF
                    t += px.toFloat() * kernel[k]
                }
                val finalVal = (t * inverseSum).coerceIn(0.0f, 255.0f)
                out[(y * width + x) * channels + c] = finalVal.toInt().toByte()
            }
        }
    }
    return out
}

/**
 * Applies a Gaussian blur with the given [sigma] to the image.
 */
public fun blur(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    sigma: Float,
): ByteArray {
    val actualSigma = if (sigma == 0.0f) 0.8f else sigma
    return blurAdvanced(image, width, height, channels, GaussianBlurParameters.newFromSigma(actualSigma))
}

/**
 * Applies Gaussian blur using analytical parameters.
 */
public fun blurAdvanced(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    params: GaussianBlurParameters,
): ByteArray {
    if (width <= 0 || height <= 0 || image.isEmpty()) return ByteArray(0)
    val xKernel = getGaussianKernel1d(params.xAxisKernelSize.toInt(), params.xAxisSigma)
    val yKernel = getGaussianKernel1d(params.yAxisKernelSize.toInt(), params.yAxisSigma)
    val temp = ByteArray(image.size)
    val out = ByteArray(image.size)
    filter1dHorizontal(image, temp, width, height, channels, xKernel)
    filter1dVertical(temp, out, width, height, channels, yKernel)
    return out
}

/**
 * Performs separable Gaussian blur on a [DynamicImage] using [GaussianBlurParameters].
 */
private fun byteArrayToShortArrayLE(bytes: ByteArray): ShortArray {
    val shorts = ShortArray(bytes.size / 2)
    for (i in shorts.indices) {
        val b0 = bytes[i * 2].toInt() and 0xFF
        val b1 = bytes[i * 2 + 1].toInt() and 0xFF
        shorts[i] = ((b1 shl 8) or b0).toShort()
    }
    return shorts
}

private fun shortArrayToByteArrayLE(shorts: ShortArray): ByteArray {
    val bytes = ByteArray(shorts.size * 2)
    for (i in shorts.indices) {
        val s = shorts[i].toInt()
        bytes[i * 2] = (s and 0xFF).toByte()
        bytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
    }
    return bytes
}

private fun byteArrayToFloatArrayLE(bytes: ByteArray): FloatArray {
    val floats = FloatArray(bytes.size / 4)
    for (i in floats.indices) {
        val b0 = bytes[i * 4].toInt() and 0xFF
        val b1 = bytes[i * 4 + 1].toInt() and 0xFF
        val b2 = bytes[i * 4 + 2].toInt() and 0xFF
        val b3 = bytes[i * 4 + 3].toInt() and 0xFF
        val bits = (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
        floats[i] = Float.fromBits(bits)
    }
    return floats
}

private fun floatArrayToByteArrayLE(floats: FloatArray): ByteArray {
    val bytes = ByteArray(floats.size * 4)
    for (i in floats.indices) {
        val bits = floats[i].toBits()
        bytes[i * 4] = (bits and 0xFF).toByte()
        bytes[i * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
        bytes[i * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
        bytes[i * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
    }
    return bytes
}

/**
 * Performs separable Gaussian blur on a [DynamicImage] using [GaussianBlurParameters].
 */
public fun gaussianBlurDynImage(
    image: DynamicImage,
    parameters: GaussianBlurParameters,
): DynamicImage {
    val xAxisKernel =
        getGaussianKernel1d(
            parameters.xAxisKernelSize.toInt(),
            parameters.xAxisSigma,
        )
    val yAxisKernel =
        getGaussianKernel1d(
            parameters.yAxisKernelSize.toInt(),
            parameters.yAxisSigma,
        )

    val filterImageSize =
        FilterImageSize(
            width = image.width().toInt(),
            height = image.height().toInt(),
        )

    val target: DynamicImage =
        when (image) {
            is DynamicImage.ImageLuma8 -> {
                val raw = image.image.asRaw()
                val dest = ByteArray(raw.size)
                filter2dSepPlane(raw, dest, filterImageSize, xAxisKernel, yAxisKernel)
                DynamicImage.ImageLuma8(ImageBuffer.createGray(image.width(), image.height(), dest)!!)
            }
            is DynamicImage.ImageLumaA8 -> {
                val raw = image.image.asRaw()
                val dest = ByteArray(raw.size)
                filter2dSepLa(raw, dest, filterImageSize, xAxisKernel, yAxisKernel)
                DynamicImage.ImageLumaA8(ImageBuffer.createGrayAlpha(image.width(), image.height(), dest)!!)
            }
            is DynamicImage.ImageRgb8 -> {
                val raw = image.image.asRaw()
                val dest = ByteArray(raw.size)
                filter2dSepRgb(raw, dest, filterImageSize, xAxisKernel, yAxisKernel)
                DynamicImage.ImageRgb8(ImageBuffer.createRgb(image.width(), image.height(), dest)!!)
            }
            is DynamicImage.ImageRgba8 -> {
                val raw = image.image.asRaw()
                val dest = ByteArray(raw.size)
                filter2dSepRgba(raw, dest, filterImageSize, xAxisKernel, yAxisKernel)
                DynamicImage.ImageRgba8(ImageBuffer.createRgba(image.width(), image.height(), dest)!!)
            }
            is DynamicImage.ImageLuma16 -> {
                val rawShorts = byteArrayToShortArrayLE(image.image.asRaw())
                val destShorts = ShortArray(rawShorts.size)
                filter2dSepPlaneU16(rawShorts, destShorts, filterImageSize, xAxisKernel, yAxisKernel)
                val destBytes = shortArrayToByteArrayLE(destShorts)
                DynamicImage.ImageLuma16(ImageBuffer.createGray16(image.width(), image.height(), destBytes)!!)
            }
            is DynamicImage.ImageLumaA16 -> {
                val rawShorts = byteArrayToShortArrayLE(image.image.asRaw())
                val destShorts = ShortArray(rawShorts.size)
                filter2dSepLaU16(rawShorts, destShorts, filterImageSize, xAxisKernel, yAxisKernel)
                val destBytes = shortArrayToByteArrayLE(destShorts)
                DynamicImage.ImageLumaA16(ImageBuffer.createGrayAlpha16(image.width(), image.height(), destBytes)!!)
            }
            is DynamicImage.ImageRgb16 -> {
                val rawShorts = byteArrayToShortArrayLE(image.image.asRaw())
                val destShorts = ShortArray(rawShorts.size)
                filter2dSepRgbU16(rawShorts, destShorts, filterImageSize, xAxisKernel, yAxisKernel)
                val destBytes = shortArrayToByteArrayLE(destShorts)
                DynamicImage.ImageRgb16(ImageBuffer.createRgb16(image.width(), image.height(), destBytes)!!)
            }
            is DynamicImage.ImageRgba16 -> {
                val rawShorts = byteArrayToShortArrayLE(image.image.asRaw())
                val destShorts = ShortArray(rawShorts.size)
                filter2dSepRgbaU16(rawShorts, destShorts, filterImageSize, xAxisKernel, yAxisKernel)
                val destBytes = shortArrayToByteArrayLE(destShorts)
                DynamicImage.ImageRgba16(ImageBuffer.createRgba16(image.width(), image.height(), destBytes)!!)
            }
            is DynamicImage.ImageRgb32F -> {
                val rawFloats = byteArrayToFloatArrayLE(image.image.asRaw())
                val destFloats = FloatArray(rawFloats.size)
                filter2dSepRgbF32(rawFloats, destFloats, filterImageSize, xAxisKernel, yAxisKernel)
                val destBytes = floatArrayToByteArrayLE(destFloats)
                DynamicImage.ImageRgb32F(ImageBuffer.createRgb32F(image.width(), image.height(), destBytes)!!)
            }
            is DynamicImage.ImageRgba32F -> {
                val rawFloats = byteArrayToFloatArrayLE(image.image.asRaw())
                val destFloats = FloatArray(rawFloats.size)
                filter2dSepRgbaF32(rawFloats, destFloats, filterImageSize, xAxisKernel, yAxisKernel)
                val destBytes = floatArrayToByteArrayLE(destFloats)
                DynamicImage.ImageRgba32F(ImageBuffer.createRgba32F(image.width(), image.height(), destBytes)!!)
            }
        }

    target.setColorSpace(image.colorSpace())
    return target
}

/**
 * Performs a 3x3 convolution filter on the image.
 */
@Suppress("UNCHECKED_CAST")
public fun <P> filter3x3(
    image: GenericImageView<P>,
    kernel: FloatArray,
): ImageBuffer<P, ByteArray> {
    require(kernel.size == 9) { "Kernel must have 9 elements" }
    val (width, height) = image.dimensions()
    val out = image.bufferLike()
    if (width < 3u || height < 3u) {
        return out
    }
    var sumKernel = 0.0f
    for (k in kernel) sumKernel += k
    val inverseSum = if (sumKernel == 0.0f) 1.0f else 1.0f / sumKernel

    val tapsX = intArrayOf(-1, 0, 1, -1, 0, 1, -1, 0, 1)
    val tapsY = intArrayOf(-1, -1, -1, 0, 0, 0, 1, 1, 1)

    for (y in 1u until height - 1u) {
        for (x in 1u until width - 1u) {
            val center = image.getPixel(x, y)
            val p: P = when (center) {
                is Luma<*> -> {
                    var sum = 0.0f
                    for (k in 0 until 9) {
                        val px = image.getPixel((x.toInt() + tapsX[k]).toUInt(), (y.toInt() + tapsY[k]).toUInt()) as Luma<*>
                        val c = (px.l as? Number)?.toFloat() ?: 0.0f
                        sum += c * kernel[k]
                    }
                    Luma((sum * inverseSum).coerceIn(0.0f, 255.0f).toInt().toUByte()) as P
                }
                is LumaA<*> -> {
                    var sumL = 0.0f
                    for (k in 0 until 9) {
                        val px = image.getPixel((x.toInt() + tapsX[k]).toUInt(), (y.toInt() + tapsY[k]).toUInt()) as LumaA<*>
                        val c = (px.l as? Number)?.toFloat() ?: 0.0f
                        sumL += c * kernel[k]
                    }
                    LumaA((sumL * inverseSum).coerceIn(0.0f, 255.0f).toInt().toUByte(), center.a) as P
                }
                is Rgb<*> -> {
                    var sumR = 0.0f
                    var sumG = 0.0f
                    var sumB = 0.0f
                    for (k in 0 until 9) {
                        val px = image.getPixel((x.toInt() + tapsX[k]).toUInt(), (y.toInt() + tapsY[k]).toUInt()) as Rgb<*>
                        sumR += ((px.r as? Number)?.toFloat() ?: 0.0f) * kernel[k]
                        sumG += ((px.g as? Number)?.toFloat() ?: 0.0f) * kernel[k]
                        sumB += ((px.b as? Number)?.toFloat() ?: 0.0f) * kernel[k]
                    }
                    Rgb(
                        (sumR * inverseSum).coerceIn(0.0f, 255.0f).toInt().toUByte(),
                        (sumG * inverseSum).coerceIn(0.0f, 255.0f).toInt().toUByte(),
                        (sumB * inverseSum).coerceIn(0.0f, 255.0f).toInt().toUByte(),
                    ) as P
                }
                is Rgba<*> -> {
                    var sumR = 0.0f
                    var sumG = 0.0f
                    var sumB = 0.0f
                    for (k in 0 until 9) {
                        val px = image.getPixel((x.toInt() + tapsX[k]).toUInt(), (y.toInt() + tapsY[k]).toUInt()) as Rgba<*>
                        sumR += ((px.r as? Number)?.toFloat() ?: 0.0f) * kernel[k]
                        sumG += ((px.g as? Number)?.toFloat() ?: 0.0f) * kernel[k]
                        sumB += ((px.b as? Number)?.toFloat() ?: 0.0f) * kernel[k]
                    }
                    Rgba(
                        (sumR * inverseSum).coerceIn(0.0f, 255.0f).toInt().toUByte(),
                        (sumG * inverseSum).coerceIn(0.0f, 255.0f).toInt().toUByte(),
                        (sumB * inverseSum).coerceIn(0.0f, 255.0f).toInt().toUByte(),
                        center.a,
                    ) as P
                }
                else -> center
            }
            out.putPixel(x, y, p)
        }
    }
    return out
}

/**
 * Applies a Gaussian blur with the given [sigma] to the image.
 */
public fun <P> blur(
    image: GenericImageView<P>,
    sigma: Float,
): ImageBuffer<P, ByteArray> {
    val actualSigma = if (sigma == 0.0f) 0.8f else sigma
    return blurAdvanced(image, GaussianBlurParameters.newFromSigma(actualSigma))
}

/**
 * Applies Gaussian blur using analytical parameters.
 */
public fun <P> blurAdvanced(
    image: GenericImageView<P>,
    params: GaussianBlurParameters,
): ImageBuffer<P, ByteArray> {
    val (width, height) = image.dimensions()
    if (width == 0u || height == 0u) return image.bufferWithDimensions(0u, 0u)
    val channels = when (image.getPixel(0u, 0u)) {
        is Luma<*> -> 1
        is LumaA<*> -> 2
        is Rgb<*> -> 3
        is Rgba<*> -> 4
        else -> 1
    }
    val raw = if (image is ImageBuffer<*, *>) {
        image.asRaw().copyOf()
    } else null
    val buf = raw ?: run {
        val b = ByteArray(width.toInt() * height.toInt() * channels)
        for (y in 0u until height) {
            for (x in 0u until width) {
                val p = image.getPixel(x, y)
                val idx = (y.toInt() * width.toInt() + x.toInt()) * channels
                when (p) {
                    is Luma<*> -> b[idx] = ((p.l as? Number)?.toInt() ?: 0).toByte()
                    is LumaA<*> -> {
                        b[idx] = ((p.l as? Number)?.toInt() ?: 0).toByte()
                        b[idx + 1] = ((p.a as? Number)?.toInt() ?: 0).toByte()
                    }
                    is Rgb<*> -> {
                        b[idx] = ((p.r as? Number)?.toInt() ?: 0).toByte()
                        b[idx + 1] = ((p.g as? Number)?.toInt() ?: 0).toByte()
                        b[idx + 2] = ((p.b as? Number)?.toInt() ?: 0).toByte()
                    }
                    is Rgba<*> -> {
                        b[idx] = ((p.r as? Number)?.toInt() ?: 0).toByte()
                        b[idx + 1] = ((p.g as? Number)?.toInt() ?: 0).toByte()
                        b[idx + 2] = ((p.b as? Number)?.toInt() ?: 0).toByte()
                        b[idx + 3] = ((p.a as? Number)?.toInt() ?: 0).toByte()
                    }
                }
            }
        }
        b
    }
    val blurred = blurAdvanced(buf, width.toInt(), height.toInt(), channels, params)
    @Suppress("UNCHECKED_CAST")
    return when (channels) {
        1 -> ImageBuffer.createGray(width, height, blurred) as ImageBuffer<P, ByteArray>
        2 -> ImageBuffer.createGrayAlpha(width, height, blurred) as ImageBuffer<P, ByteArray>
        3 -> ImageBuffer.createRgb(width, height, blurred) as ImageBuffer<P, ByteArray>
        4 -> ImageBuffer.createRgba(width, height, blurred) as ImageBuffer<P, ByteArray>
        else -> image.bufferLike()
    }
}

/**
 * Performs an unsharpen mask on the image buffer.
 */
public fun unsharpen(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    sigma: Float,
    threshold: Int,
): ByteArray {
    if (width <= 0 || height <= 0 || image.isEmpty()) return ByteArray(0)
    val blurred = blurAdvanced(image, width, height, channels, GaussianBlurParameters.newFromSigma(sigma))
    val out = ByteArray(image.size)
    for (i in image.indices) {
        val ic = image[i].toInt() and 0xFF
        val id = blurred[i].toInt() and 0xFF
        val diff = ic - id
        if (abs(diff) > threshold) {
            val e = (ic + diff).coerceIn(0, 255)
            out[i] = e.toByte()
        } else {
            out[i] = image[i]
        }
    }
    return out
}

/**
 * Performs an unsharpen mask on the image.
 */
public fun <P> unsharpen(
    image: GenericImageView<P>,
    sigma: Float,
    threshold: Int,
): ImageBuffer<P, ByteArray> {
    val (width, height) = image.dimensions()
    if (width == 0u || height == 0u) return image.bufferWithDimensions(0u, 0u)
    val blurred = blurAdvanced(image, GaussianBlurParameters.newFromSigma(sigma))
    val out = image.bufferLike()
    for (y in 0u until height) {
        for (x in 0u until width) {
            val ic = image.getPixel(x, y)
            val id = blurred.getPixel(x, y)
            @Suppress("UNCHECKED_CAST")
            val p: P = when (ic) {
                is Luma<*> -> {
                    val l1 = (ic.l as? Number)?.toInt() ?: 0
                    val l2 = ((id as Luma<*>).l as? Number)?.toInt() ?: 0
                    val diff = l1 - l2
                    val e = if (abs(diff) > threshold) (l1 + diff).coerceIn(0, 255) else l1
                    Luma(e.toUByte()) as P
                }
                is LumaA<*> -> {
                    val l1 = (ic.l as? Number)?.toInt() ?: 0
                    val l2 = ((id as LumaA<*>).l as? Number)?.toInt() ?: 0
                    val diff = l1 - l2
                    val e = if (abs(diff) > threshold) (l1 + diff).coerceIn(0, 255) else l1
                    LumaA(e.toUByte(), ic.a) as P
                }
                is Rgb<*> -> {
                    val r1 = (ic.r as? Number)?.toInt() ?: 0
                    val g1 = (ic.g as? Number)?.toInt() ?: 0
                    val b1 = (ic.b as? Number)?.toInt() ?: 0
                    val r2 = ((id as Rgb<*>).r as? Number)?.toInt() ?: 0
                    val g2 = (id.g as? Number)?.toInt() ?: 0
                    val b2 = (id.b as? Number)?.toInt() ?: 0
                    val diffR = r1 - r2
                    val diffG = g1 - g2
                    val diffB = b1 - b2
                    val er = if (abs(diffR) > threshold) (r1 + diffR).coerceIn(0, 255) else r1
                    val eg = if (abs(diffG) > threshold) (g1 + diffG).coerceIn(0, 255) else g1
                    val eb = if (abs(diffB) > threshold) (b1 + diffB).coerceIn(0, 255) else b1
                    Rgb(er.toUByte(), eg.toUByte(), eb.toUByte()) as P
                }
                is Rgba<*> -> {
                    val r1 = (ic.r as? Number)?.toInt() ?: 0
                    val g1 = (ic.g as? Number)?.toInt() ?: 0
                    val b1 = (ic.b as? Number)?.toInt() ?: 0
                    val r2 = ((id as Rgba<*>).r as? Number)?.toInt() ?: 0
                    val g2 = (id.g as? Number)?.toInt() ?: 0
                    val b2 = (id.b as? Number)?.toInt() ?: 0
                    val diffR = r1 - r2
                    val diffG = g1 - g2
                    val diffB = b1 - b2
                    val er = if (abs(diffR) > threshold) (r1 + diffR).coerceIn(0, 255) else r1
                    val eg = if (abs(diffG) > threshold) (g1 + diffG).coerceIn(0, 255) else g1
                    val eb = if (abs(diffB) > threshold) (b1 + diffB).coerceIn(0, 255) else b1
                    Rgba(er.toUByte(), eg.toUByte(), eb.toUByte(), ic.a) as P
                }
                else -> ic
            }
            out.putPixel(x, y, p)
        }
    }
    return out
}

