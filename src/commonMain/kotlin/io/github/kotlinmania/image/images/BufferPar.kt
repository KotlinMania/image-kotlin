// port-lint: source images/buffer_par.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.Luma
import io.github.kotlinmania.image.LumaA
import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.Rgba

/**
 * Parallel iterator abstraction over pixel references.
 */
public class PixelsPar<P> internal constructor(
    private val pixels: List<P>,
) : Iterable<P> {
    public val size: Int get() = pixels.size

    public fun len(): Int = pixels.size

    public fun isEmpty(): Boolean = pixels.isEmpty()

    override fun iterator(): Iterator<P> = pixels.iterator()

    public fun toList(): List<P> = pixels

    override fun toString(): String = "PixelsPar(size=$size)"
}

/**
 * Parallel iterator abstraction over mutable pixel references.
 */
public class PixelsMutPar<P> internal constructor(
    private val pixels: List<P>,
) : Iterable<P> {
    public val size: Int get() = pixels.size

    public fun len(): Int = pixels.size

    public fun isEmpty(): Boolean = pixels.isEmpty()

    override fun iterator(): Iterator<P> = pixels.iterator()

    public fun toList(): List<P> = pixels

    override fun toString(): String = "PixelsMutPar(size=$size)"
}

/**
 * Parallel iterator abstraction over pixel coordinates and pixel references.
 */
public class EnumeratePixelsPar<P> internal constructor(
    private val enumerated: List<Triple<UInt, UInt, P>>,
    public val width: UInt,
) : Iterable<Triple<UInt, UInt, P>> {
    public val size: Int get() = enumerated.size

    public fun len(): Int = enumerated.size

    public fun isEmpty(): Boolean = enumerated.isEmpty()

    override fun iterator(): Iterator<Triple<UInt, UInt, P>> = enumerated.iterator()

    public fun toList(): List<Triple<UInt, UInt, P>> = enumerated

    override fun toString(): String = "EnumeratePixelsPar(width=$width, size=$size)"
}

/**
 * Parallel iterator abstraction over mutable pixel coordinates and pixel references.
 */
public class EnumeratePixelsMutPar<P> internal constructor(
    private val enumerated: List<Triple<UInt, UInt, P>>,
    public val width: UInt,
) : Iterable<Triple<UInt, UInt, P>> {
    public val size: Int get() = enumerated.size

    public fun len(): Int = enumerated.size

    public fun isEmpty(): Boolean = enumerated.isEmpty()

    override fun iterator(): Iterator<Triple<UInt, UInt, P>> = enumerated.iterator()

    public fun toList(): List<Triple<UInt, UInt, P>> = enumerated

    override fun toString(): String = "EnumeratePixelsMutPar(width=$width, size=$size)"
}

/**
 * Returns a parallel iterator over the pixels of this image.
 */
public fun <P, Container> ImageBuffer<P, Container>.parPixels(): PixelsPar<P> =
    PixelsPar(pixelsMut())

/**
 * Returns a parallel iterator over the mutable pixels of this image.
 */
public fun <P, Container> ImageBuffer<P, Container>.parPixelsMut(): PixelsMutPar<P> =
    PixelsMutPar(pixelsMut())

/**
 * Returns a parallel iterator over the pixels of this image and their coordinates.
 */
public fun <P, Container> ImageBuffer<P, Container>.parEnumeratePixels(): EnumeratePixelsPar<P> =
    EnumeratePixelsPar(enumeratePixels(), width())

/**
 * Returns a parallel iterator over the mutable pixels of this image and their coordinates.
 */
public fun <P, Container> ImageBuffer<P, Container>.parEnumeratePixelsMut(): EnumeratePixelsMutPar<P> =
    EnumeratePixelsMutPar(enumeratePixelsMut(), width())

/**
 * Constructs a new [RgbImage] by repeated application of the supplied function.
 */
public fun fromParFnRgb(width: UInt, height: UInt, f: (UInt, UInt) -> Rgb<UByte>): RgbImage =
    ImageBuffer.createRgb(width, height, f)

/**
 * Constructs a new [RgbaImage] by repeated application of the supplied function.
 */
public fun fromParFnRgba(width: UInt, height: UInt, f: (UInt, UInt) -> Rgba<UByte>): RgbaImage =
    ImageBuffer.createRgba(width, height, f)

/**
 * Constructs a new [GrayImage] by repeated application of the supplied function.
 */
public fun fromParFnGray(width: UInt, height: UInt, f: (UInt, UInt) -> Luma<UByte>): GrayImage =
    ImageBuffer.createGray(width, height, f)

/**
 * Constructs a new [GrayAlphaImage] by repeated application of the supplied function.
 */
public fun fromParFnGrayAlpha(width: UInt, height: UInt, f: (UInt, UInt) -> LumaA<UByte>): GrayAlphaImage =
    ImageBuffer.createGrayAlpha(width, height, f)
