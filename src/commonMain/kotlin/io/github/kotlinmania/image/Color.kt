// port-lint: source color.rs
package io.github.kotlinmania.image

import kotlin.math.roundToInt

/**
 * An enumeration over supported color types and bit depths.
 */
public enum class ColorType {
    /** Pixel is 8-bit luminance */
    L8,

    /** Pixel is 8-bit luminance with an alpha channel */
    La8,

    /** Pixel contains 8-bit R, G and B channels */
    Rgb8,

    /** Pixel is 8-bit RGB with an alpha channel */
    Rgba8,

    /** Pixel is 16-bit luminance */
    L16,

    /** Pixel is 16-bit luminance with an alpha channel */
    La16,

    /** Pixel is 16-bit RGB */
    Rgb16,

    /** Pixel is 16-bit RGBA */
    Rgba16,

    /** Pixel is 32-bit float RGB */
    Rgb32F,

    /** Pixel is 32-bit float RGBA */
    Rgba32F,

    ;

    /** Returns the number of bytes contained in a pixel of `ColorType`. */
    public fun bytesPerPixel(): UByte =
        when (this) {
            L8 -> 1u
            L16, La8 -> 2u
            Rgb8 -> 3u
            Rgba8, La16 -> 4u
            Rgb16 -> 6u
            Rgba16 -> 8u
            Rgb32F -> 12u
            Rgba32F -> 16u
        }

    /** Returns if there is an alpha channel. */
    public fun hasAlpha(): Boolean =
        when (this) {
            L8, L16, Rgb8, Rgb16, Rgb32F -> false
            La8, Rgba8, La16, Rgba16, Rgba32F -> true
        }

    /** Returns false if the color scheme is grayscale, true otherwise. */
    public fun hasColor(): Boolean =
        when (this) {
            L8, L16, La8, La16 -> false
            Rgb8, Rgb16, Rgba8, Rgba16, Rgb32F, Rgba32F -> true
        }

    /** Returns the number of bits contained in a pixel of `ColorType`. */
    public fun bitsPerPixel(): UShort = (bytesPerPixel().toUInt() * 8u).toUShort()

    /** Returns the number of color channels that make up this pixel. */
    public fun channelCount(): UByte = toExtendedColorType().channelCount()

    public fun toExtendedColorType(): ExtendedColorType =
        when (this) {
            L8 -> ExtendedColorType.L8
            La8 -> ExtendedColorType.La8
            Rgb8 -> ExtendedColorType.Rgb8
            Rgba8 -> ExtendedColorType.Rgba8
            L16 -> ExtendedColorType.L16
            La16 -> ExtendedColorType.La16
            Rgb16 -> ExtendedColorType.Rgb16
            Rgba16 -> ExtendedColorType.Rgba16
            Rgb32F -> ExtendedColorType.Rgb32F
            Rgba32F -> ExtendedColorType.Rgba32F
        }
}

/**
 * An enumeration of color types encountered in image formats.
 */
public sealed interface ExtendedColorType {
    public data object A8 : ExtendedColorType

    public data object L1 : ExtendedColorType

    public data object La1 : ExtendedColorType

    public data object Rgb1 : ExtendedColorType

    public data object Rgba1 : ExtendedColorType

    public data object L2 : ExtendedColorType

    public data object La2 : ExtendedColorType

    public data object Rgb2 : ExtendedColorType

    public data object Rgba2 : ExtendedColorType

    public data object L4 : ExtendedColorType

    public data object La4 : ExtendedColorType

    public data object Rgb4 : ExtendedColorType

    public data object Rgba4 : ExtendedColorType

    public data object L8 : ExtendedColorType

    public data object La8 : ExtendedColorType

    public data object Rgb8 : ExtendedColorType

    public data object Rgba8 : ExtendedColorType

    public data object L16 : ExtendedColorType

    public data object La16 : ExtendedColorType

    public data object Rgb16 : ExtendedColorType

    public data object Rgba16 : ExtendedColorType

    public data object Bgr8 : ExtendedColorType

    public data object Bgra8 : ExtendedColorType

    public data object Rgb32F : ExtendedColorType

    public data object Rgba32F : ExtendedColorType

    public data object Cmyk8 : ExtendedColorType

    public data object Cmyk16 : ExtendedColorType

    public data class Unknown(
        public val bpp: UByte,
    ) : ExtendedColorType

    /** Get the number of channels for colors of this type. */
    public fun channelCount(): UByte =
        when (this) {
            A8, L1, L2, L4, L8, L16, is Unknown -> 1u
            La1, La2, La4, La8, La16 -> 2u
            Rgb1, Rgb2, Rgb4, Rgb8, Rgb16, Rgb32F, Bgr8 -> 3u
            Rgba1, Rgba2, Rgba4, Rgba8, Rgba16, Rgba32F, Bgra8, Cmyk8, Cmyk16 -> 4u
        }

    /** Returns the number of bits per pixel for this color type. */
    public fun bitsPerPixel(): UShort =
        when (this) {
            A8 -> 8u
            L1 -> 1u
            La1 -> 2u
            Rgb1 -> 3u
            Rgba1 -> 4u
            L2 -> 2u
            La2 -> 4u
            Rgb2 -> 6u
            Rgba2 -> 8u
            L4 -> 4u
            La4 -> 8u
            Rgb4 -> 12u
            Rgba4 -> 16u
            L8 -> 8u
            La8 -> 16u
            Rgb8 -> 24u
            Rgba8 -> 32u
            L16 -> 16u
            La16 -> 32u
            Rgb16 -> 48u
            Rgba16 -> 64u
            Rgb32F -> 96u
            Rgba32F -> 128u
            Bgr8 -> 24u
            Bgra8 -> 32u
            Cmyk8 -> 32u
            Cmyk16 -> 64u
            is Unknown -> bpp.toUShort()
        }

    /** Returns the ColorType that is equivalent to this ExtendedColorType. */
    public fun colorType(): ColorType? =
        when (this) {
            L8 -> ColorType.L8
            La8 -> ColorType.La8
            Rgb8 -> ColorType.Rgb8
            Rgba8 -> ColorType.Rgba8
            L16 -> ColorType.L16
            La16 -> ColorType.La16
            Rgb16 -> ColorType.Rgb16
            Rgba16 -> ColorType.Rgba16
            Rgb32F -> ColorType.Rgb32F
            Rgba32F -> ColorType.Rgba32F
            else -> null
        }

    /** Returns the number of bytes required to hold a width x height image of this color type. */
    public fun bufferSize(width: UInt, height: UInt): ULong {
        val bpp = bitsPerPixel().toULong()
        val totalBits = width.toULong() * bpp
        val rowPitch = (totalBits + 7uL) / 8uL
        return rowPitch * height.toULong()
    }
}

/**
 * Generic RGB color.
 */
public data class Rgb<T>(
    public var r: T,
    public var g: T,
    public var b: T,
) {
    public fun channels(): List<T> = listOf(r, g, b)

    public fun apply(f: (T) -> T) {
        r = f(r)
        g = f(g)
        b = f(b)
    }

    public fun map(f: (T) -> T): Rgb<T> = Rgb(f(r), f(g), f(b))

    public fun applyWithAlpha(f: (T) -> T, gAlpha: (T) -> T) {
        r = f(r)
        g = f(g)
        b = f(b)
    }

    public fun mapWithAlpha(f: (T) -> T, gAlpha: (T) -> T): Rgb<T> = Rgb(f(r), f(g), f(b))

    public fun applyWithoutAlpha(f: (T) -> T) {
        r = f(r)
        g = f(g)
        b = f(b)
    }

    public fun mapWithoutAlpha(f: (T) -> T): Rgb<T> = Rgb(f(r), f(g), f(b))

    public fun blend(other: Rgb<T>) {
        r = other.r
        g = other.g
        b = other.b
    }

    public companion object {
        public fun <T> from(channels: List<T>): Rgb<T> {
            require(channels.size == 3) { "Expected 3 channels for Rgb" }
            return Rgb(channels[0], channels[1], channels[2])
        }
    }
}

/**
 * Generic RGBA color.
 */
public data class Rgba<T>(
    public var r: T,
    public var g: T,
    public var b: T,
    public var a: T,
) {
    public fun channels(): List<T> = listOf(r, g, b, a)

    public fun apply(f: (T) -> T) {
        r = f(r)
        g = f(g)
        b = f(b)
        a = f(a)
    }

    public fun map(f: (T) -> T): Rgba<T> = Rgba(f(r), f(g), f(b), f(a))

    public fun applyWithAlpha(f: (T) -> T, gAlpha: (T) -> T) {
        r = f(r)
        g = f(g)
        b = f(b)
        a = gAlpha(a)
    }

    public fun mapWithAlpha(f: (T) -> T, gAlpha: (T) -> T): Rgba<T> = Rgba(f(r), f(g), f(b), gAlpha(a))

    public fun applyWithoutAlpha(f: (T) -> T) {
        r = f(r)
        g = f(g)
        b = f(b)
    }

    public fun mapWithoutAlpha(f: (T) -> T): Rgba<T> = Rgba(f(r), f(g), f(b), a)

    public companion object {
        public fun <T> from(channels: List<T>): Rgba<T> {
            require(channels.size == 4) { "Expected 4 channels for Rgba" }
            return Rgba(channels[0], channels[1], channels[2], channels[3])
        }
    }
}

/**
 * Generic grayscale color.
 */
public data class Luma<T>(
    public var l: T,
) {
    public fun channels(): List<T> = listOf(l)

    public fun apply(f: (T) -> T) {
        l = f(l)
    }

    public fun map(f: (T) -> T): Luma<T> = Luma(f(l))

    public fun blend(other: Luma<T>) {
        l = other.l
    }

    public companion object {
        public fun <T> from(channels: List<T>): Luma<T> {
            require(channels.size == 1) { "Expected 1 channel for Luma" }
            return Luma(channels[0])
        }
    }
}

/**
 * Generic grayscale color with alpha.
 */
public data class LumaA<T>(
    public var l: T,
    public var a: T,
) {
    public fun channels(): List<T> = listOf(l, a)

    public fun apply(f: (T) -> T) {
        l = f(l)
        a = f(a)
    }

    public fun map(f: (T) -> T): LumaA<T> = LumaA(f(l), f(a))

    public fun applyWithAlpha(f: (T) -> T, gAlpha: (T) -> T) {
        l = f(l)
        a = gAlpha(a)
    }

    public fun mapWithAlpha(f: (T) -> T, gAlpha: (T) -> T): LumaA<T> = LumaA(f(l), gAlpha(a))

    public companion object {
        public fun <T> from(channels: List<T>): LumaA<T> {
            require(channels.size == 2) { "Expected 2 channels for LumaA" }
            return LumaA(channels[0], channels[1])
        }
    }
}

public fun Rgba<UByte>.blendUByte(other: Rgba<UByte>) {
    val otherAlpha = other.a.toInt()
    if (otherAlpha == 0) return
    if (otherAlpha == 255) {
        r = other.r
        g = other.g
        b = other.b
        a = other.a
        return
    }

    val maxT = 255.0f
    val bgR = r.toFloat() / maxT
    val bgG = g.toFloat() / maxT
    val bgB = b.toFloat() / maxT
    val bgA = a.toFloat() / maxT

    val fgR = other.r.toFloat() / maxT
    val fgG = other.g.toFloat() / maxT
    val fgB = other.b.toFloat() / maxT
    val fgA = other.a.toFloat() / maxT

    val alphaFinal = bgA + fgA - bgA * fgA
    if (alphaFinal == 0.0f) return

    val outRA = fgR * fgA + bgR * bgA * (1.0f - fgA)
    val outGA = fgG * fgA + bgG * bgA * (1.0f - fgA)
    val outBA = fgB * fgA + bgB * bgA * (1.0f - fgA)

    r = ((outRA / alphaFinal) * maxT).roundToInt().coerceIn(0, 255).toUByte()
    g = ((outGA / alphaFinal) * maxT).roundToInt().coerceIn(0, 255).toUByte()
    b = ((outBA / alphaFinal) * maxT).roundToInt().coerceIn(0, 255).toUByte()
    a = (alphaFinal * maxT).roundToInt().coerceIn(0, 255).toUByte()
}

public fun LumaA<UByte>.blendUByte(other: LumaA<UByte>) {
    val maxT = 255.0f
    val bgLuma = l.toFloat() / maxT
    val bgA = a.toFloat() / maxT
    val fgLuma = other.l.toFloat() / maxT
    val fgA = other.a.toFloat() / maxT

    val alphaFinal = bgA + fgA - bgA * fgA
    if (alphaFinal == 0.0f) return

    val bgLumaA = bgLuma * bgA
    val fgLumaA = fgLuma * fgA
    val outLumaA = fgLumaA + bgLumaA * (1.0f - fgA)
    val outLuma = outLumaA / alphaFinal

    l = (maxT * outLuma).roundToInt().coerceIn(0, 255).toUByte()
    a = (maxT * alphaFinal).roundToInt().coerceIn(0, 255).toUByte()
}

public fun Rgb<UByte>.toLuma(): Luma<UByte> {
    val l = (2126u * r.toUInt() + 7152u * g.toUInt() + 722u * b.toUInt()) / 10000u
    return Luma(l.toUByte())
}
