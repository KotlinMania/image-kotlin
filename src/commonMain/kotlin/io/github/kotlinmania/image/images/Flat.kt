// port-lint: source image/src/images/flat.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.Luma
import io.github.kotlinmania.image.LumaA
import io.github.kotlinmania.image.ParameterError
import io.github.kotlinmania.image.ParameterErrorKind
import io.github.kotlinmania.image.Rgb
import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.blendUByte

/**
 * Different normal forms of buffers.
 *
 * A normal form is an unaliased buffer with some additional constraints. The [ImageBuffer] uses
 * row major form with packed samples.
 */
public enum class NormalForm {
    /**
     * No pixel aliases another.
     */
    Unaliased,

    /**
     * At least pixels are packed.
     */
    PixelPacked,

    /**
     * All samples are packed.
     */
    ImagePacked,

    /**
     * The samples are in row-major form and all samples are packed.
     */
    RowMajorPacked,

    /**
     * The samples are in column-major form and all samples are packed.
     */
    ColumnMajorPacked,

    ;

    /**
     * Compares logical preconditions.
     */
    public fun partialCompareTo(other: NormalForm): Int? {
        if (this == other) return 0
        if (this == Unaliased) return -1
        if (other == Unaliased) return 1

        if (this == PixelPacked && (other == ColumnMajorPacked || other == RowMajorPacked)) return -1
        if (other == PixelPacked && (this == RowMajorPacked || this == ColumnMajorPacked)) return 1

        if (this == ImagePacked && (other == ColumnMajorPacked || other == RowMajorPacked)) return -1
        if (other == ImagePacked && (this == RowMajorPacked || this == ColumnMajorPacked)) return 1

        return null
    }

    public fun partialCmp(other: NormalForm): Int? = partialCompareTo(other)
}

/**
 * Denotes invalid flat sample buffers when trying to convert to stricter types.
 */
public sealed class FlatError : Exception() {
    /** The represented image was too large. */
    public data object TooLarge : FlatError() {
        override val message: String = "The layout is too large"
    }

    /** The represented image can not use this representation. */
    public data class NormalFormRequired(
        public val form: NormalForm,
    ) : FlatError() {
        override val message: String =
            "The layout needs to " +
                when (form) {
                    NormalForm.ColumnMajorPacked -> "be packed and in column major form"
                    NormalForm.ImagePacked -> "be fully packed"
                    NormalForm.PixelPacked -> "have packed pixels"
                    NormalForm.RowMajorPacked -> "be packed and in row major form"
                    NormalForm.Unaliased -> "not have any aliasing channels"
                }
    }

    /** The color format did not match the channel count. */
    public data class ChannelCountMismatch(
        public val layoutChannels: UByte,
        public val pixelChannels: UByte,
    ) : FlatError() {
        override val message: String =
            "The channel count of the chosen pixel (=$pixelChannels) does agree with the layout (=$layoutChannels)"
    }

    /** The chosen color type does not match the hint. */
    public data class WrongColor(
        public val color: ColorType,
    ) : FlatError() {
        override val message: String = "The chosen color type does not match the hint $color"
    }

    public fun toImageError(): ImageError =
        when (this) {
            is TooLarge -> ImageError.Parameter(ParameterError.fromKind(ParameterErrorKind.DimensionMismatch))
            is NormalFormRequired -> ImageError.Decoding(DecodingError.new(ImageFormatHint.Unknown, this))
            is ChannelCountMismatch -> ImageError.Parameter(ParameterError.fromKind(ParameterErrorKind.DimensionMismatch))
            is WrongColor ->
                ImageError.Unsupported(
                    UnsupportedError.fromFormatAndKind(
                        ImageFormatHint.Unknown,
                        UnsupportedErrorKind.Color(color.toExtendedColorType()),
                    ),
                )
        }
}

/**
 * Helper struct for an unnamed (stride, length) pair.
 */
internal data class Dim(
    val stride: Int,
    val length: Int,
) : Comparable<Dim> {
    fun stride(): Int = stride

    fun checkedLen(): Int? {
        val result = stride.toLong() * length.toLong()
        return if (result > Int.MAX_VALUE.toLong() || result < 0L) null else result.toInt()
    }

    fun len(): Int = stride * length

    override fun compareTo(other: Dim): Int {
        val c = stride.compareTo(other.stride)
        return if (c != 0) c else length.compareTo(other.length)
    }
}

/**
 * A description of a sample buffer layout.
 */
public data class SampleLayout(
    public val channels: UByte,
    public val channelStride: Int,
    public val width: UInt,
    public val widthStride: Int,
    public val height: UInt,
    public val heightStride: Int,
) {
    public fun stridesCwh(): Triple<Int, Int, Int> =
        Triple(channelStride, widthStride, heightStride)

    public fun extents(): Triple<Int, Int, Int> =
        Triple(channels.toInt(), width.toInt(), height.toInt())

    public fun extentsCwh(): Triple<Int, UInt, UInt> =
        Triple(channels.toInt(), width, height)

    public fun bounds(): Triple<UByte, UInt, UInt> =
        Triple(channels, width, height)

    public fun totalSamples(): Long =
        channels.toLong() * width.toLong() * height.toLong()

    public fun sampleIndex(channel: Int, x: UInt, y: UInt): Long =
        (channel * channelStride + x.toLong() * widthStride + y.toLong() * heightStride)

    public fun minLength(): Int? {
        if (width == 0u || height == 0u || channels == 0.toUByte()) {
            return 0
        }
        val idx = index((channels.toInt() - 1).toUByte(), width - 1u, height - 1u) ?: return null
        return if (idx == Int.MAX_VALUE) null else idx + 1
    }

    public fun fits(len: Int): Boolean = minLength()?.let { len >= it } ?: false

    internal fun increasingStrideDims(): List<Dim> {
        val grouped =
            mutableListOf(
                Dim(channelStride, channels.toInt()),
                Dim(widthStride, width.toInt()),
                Dim(heightStride, height.toInt()),
            )
        grouped.sort()
        return grouped
    }

    public fun hasAliasedSamples(): Boolean {
        val grouped = increasingStrideDims()
        val minDim = grouped[0]
        val midDim = grouped[1]
        val maxDim = grouped[2]

        val minSize = minDim.checkedLen() ?: return true
        val midSize = midDim.checkedLen() ?: return true

        if (maxDim.checkedLen() == null) {
            return true
        }

        return minSize > midDim.stride || midSize > maxDim.stride
    }

    public fun isNormal(form: NormalForm): Boolean {
        if (hasAliasedSamples()) {
            return false
        }

        if ((form == NormalForm.PixelPacked || form == NormalForm.RowMajorPacked || form == NormalForm.ColumnMajorPacked) && channelStride != 1) {
            return false
        }

        if (form == NormalForm.ImagePacked || form == NormalForm.RowMajorPacked || form == NormalForm.ColumnMajorPacked) {
            val grouped = increasingStrideDims()
            val minDim = grouped[0]
            val midDim = grouped[1]
            val maxDim = grouped[2]

            if (minDim.stride != 1) {
                return false
            }
            if (minDim.len() != midDim.stride) {
                return false
            }
            if (midDim.len() != maxDim.stride) {
                return false
            }
        }

        if (form == NormalForm.RowMajorPacked) {
            if (widthStride != channels.toInt()) {
                return false
            }
            if (width.toInt() * widthStride != heightStride) {
                return false
            }
        }

        if (form == NormalForm.ColumnMajorPacked) {
            if (heightStride != channels.toInt()) {
                return false
            }
            if (height.toInt() * heightStride != widthStride) {
                return false
            }
        }

        return true
    }

    public fun inBounds(channel: UByte, x: UInt, y: UInt): Boolean =
        channel < channels && x < width && y < height

    public fun index(channel: UByte, x: UInt, y: UInt): Int? {
        if (!inBounds(channel, x, y)) {
            return null
        }
        return indexIgnoringBounds(channel.toInt(), x.toInt(), y.toInt())
    }

    public fun indexIgnoringBounds(channel: Int, x: Int, y: Int): Int? {
        val idxC = channel.toLong() * channelStride.toLong()
        val idxX = x.toLong() * widthStride.toLong()
        val idxY = y.toLong() * heightStride.toLong()
        val total = idxC + idxX + idxY
        return if (total < 0L || total > Int.MAX_VALUE.toLong()) null else total.toInt()
    }

    public fun inBoundsIndex(channel: UByte, x: UInt, y: UInt): Int {
        val (cStride, xStride, yStride) = stridesCwh()
        return (y.toInt() * yStride) + (x.toInt() * xStride) + (channel.toInt() * cStride)
    }

    public fun panicCwhOutOfBounds(channel: UByte, x: UInt, y: UInt): Nothing = throw IndexOutOfBoundsException("Sample layout index ($channel, $x, $y) out of bounds ($channels, $width, $height)")

    public fun panicPixelOutOfBounds(x: UInt, y: UInt): Nothing = throw IndexOutOfBoundsException("Sample layout pixel ($x, $y) out of bounds ($width, $height)")

    public fun shrinkTo(channels: UByte, width: UInt, height: UInt): SampleLayout =
        copy(
            channels = minOf(this.channels, channels),
            width = minOf(this.width, width),
            height = minOf(this.height, height),
        )

    public companion object {
        public fun rowMajorPacked(channels: UByte, width: UInt, height: UInt): SampleLayout {
            val ch = channels.toInt()
            val w = width.toInt()
            val heightStride = ch * w
            return SampleLayout(
                channels = channels,
                channelStride = 1,
                width = width,
                widthStride = ch,
                height = height,
                heightStride = heightStride,
            )
        }

        public fun columnMajorPacked(channels: UByte, width: UInt, height: UInt): SampleLayout {
            val ch = channels.toInt()
            val h = height.toInt()
            val widthStride = ch * h
            return SampleLayout(
                channels = channels,
                channelStride = 1,
                width = width,
                widthStride = widthStride,
                height = height,
                heightStride = ch,
            )
        }
    }
}

/**
 * A flat buffer over a multi-channel image.
 */
public data class FlatSamples<Buffer>(
    public val samples: Buffer,
    public val layout: SampleLayout,
    public val colorHint: ColorType? = null,
) {
    public fun stridesCwh(): Triple<Int, Int, Int> = layout.stridesCwh()

    public fun extents(): Triple<Int, Int, Int> = layout.extents()

    public fun bounds(): Triple<UByte, UInt, UInt> = layout.bounds()

    public fun minLength(): Int? = layout.minLength()

    public fun fits(len: Int): Boolean = layout.fits(len)

    public fun hasAliasedSamples(): Boolean = layout.hasAliasedSamples()

    public fun isNormal(form: NormalForm): Boolean = layout.isNormal(form)

    public fun inBounds(channel: UByte, x: UInt, y: UInt): Boolean = layout.inBounds(channel, x, y)

    public fun index(channel: UByte, x: UInt, y: UInt): Int? = layout.index(channel, x, y)

    public fun indexMut(channel: UByte, x: UInt, y: UInt): Int? = layout.index(channel, x, y)

    public fun indexIgnoringBounds(channel: Int, x: Int, y: Int): Int? = layout.indexIgnoringBounds(channel, x, y)

    public fun inBoundsIndex(channel: UByte, x: UInt, y: UInt): Int = layout.inBoundsIndex(channel, x, y)

    public fun fmt(): String = "FlatSamples(layout=$layout, colorHint=$colorHint)"

    public fun asSlice(): Buffer = samples

    public fun asMutSlice(): Buffer = samples

    public fun asRef(): FlatSamples<Buffer> = this

    public fun asMut(): FlatSamples<Buffer> = this

    public fun shrinkTo(channels: UByte, width: UInt, height: UInt): FlatSamples<Buffer> =
        copy(layout = layout.shrinkTo(channels, width, height))

    public companion object {
        public fun <Buffer> from(samples: Buffer, layout: SampleLayout, colorHint: ColorType? = null): FlatSamples<Buffer> =
            FlatSamples(samples, layout, colorHint)

        public fun withMonocolor(pixel: Rgb<UByte>, width: UInt, height: UInt): FlatSamples<ByteArray> {
            val bytes = byteArrayOf(pixel.r.toByte(), pixel.g.toByte(), pixel.b.toByte())
            return FlatSamples(
                samples = bytes,
                layout =
                    SampleLayout(
                        channels = 3u,
                        channelStride = 0,
                        width = width,
                        widthStride = 0,
                        height = height,
                        heightStride = 0,
                    ),
                colorHint = ColorType.Rgb8,
            )
        }

        public fun withMonocolor(pixel: Rgba<UByte>, width: UInt, height: UInt): FlatSamples<ByteArray> {
            val bytes = byteArrayOf(pixel.r.toByte(), pixel.g.toByte(), pixel.b.toByte(), pixel.a.toByte())
            return FlatSamples(
                samples = bytes,
                layout =
                    SampleLayout(
                        channels = 4u,
                        channelStride = 0,
                        width = width,
                        widthStride = 0,
                        height = height,
                        heightStride = 0,
                    ),
                colorHint = ColorType.Rgba8,
            )
        }

        public fun withMonocolor(pixel: Luma<UByte>, width: UInt, height: UInt): FlatSamples<ByteArray> {
            val bytes = byteArrayOf(pixel.l.toByte())
            return FlatSamples(
                samples = bytes,
                layout =
                    SampleLayout(
                        channels = 1u,
                        channelStride = 0,
                        width = width,
                        widthStride = 0,
                        height = height,
                        heightStride = 0,
                    ),
                colorHint = ColorType.L8,
            )
        }

        public fun withMonocolor(pixel: LumaA<UByte>, width: UInt, height: UInt): FlatSamples<ByteArray> {
            val bytes = byteArrayOf(pixel.l.toByte(), pixel.a.toByte())
            return FlatSamples(
                samples = bytes,
                layout =
                    SampleLayout(
                        channels = 2u,
                        channelStride = 0,
                        width = width,
                        widthStride = 0,
                        height = height,
                        heightStride = 0,
                    ),
                colorHint = ColorType.La8,
            )
        }
    }
}

public fun FlatSamples<ByteArray>.getSample(channel: UByte, x: UInt, y: UInt): UByte? {
    val idx = index(channel, x, y) ?: return null
    return if (idx in samples.indices) samples[idx].toUByte() else null
}

public fun FlatSamples<ByteArray>.getSample(channel: Int, x: UInt, y: UInt): Byte {
    val idx = layout.sampleIndex(channel, x, y).toInt()
    return samples[idx]
}

public fun FlatSamples<ByteArray>.getMutSample(channel: UByte, x: UInt, y: UInt): UByte? {
    val idx = index(channel, x, y) ?: return null
    return if (idx in samples.indices) samples[idx].toUByte() else null
}

public fun FlatSamples<ByteArray>.setSample(channel: UByte, x: UInt, y: UInt, value: UByte): Boolean {
    val idx = index(channel, x, y) ?: return false
    if (idx !in samples.indices) return false
    samples[idx] = value.toByte()
    return true
}

public fun FlatSamples<ByteArray>.setSample(channel: Int, x: UInt, y: UInt, value: Byte) {
    val idx = layout.sampleIndex(channel, x, y).toInt()
    samples[idx] = value
}

public fun FlatSamples<ByteArray>.toVec(): FlatSamples<ByteArray> =
    FlatSamples(samples.copyOf(), layout, colorHint)

public fun FlatSamples<ByteArray>.imageSlice(): ByteArray? {
    val minLen = minLength() ?: return null
    if (samples.size < minLen) return null
    return samples.copyOfRange(0, minLen)
}

public fun FlatSamples<ByteArray>.imageMutSlice(): ByteArray? {
    val minLen = minLength() ?: return null
    if (samples.size < minLen) return null
    return samples.copyOfRange(0, minLen)
}

public fun FlatSamples<ByteArray>.asViewWithMutSamplesRgb(): Result<View<ByteArray, Rgb<UByte>>> = asViewRgb()

public fun FlatSamples<ByteArray>.asViewWithMutSamplesRgba(): Result<View<ByteArray, Rgba<UByte>>> = asViewRgba()

public fun FlatSamples<ByteArray>.asViewWithMutSamplesLuma(): Result<View<ByteArray, Luma<UByte>>> = asViewLuma()

public fun FlatSamples<ByteArray>.asViewWithMutSamplesLumaA(): Result<View<ByteArray, LumaA<UByte>>> = asViewLumaA()

public fun FlatSamples<ByteArray>.asViewRgb(): Result<View<ByteArray, Rgb<UByte>>> {
    if (layout.channels != 3.toUByte()) {
        return Result.failure(FlatError.ChannelCountMismatch(layout.channels, 3u).toImageError())
    }
    if (!layout.fits(samples.size)) {
        return Result.failure(FlatError.TooLarge.toImageError())
    }
    return Result.success(
        View(this) { flat, x, y ->
            val base = flat.layout.inBoundsIndex(0u, x, y)
            val cs = flat.layout.channelStride
            Rgb(
                flat.samples[base].toUByte(),
                flat.samples[base + cs].toUByte(),
                flat.samples[base + 2 * cs].toUByte(),
            )
        },
    )
}

public fun FlatSamples<ByteArray>.asViewRgba(): Result<View<ByteArray, Rgba<UByte>>> {
    if (layout.channels != 4.toUByte()) {
        return Result.failure(FlatError.ChannelCountMismatch(layout.channels, 4u).toImageError())
    }
    if (!layout.fits(samples.size)) {
        return Result.failure(FlatError.TooLarge.toImageError())
    }
    return Result.success(
        View(this) { flat, x, y ->
            val base = flat.layout.inBoundsIndex(0u, x, y)
            val cs = flat.layout.channelStride
            Rgba(
                flat.samples[base].toUByte(),
                flat.samples[base + cs].toUByte(),
                flat.samples[base + 2 * cs].toUByte(),
                flat.samples[base + 3 * cs].toUByte(),
            )
        },
    )
}

public fun FlatSamples<ByteArray>.asViewLuma(): Result<View<ByteArray, Luma<UByte>>> {
    if (layout.channels != 1.toUByte()) {
        return Result.failure(FlatError.ChannelCountMismatch(layout.channels, 1u).toImageError())
    }
    if (!layout.fits(samples.size)) {
        return Result.failure(FlatError.TooLarge.toImageError())
    }
    return Result.success(
        View(this) { flat, x, y ->
            val base = flat.layout.inBoundsIndex(0u, x, y)
            Luma(flat.samples[base].toUByte())
        },
    )
}

public fun FlatSamples<ByteArray>.asViewLumaA(): Result<View<ByteArray, LumaA<UByte>>> {
    if (layout.channels != 2.toUByte()) {
        return Result.failure(FlatError.ChannelCountMismatch(layout.channels, 2u).toImageError())
    }
    if (!layout.fits(samples.size)) {
        return Result.failure(FlatError.TooLarge.toImageError())
    }
    return Result.success(
        View(this) { flat, x, y ->
            val base = flat.layout.inBoundsIndex(0u, x, y)
            val cs = flat.layout.channelStride
            LumaA(
                flat.samples[base].toUByte(),
                flat.samples[base + cs].toUByte(),
            )
        },
    )
}

public fun FlatSamples<ByteArray>.asViewMutRgb(): Result<ViewMut<ByteArray, Rgb<UByte>>> {
    if (!layout.isNormal(NormalForm.PixelPacked)) {
        return Result.failure(FlatError.NormalFormRequired(NormalForm.PixelPacked).toImageError())
    }
    if (layout.channels != 3.toUByte()) {
        return Result.failure(FlatError.ChannelCountMismatch(layout.channels, 3u).toImageError())
    }
    if (!layout.fits(samples.size)) {
        return Result.failure(FlatError.TooLarge.toImageError())
    }
    return Result.success(
        ViewMut(
            inner = this,
            pixelReader = { flat, x, y ->
                val base = flat.layout.inBoundsIndex(0u, x, y)
                Rgb(
                    flat.samples[base].toUByte(),
                    flat.samples[base + 1].toUByte(),
                    flat.samples[base + 2].toUByte(),
                )
            },
            pixelWriter = { flat, x, y, p ->
                val base = flat.layout.inBoundsIndex(0u, x, y)
                flat.samples[base] = p.r.toByte()
                flat.samples[base + 1] = p.g.toByte()
                flat.samples[base + 2] = p.b.toByte()
            },
        ),
    )
}

public fun FlatSamples<ByteArray>.asViewMutRgba(): Result<ViewMut<ByteArray, Rgba<UByte>>> {
    if (!layout.isNormal(NormalForm.PixelPacked)) {
        return Result.failure(FlatError.NormalFormRequired(NormalForm.PixelPacked).toImageError())
    }
    if (layout.channels != 4.toUByte()) {
        return Result.failure(FlatError.ChannelCountMismatch(layout.channels, 4u).toImageError())
    }
    if (!layout.fits(samples.size)) {
        return Result.failure(FlatError.TooLarge.toImageError())
    }
    return Result.success(
        ViewMut(
            inner = this,
            pixelReader = { flat, x, y ->
                val base = flat.layout.inBoundsIndex(0u, x, y)
                Rgba(
                    flat.samples[base].toUByte(),
                    flat.samples[base + 1].toUByte(),
                    flat.samples[base + 2].toUByte(),
                    flat.samples[base + 3].toUByte(),
                )
            },
            pixelWriter = { flat, x, y, p ->
                val base = flat.layout.inBoundsIndex(0u, x, y)
                flat.samples[base] = p.r.toByte()
                flat.samples[base + 1] = p.g.toByte()
                flat.samples[base + 2] = p.b.toByte()
                flat.samples[base + 3] = p.a.toByte()
            },
            pixelBlender = { flat, x, y, p ->
                val base = flat.layout.inBoundsIndex(0u, x, y)
                val cur =
                    Rgba(
                        flat.samples[base].toUByte(),
                        flat.samples[base + 1].toUByte(),
                        flat.samples[base + 2].toUByte(),
                        flat.samples[base + 3].toUByte(),
                    )
                cur.blendUByte(p)
                flat.samples[base] = cur.r.toByte()
                flat.samples[base + 1] = cur.g.toByte()
                flat.samples[base + 2] = cur.b.toByte()
                flat.samples[base + 3] = cur.a.toByte()
            },
        ),
    )
}

public fun FlatSamples<ByteArray>.asViewMutLuma(): Result<ViewMut<ByteArray, Luma<UByte>>> {
    if (!layout.isNormal(NormalForm.PixelPacked)) {
        return Result.failure(FlatError.NormalFormRequired(NormalForm.PixelPacked).toImageError())
    }
    if (layout.channels != 1.toUByte()) {
        return Result.failure(FlatError.ChannelCountMismatch(layout.channels, 1u).toImageError())
    }
    if (!layout.fits(samples.size)) {
        return Result.failure(FlatError.TooLarge.toImageError())
    }
    return Result.success(
        ViewMut(
            inner = this,
            pixelReader = { flat, x, y ->
                val base = flat.layout.inBoundsIndex(0u, x, y)
                Luma(flat.samples[base].toUByte())
            },
            pixelWriter = { flat, x, y, p ->
                val base = flat.layout.inBoundsIndex(0u, x, y)
                flat.samples[base] = p.l.toByte()
            },
        ),
    )
}

public fun FlatSamples<ByteArray>.asViewMutLumaA(): Result<ViewMut<ByteArray, LumaA<UByte>>> {
    if (!layout.isNormal(NormalForm.PixelPacked)) {
        return Result.failure(FlatError.NormalFormRequired(NormalForm.PixelPacked).toImageError())
    }
    if (layout.channels != 2.toUByte()) {
        return Result.failure(FlatError.ChannelCountMismatch(layout.channels, 2u).toImageError())
    }
    if (!layout.fits(samples.size)) {
        return Result.failure(FlatError.TooLarge.toImageError())
    }
    return Result.success(
        ViewMut(
            inner = this,
            pixelReader = { flat, x, y ->
                val base = flat.layout.inBoundsIndex(0u, x, y)
                LumaA(
                    flat.samples[base].toUByte(),
                    flat.samples[base + 1].toUByte(),
                )
            },
            pixelWriter = { flat, x, y, p ->
                val base = flat.layout.inBoundsIndex(0u, x, y)
                flat.samples[base] = p.l.toByte()
                flat.samples[base + 1] = p.a.toByte()
            },
            pixelBlender = { flat, x, y, p ->
                val base = flat.layout.inBoundsIndex(0u, x, y)
                val cur =
                    LumaA(
                        flat.samples[base].toUByte(),
                        flat.samples[base + 1].toUByte(),
                    )
                cur.blendUByte(p)
                flat.samples[base] = cur.l.toByte()
                flat.samples[base + 1] = cur.a.toByte()
            },
        ),
    )
}

public fun FlatSamples<ByteArray>.tryIntoRgbImage(): Result<RgbImage> {
    if (!isNormal(NormalForm.RowMajorPacked)) {
        return Result.failure(FlatError.NormalFormRequired(NormalForm.RowMajorPacked).toImageError())
    }
    if (layout.channels != 3.toUByte()) {
        return Result.failure(FlatError.ChannelCountMismatch(layout.channels, 3u).toImageError())
    }
    if (!fits(samples.size)) {
        return Result.failure(FlatError.TooLarge.toImageError())
    }
    val img =
        ImageBuffer.createRgb(layout.width, layout.height, samples)
            ?: return Result.failure(FlatError.TooLarge.toImageError())
    return Result.success(img)
}

public fun FlatSamples<ByteArray>.tryIntoRgbaImage(): Result<RgbaImage> {
    if (!isNormal(NormalForm.RowMajorPacked)) {
        return Result.failure(FlatError.NormalFormRequired(NormalForm.RowMajorPacked).toImageError())
    }
    if (layout.channels != 4.toUByte()) {
        return Result.failure(FlatError.ChannelCountMismatch(layout.channels, 4u).toImageError())
    }
    if (!fits(samples.size)) {
        return Result.failure(FlatError.TooLarge.toImageError())
    }
    val img =
        ImageBuffer.createRgba(layout.width, layout.height, samples)
            ?: return Result.failure(FlatError.TooLarge.toImageError())
    return Result.success(img)
}

public fun FlatSamples<ByteArray>.tryIntoGrayImage(): Result<GrayImage> {
    if (!isNormal(NormalForm.RowMajorPacked)) {
        return Result.failure(FlatError.NormalFormRequired(NormalForm.RowMajorPacked).toImageError())
    }
    if (layout.channels != 1.toUByte()) {
        return Result.failure(FlatError.ChannelCountMismatch(layout.channels, 1u).toImageError())
    }
    if (!fits(samples.size)) {
        return Result.failure(FlatError.TooLarge.toImageError())
    }
    val img =
        ImageBuffer.createGray(layout.width, layout.height, samples)
            ?: return Result.failure(FlatError.TooLarge.toImageError())
    return Result.success(img)
}

public fun FlatSamples<ByteArray>.tryIntoGrayAlphaImage(): Result<GrayAlphaImage> {
    if (!isNormal(NormalForm.RowMajorPacked)) {
        return Result.failure(FlatError.NormalFormRequired(NormalForm.RowMajorPacked).toImageError())
    }
    if (layout.channels != 2.toUByte()) {
        return Result.failure(FlatError.ChannelCountMismatch(layout.channels, 2u).toImageError())
    }
    if (!fits(samples.size)) {
        return Result.failure(FlatError.TooLarge.toImageError())
    }
    val img =
        ImageBuffer.createGrayAlpha(layout.width, layout.height, samples)
            ?: return Result.failure(FlatError.TooLarge.toImageError())
    return Result.success(img)
}

public fun FlatSamples<ByteArray>.tryIntoBufferRgb(): Result<RgbImage> = tryIntoRgbImage()

public fun FlatSamples<ByteArray>.tryIntoBufferRgba(): Result<RgbaImage> = tryIntoRgbaImage()

public fun FlatSamples<ByteArray>.tryIntoBufferLuma(): Result<GrayImage> = tryIntoGrayImage()

public fun FlatSamples<ByteArray>.tryIntoBufferLumaA(): Result<GrayAlphaImage> = tryIntoGrayAlphaImage()

public fun FlatSamples<ByteArray>.tryIntoBuffer(): Result<RgbImage> = tryIntoRgbImage()

public fun FlatSamples<ByteArray>.asView(): Result<View<ByteArray, Rgb<UByte>>> = asViewRgb()

public fun FlatSamples<ByteArray>.asViewWithMutSamples(): Result<View<ByteArray, Rgb<UByte>>> = asViewWithMutSamplesRgb()

public fun FlatSamples<ByteArray>.asViewMut(): Result<ViewMut<ByteArray, Rgb<UByte>>> = asViewMutRgb()

/**
 * A flat buffer that can be used as an image view.
 */
public class View<Buffer, P>(
    public val inner: FlatSamples<Buffer>,
    private val pixelReader: (FlatSamples<Buffer>, UInt, UInt) -> P,
) : GenericImageView<P> {
    override fun dimensions(): Pair<UInt, UInt> = Pair(inner.layout.width, inner.layout.height)

    override fun width(): UInt = inner.layout.width

    override fun height(): UInt = inner.layout.height

    override fun inBounds(x: UInt, y: UInt): Boolean = inner.layout.inBounds(0u, x, y)

    override fun getPixel(x: UInt, y: UInt): P {
        require(inBounds(x, y)) { "Image index ($x, $y) out of bounds ${dimensions()}" }
        return pixelReader(inner, x, y)
    }

    public fun tryUpgrade(): Result<ViewMut<Buffer, P>> {
        if (!inner.layout.isNormal(NormalForm.PixelPacked)) {
            return Result.failure(FlatError.NormalFormRequired(NormalForm.PixelPacked).toImageError())
        }
        return Result.success(
            ViewMut(
                inner = inner,
                pixelReader = pixelReader,
                pixelWriter = { _, _, _, _ -> },
                pixelBlender = { _, _, _, _ -> },
            ),
        )
    }

    public fun flat(): FlatSamples<Buffer> = inner

    public fun samples(): Buffer = inner.samples

    public fun intoInner(): FlatSamples<Buffer> = inner

    public fun minLength(): Int = inner.minLength() ?: 0
}

/**
 * A mutable version of a flat buffer view.
 */
public class ViewMut<Buffer, P>(
    public val inner: FlatSamples<Buffer>,
    private val pixelReader: (FlatSamples<Buffer>, UInt, UInt) -> P,
    private val pixelWriter: (FlatSamples<Buffer>, UInt, UInt, P) -> Unit,
    private val pixelBlender: (FlatSamples<Buffer>, UInt, UInt, P) -> Unit = pixelWriter,
) : GenericImage<P> {
    override fun dimensions(): Pair<UInt, UInt> = Pair(inner.layout.width, inner.layout.height)

    override fun width(): UInt = inner.layout.width

    override fun height(): UInt = inner.layout.height

    override fun inBounds(x: UInt, y: UInt): Boolean = inner.layout.inBounds(0u, x, y)

    override fun getPixel(x: UInt, y: UInt): P {
        require(inBounds(x, y)) { "Image index ($x, $y) out of bounds ${dimensions()}" }
        return pixelReader(inner, x, y)
    }

    public fun getPixelMut(x: UInt, y: UInt): P = getPixel(x, y)

    override fun putPixel(x: UInt, y: UInt, pixel: P) {
        require(inBounds(x, y)) { "Image index ($x, $y) out of bounds ${dimensions()}" }
        pixelWriter(inner, x, y, pixel)
    }

    override fun blendPixel(x: UInt, y: UInt, pixel: P) {
        require(inBounds(x, y)) { "Image index ($x, $y) out of bounds ${dimensions()}" }
        pixelBlender(inner, x, y, pixel)
    }

    public fun flat(): FlatSamples<Buffer> = inner

    public fun samples(): Buffer = inner.samples

    public fun intoInner(): FlatSamples<Buffer> = inner

    public fun minLength(): Int = inner.minLength() ?: 0
}

public typealias Error = FlatError
public typealias Output = Any?
public typealias Pixel = Any?

public data class NormalFormRequiredError(
    public val form: NormalForm,
) : Exception("Required sample buffer in normal form $form")
