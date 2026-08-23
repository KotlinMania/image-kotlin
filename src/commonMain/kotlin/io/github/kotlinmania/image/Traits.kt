// port-lint: source traits.rs
package io.github.kotlinmania.image

/**
 * Types which are safe to treat as an immutable byte slice in a pixel layout
 * for image encoding.
 */
public interface EncodableLayout {
    /**
     * Get the bytes of this value.
     */
    public fun asBytes(): ByteArray
}

public class ByteArrayEncodableLayout(
    private val array: ByteArray,
) : EncodableLayout {
    override fun asBytes(): ByteArray = array
}

public class ShortArrayEncodableLayout(
    private val array: ShortArray,
) : EncodableLayout {
    override fun asBytes(): ByteArray {
        val result = ByteArray(array.size * 2)
        for (i in array.indices) {
            val v = array[i].toInt()
            result[i * 2] = (v and 0xFF).toByte()
            result[i * 2 + 1] = ((v ushr 8) and 0xFF).toByte()
        }
        return result
    }
}

public class FloatArrayEncodableLayout(
    private val array: FloatArray,
) : EncodableLayout {
    override fun asBytes(): ByteArray {
        val result = ByteArray(array.size * 4)
        for (i in array.indices) {
            val bits = array[i].toRawBits()
            result[i * 4] = (bits and 0xFF).toByte()
            result[i * 4 + 1] = ((bits ushr 8) and 0xFF).toByte()
            result[i * 4 + 2] = ((bits ushr 16) and 0xFF).toByte()
            result[i * 4 + 3] = ((bits ushr 24) and 0xFF).toByte()
        }
        return result
    }
}

/**
 * The type of each channel in a pixel. For example, this can be `UByte`, `UShort`, `Float`.
 */
public interface Primitive<T : Comparable<T>> {
    public val defaultMaxValue: T
    public val defaultMinValue: T
}

public object UBytePrimitive : Primitive<UByte> {
    override val defaultMaxValue: UByte = UByte.MAX_VALUE
    override val defaultMinValue: UByte = UByte.MIN_VALUE
}

public object UShortPrimitive : Primitive<UShort> {
    override val defaultMaxValue: UShort = UShort.MAX_VALUE
    override val defaultMinValue: UShort = UShort.MIN_VALUE
}

public object UIntPrimitive : Primitive<UInt> {
    override val defaultMaxValue: UInt = UInt.MAX_VALUE
    override val defaultMinValue: UInt = UInt.MIN_VALUE
}

public object ULongPrimitive : Primitive<ULong> {
    override val defaultMaxValue: ULong = ULong.MAX_VALUE
    override val defaultMinValue: ULong = ULong.MIN_VALUE
}

public object BytePrimitive : Primitive<Byte> {
    override val defaultMaxValue: Byte = Byte.MAX_VALUE
    override val defaultMinValue: Byte = Byte.MIN_VALUE
}

public object ShortPrimitive : Primitive<Short> {
    override val defaultMaxValue: Short = Short.MAX_VALUE
    override val defaultMinValue: Short = Short.MIN_VALUE
}

public object IntPrimitive : Primitive<Int> {
    override val defaultMaxValue: Int = Int.MAX_VALUE
    override val defaultMinValue: Int = Int.MIN_VALUE
}

public object LongPrimitive : Primitive<Long> {
    override val defaultMaxValue: Long = Long.MAX_VALUE
    override val defaultMinValue: Long = Long.MIN_VALUE
}

public object FloatPrimitive : Primitive<Float> {
    override val defaultMaxValue: Float = 1.0f
    override val defaultMinValue: Float = 0.0f
}

public object DoublePrimitive : Primitive<Double> {
    override val defaultMaxValue: Double = 1.0
    override val defaultMinValue: Double = 0.0
}

/**
 * An `Enlargeable` value should be enough to calculate the sum (average) of a few hundred or thousand values.
 */
public interface Enlargeable<T, Larger> {
    public fun clampFrom(n: Larger): T

    public fun toLarger(value: T): Larger
}

public object UByteEnlargeable : Enlargeable<UByte, UInt> {
    override fun clampFrom(n: UInt): UByte = n.coerceIn(0u, 255u).toUByte()

    override fun toLarger(value: UByte): UInt = value.toUInt()
}

public object UShortEnlargeable : Enlargeable<UShort, UInt> {
    override fun clampFrom(n: UInt): UShort = n.coerceIn(0u, 65535u).toUShort()

    override fun toLarger(value: UShort): UInt = value.toUInt()
}

public object UIntEnlargeable : Enlargeable<UInt, ULong> {
    override fun clampFrom(n: ULong): UInt = n.coerceIn(0uL, UInt.MAX_VALUE.toULong()).toUInt()

    override fun toLarger(value: UInt): ULong = value.toULong()
}

public object FloatEnlargeable : Enlargeable<Float, Double> {
    override fun clampFrom(n: Double): Float = n.toFloat()

    override fun toLarger(value: Float): Double = value.toDouble()
}

/**
 * Linear interpolation without involving floating numbers.
 */
public interface Lerp<T, Ratio> {
    public fun lerp(a: T, b: T, ratio: Ratio): T
}

public object UByteLerp : Lerp<UByte, Float> {
    override fun lerp(a: UByte, b: UByte, ratio: Float): UByte {
        val af = a.toFloat()
        val bf = b.toFloat()
        val res = af + (bf - af) * ratio
        return res.coerceIn(0.0f, 255.0f).toInt().toUByte()
    }
}

public object UShortLerp : Lerp<UShort, Float> {
    override fun lerp(a: UShort, b: UShort, ratio: Float): UShort {
        val af = a.toFloat()
        val bf = b.toFloat()
        val res = af + (bf - af) * ratio
        return res.coerceIn(0.0f, 65535.0f).toInt().toUShort()
    }
}

public object FloatLerp : Lerp<Float, Float> {
    override fun lerp(a: Float, b: Float, ratio: Float): Float = a + (b - a) * ratio
}

/**
 * The pixel with an associated [ExtendedColorType].
 */
public interface PixelWithColorType {
    /**
     * This pixel has the format of one of the predefined [ExtendedColorType]s.
     */
    public val colorType: ExtendedColorType
}

/**
 * A generalized pixel.
 */
public interface Pixel<Subpixel> {
    /** The number of channels of this pixel type. */
    public val channelCount: UByte

    /** Returns the components as a list. */
    public fun channels(): List<Subpixel>

    /** A string that can help interpret the meaning of each channel. */
    public val colorModel: String

    /** Returns true if the alpha channel is contained. */
    public val hasAlpha: Boolean get() = false

    /** Retrieve the value of the alpha channel for this pixel. */
    public fun alpha(): Subpixel

    /** Convert this pixel to RGB. */
    public fun toRgb(): Rgb<Subpixel>

    /** Convert this pixel to RGB with an alpha channel. */
    public fun toRgba(): Rgba<Subpixel>

    /** Convert this pixel to luma. */
    public fun toLuma(): Luma<Subpixel>

    /** Convert this pixel to luma with an alpha channel. */
    public fun toLumaAlpha(): LumaA<Subpixel>

    /** Apply the function [f] to each channel of this pixel. */
    public fun map(f: (Subpixel) -> Subpixel): Pixel<Subpixel>

    /** Apply the function [f] to each channel except the alpha channel, and [g] to the alpha channel. */
    public fun mapWithAlpha(f: (Subpixel) -> Subpixel, g: (Subpixel) -> Subpixel): Pixel<Subpixel>

    /** Invert this pixel. */
    public fun invert()

    /** Blend the color of a given pixel into this one, taking into account alpha channels. */
    public fun blend(other: Pixel<Subpixel>)
}
