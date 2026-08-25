// port-lint: source images/dynimage.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.LimitError
import io.github.kotlinmania.image.LimitErrorKind
import io.github.kotlinmania.image.Luma
import io.github.kotlinmania.image.LumaA
import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.imageops.FilterType
import io.github.kotlinmania.image.imageops.GaussianBlurParameters
import io.github.kotlinmania.image.imageops.brighten
import io.github.kotlinmania.image.imageops.contrast
import io.github.kotlinmania.image.imageops.fastBlur
import io.github.kotlinmania.image.imageops.filter1dHorizontal
import io.github.kotlinmania.image.imageops.filter1dVertical
import io.github.kotlinmania.image.imageops.flipHorizontal
import io.github.kotlinmania.image.imageops.flipHorizontalInPlace
import io.github.kotlinmania.image.imageops.flipVertical
import io.github.kotlinmania.image.imageops.flipVerticalInPlace
import io.github.kotlinmania.image.imageops.huerotate
import io.github.kotlinmania.image.imageops.invert
import io.github.kotlinmania.image.imageops.resize
import io.github.kotlinmania.image.imageops.rotate180
import io.github.kotlinmania.image.imageops.rotate180InPlace
import io.github.kotlinmania.image.imageops.rotate270
import io.github.kotlinmania.image.imageops.rotate90
import io.github.kotlinmania.image.imageops.thumbnail
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.encoderForFormat
import io.github.kotlinmania.image.io.guessFormat
import io.github.kotlinmania.image.io.load
import io.github.kotlinmania.image.math.resizeDimensions
import io.github.kotlinmania.image.metadata.Cicp
import io.github.kotlinmania.image.metadata.CicpColorPrimaries
import io.github.kotlinmania.image.metadata.CicpTransferCharacteristics
import io.github.kotlinmania.image.metadata.Orientation
import io.github.kotlinmania.image.toLuma
import io.github.kotlinmania.image.toLumaAlpha
import io.github.kotlinmania.image.toRgb
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A Dynamic Image.
 *
 * Represents an image whose pixel format is determined at runtime.
 */
public sealed class DynamicImage : GenericImage<Rgba<UByte>> {
    @ConsistentCopyVisibility
    public data class ImageLuma8 internal constructor(
        public val image: GrayImage,
    ) : DynamicImage()

    @ConsistentCopyVisibility
    public data class ImageLumaA8 internal constructor(
        public val image: GrayAlphaImage,
    ) : DynamicImage()

    @ConsistentCopyVisibility
    public data class ImageRgb8 internal constructor(
        public val image: RgbImage,
    ) : DynamicImage()

    @ConsistentCopyVisibility
    public data class ImageRgba8 internal constructor(
        public val image: RgbaImage,
    ) : DynamicImage()

    @ConsistentCopyVisibility
    public data class ImageLuma16 internal constructor(
        public val image: Gray16Image,
    ) : DynamicImage()

    @ConsistentCopyVisibility
    public data class ImageLumaA16 internal constructor(
        public val image: GrayAlpha16Image,
    ) : DynamicImage()

    @ConsistentCopyVisibility
    public data class ImageRgb16 internal constructor(
        public val image: Rgb16Image,
    ) : DynamicImage()

    @ConsistentCopyVisibility
    public data class ImageRgba16 internal constructor(
        public val image: Rgba16Image,
    ) : DynamicImage()

    @ConsistentCopyVisibility
    public data class ImageRgb32F internal constructor(
        public val image: Rgb32FImage,
    ) : DynamicImage()

    @ConsistentCopyVisibility
    public data class ImageRgba32F internal constructor(
        public val image: Rgba32FImage,
    ) : DynamicImage()

    override fun width(): UInt =
        when (this) {
            is ImageLuma8 -> image.width()
            is ImageLumaA8 -> image.width()
            is ImageRgb8 -> image.width()
            is ImageRgba8 -> image.width()
            is ImageLuma16 -> image.width()
            is ImageLumaA16 -> image.width()
            is ImageRgb16 -> image.width()
            is ImageRgba16 -> image.width()
            is ImageRgb32F -> image.width()
            is ImageRgba32F -> image.width()
        }

    override fun height(): UInt =
        when (this) {
            is ImageLuma8 -> image.height()
            is ImageLumaA8 -> image.height()
            is ImageRgb8 -> image.height()
            is ImageRgba8 -> image.height()
            is ImageLuma16 -> image.height()
            is ImageLumaA16 -> image.height()
            is ImageRgb16 -> image.height()
            is ImageRgba16 -> image.height()
            is ImageRgb32F -> image.height()
            is ImageRgba32F -> image.height()
        }

    override fun dimensions(): Pair<UInt, UInt> = Pair(width(), height())

    public fun color(): ColorType =
        when (this) {
            is ImageLuma8 -> ColorType.L8
            is ImageLumaA8 -> ColorType.La8
            is ImageRgb8 -> ColorType.Rgb8
            is ImageRgba8 -> ColorType.Rgba8
            is ImageLuma16 -> ColorType.L16
            is ImageLumaA16 -> ColorType.La16
            is ImageRgb16 -> ColorType.Rgb16
            is ImageRgba16 -> ColorType.Rgba16
            is ImageRgb32F -> ColorType.Rgb32F
            is ImageRgba32F -> ColorType.Rgba32F
        }

    public fun hasAlpha(): Boolean = color().hasAlpha()

    public fun asBytes(): ByteArray =
        when (this) {
            is ImageLuma8 -> image.asRaw()
            is ImageLumaA8 -> image.asRaw()
            is ImageRgb8 -> image.asRaw()
            is ImageRgba8 -> image.asRaw()
            is ImageLuma16 -> image.asRaw()
            is ImageLumaA16 -> image.asRaw()
            is ImageRgb16 -> image.asRaw()
            is ImageRgba16 -> image.asRaw()
            is ImageRgb32F -> image.asRaw()
            is ImageRgba32F -> image.asRaw()
        }

    public fun intoBytes(): ByteArray = asBytes()

    public fun asRgb8(): RgbImage? = (this as? ImageRgb8)?.image

    public fun asMutRgb8(): RgbImage? = asRgb8()

    public fun asRgba8(): RgbaImage? = (this as? ImageRgba8)?.image

    public fun asMutRgba8(): RgbaImage? = asRgba8()

    public fun asLuma8(): GrayImage? = (this as? ImageLuma8)?.image

    public fun asMutLuma8(): GrayImage? = asLuma8()

    public fun asLumaAlpha8(): GrayAlphaImage? = (this as? ImageLumaA8)?.image

    public fun asMutLumaAlpha8(): GrayAlphaImage? = asLumaAlpha8()

    public fun asRgb16(): Rgb16Image? = (this as? ImageRgb16)?.image

    public fun asMutRgb16(): Rgb16Image? = asRgb16()

    public fun asRgba16(): Rgba16Image? = (this as? ImageRgba16)?.image

    public fun asMutRgba16(): Rgba16Image? = asRgba16()

    public fun asLuma16(): Gray16Image? = (this as? ImageLuma16)?.image

    public fun asMutLuma16(): Gray16Image? = asLuma16()

    public fun asLumaAlpha16(): GrayAlpha16Image? = (this as? ImageLumaA16)?.image

    public fun asMutLumaAlpha16(): GrayAlpha16Image? = asLumaAlpha16()

    public fun asRgb32F(): Rgb32FImage? = (this as? ImageRgb32F)?.image

    public fun asMutRgb32F(): Rgb32FImage? = asRgb32F()

    public fun asRgba32F(): Rgba32FImage? = (this as? ImageRgba32F)?.image

    public fun asMutRgba32F(): Rgba32FImage? = asRgba32F()

    public fun asFlatSamplesU8(): FlatSamples<ByteArray>? =
        when (this) {
            is ImageLuma8 -> image.asFlatSamples()
            is ImageLumaA8 -> image.asFlatSamples()
            is ImageRgb8 -> image.asFlatSamples()
            is ImageRgba8 -> image.asFlatSamples()
            else -> null
        }

    public fun asFlatSamplesU16(): FlatSamples<ByteArray>? =
        when (this) {
            is ImageLuma16 -> image.asFlatSamples()
            is ImageLumaA16 -> image.asFlatSamples()
            is ImageRgb16 -> image.asFlatSamples()
            is ImageRgba16 -> image.asFlatSamples()
            else -> null
        }

    public fun asFlatSamplesF32(): FlatSamples<ByteArray>? =
        when (this) {
            is ImageRgb32F -> image.asFlatSamples()
            is ImageRgba32F -> image.asFlatSamples()
            else -> null
        }

    public fun toRgb8(): RgbImage =
        when (this) {
            is ImageRgb8 -> image.copy()
            else -> ImageBuffer.createRgb(width(), height()) { x, y -> getPixel(x, y).toRgb() }
        }

    public fun toRgba8(): RgbaImage =
        when (this) {
            is ImageRgba8 -> image.copy()
            else -> ImageBuffer.createRgba(width(), height()) { x, y -> getPixel(x, y) }
        }

    public fun toLuma8(): GrayImage =
        when (this) {
            is ImageLuma8 -> image.copy()
            else -> ImageBuffer.createGray(width(), height()) { x, y -> getPixel(x, y).toLuma() }
        }

    public fun toLumaAlpha8(): GrayAlphaImage =
        when (this) {
            is ImageLumaA8 -> image.copy()
            else -> ImageBuffer.createGrayAlpha(width(), height()) { x, y -> getPixel(x, y).toLumaAlpha() }
        }

    public fun toRgb16(): Rgb16Image =
        when (this) {
            is ImageRgb16 -> image.copy()
            else -> {
                val w = width()
                val h = height()
                val res = createRgb16(w, h)
                for (y in 0u until h) {
                    for (x in 0u until w) {
                        val p = getPixel(x, y)
                        val r = (p.r.toUInt() * 257u).toUShort()
                        val g = (p.g.toUInt() * 257u).toUShort()
                        val b = (p.b.toUInt() * 257u).toUShort()
                        res.putPixel(x, y, Rgb(r, g, b))
                    }
                }
                res
            }
        }

    public fun toRgba16(): Rgba16Image =
        when (this) {
            is ImageRgba16 -> image.copy()
            else -> {
                val w = width()
                val h = height()
                val res = createRgba16(w, h)
                for (y in 0u until h) {
                    for (x in 0u until w) {
                        val p = getPixel(x, y)
                        val r = (p.r.toUInt() * 257u).toUShort()
                        val g = (p.g.toUInt() * 257u).toUShort()
                        val b = (p.b.toUInt() * 257u).toUShort()
                        val a = (p.a.toUInt() * 257u).toUShort()
                        res.putPixel(x, y, Rgba(r, g, b, a))
                    }
                }
                res
            }
        }

    public fun toLuma16(): Gray16Image =
        when (this) {
            is ImageLuma16 -> image.copy()
            else -> {
                val w = width()
                val h = height()
                val res = createGray16(w, h)
                for (y in 0u until h) {
                    for (x in 0u until w) {
                        val luma = getPixel(x, y).toLuma()
                        val l = (luma.l.toUInt() * 257u).toUShort()
                        res.putPixel(x, y, Luma(l))
                    }
                }
                res
            }
        }

    public fun toLumaAlpha16(): GrayAlpha16Image =
        when (this) {
            is ImageLumaA16 -> image.copy()
            else -> {
                val w = width()
                val h = height()
                val res = createGrayAlpha16(w, h)
                for (y in 0u until h) {
                    for (x in 0u until w) {
                        val la = getPixel(x, y).toLumaAlpha()
                        val l = (la.l.toUInt() * 257u).toUShort()
                        val a = (la.a.toUInt() * 257u).toUShort()
                        res.putPixel(x, y, LumaA(l, a))
                    }
                }
                res
            }
        }

    public fun toRgb32F(): Rgb32FImage =
        when (this) {
            is ImageRgb32F -> image.copy()
            else -> {
                val w = width()
                val h = height()
                val res = createRgb32F(w, h)
                for (y in 0u until h) {
                    for (x in 0u until w) {
                        val p = getPixel(x, y)
                        val r = p.r.toFloat() / 255f
                        val g = p.g.toFloat() / 255f
                        val b = p.b.toFloat() / 255f
                        res.putPixel(x, y, Rgb(r, g, b))
                    }
                }
                res
            }
        }

    public fun toRgba32F(): Rgba32FImage =
        when (this) {
            is ImageRgba32F -> image.copy()
            else -> {
                val w = width()
                val h = height()
                val res = createRgba32F(w, h)
                for (y in 0u until h) {
                    for (x in 0u until w) {
                        val p = getPixel(x, y)
                        val r = p.r.toFloat() / 255f
                        val g = p.g.toFloat() / 255f
                        val b = p.b.toFloat() / 255f
                        val a = p.a.toFloat() / 255f
                        res.putPixel(x, y, Rgba(r, g, b, a))
                    }
                }
                res
            }
        }

    public fun toLuma32F(): Gray32FImage =
        when (this) {
            else -> {
                val w = width()
                val h = height()
                val res = createGray32F(w, h)
                for (y in 0u until h) {
                    for (x in 0u until w) {
                        val p = getPixel(x, y)
                        val luma = (0.2126f * p.r.toFloat() + 0.7152f * p.g.toFloat() + 0.0722f * p.b.toFloat()) / 255f
                        res.putPixel(x, y, Luma(luma))
                    }
                }
                res
            }
        }

    public fun toLumaAlpha32F(): GrayAlpha32FImage =
        when (this) {
            else -> {
                val w = width()
                val h = height()
                val res = createGrayAlpha32F(w, h)
                for (y in 0u until h) {
                    for (x in 0u until w) {
                        val p = getPixel(x, y)
                        val luma = (0.2126f * p.r.toFloat() + 0.7152f * p.g.toFloat() + 0.0722f * p.b.toFloat()) / 255f
                        val a = p.a.toFloat() / 255f
                        res.putPixel(x, y, LumaA(luma, a))
                    }
                }
                res
            }
        }

    public fun intoRgb8(): RgbImage = toRgb8()

    public fun intoRgba8(): RgbaImage = toRgba8()

    public fun intoLuma8(): GrayImage = toLuma8()

    public fun intoLumaAlpha8(): GrayAlphaImage = toLumaAlpha8()

    public fun intoRgb16(): Rgb16Image = toRgb16()

    public fun intoRgba16(): Rgba16Image = toRgba16()

    public fun intoLuma16(): Gray16Image = toLuma16()

    public fun intoLumaAlpha16(): GrayAlpha16Image = toLumaAlpha16()

    public fun intoRgb32F(): Rgb32FImage = toRgb32F()

    public fun intoRgba32F(): Rgba32FImage = toRgba32F()

    public fun intoLuma32F(): Gray32FImage = toLuma32F()

    public fun intoLumaAlpha32F(): GrayAlpha32FImage = toLumaAlpha32F()

    public fun setRgbPrimaries(color: CicpColorPrimaries) {
        when (this) {
            is ImageLuma8 -> image.setRgbPrimaries(color)
            is ImageLumaA8 -> image.setRgbPrimaries(color)
            is ImageRgb8 -> image.setRgbPrimaries(color)
            is ImageRgba8 -> image.setRgbPrimaries(color)
            is ImageLuma16 -> image.setRgbPrimaries(color)
            is ImageLumaA16 -> image.setRgbPrimaries(color)
            is ImageRgb16 -> image.setRgbPrimaries(color)
            is ImageRgba16 -> image.setRgbPrimaries(color)
            is ImageRgb32F -> image.setRgbPrimaries(color)
            is ImageRgba32F -> image.setRgbPrimaries(color)
        }
    }

    public fun setTransferFunction(tf: CicpTransferCharacteristics) {
        when (this) {
            is ImageLuma8 -> image.setTransferFunction(tf)
            is ImageLumaA8 -> image.setTransferFunction(tf)
            is ImageRgb8 -> image.setTransferFunction(tf)
            is ImageRgba8 -> image.setTransferFunction(tf)
            is ImageLuma16 -> image.setTransferFunction(tf)
            is ImageLumaA16 -> image.setTransferFunction(tf)
            is ImageRgb16 -> image.setTransferFunction(tf)
            is ImageRgba16 -> image.setTransferFunction(tf)
            is ImageRgb32F -> image.setTransferFunction(tf)
            is ImageRgba32F -> image.setTransferFunction(tf)
        }
    }

    public fun colorSpace(): Cicp =
        when (this) {
            is ImageLuma8 -> image.colorSpace()
            is ImageLumaA8 -> image.colorSpace()
            is ImageRgb8 -> image.colorSpace()
            is ImageRgba8 -> image.colorSpace()
            is ImageLuma16 -> image.colorSpace()
            is ImageLumaA16 -> image.colorSpace()
            is ImageRgb16 -> image.colorSpace()
            is ImageRgba16 -> image.colorSpace()
            is ImageRgb32F -> image.colorSpace()
            is ImageRgba32F -> image.colorSpace()
        }

    public fun setColorSpace(cicp: Cicp) {
        when (this) {
            is ImageLuma8 -> image.setColorSpace(cicp)
            is ImageLumaA8 -> image.setColorSpace(cicp)
            is ImageRgb8 -> image.setColorSpace(cicp)
            is ImageRgba8 -> image.setColorSpace(cicp)
            is ImageLuma16 -> image.setColorSpace(cicp)
            is ImageLumaA16 -> image.setColorSpace(cicp)
            is ImageRgb16 -> image.setColorSpace(cicp)
            is ImageRgba16 -> image.setColorSpace(cicp)
            is ImageRgb32F -> image.setColorSpace(cicp)
            is ImageRgba32F -> image.setColorSpace(cicp)
        }
    }

    override fun getPixel(x: UInt, y: UInt): Rgba<UByte> =
        when (this) {
            is ImageLuma8 -> {
                val l = image.getPixel(x, y).l
                Rgba(l, l, l, 255u)
            }
            is ImageLumaA8 -> {
                val p = image.getPixel(x, y)
                Rgba(p.l, p.l, p.l, p.a)
            }
            is ImageRgb8 -> {
                val p = image.getPixel(x, y)
                Rgba(p.r, p.g, p.b, 255u)
            }
            is ImageRgba8 -> image.getPixel(x, y)
            is ImageLuma16 -> {
                val l = ((image.getPixel(x, y).l.toInt() and 0xFFFF) / 257).toUByte()
                Rgba(l, l, l, 255u)
            }
            is ImageLumaA16 -> {
                val p = image.getPixel(x, y)
                val l = ((p.l.toInt() and 0xFFFF) / 257).toUByte()
                val a = ((p.a.toInt() and 0xFFFF) / 257).toUByte()
                Rgba(l, l, l, a)
            }
            is ImageRgb16 -> {
                val p = image.getPixel(x, y)
                Rgba(
                    ((p.r.toInt() and 0xFFFF) / 257).toUByte(),
                    ((p.g.toInt() and 0xFFFF) / 257).toUByte(),
                    ((p.b.toInt() and 0xFFFF) / 257).toUByte(),
                    255u,
                )
            }
            is ImageRgba16 -> {
                val p = image.getPixel(x, y)
                Rgba(
                    ((p.r.toInt() and 0xFFFF) / 257).toUByte(),
                    ((p.g.toInt() and 0xFFFF) / 257).toUByte(),
                    ((p.b.toInt() and 0xFFFF) / 257).toUByte(),
                    ((p.a.toInt() and 0xFFFF) / 257).toUByte(),
                )
            }
            is ImageRgb32F -> {
                val p = image.getPixel(x, y)
                Rgba(
                    (p.r * 255f).roundToInt().coerceIn(0, 255).toUByte(),
                    (p.g * 255f).roundToInt().coerceIn(0, 255).toUByte(),
                    (p.b * 255f).roundToInt().coerceIn(0, 255).toUByte(),
                    255u,
                )
            }
            is ImageRgba32F -> {
                val p = image.getPixel(x, y)
                Rgba(
                    (p.r * 255f).roundToInt().coerceIn(0, 255).toUByte(),
                    (p.g * 255f).roundToInt().coerceIn(0, 255).toUByte(),
                    (p.b * 255f).roundToInt().coerceIn(0, 255).toUByte(),
                    (p.a * 255f).roundToInt().coerceIn(0, 255).toUByte(),
                )
            }
        }

    override fun putPixel(x: UInt, y: UInt, pixel: Rgba<UByte>) {
        when (this) {
            is ImageLuma8 -> image.putPixel(x, y, pixel.toLuma())
            is ImageLumaA8 -> image.putPixel(x, y, pixel.toLumaAlpha())
            is ImageRgb8 -> image.putPixel(x, y, pixel.toRgb())
            is ImageRgba8 -> image.putPixel(x, y, pixel)
            is ImageLuma16 -> {
                val l = (pixel.toLuma().l.toUInt() * 257u).toUShort()
                image.putPixel(x, y, Luma(l))
            }
            is ImageLumaA16 -> {
                val la = pixel.toLumaAlpha()
                val l = (la.l.toUInt() * 257u).toUShort()
                val a = (la.a.toUInt() * 257u).toUShort()
                image.putPixel(x, y, LumaA(l, a))
            }
            is ImageRgb16 -> {
                val r = (pixel.r.toUInt() * 257u).toUShort()
                val g = (pixel.g.toUInt() * 257u).toUShort()
                val b = (pixel.b.toUInt() * 257u).toUShort()
                image.putPixel(x, y, Rgb(r, g, b))
            }
            is ImageRgba16 -> {
                val r = (pixel.r.toUInt() * 257u).toUShort()
                val g = (pixel.g.toUInt() * 257u).toUShort()
                val b = (pixel.b.toUInt() * 257u).toUShort()
                val a = (pixel.a.toUInt() * 257u).toUShort()
                image.putPixel(x, y, Rgba(r, g, b, a))
            }
            is ImageRgb32F -> {
                val r = pixel.r.toFloat() / 255f
                val g = pixel.g.toFloat() / 255f
                val b = pixel.b.toFloat() / 255f
                image.putPixel(x, y, Rgb(r, g, b))
            }
            is ImageRgba32F -> {
                val r = pixel.r.toFloat() / 255f
                val g = pixel.g.toFloat() / 255f
                val b = pixel.b.toFloat() / 255f
                val a = pixel.a.toFloat() / 255f
                image.putPixel(x, y, Rgba(r, g, b, a))
            }
        }
    }

    override fun blendPixel(x: UInt, y: UInt, pixel: Rgba<UByte>) {
        when (this) {
            is ImageLuma8 -> image.blendPixel(x, y, pixel.toLuma())
            is ImageLumaA8 -> image.blendPixel(x, y, pixel.toLumaAlpha())
            is ImageRgb8 -> image.blendPixel(x, y, pixel.toRgb())
            is ImageRgba8 -> image.blendPixel(x, y, pixel)
            else -> putPixel(x, y, pixel)
        }
    }

    public fun grayscale(): DynamicImage =
        when (this) {
            is ImageLuma8 -> ImageLuma8(image.copy())
            is ImageLumaA8 -> ImageLumaA8(toLumaAlpha8())
            is ImageRgb8 -> ImageLuma8(toLuma8())
            is ImageRgba8 -> ImageLumaA8(toLumaAlpha8())
            is ImageLuma16 -> ImageLuma16(image.copy())
            is ImageLumaA16 -> ImageLumaA16(image.copy())
            is ImageRgb16 -> ImageLuma16(toLuma16())
            is ImageRgba16 -> ImageLumaA16(toLumaAlpha16())
            is ImageRgb32F -> {
                val w = width()
                val h = height()
                val res = createRgb32F(w, h)
                for (y in 0u until h) {
                    for (x in 0u until w) {
                        val p = image.getPixel(x, y)
                        val luma = 0.2126f * p.r + 0.7152f * p.g + 0.0722f * p.b
                        res.putPixel(x, y, Rgb(luma, luma, luma))
                    }
                }
                ImageRgb32F(res)
            }
            is ImageRgba32F -> {
                val w = width()
                val h = height()
                val res = createRgba32F(w, h)
                for (y in 0u until h) {
                    for (x in 0u until w) {
                        val p = image.getPixel(x, y)
                        val luma = 0.2126f * p.r + 0.7152f * p.g + 0.0722f * p.b
                        res.putPixel(x, y, Rgba(luma, luma, luma, p.a))
                    }
                }
                ImageRgba32F(res)
            }
        }

    public fun invert() {
        when (this) {
            is ImageLuma8 -> invert(image.asRaw(), 1)
            is ImageLumaA8 -> invert(image.asRaw(), 2)
            is ImageRgb8 -> invert(image.asRaw(), 3)
            is ImageRgba8 -> invert(image.asRaw(), 4)
            is ImageLuma16 -> {
                for (y in 0u until height()) {
                    for (x in 0u until width()) {
                        val p = image.getPixel(x, y)
                        image.putPixel(x, y, Luma((65535u - p.l).toUShort()))
                    }
                }
            }
            is ImageLumaA16 -> {
                for (y in 0u until height()) {
                    for (x in 0u until width()) {
                        val p = image.getPixel(x, y)
                        image.putPixel(x, y, LumaA((65535u - p.l).toUShort(), p.a))
                    }
                }
            }
            is ImageRgb16 -> {
                for (y in 0u until height()) {
                    for (x in 0u until width()) {
                        val p = image.getPixel(x, y)
                        image.putPixel(x, y, Rgb((65535u - p.r).toUShort(), (65535u - p.g).toUShort(), (65535u - p.b).toUShort()))
                    }
                }
            }
            is ImageRgba16 -> {
                for (y in 0u until height()) {
                    for (x in 0u until width()) {
                        val p = image.getPixel(x, y)
                        image.putPixel(x, y, Rgba((65535u - p.r).toUShort(), (65535u - p.g).toUShort(), (65535u - p.b).toUShort(), p.a))
                    }
                }
            }
            is ImageRgb32F -> {
                for (y in 0u until height()) {
                    for (x in 0u until width()) {
                        val p = image.getPixel(x, y)
                        image.putPixel(x, y, Rgb(1.0f - p.r, 1.0f - p.g, 1.0f - p.b))
                    }
                }
            }
            is ImageRgba32F -> {
                for (y in 0u until height()) {
                    for (x in 0u until width()) {
                        val p = image.getPixel(x, y)
                        image.putPixel(x, y, Rgba(1.0f - p.r, 1.0f - p.g, 1.0f - p.b, p.a))
                    }
                }
            }
        }
    }

    public fun resize(nwidth: UInt, nheight: UInt, filter: FilterType): DynamicImage {
        if (nwidth == width() && nheight == height()) return cloneImage()
        val (w2, h2) = resizeDimensions(width(), height(), nwidth, nheight, false)
        return resizeExact(w2, h2, filter)
    }

    public fun resizeExact(nwidth: UInt, nheight: UInt, filter: FilterType): DynamicImage {
        val channels = color().channelCount().toInt()
        val bytes = asBytes()
        val res = resize(bytes, width().toInt(), height().toInt(), nwidth.toInt(), nheight.toInt(), channels, filter)
        return fromRawBytes(nwidth, nheight, res, color())
    }

    public fun thumbnail(nwidth: UInt, nheight: UInt): DynamicImage {
        val (w2, h2) = resizeDimensions(width(), height(), nwidth, nheight, false)
        return thumbnailExact(w2, h2)
    }

    public fun thumbnailExact(nwidth: UInt, nheight: UInt): DynamicImage {
        val channels = color().channelCount().toInt()
        val bytes = asBytes()
        val res = thumbnail(bytes, width().toInt(), height().toInt(), nwidth.toInt(), nheight.toInt(), channels)
        return fromRawBytes(nwidth, nheight, res, color())
    }

    public fun resizeToFill(nwidth: UInt, nheight: UInt, filter: FilterType): DynamicImage {
        val (w2, h2) = resizeDimensions(width(), height(), nwidth, nheight, true)
        val intermediate = resizeExact(w2, h2, filter)
        val iwidth = intermediate.width()
        val iheight = intermediate.height()
        val ratio = iwidth.toULong() * nheight.toULong()
        val nratio = nwidth.toULong() * iheight.toULong()

        return if (nratio > ratio) {
            intermediate.crop(0u, (iheight - nheight) / 2u, nwidth, nheight)
        } else {
            intermediate.crop((iwidth - nwidth) / 2u, 0u, nwidth, nheight)
        }
    }

    public fun blur(sigma: Float): DynamicImage {
        val effectiveSigma = if (sigma == 0.0f) 0.8f else sigma
        val radius = (effectiveSigma * 3.0f).roundToInt().coerceAtLeast(1)
        val kernelSize = 2 * radius + 1
        val kernel = FloatArray(kernelSize)
        var sum = 0f
        val twoSigmaSq = 2.0f * effectiveSigma * effectiveSigma
        val norm = (1.0f / (sqrt(2.0f * PI.toFloat()) * effectiveSigma))
        for (i in 0 until kernelSize) {
            val x = (i - radius).toFloat()
            kernel[i] = norm * exp(-x * x / twoSigmaSq)
            sum += kernel[i]
        }
        for (i in 0 until kernelSize) {
            kernel[i] /= sum
        }

        val channels = color().channelCount().toInt()
        val w = width().toInt()
        val h = height().toInt()
        val src = asBytes()
        val temp = ByteArray(src.size)
        val dst = ByteArray(src.size)

        filter1dHorizontal(src, temp, w, h, channels, kernel)
        filter1dVertical(temp, dst, w, h, channels, kernel)

        return fromRawBytes(width(), height(), dst, color())
    }

    public fun blurAdvanced(parameters: GaussianBlurParameters): DynamicImage {
        val channels = color().channelCount().toInt()
        val bytes = asBytes()
        val res = io.github.kotlinmania.image.imageops.blurAdvanced(bytes, width().toInt(), height().toInt(), channels, parameters)
        return fromRawBytes(width(), height(), res, color())
    }

    public fun fastBlur(sigma: Float): DynamicImage {
        val channels = color().channelCount().toInt()
        val bytes = asBytes()
        val res = fastBlur(bytes, width().toInt(), height().toInt(), channels, sigma)
        return fromRawBytes(width(), height(), res, color())
    }

    public fun unsharpen(sigma: Float, threshold: Int): DynamicImage {
        val blurred = blur(sigma)
        val srcBytes = asBytes()
        val blurBytes = blurred.asBytes()
        val dst = ByteArray(srcBytes.size)
        val channels = color().channelCount().toInt()
        val hasA = hasAlpha()

        for (i in srcBytes.indices) {
            if (hasA && (i % channels == channels - 1)) {
                dst[i] = srcBytes[i]
                continue
            }
            val orig = srcBytes[i].toInt() and 0xFF
            val blurVal = blurBytes[i].toInt() and 0xFF
            val diff = orig - blurVal
            val res =
                if (kotlin.math.abs(diff) > threshold) {
                    (orig + diff).coerceIn(0, 255)
                } else {
                    orig
                }
            dst[i] = res.toByte()
        }

        return fromRawBytes(width(), height(), dst, color())
    }

    public fun filter3x3(kernel: FloatArray): DynamicImage {
        require(kernel.size == 9) { "filter must be 3 x 3" }
        val channels = color().channelCount().toInt()
        val w = width().toInt()
        val h = height().toInt()
        val src = asBytes()
        val dst = ByteArray(src.size)
        val hasA = hasAlpha()

        for (y in 0 until h) {
            for (x in 0 until w) {
                for (c in 0 until channels) {
                    if (hasA && c == channels - 1) {
                        dst[(y * w + x) * channels + c] = src[(y * w + x) * channels + c]
                        continue
                    }
                    var sum = 0f
                    for (ky in -1..1) {
                        val py = (y + ky).coerceIn(0, h - 1)
                        for (kx in -1..1) {
                            val px = (x + kx).coerceIn(0, w - 1)
                            val pixelVal = src[(py * w + px) * channels + c].toInt() and 0xFF
                            val kWeight = kernel[(ky + 1) * 3 + (kx + 1)]
                            sum += pixelVal * kWeight
                        }
                    }
                    dst[(y * w + x) * channels + c] = sum.coerceIn(0f, 255f).toInt().toByte()
                }
            }
        }

        return fromRawBytes(width(), height(), dst, color())
    }

    public fun adjustContrast(c: Float): DynamicImage {
        val channels = color().channelCount().toInt()
        val bytes = asBytes()
        val res = contrast(bytes, width().toInt(), height().toInt(), channels, c)
        return fromRawBytes(width(), height(), res, color())
    }

    public fun brighten(value: Int): DynamicImage {
        val channels = color().channelCount().toInt()
        val bytes = asBytes()
        val res = brighten(bytes, width().toInt(), height().toInt(), channels, value)
        return fromRawBytes(width(), height(), res, color())
    }

    public fun huerotate(value: Int): DynamicImage {
        val channels = color().channelCount().toInt()
        val bytes = asBytes()
        val res = huerotate(bytes, width().toInt(), height().toInt(), channels, value)
        return fromRawBytes(width(), height(), res, color())
    }

    public fun flipv(): DynamicImage {
        val channels = color().channelCount().toInt()
        val bytes = asBytes()
        val res = flipVertical(bytes, width().toInt(), height().toInt(), channels)
        return fromRawBytes(width(), height(), res, color())
    }

    public fun flipvInPlace() {
        when (this) {
            is ImageLuma8 -> flipVerticalInPlace(image.asRaw(), width().toInt(), height().toInt(), 1)
            is ImageLumaA8 -> flipVerticalInPlace(image.asRaw(), width().toInt(), height().toInt(), 2)
            is ImageRgb8 -> flipVerticalInPlace(image.asRaw(), width().toInt(), height().toInt(), 3)
            is ImageRgba8 -> flipVerticalInPlace(image.asRaw(), width().toInt(), height().toInt(), 4)
            is ImageLuma16 -> flipVerticalInPlace(image.asRaw(), width().toInt(), height().toInt(), 2)
            is ImageLumaA16 -> flipVerticalInPlace(image.asRaw(), width().toInt(), height().toInt(), 4)
            is ImageRgb16 -> flipVerticalInPlace(image.asRaw(), width().toInt(), height().toInt(), 6)
            is ImageRgba16 -> flipVerticalInPlace(image.asRaw(), width().toInt(), height().toInt(), 8)
            is ImageRgb32F -> flipVerticalInPlace(image.asRaw(), width().toInt(), height().toInt(), 12)
            is ImageRgba32F -> flipVerticalInPlace(image.asRaw(), width().toInt(), height().toInt(), 16)
        }
    }

    public fun fliph(): DynamicImage {
        val channels = color().channelCount().toInt()
        val bytes = asBytes()
        val res = flipHorizontal(bytes, width().toInt(), height().toInt(), channels)
        return fromRawBytes(width(), height(), res, color())
    }

    public fun fliphInPlace() {
        when (this) {
            is ImageLuma8 -> flipHorizontalInPlace(image.asRaw(), width().toInt(), height().toInt(), 1)
            is ImageLumaA8 -> flipHorizontalInPlace(image.asRaw(), width().toInt(), height().toInt(), 2)
            is ImageRgb8 -> flipHorizontalInPlace(image.asRaw(), width().toInt(), height().toInt(), 3)
            is ImageRgba8 -> flipHorizontalInPlace(image.asRaw(), width().toInt(), height().toInt(), 4)
            is ImageLuma16 -> flipHorizontalInPlace(image.asRaw(), width().toInt(), height().toInt(), 2)
            is ImageLumaA16 -> flipHorizontalInPlace(image.asRaw(), width().toInt(), height().toInt(), 4)
            is ImageRgb16 -> flipHorizontalInPlace(image.asRaw(), width().toInt(), height().toInt(), 6)
            is ImageRgba16 -> flipHorizontalInPlace(image.asRaw(), width().toInt(), height().toInt(), 8)
            is ImageRgb32F -> flipHorizontalInPlace(image.asRaw(), width().toInt(), height().toInt(), 12)
            is ImageRgba32F -> flipHorizontalInPlace(image.asRaw(), width().toInt(), height().toInt(), 16)
        }
    }

    public fun rotate90(): DynamicImage {
        val channels = color().channelCount().toInt()
        val bytes = asBytes()
        val res = rotate90(bytes, width().toInt(), height().toInt(), channels)
        return fromRawBytes(height(), width(), res, color())
    }

    public fun rotate180(): DynamicImage {
        val channels = color().channelCount().toInt()
        val bytes = asBytes()
        val res = rotate180(bytes, width().toInt(), height().toInt(), channels)
        return fromRawBytes(width(), height(), res, color())
    }

    public fun rotate180InPlace() {
        when (this) {
            is ImageLuma8 -> rotate180InPlace(image.asRaw(), width().toInt(), height().toInt(), 1)
            is ImageLumaA8 -> rotate180InPlace(image.asRaw(), width().toInt(), height().toInt(), 2)
            is ImageRgb8 -> rotate180InPlace(image.asRaw(), width().toInt(), height().toInt(), 3)
            is ImageRgba8 -> rotate180InPlace(image.asRaw(), width().toInt(), height().toInt(), 4)
            is ImageLuma16 -> rotate180InPlace(image.asRaw(), width().toInt(), height().toInt(), 2)
            is ImageLumaA16 -> rotate180InPlace(image.asRaw(), width().toInt(), height().toInt(), 4)
            is ImageRgb16 -> rotate180InPlace(image.asRaw(), width().toInt(), height().toInt(), 6)
            is ImageRgba16 -> rotate180InPlace(image.asRaw(), width().toInt(), height().toInt(), 8)
            is ImageRgb32F -> rotate180InPlace(image.asRaw(), width().toInt(), height().toInt(), 12)
            is ImageRgba32F -> rotate180InPlace(image.asRaw(), width().toInt(), height().toInt(), 16)
        }
    }

    public fun rotate270(): DynamicImage {
        val channels = color().channelCount().toInt()
        val bytes = asBytes()
        val res = rotate270(bytes, width().toInt(), height().toInt(), channels)
        return fromRawBytes(height(), width(), res, color())
    }

    public fun applyOrientation(orientation: Orientation): DynamicImage =
        when (orientation) {
            Orientation.NoTransforms -> this
            Orientation.Rotate90 -> rotate90()
            Orientation.Rotate180 -> {
                val copy = cloneImage()
                copy.rotate180InPlace()
                copy
            }
            Orientation.Rotate270 -> rotate270()
            Orientation.FlipHorizontal -> {
                val copy = cloneImage()
                copy.fliphInPlace()
                copy
            }
            Orientation.FlipVertical -> {
                val copy = cloneImage()
                copy.flipvInPlace()
                copy
            }
            Orientation.Rotate90FlipH -> {
                val copy = rotate90()
                copy.fliphInPlace()
                copy
            }
            Orientation.Rotate270FlipH -> {
                val copy = rotate270()
                copy.fliphInPlace()
                copy
            }
        }

    public fun crop(x: UInt, y: UInt, width: UInt, height: UInt): DynamicImage =
        cropImm(x, y, width, height)

    public fun cropImm(x: UInt, y: UInt, width: UInt, height: UInt): DynamicImage {
        val (iwidth, iheight) = dimensions()
        val nx = minOf(x, iwidth)
        val ny = minOf(y, iheight)
        val nw = minOf(width, iwidth - nx)
        val nh = minOf(height, iheight - ny)

        return when (this) {
            is ImageLuma8 -> ImageLuma8(ImageBuffer.createGray(nw, nh) { cx, cy -> image.getPixel(nx + cx, ny + cy) })
            is ImageLumaA8 -> ImageLumaA8(ImageBuffer.createGrayAlpha(nw, nh) { cx, cy -> image.getPixel(nx + cx, ny + cy) })
            is ImageRgb8 -> ImageRgb8(ImageBuffer.createRgb(nw, nh) { cx, cy -> image.getPixel(nx + cx, ny + cy) })
            is ImageRgba8 -> ImageRgba8(ImageBuffer.createRgba(nw, nh) { cx, cy -> image.getPixel(nx + cx, ny + cy) })
            is ImageLuma16 -> {
                val res = createGray16(nw, nh)
                for (cy in 0u until nh) {
                    for (cx in 0u until nw) {
                        res.putPixel(cx, cy, image.getPixel(nx + cx, ny + cy))
                    }
                }
                ImageLuma16(res)
            }
            is ImageLumaA16 -> {
                val res = createGrayAlpha16(nw, nh)
                for (cy in 0u until nh) {
                    for (cx in 0u until nw) {
                        res.putPixel(cx, cy, image.getPixel(nx + cx, ny + cy))
                    }
                }
                ImageLumaA16(res)
            }
            is ImageRgb16 -> {
                val res = createRgb16(nw, nh)
                for (cy in 0u until nh) {
                    for (cx in 0u until nw) {
                        res.putPixel(cx, cy, image.getPixel(nx + cx, ny + cy))
                    }
                }
                ImageRgb16(res)
            }
            is ImageRgba16 -> {
                val res = createRgba16(nw, nh)
                for (cy in 0u until nh) {
                    for (cx in 0u until nw) {
                        res.putPixel(cx, cy, image.getPixel(nx + cx, ny + cy))
                    }
                }
                ImageRgba16(res)
            }
            is ImageRgb32F -> {
                val res = createRgb32F(nw, nh)
                for (cy in 0u until nh) {
                    for (cx in 0u until nw) {
                        res.putPixel(cx, cy, image.getPixel(nx + cx, ny + cy))
                    }
                }
                ImageRgb32F(res)
            }
            is ImageRgba32F -> {
                val res = createRgba32F(nw, nh)
                for (cy in 0u until nh) {
                    for (cx in 0u until nw) {
                        res.putPixel(cx, cy, image.getPixel(nx + cx, ny + cy))
                    }
                }
                ImageRgba32F(res)
            }
        }
    }

    public fun writeTo(writer: IoWrite, format: ImageFormat) {
        val encoder = encoderForFormat(format, writer)
        writeWithEncoder(encoder)
    }

    public fun writeWithEncoder(encoder: ImageEncoder) {
        encoder.writeImage(asBytes(), width(), height(), color().toExtendedColorType())
    }

    private fun cloneImage(): DynamicImage =
        when (this) {
            is ImageLuma8 -> ImageLuma8(image.copy())
            is ImageLumaA8 -> ImageLumaA8(image.copy())
            is ImageRgb8 -> ImageRgb8(image.copy())
            is ImageRgba8 -> ImageRgba8(image.copy())
            is ImageLuma16 -> ImageLuma16(image.copy())
            is ImageLumaA16 -> ImageLumaA16(image.copy())
            is ImageRgb16 -> ImageRgb16(image.copy())
            is ImageRgba16 -> ImageRgba16(image.copy())
            is ImageRgb32F -> ImageRgb32F(image.copy())
            is ImageRgba32F -> ImageRgba32F(image.copy())
        }

    public companion object {
        public fun new(w: UInt, h: UInt, color: ColorType): DynamicImage =
            when (color) {
                ColorType.L8 -> newLuma8(w, h)
                ColorType.La8 -> newLumaA8(w, h)
                ColorType.Rgb8 -> newRgb8(w, h)
                ColorType.Rgba8 -> newRgba8(w, h)
                ColorType.L16 -> newLuma16(w, h)
                ColorType.La16 -> newLumaA16(w, h)
                ColorType.Rgb16 -> newRgb16(w, h)
                ColorType.Rgba16 -> newRgba16(w, h)
                ColorType.Rgb32F -> newRgb32F(w, h)
                ColorType.Rgba32F -> newRgba32F(w, h)
            }

        public fun newLuma8(w: UInt, h: UInt): DynamicImage =
            ImageLuma8(createGray(w, h))

        public fun newLumaA8(w: UInt, h: UInt): DynamicImage =
            ImageLumaA8(createGrayAlpha(w, h))

        public fun newRgb8(w: UInt, h: UInt): DynamicImage =
            ImageRgb8(createRgb(w, h))

        public fun newRgba8(w: UInt, h: UInt): DynamicImage =
            ImageRgba8(createRgba(w, h))

        public fun newLuma16(w: UInt, h: UInt): DynamicImage =
            ImageLuma16(createGray16(w, h))

        public fun newLumaA16(w: UInt, h: UInt): DynamicImage =
            ImageLumaA16(createGrayAlpha16(w, h))

        public fun newRgb16(w: UInt, h: UInt): DynamicImage =
            ImageRgb16(createRgb16(w, h))

        public fun newRgba16(w: UInt, h: UInt): DynamicImage =
            ImageRgba16(createRgba16(w, h))

        public fun newRgb32F(w: UInt, h: UInt): DynamicImage =
            ImageRgb32F(createRgb32F(w, h))

        public fun newRgba32F(w: UInt, h: UInt): DynamicImage =
            ImageRgba32F(createRgba32F(w, h))

        public fun fromDecoder(decoder: ImageDecoder): DynamicImage =
            decoderToImage(decoder)
    }
}

private fun fromRawBytes(w: UInt, h: UInt, buf: ByteArray, color: ColorType): DynamicImage =
    when (color) {
        ColorType.L8 -> DynamicImage.ImageLuma8(createGray(w, h, buf))
        ColorType.La8 -> DynamicImage.ImageLumaA8(createGrayAlpha(w, h, buf))
        ColorType.Rgb8 -> DynamicImage.ImageRgb8(createRgb(w, h, buf))
        ColorType.Rgba8 -> DynamicImage.ImageRgba8(createRgba(w, h, buf))
        ColorType.L16 -> DynamicImage.ImageLuma16(createGray16(w, h, buf))
        ColorType.La16 -> DynamicImage.ImageLumaA16(createGrayAlpha16(w, h, buf))
        ColorType.Rgb16 -> DynamicImage.ImageRgb16(createRgb16(w, h, buf))
        ColorType.Rgba16 -> DynamicImage.ImageRgba16(createRgba16(w, h, buf))
        ColorType.Rgb32F -> DynamicImage.ImageRgb32F(createRgb32F(w, h, buf))
        ColorType.Rgba32F -> DynamicImage.ImageRgba32F(createRgba32F(w, h, buf))
    }

private fun decoderToImage(decoder: ImageDecoder): DynamicImage {
    val (w, h) = decoder.dimensions()
    val colorType = decoder.colorType()
    val totalBytes = decoder.totalBytes()
    if (totalBytes > Int.MAX_VALUE.toULong()) {
        throw ImageError.Limits(LimitError(LimitErrorKind.InsufficientMemory))
    }
    val buf = ByteArray(totalBytes.toInt())
    decoder.readImage(buf)
    return fromRawBytes(w, h, buf, colorType)
}

public fun loadFromMemory(buffer: ByteArray): DynamicImage {
    val format = guessFormat(buffer)
    return loadFromMemoryWithFormat(buffer, format)
}

public fun loadFromMemoryWithFormat(buf: ByteArray, format: ImageFormat): DynamicImage {
    val reader = BufferIoRead(buf)
    return load(reader, format)
}

public fun writeBufferWithFormat(
    writer: IoWrite,
    buf: ByteArray,
    width: UInt,
    height: UInt,
    color: ExtendedColorType,
    format: ImageFormat,
) {
    val encoder = encoderForFormat(format, writer)
    encoder.writeImage(buf, width, height, color)
}
