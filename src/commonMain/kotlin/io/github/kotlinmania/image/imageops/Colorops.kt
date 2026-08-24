// port-lint: source imageops/colorops.rs
package io.github.kotlinmania.image.imageops

import kotlin.math.cos
import kotlin.math.sin

/**
 * Inverts pixel values in-place (255 - value). Alpha channel if present is left untouched.
 */
public fun invert(image: ByteArray, channels: Int = 4) {
    for (i in image.indices) {
        if (channels == 4 && (i % 4 == 3)) continue
        val v = image[i].toInt() and 0xFF
        image[i] = (255 - v).toByte()
    }
}

/**
 * Converts RGB/RGBA image into grayscale. Alpha channel is discarded.
 */
public fun grayscale(image: ByteArray, width: Int, height: Int, channels: Int): ByteArray {
    val numPixels = width * height
    val out = ByteArray(numPixels)
    for (i in 0 until numPixels) {
        val srcIdx = i * channels
        val r = image[srcIdx].toInt() and 0xFF
        val g = if (channels >= 2) image[srcIdx + 1].toInt() and 0xFF else r
        val b = if (channels >= 3) image[srcIdx + 2].toInt() and 0xFF else r
        // Rec. 601 luma formula
        val luma = (0.299 * r + 0.587 * g + 0.114 * b).coerceIn(0.0, 255.0).toInt()
        out[i] = luma.toByte()
    }
    return out
}

/**
 * Converts RGB/RGBA image into grayscale with alpha preserved.
 */
public fun grayscaleAlpha(image: ByteArray, width: Int, height: Int, channels: Int): ByteArray {
    val numPixels = width * height
    val out = ByteArray(numPixels * 2)
    for (i in 0 until numPixels) {
        val srcIdx = i * channels
        val r = image[srcIdx].toInt() and 0xFF
        val g = if (channels >= 2) image[srcIdx + 1].toInt() and 0xFF else r
        val b = if (channels >= 3) image[srcIdx + 2].toInt() and 0xFF else r
        val a = if (channels >= 4) image[srcIdx + 3] else (-1).toByte()
        val luma = (0.299 * r + 0.587 * g + 0.114 * b).coerceIn(0.0, 255.0).toInt()
        out[i * 2] = luma.toByte()
        out[i * 2 + 1] = a
    }
    return out
}

/**
 * Adjusts contrast of an image.
 */
public fun contrast(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    contrast: Float,
): ByteArray {
    require(width >= 0 && height >= 0)
    val out = image.copyOf()
    contrastInPlace(out, channels, contrast)
    return out
}

/**
 * Adjusts contrast of an image in place.
 */
public fun contrastInPlace(
    image: ByteArray,
    channels: Int,
    contrast: Float,
) {
    val factor = (100.0f + contrast) / 100.0f
    val percent = factor * factor
    for (i in image.indices) {
        if (channels == 4 && (i % 4 == 3)) {
            continue
        }
        val c = (image[i].toInt() and 0xFF).toFloat()
        val d = ((c / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
        image[i] = d.coerceIn(0.0f, 255.0f).toInt().toByte()
    }
}

/**
 * Brightens the supplied image.
 */
public fun brighten(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    value: Int,
): ByteArray {
    require(width >= 0 && height >= 0)
    val out = image.copyOf()
    brightenInPlace(out, channels, value)
    return out
}

/**
 * Brightens the supplied image in place.
 */
public fun brightenInPlace(
    image: ByteArray,
    channels: Int,
    value: Int,
) {
    for (i in image.indices) {
        if (channels == 4 && (i % 4 == 3)) {
            continue
        }
        val c = image[i].toInt() and 0xFF
        val d = (c + value).coerceIn(0, 255)
        image[i] = d.toByte()
    }
}

/**
 * Hue rotates RGB/RGBA image in degrees.
 */
public fun huerotate(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    degrees: Int,
): ByteArray {
    val out = image.copyOf()
    huerotateInPlace(out, width, height, channels, degrees)
    return out
}

/**
 * Hue rotates RGB/RGBA image in degrees in place.
 */
public fun huerotateInPlace(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    degrees: Int,
) {
    val rad = degrees.toDouble() * 3.141592653589793 / 180.0
    val cosv = cos(rad)
    val sinv = sin(rad)

    val m0 = 0.213 + cosv * 0.787 - sinv * 0.213
    val m1 = 0.715 - cosv * 0.715 - sinv * 0.715
    val m2 = 0.072 - cosv * 0.072 + sinv * 0.928

    val m3 = 0.213 - cosv * 0.213 + sinv * 0.143
    val m4 = 0.715 + cosv * 0.285 + sinv * 0.140
    val m5 = 0.072 - cosv * 0.072 - sinv * 0.283

    val m6 = 0.213 - cosv * 0.213 - sinv * 0.787
    val m7 = 0.715 - cosv * 0.715 + sinv * 0.715
    val m8 = 0.072 + cosv * 0.928 + sinv * 0.072

    val numPixels = width * height
    for (i in 0 until numPixels) {
        val idx = i * channels
        val r = (image[idx].toInt() and 0xFF).toDouble()
        val g = (image[idx + 1].toInt() and 0xFF).toDouble()
        val b = (image[idx + 2].toInt() and 0xFF).toDouble()

        val nr = (m0 * r + m1 * g + m2 * b).coerceIn(0.0, 255.0).toInt()
        val ng = (m3 * r + m4 * g + m5 * b).coerceIn(0.0, 255.0).toInt()
        val nb = (m6 * r + m7 * g + m8 * b).coerceIn(0.0, 255.0).toInt()

        image[idx] = nr.toByte()
        image[idx + 1] = ng.toByte()
        image[idx + 2] = nb.toByte()
    }
}

/**
 * A color map interface.
 */
public interface ColorMap<T> {
    public fun indexOf(color: T): Int

    public fun lookup(index: Int): T? = null

    public fun hasLookup(): Boolean = false

    public fun mapColor(color: T): T
}

/**
 * A bi-level color map.
 */
public object BiLevel : ColorMap<UByte> {
    override fun indexOf(color: UByte): Int = if (color.toInt() > 127) 1 else 0

    override fun lookup(index: Int): UByte? =
        when (index) {
            0 -> 0u.toUByte()
            1 -> 255u.toUByte()
            else -> null
        }

    override fun hasLookup(): Boolean = true

    override fun mapColor(color: UByte): UByte = (0xFF * indexOf(color)).toUByte()
}

/**
 * Floyd-Steinberg error diffusion helper.
 */
private fun diffuseErr(
    image: ByteArray,
    pixelOffset: Int,
    channels: Int,
    error: ShortArray,
    factor: Short,
) {
    val count = minOf(channels, error.size)
    for (i in 0 until count) {
        val current = (image[pixelOffset + i].toInt() and 0xFF).toShort()
        val diffused = current + error[i] * factor / 16
        val clamped = diffused.coerceIn(0, 255)
        image[pixelOffset + i] = clamped.toByte()
    }
}

/**
 * Reduces the colors of the grayscale image using Floyd-Steinberg dithering.
 */
public fun dither(
    image: ByteArray,
    width: Int,
    height: Int,
    colorMap: ColorMap<UByte>,
) {
    if (width <= 0 || height <= 0) return
    val err = ShortArray(1)

    fun doDithering(x: Int, y: Int) {
        val idx = y * width + x
        val oldPixel = (image[idx].toInt() and 0xFF).toUByte()
        val newPixel = colorMap.mapColor(oldPixel)
        image[idx] = newPixel.toByte()
        err[0] = (oldPixel.toInt() - newPixel.toInt()).toShort()
    }

    for (y in 0 until height - 1) {
        var x = 0
        doDithering(x, y)
        if (x + 1 < width) diffuseErr(image, y * width + (x + 1), 1, err, 7)
        diffuseErr(image, (y + 1) * width + x, 1, err, 5)
        if (x + 1 < width) diffuseErr(image, (y + 1) * width + (x + 1), 1, err, 1)

        for (curX in 1 until width - 1) {
            x = curX
            doDithering(x, y)
            diffuseErr(image, y * width + (x + 1), 1, err, 7)
            diffuseErr(image, (y + 1) * width + (x - 1), 1, err, 3)
            diffuseErr(image, (y + 1) * width + x, 1, err, 5)
            diffuseErr(image, (y + 1) * width + (x + 1), 1, err, 1)
        }

        if (width > 1) {
            x = width - 1
            doDithering(x, y)
            diffuseErr(image, (y + 1) * width + (x - 1), 1, err, 3)
            diffuseErr(image, (y + 1) * width + x, 1, err, 5)
        }
    }

    val y = height - 1
    var x = 0
    doDithering(x, y)
    if (x + 1 < width) diffuseErr(image, y * width + (x + 1), 1, err, 7)
    for (curX in 1 until width - 1) {
        x = curX
        doDithering(x, y)
        diffuseErr(image, y * width + (x + 1), 1, err, 7)
    }
    if (width > 1) {
        x = width - 1
        doDithering(x, y)
    }
}

/**
 * Reduces the colors using the supplied color map and returns an image of the indices.
 */
public fun indexColors(
    image: ByteArray,
    width: Int,
    height: Int,
    colorMap: ColorMap<UByte>,
): ByteArray {
    val out = ByteArray(width * height)
    for (i in 0 until width * height) {
        val pixel = (image[i].toInt() and 0xFF).toUByte()
        out[i] = colorMap.indexOf(pixel).toByte()
    }
    return out
}
