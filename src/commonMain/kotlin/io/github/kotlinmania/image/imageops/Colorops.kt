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
 * Converts RGB/RGBA image into grayscale.
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
 * Adjusts contrast of an image.
 */
public fun contrast(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    contrast: Float,
): ByteArray {
    val factor = (100.0f + contrast) / 100.0f
    val percent = factor * factor
    val out = ByteArray(image.size)
    for (i in image.indices) {
        if (channels == 4 && (i % 4 == 3)) {
            out[i] = image[i]
            continue
        }
        val c = (image[i].toInt() and 0xFF).toFloat()
        val d = ((c / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
        out[i] = d.coerceIn(0.0f, 255.0f).toInt().toByte()
    }
    return out
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
    val out = ByteArray(image.size)
    for (i in image.indices) {
        if (channels == 4 && (i % 4 == 3)) {
            out[i] = image[i]
            continue
        }
        val c = image[i].toInt() and 0xFF
        val d = (c + value).coerceIn(0, 255)
        out[i] = d.toByte()
    }
    return out
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

    val out = ByteArray(image.size)
    val numPixels = width * height
    for (i in 0 until numPixels) {
        val idx = i * channels
        val r = (image[idx].toInt() and 0xFF).toDouble()
        val g = (image[idx + 1].toInt() and 0xFF).toDouble()
        val b = (image[idx + 2].toInt() and 0xFF).toDouble()

        val nr = (m0 * r + m1 * g + m2 * b).coerceIn(0.0, 255.0).toInt()
        val ng = (m3 * r + m4 * g + m5 * b).coerceIn(0.0, 255.0).toInt()
        val nb = (m6 * r + m7 * g + m8 * b).coerceIn(0.0, 255.0).toInt()

        out[idx] = nr.toByte()
        out[idx + 1] = ng.toByte()
        out[idx + 2] = nb.toByte()
        if (channels == 4) {
            out[idx + 3] = image[idx + 3]
        }
    }
    return out
}
