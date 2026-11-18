// port-lint: source codecs/avif/yuv.rs
package io.github.kotlinmania.image.codecs.avif

import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.io.ImageFormat
import kotlin.math.roundToInt

/**
 * Representation of inversion matrix.
 */
public data class CbCrInverseTransform(
    val yCoef: Int,
    val crCoef: Int,
    val cbCoef: Int,
    val gCoeff1: Int,
    val gCoeff2: Int,
)

/**
 * Declares plane definition for error reporting.
 */
public enum class PlaneDefinition(
    private val description: String,
) {
    Y("Luma"),
    U("U chroma"),
    V("V chroma"),
    ;

    override fun toString(): String = description
}

/**
 * Precondition checks for YUV plane buffer sizes.
 */
public fun checkYuvPlanePreconditions(
    plane: ByteArray,
    planeDefinition: PlaneDefinition,
    stride: Int,
    height: Int,
) {
    val expected = stride * height
    if (plane.size != expected) {
        throw ImageError.Decoding(
            DecodingError(
                ImageFormatHint.Exact(ImageFormat.Avif),
                "For plane $planeDefinition expected size is $expected but was received ${plane.size}",
            ),
        )
    }
}

/**
 * Precondition checks for RGB destination buffer size.
 */
public fun checkRgbPreconditions(
    rgbData: ByteArray,
    stride: Int,
    height: Int,
) {
    val expected = stride * height
    if (rgbData.size != expected) {
        throw ImageError.Decoding(
            DecodingError(
                ImageFormatHint.Exact(ImageFormat.Avif),
                "For RGB destination expected size is $expected but was received ${rgbData.size}",
            ),
        )
    }
}

/**
 * Transformation YUV to RGB with coefficients as specified in ITU-R.
 */
public fun getInverseTransform(
    rangeBgra: UInt,
    rangeY: UInt,
    rangeUv: UInt,
    kr: Float,
    kb: Float,
    precision: UInt,
): CbCrInverseTransform {
    val rangeUvF = rangeBgra.toFloat() / rangeUv.toFloat()
    val yCoefF = rangeBgra.toFloat() / rangeY.toFloat()
    val crCoeffF = (2.0f * (1.0f - kr)) * rangeUvF
    val cbCoeffF = (2.0f * (1.0f - kb)) * rangeUvF
    val kg = 1.0f - kr - kb
    require(kg != 0.0f) { "1.0f - kr - kg must not be 0" }
    val gCoeff1F = (2.0f * ((1.0f - kr) * kr / kg)) * rangeUvF
    val gCoeff2F = (2.0f * ((1.0f - kb) * kb / kg)) * rangeUvF

    val precisionScale = 1 shl precision.toInt()
    val crCoef = (crCoeffF * precisionScale).roundToInt()
    val cbCoef = (cbCoeffF * precisionScale).roundToInt()
    val yCoef = (yCoefF * precisionScale).roundToInt()
    val gCoeff1 = (gCoeff1F * precisionScale).roundToInt()
    val gCoeff2 = (gCoeff2F * precisionScale).roundToInt()

    return CbCrInverseTransform(
        yCoef = yCoef,
        crCoef = crCoef,
        cbCoef = cbCoef,
        gCoeff1 = gCoeff1,
        gCoeff2 = gCoeff2,
    )
}

/**
 * Declares YUV range TV (limited) or PC (full).
 */
public enum class YuvIntensityRange {
    Tv,
    Pc,
    ;

    public fun getYuvRange(depth: UInt): YuvChromaRange =
        when (this) {
            Tv ->
                YuvChromaRange(
                    biasY = 16u shl (depth.toInt() - 8),
                    biasUv = 1u shl (depth.toInt() - 1),
                    rangeY = 219u shl (depth.toInt() - 8),
                    rangeUv = 224u shl (depth.toInt() - 8),
                    range = this,
                )
            Pc ->
                YuvChromaRange(
                    biasY = 0u,
                    biasUv = 1u shl (depth.toInt() - 1),
                    rangeY = (1u shl depth.toInt()) - 1u,
                    rangeUv = (1u shl depth.toInt()) - 1u,
                    range = this,
                )
        }
}

/**
 * YUV chroma range specification.
 */
public data class YuvChromaRange(
    val biasY: UInt,
    val biasUv: UInt,
    val rangeY: UInt,
    val rangeUv: UInt,
    val range: YuvIntensityRange,
)

/**
 * Declares standard prebuilt YUV conversion matrices.
 */
public enum class YuvStandardMatrix {
    Bt601,
    Bt709,
    Bt2020,
    Smpte240,
    Bt4706,
    ;

    public fun getKrKb(): Pair<Float, Float> =
        when (this) {
            Bt601 -> Pair(0.299f, 0.114f)
            Bt709 -> Pair(0.2126f, 0.0722f)
            Bt2020 -> Pair(0.2627f, 0.0593f)
            Smpte240 -> Pair(0.087f, 0.212f)
            Bt4706 -> Pair(0.2220f, 0.0713f)
        }
}

/**
 * Planar YUV image data representation.
 */
public class YuvPlanarImage(
    public val yPlane: ByteArray,
    public val yStride: Int,
    public val uPlane: ByteArray,
    public val uStride: Int,
    public val vPlane: ByteArray,
    public val vStride: Int,
    public val width: Int,
    public val height: Int,
)

/**
 * Saturating rounding shift right against bit depth.
 */
public fun qrshr(
    value: Int,
    precision: Int,
    bitDepth: Int,
): Int {
    val rounding = 1 shl (precision - 1)
    val maxValue = (1 shl bitDepth) - 1
    return ((value + rounding) shr precision).coerceIn(0, maxValue)
}

/**
 * Computes YCbCr inverse.
 */
private fun ycbcrExecute(
    dst: ByteArray,
    dstOffset: Int,
    yValue: Int,
    cb: Int,
    cr: Int,
    transform: CbCrInverseTransform,
    precision: Int,
    channels: Int,
    bitDepth: Int,
) {
    val yScaled = yValue * transform.yCoef
    val r = qrshr(yScaled + transform.crCoef * cr, precision, bitDepth)
    val b = qrshr(yScaled + transform.cbCoef * cb, precision, bitDepth)
    val g = qrshr(yScaled - transform.gCoeff1 * cr - transform.gCoeff2 * cb, precision, bitDepth)

    dst[dstOffset] = r.toByte()
    dst[dstOffset + 1] = g.toByte()
    dst[dstOffset + 2] = b.toByte()
    if (channels == 4) {
        val maxValue = (1 shl bitDepth) - 1
        dst[dstOffset + 3] = maxValue.toByte()
    }
}

/**
 * Converts YUV 400 planar format 8-bit to RGBA 8-bit.
 */
public fun yuv400ToRgba8(
    image: YuvPlanarImage,
    rgba: ByteArray,
    range: YuvIntensityRange,
    matrix: YuvStandardMatrix,
) {
    checkYuvPlanePreconditions(image.yPlane, PlaneDefinition.Y, image.yStride, image.height)
    checkRgbPreconditions(rgba, image.width * 4, image.height)

    val maxValue = 255
    if (range == YuvIntensityRange.Pc) {
        for (y in 0 until image.height) {
            val yRow = y * image.yStride
            val rgbaRow = y * image.width * 4
            for (x in 0 until image.width) {
                val r = image.yPlane[yRow + x]
                val dstIdx = rgbaRow + x * 4
                rgba[dstIdx] = r
                rgba[dstIdx + 1] = r
                rgba[dstIdx + 2] = r
                rgba[dstIdx + 3] = maxValue.toByte()
            }
        }
        return
    }

    val yuvRange = range.getYuvRange(8u)
    val (kr, kb) = matrix.getKrKb()
    val precision = 11
    val inverseTransform = getInverseTransform(255u, yuvRange.rangeY, yuvRange.rangeUv, kr, kb, precision.toUInt())
    val biasY = yuvRange.biasY.toInt()

    for (y in 0 until image.height) {
        val yRow = y * image.yStride
        val rgbaRow = y * image.width * 4
        for (x in 0 until image.width) {
            val ySrc = image.yPlane[yRow + x].toInt() and 0xFF
            val yValue = (ySrc - biasY) * inverseTransform.yCoef
            val r = qrshr(yValue, precision, 8).toByte()
            val dstIdx = rgbaRow + x * 4
            rgba[dstIdx] = r
            rgba[dstIdx + 1] = r
            rgba[dstIdx + 2] = r
            rgba[dstIdx + 3] = maxValue.toByte()
        }
    }
}

/**
 * Process a halved chroma row for CbCr.
 */
private fun processHalvedChromaRowCbCr(
    image: YuvPlanarImage,
    rgba: ByteArray,
    transform: CbCrInverseTransform,
    range: YuvChromaRange,
    channels: Int,
    precision: Int,
    bitDepth: Int,
) {
    val chromaWidth = (image.width + 1) / 2
    val biasY = range.biasY.toInt()
    val biasUv = range.biasUv.toInt()

    val pairs = image.width / 2
    for (i in 0 until pairs) {
        val y0 = (image.yPlane[i * 2].toInt() and 0xFF) - biasY
        val y1 = (image.yPlane[i * 2 + 1].toInt() and 0xFF) - biasY
        val cb = (image.uPlane[i].toInt() and 0xFF) - biasUv
        val cr = (image.vPlane[i].toInt() and 0xFF) - biasUv

        ycbcrExecute(rgba, i * 2 * channels, y0, cb, cr, transform, precision, channels, bitDepth)
        ycbcrExecute(rgba, (i * 2 + 1) * channels, y1, cb, cr, transform, precision, channels, bitDepth)
    }

    if (image.width % 2 != 0) {
        val lastIdx = pairs * 2
        val chromaIdx = chromaWidth - 1
        val yValue = (image.yPlane[lastIdx].toInt() and 0xFF) - biasY
        val cb = (image.uPlane[chromaIdx].toInt() and 0xFF) - biasUv
        val cr = (image.vPlane[chromaIdx].toInt() and 0xFF) - biasUv
        ycbcrExecute(rgba, lastIdx * channels, yValue, cb, cr, transform, precision, channels, bitDepth)
    }
}

/**
 * Converts YUV 420 8-bit planar format to RGBA 8-bit.
 */
public fun yuv420ToRgba8(
    image: YuvPlanarImage,
    rgb: ByteArray,
    range: YuvIntensityRange,
    matrix: YuvStandardMatrix,
) {
    val chromaHeight = (image.height + 1) / 2
    checkYuvPlanePreconditions(image.yPlane, PlaneDefinition.Y, image.yStride, image.height)
    checkYuvPlanePreconditions(image.uPlane, PlaneDefinition.U, image.uStride, chromaHeight)
    checkYuvPlanePreconditions(image.vPlane, PlaneDefinition.V, image.vStride, chromaHeight)
    checkRgbPreconditions(rgb, image.width * 4, image.height)

    val yuvRange = range.getYuvRange(8u)
    val (kr, kb) = matrix.getKrKb()
    val precision = 13
    val inverseTransform = getInverseTransform(255u, yuvRange.rangeY, yuvRange.rangeUv, kr, kb, precision.toUInt())

    val rgbStride = image.width * 4
    val pairs = image.height / 2
    for (pair in 0 until pairs) {
        val uRow = pair * image.uStride
        val vRow = pair * image.vStride
        val uSub = image.uPlane.copyOfRange(uRow, uRow + (image.width + 1) / 2)
        val vSub = image.vPlane.copyOfRange(vRow, vRow + (image.width + 1) / 2)

        for (subRow in 0 until 2) {
            val yIdx = (pair * 2 + subRow) * image.yStride
            val ySub = image.yPlane.copyOfRange(yIdx, yIdx + image.width)
            val rowImage =
                YuvPlanarImage(
                    yPlane = ySub,
                    yStride = 0,
                    uPlane = uSub,
                    uStride = 0,
                    vPlane = vSub,
                    vStride = 0,
                    width = image.width,
                    height = image.height,
                )
            val rowDst = ByteArray(rgbStride)
            processHalvedChromaRowCbCr(rowImage, rowDst, inverseTransform, yuvRange, 4, precision, 8)
            rowDst.copyInto(rgb, (pair * 2 + subRow) * rgbStride)
        }
    }

    if (image.height % 2 != 0) {
        val lastRow = image.height - 1
        val lastChromaRow = chromaHeight - 1
        val uRow = lastChromaRow * image.uStride
        val vRow = lastChromaRow * image.vStride
        val uSub = image.uPlane.copyOfRange(uRow, uRow + (image.width + 1) / 2)
        val vSub = image.vPlane.copyOfRange(vRow, vRow + (image.width + 1) / 2)
        val yIdx = lastRow * image.yStride
        val ySub = image.yPlane.copyOfRange(yIdx, yIdx + image.width)
        val rowImage =
            YuvPlanarImage(
                yPlane = ySub,
                yStride = 0,
                uPlane = uSub,
                uStride = 0,
                vPlane = vSub,
                vStride = 0,
                width = image.width,
                height = image.height,
            )
        val rowDst = ByteArray(rgbStride)
        processHalvedChromaRowCbCr(rowImage, rowDst, inverseTransform, yuvRange, 4, precision, 8)
        rowDst.copyInto(rgb, lastRow * rgbStride)
    }
}

/**
 * Converts YUV 422 8-bit planar format to RGBA 8-bit.
 */
public fun yuv422ToRgba8(
    image: YuvPlanarImage,
    rgb: ByteArray,
    range: YuvIntensityRange,
    matrix: YuvStandardMatrix,
) {
    checkYuvPlanePreconditions(image.yPlane, PlaneDefinition.Y, image.yStride, image.height)
    checkYuvPlanePreconditions(image.uPlane, PlaneDefinition.U, image.uStride, image.height)
    checkYuvPlanePreconditions(image.vPlane, PlaneDefinition.V, image.vStride, image.height)
    checkRgbPreconditions(rgb, image.width * 4, image.height)

    val yuvRange = range.getYuvRange(8u)
    val (kr, kb) = matrix.getKrKb()
    val precision = 13
    val inverseTransform = getInverseTransform(255u, yuvRange.rangeY, yuvRange.rangeUv, kr, kb, precision.toUInt())

    val rgbStride = image.width * 4
    for (row in 0 until image.height) {
        val yIdx = row * image.yStride
        val uIdx = row * image.uStride
        val vIdx = row * image.vStride
        val ySub = image.yPlane.copyOfRange(yIdx, yIdx + image.width)
        val uSub = image.uPlane.copyOfRange(uIdx, uIdx + (image.width + 1) / 2)
        val vSub = image.vPlane.copyOfRange(vIdx, vIdx + (image.width + 1) / 2)

        val rowImage =
            YuvPlanarImage(
                yPlane = ySub,
                yStride = 0,
                uPlane = uSub,
                uStride = 0,
                vPlane = vSub,
                vStride = 0,
                width = image.width,
                height = image.height,
            )
        val rowDst = ByteArray(rgbStride)
        processHalvedChromaRowCbCr(rowImage, rowDst, inverseTransform, yuvRange, 4, precision, 8)
        rowDst.copyInto(rgb, row * rgbStride)
    }
}

/**
 * Converts YUV 444 8-bit planar format to RGBA 8-bit.
 */
public fun yuv444ToRgba8(
    image: YuvPlanarImage,
    rgba: ByteArray,
    range: YuvIntensityRange,
    matrix: YuvStandardMatrix,
) {
    checkYuvPlanePreconditions(image.yPlane, PlaneDefinition.Y, image.yStride, image.height)
    checkYuvPlanePreconditions(image.uPlane, PlaneDefinition.U, image.uStride, image.height)
    checkYuvPlanePreconditions(image.vPlane, PlaneDefinition.V, image.vStride, image.height)
    checkRgbPreconditions(rgba, image.width * 4, image.height)

    val yuvRange = range.getYuvRange(8u)
    val (kr, kb) = matrix.getKrKb()
    val precision = 13
    val inverseTransform = getInverseTransform(255u, yuvRange.rangeY, yuvRange.rangeUv, kr, kb, precision.toUInt())

    val biasY = yuvRange.biasY.toInt()
    val biasUv = yuvRange.biasUv.toInt()
    val rgbStride = image.width * 4

    for (row in 0 until image.height) {
        val yRow = row * image.yStride
        val uRow = row * image.uStride
        val vRow = row * image.vStride
        val dstRow = row * rgbStride

        for (col in 0 until image.width) {
            val yValue = (image.yPlane[yRow + col].toInt() and 0xFF) - biasY
            val cb = (image.uPlane[uRow + col].toInt() and 0xFF) - biasUv
            val cr = (image.vPlane[vRow + col].toInt() and 0xFF) - biasUv

            ycbcrExecute(rgba, dstRow + col * 4, yValue, cb, cr, inverseTransform, precision, 4, 8)
        }
    }
}

/**
 * Converts GBR 8-bit planar format to RGBA 8-bit.
 */
public fun gbrToRgba8(
    image: YuvPlanarImage,
    rgb: ByteArray,
    range: YuvIntensityRange,
) {
    checkYuvPlanePreconditions(image.yPlane, PlaneDefinition.Y, image.yStride, image.height)
    checkYuvPlanePreconditions(image.uPlane, PlaneDefinition.U, image.uStride, image.height)
    checkYuvPlanePreconditions(image.vPlane, PlaneDefinition.V, image.vStride, image.height)
    checkRgbPreconditions(rgb, image.width * 4, image.height)

    val rgbStride = image.width * 4
    val maxValue = 255

    when (range) {
        YuvIntensityRange.Tv -> {
            val precision = 11
            val yuvRange = range.getYuvRange(8u)
            val rangeRgba = 255
            val yCoef = ((rangeRgba.toFloat() / yuvRange.rangeY.toFloat()) * (1 shl precision)).roundToInt()
            val yBias = yuvRange.biasY.toInt()

            for (row in 0 until image.height) {
                val yRow = row * image.yStride
                val uRow = row * image.uStride
                val vRow = row * image.vStride
                val dstRow = row * rgbStride

                for (col in 0 until image.width) {
                    val ySrc = image.yPlane[yRow + col].toInt() and 0xFF
                    val uSrc = image.uPlane[uRow + col].toInt() and 0xFF
                    val vSrc = image.vPlane[vRow + col].toInt() and 0xFF

                    val dstIdx = dstRow + col * 4
                    rgb[dstIdx] = qrshr((vSrc - yBias) * yCoef, precision, 8).toByte()
                    rgb[dstIdx + 1] = qrshr((ySrc - yBias) * yCoef, precision, 8).toByte()
                    rgb[dstIdx + 2] = qrshr((uSrc - yBias) * yCoef, precision, 8).toByte()
                    rgb[dstIdx + 3] = maxValue.toByte()
                }
            }
        }
        YuvIntensityRange.Pc -> {
            for (row in 0 until image.height) {
                val yRow = row * image.yStride
                val uRow = row * image.uStride
                val vRow = row * image.vStride
                val dstRow = row * rgbStride

                for (col in 0 until image.width) {
                    val dstIdx = dstRow + col * 4
                    rgb[dstIdx] = image.vPlane[vRow + col]
                    rgb[dstIdx + 1] = image.yPlane[yRow + col]
                    rgb[dstIdx + 2] = image.uPlane[uRow + col]
                    rgb[dstIdx + 3] = maxValue.toByte()
                }
            }
        }
    }
}
