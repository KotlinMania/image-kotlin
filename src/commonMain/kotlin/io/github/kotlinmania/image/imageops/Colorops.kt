// port-lint: source imageops/colorops.rs
package io.github.kotlinmania.image.imageops

import io.github.kotlinmania.image.GrayAlphaImage
import io.github.kotlinmania.image.GrayImage
import io.github.kotlinmania.image.Luma
import io.github.kotlinmania.image.LumaA
import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.images.GenericImage
import io.github.kotlinmania.image.images.GenericImageView
import io.github.kotlinmania.image.images.ImageBuffer
import kotlin.math.cos
import kotlin.math.sin

/**
 * Associated subpixel type for generic image color operations.
 */
public typealias Subpixel = UByte

/**
 * Associated color type for color map operations.
 */
public typealias Color = Any

/**
 * Convert the supplied image to grayscale. Alpha channel is discarded.
 */
public fun <I : GenericImageView<*>> grayscale(
    image: I,
): GrayImage = grayscaleWithType(image)

/**
 * Convert the supplied image to grayscale. Alpha channel is preserved.
 */
public fun <I : GenericImageView<*>> grayscaleAlpha(
    image: I,
): GrayAlphaImage = grayscaleWithTypeAlpha(image)

/**
 * Convert the supplied image to a grayscale image with the specified pixel type. Alpha channel is discarded.
 */
public fun <I : GenericImageView<*>> grayscaleWithType(
    image: I,
): GrayImage {
    val (width, height) = image.dimensions()
    val out = ImageBuffer.createGray(width, height)
    for (y in 0u until height) {
        for (x in 0u until width) {
            val p = image.getPixel(x, y)
            val luma = when (p) {
                is Luma<*> -> (p.l as? UByte) ?: ((p.l as? Number)?.toLong()?.coerceIn(0, 255)?.toUByte() ?: 0u.toUByte())
                is LumaA<*> -> (p.l as? UByte) ?: ((p.l as? Number)?.toLong()?.coerceIn(0, 255)?.toUByte() ?: 0u.toUByte())
                is Rgb<*> -> {
                    val r = (p.r as? Number)?.toDouble()?.coerceIn(0.0, 255.0) ?: 0.0
                    val g = (p.g as? Number)?.toDouble()?.coerceIn(0.0, 255.0) ?: 0.0
                    val b = (p.b as? Number)?.toDouble()?.coerceIn(0.0, 255.0) ?: 0.0
                    (0.299 * r + 0.587 * g + 0.114 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                }
                is Rgba<*> -> {
                    val r = (p.r as? Number)?.toDouble()?.coerceIn(0.0, 255.0) ?: 0.0
                    val g = (p.g as? Number)?.toDouble()?.coerceIn(0.0, 255.0) ?: 0.0
                    val b = (p.b as? Number)?.toDouble()?.coerceIn(0.0, 255.0) ?: 0.0
                    (0.299 * r + 0.587 * g + 0.114 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                }
                else -> 0u.toUByte()
            }
            out.putPixel(x, y, Luma(luma))
        }
    }
    return out
}

/**
 * Convert the supplied image to a grayscale image with the specified pixel type. Alpha channel is preserved.
 */
public fun <I : GenericImageView<*>> grayscaleWithTypeAlpha(
    image: I,
): GrayAlphaImage {
    val (width, height) = image.dimensions()
    val out = ImageBuffer.createGrayAlpha(width, height)
    for (y in 0u until height) {
        for (x in 0u until width) {
            val p = image.getPixel(x, y)
            val (luma, alpha) = when (p) {
                is Luma<*> -> {
                    val l = (p.l as? UByte) ?: ((p.l as? Number)?.toLong()?.coerceIn(0, 255)?.toUByte() ?: 0u.toUByte())
                    Pair(l, 255u.toUByte())
                }
                is LumaA<*> -> {
                    val l = (p.l as? UByte) ?: ((p.l as? Number)?.toLong()?.coerceIn(0, 255)?.toUByte() ?: 0u.toUByte())
                    val a = (p.a as? UByte) ?: ((p.a as? Number)?.toLong()?.coerceIn(0, 255)?.toUByte() ?: 255u.toUByte())
                    Pair(l, a)
                }
                is Rgb<*> -> {
                    val r = (p.r as? Number)?.toDouble()?.coerceIn(0.0, 255.0) ?: 0.0
                    val g = (p.g as? Number)?.toDouble()?.coerceIn(0.0, 255.0) ?: 0.0
                    val b = (p.b as? Number)?.toDouble()?.coerceIn(0.0, 255.0) ?: 0.0
                    val l = (0.299 * r + 0.587 * g + 0.114 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                    Pair(l, 255u.toUByte())
                }
                is Rgba<*> -> {
                    val r = (p.r as? Number)?.toDouble()?.coerceIn(0.0, 255.0) ?: 0.0
                    val g = (p.g as? Number)?.toDouble()?.coerceIn(0.0, 255.0) ?: 0.0
                    val b = (p.b as? Number)?.toDouble()?.coerceIn(0.0, 255.0) ?: 0.0
                    val l = (0.299 * r + 0.587 * g + 0.114 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                    val a = (p.a as? UByte) ?: ((p.a as? Number)?.toLong()?.coerceIn(0, 255)?.toUByte() ?: 255u.toUByte())
                    Pair(l, a)
                }
                else -> Pair(0u.toUByte(), 255u.toUByte())
            }
            out.putPixel(x, y, LumaA(luma, alpha))
        }
    }
    return out
}

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
 * Invert each pixel within the supplied image in place.
 */
@Suppress("UNCHECKED_CAST")
public fun <P> invert(image: GenericImage<P>) {
    val (width, height) = image.dimensions()
    for (y in 0u until height) {
        for (x in 0u until width) {
            val p = image.getPixel(x, y)
            val inv: P = when (p) {
                is Luma<*> -> {
                    if (p.l is UByte) Luma((255 - (p.l as UByte).toInt()).toUByte()) as P
                    else p
                }
                is LumaA<*> -> {
                    if (p.l is UByte) LumaA((255 - (p.l as UByte).toInt()).toUByte(), p.a) as P
                    else p
                }
                is Rgb<*> -> {
                    if (p.r is UByte) {
                        Rgb(
                            (255 - (p.r as UByte).toInt()).toUByte(),
                            (255 - (p.g as UByte).toInt()).toUByte(),
                            (255 - (p.b as UByte).toInt()).toUByte(),
                        ) as P
                    } else p
                }
                is Rgba<*> -> {
                    if (p.r is UByte) {
                        Rgba(
                            (255 - (p.r as UByte).toInt()).toUByte(),
                            (255 - (p.g as UByte).toInt()).toUByte(),
                            (255 - (p.b as UByte).toInt()).toUByte(),
                            p.a,
                        ) as P
                    } else p
                }
                else -> p
            }
            image.putPixel(x, y, inv)
        }
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
 * Adjusts contrast of an image.
 */
@Suppress("UNCHECKED_CAST")
public fun <P> contrast(image: GenericImageView<P>, contrast: Float): ImageBuffer<P, ByteArray> {
    val (width, height) = image.dimensions()
    val out = image.bufferLike()
    val factor = (100.0f + contrast) / 100.0f
    val percent = factor * factor

    for (y in 0u until height) {
        for (x in 0u until width) {
            val p = image.getPixel(x, y)
            val res: P = when (p) {
                is Luma<*> -> {
                    if (p.l is UByte) {
                        val c = (p.l as UByte).toFloat()
                        val d = ((c / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        Luma(d.coerceIn(0.0f, 255.0f).toInt().toUByte()) as P
                    } else p
                }
                is LumaA<*> -> {
                    if (p.l is UByte) {
                        val c = (p.l as UByte).toFloat()
                        val d = ((c / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        LumaA(d.coerceIn(0.0f, 255.0f).toInt().toUByte(), p.a) as P
                    } else p
                }
                is Rgb<*> -> {
                    if (p.r is UByte) {
                        val r = (p.r as UByte).toFloat()
                        val g = (p.g as UByte).toFloat()
                        val b = (p.b as UByte).toFloat()
                        val dr = ((r / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        val dg = ((g / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        val db = ((b / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        Rgb(
                            dr.coerceIn(0.0f, 255.0f).toInt().toUByte(),
                            dg.coerceIn(0.0f, 255.0f).toInt().toUByte(),
                            db.coerceIn(0.0f, 255.0f).toInt().toUByte(),
                        ) as P
                    } else p
                }
                is Rgba<*> -> {
                    if (p.r is UByte) {
                        val r = (p.r as UByte).toFloat()
                        val g = (p.g as UByte).toFloat()
                        val b = (p.b as UByte).toFloat()
                        val dr = ((r / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        val dg = ((g / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        val db = ((b / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        Rgba(
                            dr.coerceIn(0.0f, 255.0f).toInt().toUByte(),
                            dg.coerceIn(0.0f, 255.0f).toInt().toUByte(),
                            db.coerceIn(0.0f, 255.0f).toInt().toUByte(),
                            p.a,
                        ) as P
                    } else p
                }
                else -> p
            }
            out.putPixel(x, y, res)
        }
    }
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
 * Adjusts contrast of an image in place.
 */
@Suppress("UNCHECKED_CAST")
public fun <P> contrastInPlace(image: GenericImage<P>, contrast: Float) {
    val (width, height) = image.dimensions()
    val factor = (100.0f + contrast) / 100.0f
    val percent = factor * factor

    for (y in 0u until height) {
        for (x in 0u until width) {
            val p = image.getPixel(x, y)
            val res: P = when (p) {
                is Luma<*> -> {
                    if (p.l is UByte) {
                        val c = (p.l as UByte).toFloat()
                        val d = ((c / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        Luma(d.coerceIn(0.0f, 255.0f).toInt().toUByte()) as P
                    } else p
                }
                is LumaA<*> -> {
                    if (p.l is UByte) {
                        val c = (p.l as UByte).toFloat()
                        val d = ((c / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        LumaA(d.coerceIn(0.0f, 255.0f).toInt().toUByte(), p.a) as P
                    } else p
                }
                is Rgb<*> -> {
                    if (p.r is UByte) {
                        val r = (p.r as UByte).toFloat()
                        val g = (p.g as UByte).toFloat()
                        val b = (p.b as UByte).toFloat()
                        val dr = ((r / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        val dg = ((g / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        val db = ((b / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        Rgb(
                            dr.coerceIn(0.0f, 255.0f).toInt().toUByte(),
                            dg.coerceIn(0.0f, 255.0f).toInt().toUByte(),
                            db.coerceIn(0.0f, 255.0f).toInt().toUByte(),
                        ) as P
                    } else p
                }
                is Rgba<*> -> {
                    if (p.r is UByte) {
                        val r = (p.r as UByte).toFloat()
                        val g = (p.g as UByte).toFloat()
                        val b = (p.b as UByte).toFloat()
                        val dr = ((r / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        val dg = ((g / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        val db = ((b / 255.0f - 0.5f) * percent + 0.5f) * 255.0f
                        Rgba(
                            dr.coerceIn(0.0f, 255.0f).toInt().toUByte(),
                            dg.coerceIn(0.0f, 255.0f).toInt().toUByte(),
                            db.coerceIn(0.0f, 255.0f).toInt().toUByte(),
                            p.a,
                        ) as P
                    } else p
                }
                else -> p
            }
            image.putPixel(x, y, res)
        }
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
 * Brightens the supplied image.
 */
@Suppress("UNCHECKED_CAST")
public fun <P> brighten(image: GenericImageView<P>, value: Int): ImageBuffer<P, ByteArray> {
    val (width, height) = image.dimensions()
    val out = image.bufferLike()
    for (y in 0u until height) {
        for (x in 0u until width) {
            val p = image.getPixel(x, y)
            val res: P = when (p) {
                is Luma<*> -> {
                    if (p.l is UByte) {
                        val c = (p.l as UByte).toInt()
                        Luma((c + value).coerceIn(0, 255).toUByte()) as P
                    } else p
                }
                is LumaA<*> -> {
                    if (p.l is UByte) {
                        val c = (p.l as UByte).toInt()
                        LumaA((c + value).coerceIn(0, 255).toUByte(), p.a) as P
                    } else p
                }
                is Rgb<*> -> {
                    if (p.r is UByte) {
                        val r = (p.r as UByte).toInt()
                        val g = (p.g as UByte).toInt()
                        val b = (p.b as UByte).toInt()
                        Rgb(
                            (r + value).coerceIn(0, 255).toUByte(),
                            (g + value).coerceIn(0, 255).toUByte(),
                            (b + value).coerceIn(0, 255).toUByte(),
                        ) as P
                    } else p
                }
                is Rgba<*> -> {
                    if (p.r is UByte) {
                        val r = (p.r as UByte).toInt()
                        val g = (p.g as UByte).toInt()
                        val b = (p.b as UByte).toInt()
                        Rgba(
                            (r + value).coerceIn(0, 255).toUByte(),
                            (g + value).coerceIn(0, 255).toUByte(),
                            (b + value).coerceIn(0, 255).toUByte(),
                            p.a,
                        ) as P
                    } else p
                }
                else -> p
            }
            out.putPixel(x, y, res)
        }
    }
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
 * Brightens the supplied image in place.
 */
@Suppress("UNCHECKED_CAST")
public fun <P> brightenInPlace(image: GenericImage<P>, value: Int) {
    val (width, height) = image.dimensions()
    for (y in 0u until height) {
        for (x in 0u until width) {
            val p = image.getPixel(x, y)
            val res: P = when (p) {
                is Luma<*> -> {
                    if (p.l is UByte) {
                        val c = (p.l as UByte).toInt()
                        Luma((c + value).coerceIn(0, 255).toUByte()) as P
                    } else p
                }
                is LumaA<*> -> {
                    if (p.l is UByte) {
                        val c = (p.l as UByte).toInt()
                        LumaA((c + value).coerceIn(0, 255).toUByte(), p.a) as P
                    } else p
                }
                is Rgb<*> -> {
                    if (p.r is UByte) {
                        val r = (p.r as UByte).toInt()
                        val g = (p.g as UByte).toInt()
                        val b = (p.b as UByte).toInt()
                        Rgb(
                            (r + value).coerceIn(0, 255).toUByte(),
                            (g + value).coerceIn(0, 255).toUByte(),
                            (b + value).coerceIn(0, 255).toUByte(),
                        ) as P
                    } else p
                }
                is Rgba<*> -> {
                    if (p.r is UByte) {
                        val r = (p.r as UByte).toInt()
                        val g = (p.g as UByte).toInt()
                        val b = (p.b as UByte).toInt()
                        Rgba(
                            (r + value).coerceIn(0, 255).toUByte(),
                            (g + value).coerceIn(0, 255).toUByte(),
                            (b + value).coerceIn(0, 255).toUByte(),
                            p.a,
                        ) as P
                    } else p
                }
                else -> p
            }
            image.putPixel(x, y, res)
        }
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
 * Hue rotates RGB/RGBA image in degrees.
 */
@Suppress("UNCHECKED_CAST")
public fun <P> huerotate(image: GenericImageView<P>, degrees: Int): ImageBuffer<P, ByteArray> {
    val (width, height) = image.dimensions()
    val out = image.bufferLike()
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

    for (y in 0u until height) {
        for (x in 0u until width) {
            val p = image.getPixel(x, y)
            val res: P = when (p) {
                is Rgb<*> -> {
                    if (p.r is UByte) {
                        val r = (p.r as UByte).toDouble()
                        val g = (p.g as UByte).toDouble()
                        val b = (p.b as UByte).toDouble()
                        val nr = (m0 * r + m1 * g + m2 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                        val ng = (m3 * r + m4 * g + m5 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                        val nb = (m6 * r + m7 * g + m8 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                        Rgb(nr, ng, nb) as P
                    } else p
                }
                is Rgba<*> -> {
                    if (p.r is UByte) {
                        val r = (p.r as UByte).toDouble()
                        val g = (p.g as UByte).toDouble()
                        val b = (p.b as UByte).toDouble()
                        val nr = (m0 * r + m1 * g + m2 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                        val ng = (m3 * r + m4 * g + m5 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                        val nb = (m6 * r + m7 * g + m8 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                        Rgba(nr, ng, nb, p.a) as P
                    } else p
                }
                else -> p
            }
            out.putPixel(x, y, res)
        }
    }
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
 * Hue rotates RGB/RGBA image in degrees in place.
 */
@Suppress("UNCHECKED_CAST")
public fun <P> huerotateInPlace(image: GenericImage<P>, degrees: Int) {
    val (width, height) = image.dimensions()
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

    for (y in 0u until height) {
        for (x in 0u until width) {
            val p = image.getPixel(x, y)
            val res: P = when (p) {
                is Rgb<*> -> {
                    if (p.r is UByte) {
                        val r = (p.r as UByte).toDouble()
                        val g = (p.g as UByte).toDouble()
                        val b = (p.b as UByte).toDouble()
                        val nr = (m0 * r + m1 * g + m2 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                        val ng = (m3 * r + m4 * g + m5 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                        val nb = (m6 * r + m7 * g + m8 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                        Rgb(nr, ng, nb) as P
                    } else p
                }
                is Rgba<*> -> {
                    if (p.r is UByte) {
                        val r = (p.r as UByte).toDouble()
                        val g = (p.g as UByte).toDouble()
                        val b = (p.b as UByte).toDouble()
                        val nr = (m0 * r + m1 * g + m2 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                        val ng = (m3 * r + m4 * g + m5 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                        val nb = (m6 * r + m7 * g + m8 * b).coerceIn(0.0, 255.0).toInt().toUByte()
                        Rgba(nr, ng, nb, p.a) as P
                    } else p
                }
                else -> p
            }
            image.putPixel(x, y, res)
        }
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
 * Reduces the colors of the grayscale ImageBuffer using Floyd-Steinberg dithering.
 */
public fun dither(
    image: ImageBuffer<Luma<UByte>, ByteArray>,
    colorMap: ColorMap<UByte>,
) {
    dither(image.asRaw(), image.width().toInt(), image.height().toInt(), colorMap)
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

/**
 * Reduces the colors using the supplied color map and returns an ImageBuffer of the indices.
 */
public fun indexColors(
    image: ImageBuffer<Luma<UByte>, ByteArray>,
    colorMap: ColorMap<UByte>,
): ImageBuffer<Luma<UByte>, ByteArray> {
    val raw = indexColors(image.asRaw(), image.width().toInt(), image.height().toInt(), colorMap)
    return ImageBuffer.createGray(image.width(), image.height(), raw)!!
}

/**
 * Collects pixel differences between two generic images.
 */
public fun <P> pixelDiffs(
    left: GenericImageView<P>,
    right: GenericImageView<P>,
): List<Pair<Pair<UInt, UInt>, Pair<P, P>>> {
    val (w1, h1) = left.dimensions()
    val (w2, h2) = right.dimensions()
    val minW = minOf(w1, w2)
    val minH = minOf(h1, h2)
    val diffs = mutableListOf<Pair<Pair<UInt, UInt>, Pair<P, P>>>()
    for (y in 0u until minH) {
        for (x in 0u until minW) {
            val p1 = left.getPixel(x, y)
            val p2 = right.getPixel(x, y)
            if (p1 != p2) {
                diffs.add(Pair(Pair(x, y), Pair(p1, p2)))
            }
        }
    }
    return diffs
}
