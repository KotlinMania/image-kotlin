// port-lint: source codecs/avif/ycgco.rs
package io.github.kotlinmania.image.codecs.avif

import kotlin.math.roundToInt

/**
 * Computes YCgCo inverse in limited range.
 */
private fun ycgcoExecuteLimited(
    dst: ByteArray,
    dstOffset: Int,
    yValue: Int,
    cg: Int,
    co: Int,
    scale: Int,
    precision: Int,
    channels: Int,
    bitDepth: Int,
) {
    val t0 = yValue - cg
    val r = qrshr((t0 + co) * scale, precision, bitDepth)
    val b = qrshr((t0 - co) * scale, precision, bitDepth)
    val g = qrshr((yValue + cg) * scale, precision, bitDepth)

    dst[dstOffset] = r.toByte()
    dst[dstOffset + 1] = g.toByte()
    dst[dstOffset + 2] = b.toByte()
    if (channels == 4) {
        val maxValue = (1 shl bitDepth) - 1
        dst[dstOffset + 3] = maxValue.toByte()
    }
}

/**
 * Computes YCgCo inverse in full range.
 */
private fun ycgcoExecuteFull(
    dst: ByteArray,
    dstOffset: Int,
    yValue: Int,
    cg: Int,
    co: Int,
    channels: Int,
    bitDepth: Int,
) {
    val t0 = yValue - cg
    val maxValue = (1 shl bitDepth) - 1
    val r = (t0 + co).coerceIn(0, maxValue)
    val b = (t0 - co).coerceIn(0, maxValue)
    val g = (yValue + cg).coerceIn(0, maxValue)

    dst[dstOffset] = r.toByte()
    dst[dstOffset + 1] = g.toByte()
    dst[dstOffset + 2] = b.toByte()
    if (channels == 4) {
        dst[dstOffset + 3] = maxValue.toByte()
    }
}

/**
 * Process a halved chroma row for YCgCo.
 */
private fun processHalvedChromaRowCgco(
    image: YuvPlanarImage,
    rgba: ByteArray,
    range: YuvChromaRange,
    scaleCoef: Int,
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
        val cg = (image.uPlane[i].toInt() and 0xFF) - biasUv
        val co = (image.vPlane[i].toInt() and 0xFF) - biasUv

        if (range.range == YuvIntensityRange.Tv) {
            ycgcoExecuteLimited(rgba, i * 2 * channels, y0, cg, co, scaleCoef, precision, channels, bitDepth)
            ycgcoExecuteLimited(rgba, (i * 2 + 1) * channels, y1, cg, co, scaleCoef, precision, channels, bitDepth)
        } else {
            ycgcoExecuteFull(rgba, i * 2 * channels, y0, cg, co, channels, bitDepth)
            ycgcoExecuteFull(rgba, (i * 2 + 1) * channels, y1, cg, co, channels, bitDepth)
        }
    }

    if (image.width % 2 != 0) {
        val lastIdx = pairs * 2
        val chromaIdx = chromaWidth - 1
        val yValue = (image.yPlane[lastIdx].toInt() and 0xFF) - biasY
        val cg = (image.uPlane[chromaIdx].toInt() and 0xFF) - biasUv
        val co = (image.vPlane[chromaIdx].toInt() and 0xFF) - biasUv

        if (range.range == YuvIntensityRange.Tv) {
            ycgcoExecuteLimited(rgba, lastIdx * channels, yValue, cg, co, scaleCoef, precision, channels, bitDepth)
        } else {
            ycgcoExecuteFull(rgba, lastIdx * channels, yValue, cg, co, channels, bitDepth)
        }
    }
}

/**
 * Converts YCgCo 420 8-bit planar format to RGBA 8-bit.
 */
public fun ycgco420ToRgba8(
    image: YuvPlanarImage,
    rgb: ByteArray,
    range: YuvIntensityRange,
) {
    val chromaHeight = (image.height + 1) / 2
    checkYuvPlanePreconditions(image.yPlane, PlaneDefinition.Y, image.yStride, image.height)
    checkYuvPlanePreconditions(image.uPlane, PlaneDefinition.U, image.uStride, chromaHeight)
    checkYuvPlanePreconditions(image.vPlane, PlaneDefinition.V, image.vStride, chromaHeight)
    checkRgbPreconditions(rgb, image.width * 4, image.height)

    val yuvRange = range.getYuvRange(8u)
    val precision = 13
    val maxValue = 255
    val scaleCoef =
        if (range == YuvIntensityRange.Tv) {
            ((maxValue.toFloat() / yuvRange.rangeY.toFloat()) * (1 shl precision)).roundToInt()
        } else {
            1
        }

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
            processHalvedChromaRowCgco(rowImage, rowDst, yuvRange, scaleCoef, 4, precision, 8)
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
        processHalvedChromaRowCgco(rowImage, rowDst, yuvRange, scaleCoef, 4, precision, 8)
        rowDst.copyInto(rgb, lastRow * rgbStride)
    }
}

/**
 * Converts YCgCo 422 8-bit planar format to RGBA 8-bit.
 */
public fun ycgco422ToRgba8(
    image: YuvPlanarImage,
    rgb: ByteArray,
    range: YuvIntensityRange,
) {
    checkYuvPlanePreconditions(image.yPlane, PlaneDefinition.Y, image.yStride, image.height)
    checkYuvPlanePreconditions(image.uPlane, PlaneDefinition.U, image.uStride, image.height)
    checkYuvPlanePreconditions(image.vPlane, PlaneDefinition.V, image.vStride, image.height)
    checkRgbPreconditions(rgb, image.width * 4, image.height)

    val yuvRange = range.getYuvRange(8u)
    val precision = 13
    val maxValue = 255
    val scaleCoef =
        if (range == YuvIntensityRange.Tv) {
            ((maxValue.toFloat() / yuvRange.rangeY.toFloat()) * (1 shl precision)).roundToInt()
        } else {
            1
        }

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
        processHalvedChromaRowCgco(rowImage, rowDst, yuvRange, scaleCoef, 4, precision, 8)
        rowDst.copyInto(rgb, row * rgbStride)
    }
}

/**
 * Converts YCgCo 444 8-bit planar format to RGBA 8-bit.
 */
public fun ycgco444ToRgba8(
    image: YuvPlanarImage,
    rgba: ByteArray,
    range: YuvIntensityRange,
) {
    checkYuvPlanePreconditions(image.yPlane, PlaneDefinition.Y, image.yStride, image.height)
    checkYuvPlanePreconditions(image.uPlane, PlaneDefinition.U, image.uStride, image.height)
    checkYuvPlanePreconditions(image.vPlane, PlaneDefinition.V, image.vStride, image.height)
    checkRgbPreconditions(rgba, image.width * 4, image.height)

    val yuvRange = range.getYuvRange(8u)
    val precision = 13
    val maxValue = 255
    val scaleCoef =
        if (range == YuvIntensityRange.Tv) {
            ((maxValue.toFloat() / yuvRange.rangeY.toFloat()) * (1 shl precision)).roundToInt()
        } else {
            1
        }

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
            val cg = (image.uPlane[uRow + col].toInt() and 0xFF) - biasUv
            val co = (image.vPlane[vRow + col].toInt() and 0xFF) - biasUv

            if (range == YuvIntensityRange.Tv) {
                ycgcoExecuteLimited(rgba, dstRow + col * 4, yValue, cg, co, scaleCoef, precision, 4, 8)
            } else {
                ycgcoExecuteFull(rgba, dstRow + col * 4, yValue, cg, co, 4, 8)
            }
        }
    }
}
