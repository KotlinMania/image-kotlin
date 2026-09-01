// port-lint: source codecs/jpeg/encoder.rs
package io.github.kotlinmania.image.codecs.jpeg

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.EncodingError
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.images.DynamicImage
import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoErrorKind
import io.github.kotlinmania.image.io.IoException
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.MethodSealedToImage
import io.github.kotlinmania.image.io.writeAll
import kotlin.math.roundToInt

// Markers
private const val SOF0: UByte = 0xC0u
private const val DHT: UByte = 0xC4u
private const val SOI: UByte = 0xD8u
private const val EOI: UByte = 0xD9u
private const val SOS: UByte = 0xDAu
private const val DQT: UByte = 0xDBu
private const val APP0: UByte = 0xE0u
private const val APP1: UByte = 0xE1u
private const val APP2: UByte = 0xE2u

// Section K.1 Table K.1
private val STD_LUMA_QTABLE =
    ubyteArrayOf(
        16u,
        11u,
        10u,
        16u,
        24u,
        40u,
        51u,
        61u,
        12u,
        12u,
        14u,
        19u,
        26u,
        58u,
        60u,
        55u,
        14u,
        13u,
        16u,
        24u,
        40u,
        57u,
        69u,
        56u,
        14u,
        17u,
        22u,
        29u,
        51u,
        87u,
        80u,
        62u,
        18u,
        22u,
        37u,
        56u,
        68u,
        109u,
        103u,
        77u,
        24u,
        35u,
        55u,
        64u,
        81u,
        104u,
        113u,
        92u,
        49u,
        64u,
        78u,
        87u,
        103u,
        121u,
        120u,
        101u,
        72u,
        92u,
        95u,
        98u,
        112u,
        100u,
        103u,
        99u,
    )

// Table K.2
private val STD_CHROMA_QTABLE =
    ubyteArrayOf(
        17u,
        18u,
        24u,
        47u,
        99u,
        99u,
        99u,
        99u,
        18u,
        21u,
        26u,
        66u,
        99u,
        99u,
        99u,
        99u,
        24u,
        26u,
        56u,
        99u,
        99u,
        99u,
        99u,
        99u,
        47u,
        66u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
        99u,
    )

// Code lengths and values for Table K.3
internal val STD_LUMA_DC_CODE_LENGTHS =
    ubyteArrayOf(
        0x00u,
        0x01u,
        0x05u,
        0x01u,
        0x01u,
        0x01u,
        0x01u,
        0x01u,
        0x01u,
        0x00u,
        0x00u,
        0x00u,
        0x00u,
        0x00u,
        0x00u,
        0x00u,
    )

internal val STD_LUMA_DC_VALUES =
    ubyteArrayOf(
        0x00u,
        0x01u,
        0x02u,
        0x03u,
        0x04u,
        0x05u,
        0x06u,
        0x07u,
        0x08u,
        0x09u,
        0x0Au,
        0x0Bu,
    )

private val STD_LUMA_DC_HUFF_LUT: Array<Pair<UByte, UShort>> =
    buildHuffLutConst(STD_LUMA_DC_CODE_LENGTHS, STD_LUMA_DC_VALUES)

// Code lengths and values for Table K.4
private val STD_CHROMA_DC_CODE_LENGTHS =
    ubyteArrayOf(
        0x00u,
        0x03u,
        0x01u,
        0x01u,
        0x01u,
        0x01u,
        0x01u,
        0x01u,
        0x01u,
        0x01u,
        0x01u,
        0x00u,
        0x00u,
        0x00u,
        0x00u,
        0x00u,
    )

private val STD_CHROMA_DC_VALUES =
    ubyteArrayOf(
        0x00u,
        0x01u,
        0x02u,
        0x03u,
        0x04u,
        0x05u,
        0x06u,
        0x07u,
        0x08u,
        0x09u,
        0x0Au,
        0x0Bu,
    )

private val STD_CHROMA_DC_HUFF_LUT: Array<Pair<UByte, UShort>> =
    buildHuffLutConst(STD_CHROMA_DC_CODE_LENGTHS, STD_CHROMA_DC_VALUES)

// Code lengths and values for Table K.5
private val STD_LUMA_AC_CODE_LENGTHS =
    ubyteArrayOf(
        0x00u,
        0x02u,
        0x01u,
        0x03u,
        0x03u,
        0x02u,
        0x04u,
        0x03u,
        0x05u,
        0x05u,
        0x04u,
        0x04u,
        0x00u,
        0x00u,
        0x01u,
        0x7Du,
    )

private val STD_LUMA_AC_VALUES =
    ubyteArrayOf(
        0x01u,
        0x02u,
        0x03u,
        0x00u,
        0x04u,
        0x11u,
        0x05u,
        0x12u,
        0x21u,
        0x31u,
        0x41u,
        0x06u,
        0x13u,
        0x51u,
        0x61u,
        0x07u,
        0x22u,
        0x71u,
        0x14u,
        0x32u,
        0x81u,
        0x91u,
        0xA1u,
        0x08u,
        0x23u,
        0x42u,
        0xB1u,
        0xC1u,
        0x15u,
        0x52u,
        0xD1u,
        0xF0u,
        0x24u,
        0x33u,
        0x62u,
        0x72u,
        0x82u,
        0x09u,
        0x0Au,
        0x16u,
        0x17u,
        0x18u,
        0x19u,
        0x1Au,
        0x25u,
        0x26u,
        0x27u,
        0x28u,
        0x29u,
        0x2Au,
        0x34u,
        0x35u,
        0x36u,
        0x37u,
        0x38u,
        0x39u,
        0x3Au,
        0x43u,
        0x44u,
        0x45u,
        0x46u,
        0x47u,
        0x48u,
        0x49u,
        0x4Au,
        0x53u,
        0x54u,
        0x55u,
        0x56u,
        0x57u,
        0x58u,
        0x59u,
        0x5Au,
        0x63u,
        0x64u,
        0x65u,
        0x66u,
        0x67u,
        0x68u,
        0x69u,
        0x6Au,
        0x73u,
        0x74u,
        0x75u,
        0x76u,
        0x77u,
        0x78u,
        0x79u,
        0x7Au,
        0x83u,
        0x84u,
        0x85u,
        0x86u,
        0x87u,
        0x88u,
        0x89u,
        0x8Au,
        0x92u,
        0x93u,
        0x94u,
        0x95u,
        0x96u,
        0x97u,
        0x98u,
        0x99u,
        0x9Au,
        0xA2u,
        0xA3u,
        0xA4u,
        0xA5u,
        0xA6u,
        0xA7u,
        0xA8u,
        0xA9u,
        0xAAu,
        0xB2u,
        0xB3u,
        0xB4u,
        0xB5u,
        0xB6u,
        0xB7u,
        0xB8u,
        0xB9u,
        0xBAu,
        0xC2u,
        0xC3u,
        0xC4u,
        0xC5u,
        0xC6u,
        0xC7u,
        0xC8u,
        0xC9u,
        0xCAu,
        0xD2u,
        0xD3u,
        0xD4u,
        0xD5u,
        0xD6u,
        0xD7u,
        0xD8u,
        0xD9u,
        0xDAu,
        0xE1u,
        0xE2u,
        0xE3u,
        0xE4u,
        0xE5u,
        0xE6u,
        0xE7u,
        0xE8u,
        0xE9u,
        0xEAu,
        0xF1u,
        0xF2u,
        0xF3u,
        0xF4u,
        0xF5u,
        0xF6u,
        0xF7u,
        0xF8u,
        0xF9u,
        0xFAu,
    )

private val STD_LUMA_AC_HUFF_LUT: Array<Pair<UByte, UShort>> =
    buildHuffLutConst(STD_LUMA_AC_CODE_LENGTHS, STD_LUMA_AC_VALUES)

// Code lengths and values for Table K.6
private val STD_CHROMA_AC_CODE_LENGTHS =
    ubyteArrayOf(
        0x00u,
        0x02u,
        0x01u,
        0x02u,
        0x04u,
        0x04u,
        0x03u,
        0x04u,
        0x07u,
        0x05u,
        0x04u,
        0x04u,
        0x00u,
        0x01u,
        0x02u,
        0x77u,
    )

private val STD_CHROMA_AC_VALUES =
    ubyteArrayOf(
        0x00u,
        0x01u,
        0x02u,
        0x03u,
        0x11u,
        0x04u,
        0x05u,
        0x21u,
        0x31u,
        0x06u,
        0x12u,
        0x41u,
        0x51u,
        0x07u,
        0x61u,
        0x71u,
        0x13u,
        0x22u,
        0x32u,
        0x81u,
        0x08u,
        0x14u,
        0x42u,
        0x91u,
        0xA1u,
        0xB1u,
        0xC1u,
        0x09u,
        0x23u,
        0x33u,
        0x52u,
        0xF0u,
        0x15u,
        0x62u,
        0x72u,
        0xD1u,
        0x0Au,
        0x16u,
        0x24u,
        0x34u,
        0xE1u,
        0x25u,
        0xF1u,
        0x17u,
        0x18u,
        0x19u,
        0x1Au,
        0x26u,
        0x27u,
        0x28u,
        0x29u,
        0x2Au,
        0x35u,
        0x36u,
        0x37u,
        0x38u,
        0x39u,
        0x3Au,
        0x43u,
        0x44u,
        0x45u,
        0x46u,
        0x47u,
        0x48u,
        0x49u,
        0x4Au,
        0x53u,
        0x54u,
        0x55u,
        0x56u,
        0x57u,
        0x58u,
        0x59u,
        0x5Au,
        0x63u,
        0x64u,
        0x65u,
        0x66u,
        0x67u,
        0x68u,
        0x69u,
        0x6Au,
        0x73u,
        0x74u,
        0x75u,
        0x76u,
        0x77u,
        0x78u,
        0x79u,
        0x7Au,
        0x82u,
        0x83u,
        0x84u,
        0x85u,
        0x86u,
        0x87u,
        0x88u,
        0x89u,
        0x8Au,
        0x92u,
        0x93u,
        0x94u,
        0x95u,
        0x96u,
        0x97u,
        0x98u,
        0x99u,
        0x9Au,
        0xA2u,
        0xA3u,
        0xA4u,
        0xA5u,
        0xA6u,
        0xA7u,
        0xA8u,
        0xA9u,
        0xAAu,
        0xB2u,
        0xB3u,
        0xB4u,
        0xB5u,
        0xB6u,
        0xB7u,
        0xB8u,
        0xB9u,
        0xBAu,
        0xC2u,
        0xC3u,
        0xC4u,
        0xC5u,
        0xC6u,
        0xC7u,
        0xC8u,
        0xC9u,
        0xCAu,
        0xD2u,
        0xD3u,
        0xD4u,
        0xD5u,
        0xD6u,
        0xD7u,
        0xD8u,
        0xD9u,
        0xDAu,
        0xE2u,
        0xE3u,
        0xE4u,
        0xE5u,
        0xE6u,
        0xE7u,
        0xE8u,
        0xE9u,
        0xEAu,
        0xF2u,
        0xF3u,
        0xF4u,
        0xF5u,
        0xF6u,
        0xF7u,
        0xF8u,
        0xF9u,
        0xFAu,
    )

private val STD_CHROMA_AC_HUFF_LUT: Array<Pair<UByte, UShort>> =
    buildHuffLutConst(STD_CHROMA_AC_CODE_LENGTHS, STD_CHROMA_AC_VALUES)

internal const val DCCLASS: UByte = 0u
internal const val ACCLASS: UByte = 1u

internal const val LUMADESTINATION: UByte = 0u
internal const val CHROMADESTINATION: UByte = 1u

internal const val LUMAID: UByte = 1u
internal const val CHROMABLUEID: UByte = 2u
internal const val CHROMAREDID: UByte = 3u

private val UNZIGZAG =
    intArrayOf(
        0,
        1,
        8,
        16,
        9,
        2,
        3,
        10,
        17,
        24,
        32,
        25,
        18,
        11,
        4,
        5,
        12,
        19,
        26,
        33,
        40,
        48,
        41,
        34,
        27,
        20,
        13,
        6,
        7,
        14,
        21,
        28,
        35,
        42,
        49,
        56,
        57,
        50,
        43,
        36,
        29,
        22,
        15,
        23,
        30,
        37,
        44,
        51,
        58,
        59,
        52,
        45,
        38,
        31,
        39,
        46,
        53,
        60,
        61,
        54,
        47,
        55,
        62,
        63,
    )

private val EXIF_HEADER = byteArrayOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00)

internal data class Component(
    val id: UByte,
    val h: UByte,
    val v: UByte,
    val tq: UByte,
    val dcTable: UByte,
    val acTable: UByte,
    val dcPred: Int = 0,
)

internal class BitWriter(
    private val writer: IoWrite,
) {
    private var accumulator: UInt = 0u
    private var nbits: Int = 0

    fun writeBits(bits: UShort, size: UByte) {
        if (size == 0.toUByte()) {
            return
        }
        val s = size.toInt()
        nbits += s
        accumulator = accumulator or (bits.toUInt() shl (32 - nbits))

        while (nbits >= 8) {
            val byte = (accumulator shr 24).toUByte().toByte()
            writer.writeAll(byteArrayOf(byte))
            if (byte == 0xFF.toByte()) {
                writer.writeAll(byteArrayOf(0x00))
            }
            nbits -= 8
            accumulator = accumulator shl 8
        }
    }

    fun padByte() {
        writeBits(0x7Fu.toUShort(), 7u)
    }

    fun huffmanEncode(value: UByte, table: Array<Pair<UByte, UShort>>) {
        val (size, code) = table[value.toInt()]
        check(size <= 16u) { "bad huffman value" }
        writeBits(code, size)
    }

    fun writeBlock(
        block: IntArray,
        prevdc: Int,
        dctable: Array<Pair<UByte, UShort>>,
        actable: Array<Pair<UByte, UShort>>,
    ): Int {
        val dcval = block[0]
        val diff = dcval - prevdc
        val (size, value) = encodeCoefficient(diff)

        huffmanEncode(size, dctable)
        writeBits(value, size)

        var zeroRun = 0
        for (i in 1 until 64) {
            val k = UNZIGZAG[i]
            if (block[k] == 0) {
                zeroRun += 1
            } else {
                while (zeroRun > 15) {
                    huffmanEncode(0xF0u, actable)
                    zeroRun -= 16
                }
                val (coeffSize, coeffValue) = encodeCoefficient(block[k])
                val symbol = ((zeroRun shl 4) or coeffSize.toInt()).toUByte()
                huffmanEncode(symbol, actable)
                writeBits(coeffValue, coeffSize)
                zeroRun = 0
            }
        }

        if (block[UNZIGZAG[63]] == 0) {
            huffmanEncode(0x00u, actable)
        }

        return dcval
    }

    fun writeMarker(marker: UByte) {
        writer.writeAll(byteArrayOf(0xFF.toByte(), marker.toByte()))
    }

    fun writeSegment(marker: UByte, data: ByteArray) {
        writer.writeAll(byteArrayOf(0xFF.toByte(), marker.toByte()))
        val len = data.size + 2
        val lenBytes = byteArrayOf(((len shr 8) and 0xFF).toByte(), (len and 0xFF).toByte())
        writer.writeAll(lenBytes)
        writer.writeAll(data)
    }
}

/**
 * Represents a unit in which the density of an image is measured.
 */
public enum class PixelDensityUnit {
    /** Absence of a unit, values indicate pixel aspect ratio. */
    PixelAspectRatio,

    /** Pixels per inch (2.54 cm). */
    Inches,

    /** Pixels per centimeter. */
    Centimeters,
}

/**
 * Represents the pixel density of an image.
 */
public class PixelDensity(
    public val xDensity: UShort,
    public val yDensity: UShort,
    public val unit: PixelDensityUnit,
) {
    internal constructor(density: Pair<UShort, UShort>, unit: PixelDensityUnit) : this(density.first, density.second, unit)

    public companion object {
        /**
         * Creates a pixel density where horizontal and vertical densities are equal, in DPI.
         */
        public fun dpi(density: UShort): PixelDensity =
            PixelDensity(density, density, PixelDensityUnit.Inches)

        /**
         * Default pixel density with aspect ratio 1:1.
         */
        public fun defaultDensity(): PixelDensity =
            PixelDensity(1u.toUShort(), 1u.toUShort(), PixelDensityUnit.PixelAspectRatio)

        /**
         * Default pixel density instance.
         */
        public fun default(): PixelDensity = defaultDensity()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PixelDensity) return false
        return xDensity == other.xDensity && yDensity == other.yDensity && unit == other.unit
    }

    override fun hashCode(): Int {
        var result = xDensity.hashCode()
        result = 31 * result + yDensity.hashCode()
        result = 31 * result + unit.hashCode()
        return result
    }

    override fun toString(): String = "PixelDensity(density=($xDensity, $yDensity), unit=$unit)"
}

/**
 * Errors that can occur when encoding a JPEG image.
 */
public sealed class EncoderError : Exception() {
    /**
     * Formats the encoder error.
     */
    public abstract fun fmt(): String

    /**
     * JPEG does not support this image size.
     */
    public data class InvalidSize(
        public val width: UInt,
        public val height: UInt,
    ) : EncoderError() {
        /**
         * Formats the invalid size error.
         */
        override fun fmt(): String =
            "Invalid image size ($width x $height) to encode as JPEG: width and height must be >= 1 and <= 65535"

        override val message: String get() = fmt()
        override fun toString(): String = fmt()
    }

    /**
     * Converts this encoder error to a generic [ImageError].
     */
    public fun toImageError(): ImageError =
        ImageError.Encoding(EncodingError.new(ImageFormatHint.Exact(ImageFormat.Jpeg), this))

    public companion object {
        /**
         * Converts an [EncoderError] to [ImageError].
         */
        public fun from(e: EncoderError): ImageError = e.toImageError()
    }
}

internal fun encodeCoefficient(coefficient: Int): Pair<UByte, UShort> {
    val magnitude = kotlin.math.abs(coefficient)
    var temp = magnitude
    var numBits: UByte = 0u

    while (temp > 0) {
        temp = temp shr 1
        numBits = (numBits.toInt() + 1).toUByte()
    }

    val mask = (1 shl numBits.toInt()) - 1
    val value =
        if (coefficient < 0) {
            ((coefficient - 1) and mask).toUShort()
        } else {
            (coefficient and mask).toUShort()
        }

    return numBits to value
}

internal fun buildJfifHeader(density: PixelDensity): ByteArray {
    val result = mutableListOf<Byte>()
    result.add('J'.code.toByte())
    result.add('F'.code.toByte())
    result.add('I'.code.toByte())
    result.add('F'.code.toByte())
    result.add(0)
    result.add(0x01)
    result.add(0x02)
    result.add(
        when (density.unit) {
            PixelDensityUnit.PixelAspectRatio -> 0x00
            PixelDensityUnit.Inches -> 0x01
            PixelDensityUnit.Centimeters -> 0x02
        }.toByte(),
    )
    val dx = density.xDensity.toInt()
    result.add(((dx shr 8) and 0xFF).toByte())
    result.add((dx and 0xFF).toByte())
    val dy = density.yDensity.toInt()
    result.add(((dy shr 8) and 0xFF).toByte())
    result.add((dy and 0xFF).toByte())
    result.add(0)
    result.add(0)
    return result.toByteArray()
}

internal fun buildFrameHeader(
    precision: UByte,
    width: UShort,
    height: UShort,
    components: List<Component>,
): ByteArray {
    val result = mutableListOf<Byte>()
    result.add(precision.toByte())
    val h = height.toInt()
    result.add(((h shr 8) and 0xFF).toByte())
    result.add((h and 0xFF).toByte())
    val w = width.toInt()
    result.add(((w shr 8) and 0xFF).toByte())
    result.add((w and 0xFF).toByte())
    result.add(components.size.toByte())

    for (comp in components) {
        val hv = ((comp.h.toInt() shl 4) or comp.v.toInt()).toByte()
        result.add(comp.id.toByte())
        result.add(hv)
        result.add(comp.tq.toByte())
    }
    return result.toByteArray()
}

internal fun buildScanHeader(components: List<Component>): ByteArray {
    val result = mutableListOf<Byte>()
    result.add(components.size.toByte())

    for (comp in components) {
        val tables = ((comp.dcTable.toInt() shl 4) or comp.acTable.toInt()).toByte()
        result.add(comp.id.toByte())
        result.add(tables)
    }

    result.add(0)
    result.add(63)
    result.add(0)
    return result.toByteArray()
}

internal fun buildHuffmanSegment(
    cls: UByte,
    destination: UByte,
    numcodes: UByteArray,
    values: UByteArray,
): ByteArray {
    val result = mutableListOf<Byte>()
    val tcth = ((cls.toInt() shl 4) or destination.toInt()).toByte()
    result.add(tcth)

    for (b in numcodes) {
        result.add(b.toByte())
    }
    for (v in values) {
        result.add(v.toByte())
    }
    return result.toByteArray()
}

internal fun buildQuantizationSegment(precision: UByte, identifier: UByte, qtable: UByteArray): ByteArray {
    val result = mutableListOf<Byte>()
    val p = if (precision == 8.toUByte()) 0 else 1
    val pqtq = ((p shl 4) or identifier.toInt()).toByte()
    result.add(pqtq)

    for (i in UNZIGZAG) {
        result.add(qtable[i].toByte())
    }
    return result.toByteArray()
}

internal fun rgbToYcbcr(r: Int, g: Int, b: Int): Triple<UByte, UByte, UByte> {
    val cYr = 19595
    val cYg = 38469
    val cYb = 7471
    val yRounding = (1 shl 15) - 1
    val cUr = 11059
    val cUg = 21709
    val cUb = 32768
    val uvBiasRounding = (128 * (1 shl 16)) + ((1 shl 15) - 1)
    val cVr = cUb
    val cVg = 27439
    val cVb = 5329

    val y = (cYr * r + cYg * g + cYb * b + yRounding) shr 16
    val cb = (-cUr * r - cUg * g + cUb * b + uvBiasRounding) shr 16
    val cr = (cVr * r - cVg * g - cVb * b + uvBiasRounding) shr 16

    return Triple(y.coerceIn(0, 255).toUByte(), cb.coerceIn(0, 255).toUByte(), cr.coerceIn(0, 255).toUByte())
}

internal fun pixelAtOrNear(image: ByteArray, width: Int, height: Int, channels: Int, x: Int, y: Int): ByteArray {
    val clampedX = x.coerceIn(0, width - 1)
    val clampedY = y.coerceIn(0, height - 1)
    val offset = (clampedY * width + clampedX) * channels
    return image.copyOfRange(offset, offset + channels)
}

internal fun copyBlocksYcbcr(
    image: ByteArray,
    width: Int,
    height: Int,
    x0: Int,
    y0: Int,
    yb: UByteArray,
    cbb: UByteArray,
    crb: UByteArray,
) {
    for (y in 0 until 8) {
        for (x in 0 until 8) {
            val px = pixelAtOrNear(image, width, height, 3, x0 + x, y0 + y)
            val r = px[0].toInt() and 0xFF
            val g = px[1].toInt() and 0xFF
            val b = px[2].toInt() and 0xFF
            val (yc, cb, cr) = rgbToYcbcr(r, g, b)
            val idx = y * 8 + x
            yb[idx] = yc
            cbb[idx] = cb
            crb[idx] = cr
        }
    }
}

internal fun copyBlocksGray(
    image: ByteArray,
    width: Int,
    height: Int,
    x0: Int,
    y0: Int,
    gb: UByteArray,
) {
    for (y in 0 until 8) {
        for (x in 0 until 8) {
            val px = pixelAtOrNear(image, width, height, 1, x0 + x, y0 + y)
            val idx = y * 8 + x
            gb[idx] = (px[0].toInt() and 0xFF).toUByte()
        }
    }
}

/**
 * Representation of a JPEG encoder.
 */
public class JpegEncoder(
    private val writer: IoWrite,
    private val quality: UByte = 75u,
) : ImageEncoder {
    public constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite, 75u)

    public constructor(writeBuffer: BufferIoWrite, quality: UByte) : this(writeBuffer as IoWrite, quality)

    public fun writeExif() {
        if (exifMetadata.isNotEmpty()) {
            val formatted = EXIF_HEADER + exifMetadata
            bitWriter.writeSegment(APP1, formatted)
        }
    }

    /**
     * Encodes a dynamic image to the JPEG writer.
     */
    public fun encodeImage(image: DynamicImage) {
        val w = image.width()
        val h = image.height()
        when (val color = image.color()) {
            ColorType.L8 -> encode(image.asBytes(), w, h, ExtendedColorType.L8)
            ColorType.Rgb8 -> encode(image.asBytes(), w, h, ExtendedColorType.Rgb8)
            ColorType.Rgba8 -> encode(image.asBytes(), w, h, ExtendedColorType.Rgba8)
            ColorType.La8, ColorType.L16, ColorType.La16 -> {
                val luma = DynamicImage.ImageLuma8(image.toLuma8())
                encode(luma.asBytes(), w, h, ExtendedColorType.L8)
            }
            ColorType.Rgb16, ColorType.Rgba16, ColorType.Rgb32F, ColorType.Rgba32F -> {
                val rgb = DynamicImage.ImageRgb8(image.toRgb8())
                encode(rgb.asBytes(), w, h, ExtendedColorType.Rgb8)
            }
        }
    }

    /**
     * Encodes the image as grayscale.
     */
    public fun encodeGray(image: DynamicImage) {
        val w = image.width()
        val h = image.height()
        val luma = if (image.color() == ColorType.L8) image else DynamicImage.ImageLuma8(image.toLuma8())
        encode(luma.asBytes(), w, h, ExtendedColorType.L8)
    }

    /**
     * Encodes the image as RGB.
     */
    public fun encodeRgb(image: DynamicImage) {
        val w = image.width()
        val h = image.height()
        val rgb = if (image.color() == ColorType.Rgb8) image else DynamicImage.ImageRgb8(image.toRgb8())
        encode(rgb.asBytes(), w, h, ExtendedColorType.Rgb8)
    }

    private val bitWriter = BitWriter(writer)
    private var pixelDensity: PixelDensity = PixelDensity.defaultDensity()
    private var iccProfile: ByteArray = ByteArray(0)
    private var exifMetadata: ByteArray = ByteArray(0)

    private val components =
        listOf(
            Component(id = LUMAID, h = 1u, v = 1u, tq = LUMADESTINATION, dcTable = LUMADESTINATION, acTable = LUMADESTINATION),
            Component(id = CHROMABLUEID, h = 1u, v = 1u, tq = CHROMADESTINATION, dcTable = CHROMADESTINATION, acTable = CHROMADESTINATION),
            Component(id = CHROMAREDID, h = 1u, v = 1u, tq = CHROMADESTINATION, dcTable = CHROMADESTINATION, acTable = CHROMADESTINATION),
        )

    private val tables: List<UByteArray> =
        run {
            val q = quality.toInt().coerceIn(1, 100)
            val scale = if (q < 50) 5000 / q else 200 - q * 2
            val lumaTable = UByteArray(64)
            val chromaTable = UByteArray(64)
            for (i in 0 until 64) {
                lumaTable[i] = ((STD_LUMA_QTABLE[i].toInt() * scale + 50) / 100).coerceIn(1, 255).toUByte()
                chromaTable[i] = ((STD_CHROMA_QTABLE[i].toInt() * scale + 50) / 100).coerceIn(1, 255).toUByte()
            }
            listOf(lumaTable, chromaTable)
        }

    /**
     * Set the pixel density of the images the encoder will encode.
     */
    public fun setPixelDensity(pixelDensity: PixelDensity) {
        this.pixelDensity = pixelDensity
    }

    override fun setIccProfile(iccProfile: ByteArray) {
        this.iccProfile = iccProfile
    }

    override fun setExifMetadata(exif: ByteArray) {
        this.exifMetadata = exif
    }

    /**
     * Encodes the image stored in [image] of dimensions [width]x[height] and [ExtendedColorType] [colorType].
     */
    public fun encode(
        image: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ) {
        val expectedBufferLen = colorType.bufferSize(width, height)
        require(expectedBufferLen == image.size.toULong()) {
            "Invalid buffer length: expected $expectedBufferLen got ${image.size} for ${width}x$height image"
        }

        val w = width.toInt()
        val h = height.toInt()
        if (w < 1 || h < 1 || w > 65535 || h > 65535) {
            throw ImageError.Encoding(
                EncodingError(
                    ImageFormatHint.Exact(ImageFormat.Jpeg),
                    "Invalid image size ($width x $height) to encode as JPEG: width and height must be >= 1 and <= 65535",
                ),
            )
        }

        when (colorType) {
            ExtendedColorType.L8 -> encodeImageRaw(image, w, h, 1)
            ExtendedColorType.Rgb8 -> encodeImageRaw(image, w, h, 3)
            ExtendedColorType.Rgba8 -> {
                // Convert RGBA8 to RGB8 by dropping alpha
                val rgb = ByteArray(w * h * 3)
                for (i in 0 until w * h) {
                    rgb[i * 3] = image[i * 4]
                    rgb[i * 3 + 1] = image[i * 4 + 1]
                    rgb[i * 3 + 2] = image[i * 4 + 2]
                }
                encodeImageRaw(rgb, w, h, 3)
            }
            else -> throw ImageError.Unsupported(
                UnsupportedError(
                    ImageFormatHint.Exact(ImageFormat.Jpeg),
                    UnsupportedErrorKind.Color(colorType),
                ),
            )
        }
    }

    override fun writeImage(
        buf: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ) {
        encode(buf, width, height, colorType)
    }

    override fun makeCompatibleImg(
        sealed: MethodSealedToImage,
        input: DynamicImage,
    ): DynamicImage? =
        when (input.color()) {
            ColorType.L8, ColorType.Rgb8 -> null
            ColorType.La8, ColorType.L16, ColorType.La16 -> DynamicImage.ImageLuma8(input.toLuma8())
            ColorType.Rgba8, ColorType.Rgb16, ColorType.Rgb32F, ColorType.Rgba16, ColorType.Rgba32F -> DynamicImage.ImageRgb8(input.toRgb8())
        }

    private fun encodeImageRaw(
        image: ByteArray,
        width: Int,
        height: Int,
        numComponents: Int,
    ) {
        bitWriter.writeMarker(SOI)
        bitWriter.writeSegment(APP0, buildJfifHeader(pixelDensity))

        if (exifMetadata.isNotEmpty()) {
            val formatted = EXIF_HEADER + exifMetadata
            bitWriter.writeSegment(APP1, formatted)
        }

        writeIccProfileChunks()

        val compSlice = components.subList(0, numComponents)
        bitWriter.writeSegment(SOF0, buildFrameHeader(8u, width.toUShort(), height.toUShort(), compSlice))

        val numTables = if (numComponents == 1) 1 else 2
        for (i in 0 until numTables) {
            bitWriter.writeSegment(DQT, buildQuantizationSegment(8u, i.toUByte(), tables[i]))
        }

        bitWriter.writeSegment(DHT, buildHuffmanSegment(DCCLASS, LUMADESTINATION, STD_LUMA_DC_CODE_LENGTHS, STD_LUMA_DC_VALUES))
        bitWriter.writeSegment(DHT, buildHuffmanSegment(ACCLASS, LUMADESTINATION, STD_LUMA_AC_CODE_LENGTHS, STD_LUMA_AC_VALUES))

        if (numComponents == 3) {
            bitWriter.writeSegment(DHT, buildHuffmanSegment(DCCLASS, CHROMADESTINATION, STD_CHROMA_DC_CODE_LENGTHS, STD_CHROMA_DC_VALUES))
            bitWriter.writeSegment(DHT, buildHuffmanSegment(ACCLASS, CHROMADESTINATION, STD_CHROMA_AC_CODE_LENGTHS, STD_CHROMA_AC_VALUES))
        }

        bitWriter.writeSegment(SOS, buildScanHeader(compSlice))

        if (numComponents == 3) {
            encodeRgbRaw(image, width, height)
        } else {
            encodeGrayRaw(image, width, height)
        }

        bitWriter.padByte()
        bitWriter.writeMarker(EOI)
    }

    private fun encodeGrayRaw(image: ByteArray, width: Int, height: Int) {
        val yblock = UByteArray(64)
        val dctYblock = IntArray(64)
        var yDcprev = 0

        var y0 = 0
        while (y0 < height) {
            var x0 = 0
            while (x0 < width) {
                for (by in 0 until 8) {
                    val y = (y0 + by).coerceAtMost(height - 1)
                    for (bx in 0 until 8) {
                        val x = (x0 + bx).coerceAtMost(width - 1)
                        yblock[by * 8 + bx] = (image[y * width + x].toInt() and 0xFF).toUByte()
                    }
                }

                fdct(yblock, dctYblock)

                for (i in 0 until 64) {
                    val tableVal = tables[0][i].toFloat()
                    dctYblock[i] = ((dctYblock[i] / 8).toFloat() / tableVal).roundToInt()
                }

                yDcprev = bitWriter.writeBlock(dctYblock, yDcprev, STD_LUMA_DC_HUFF_LUT, STD_LUMA_AC_HUFF_LUT)
                x0 += 8
            }
            y0 += 8
        }
    }

    private fun encodeRgbRaw(image: ByteArray, width: Int, height: Int) {
        val yblock = UByteArray(64)
        val cbBlock = UByteArray(64)
        val crBlock = UByteArray(64)
        val dctYblock = IntArray(64)
        val dctCbBlock = IntArray(64)
        val dctCrBlock = IntArray(64)
        var yDcprev = 0
        var cbDcprev = 0
        var crDcprev = 0

        var y0 = 0
        while (y0 < height) {
            var x0 = 0
            while (x0 < width) {
                for (by in 0 until 8) {
                    val y = (y0 + by).coerceAtMost(height - 1)
                    for (bx in 0 until 8) {
                        val x = (x0 + bx).coerceAtMost(width - 1)
                        val offset = (y * width + x) * 3
                        val r = image[offset].toInt() and 0xFF
                        val g = image[offset + 1].toInt() and 0xFF
                        val b = image[offset + 2].toInt() and 0xFF
                        val (yc, cb, cr) = rgbToYcbcr(r, g, b)
                        val idx = by * 8 + bx
                        yblock[idx] = yc
                        cbBlock[idx] = cb
                        crBlock[idx] = cr
                    }
                }

                fdct(yblock, dctYblock)
                fdct(cbBlock, dctCbBlock)
                fdct(crBlock, dctCrBlock)

                for (i in 0 until 64) {
                    val lumaTbl = tables[0][i].toFloat()
                    val chromaTbl = tables[1][i].toFloat()
                    dctYblock[i] = ((dctYblock[i] / 8).toFloat() / lumaTbl).roundToInt()
                    dctCbBlock[i] = ((dctCbBlock[i] / 8).toFloat() / chromaTbl).roundToInt()
                    dctCrBlock[i] = ((dctCrBlock[i] / 8).toFloat() / chromaTbl).roundToInt()
                }

                yDcprev = bitWriter.writeBlock(dctYblock, yDcprev, STD_LUMA_DC_HUFF_LUT, STD_LUMA_AC_HUFF_LUT)
                cbDcprev = bitWriter.writeBlock(dctCbBlock, cbDcprev, STD_CHROMA_DC_HUFF_LUT, STD_CHROMA_AC_HUFF_LUT)
                crDcprev = bitWriter.writeBlock(dctCrBlock, crDcprev, STD_CHROMA_DC_HUFF_LUT, STD_CHROMA_AC_HUFF_LUT)

                x0 += 8
            }
            y0 += 8
        }
    }

    private fun writeIccProfileChunks() {
        if (iccProfile.isEmpty()) return
        val maxChunkSize = 65533 - 14
        val maxChunkCount = 255
        val maxIccSize = maxChunkSize * maxChunkCount
        if (iccProfile.size > maxIccSize) {
            throw IoException(IoErrorKind.InvalidInput, "ICC profile too large")
        }

        val chunks = iccProfile.toList().chunked(maxChunkSize)
        val numChunks = chunks.size.toByte()

        for ((i, chunk) in chunks.withIndex()) {
            val chunkNumber = (i + 1).toByte()
            val header = "ICC_PROFILE\u0000".encodeToByteArray()
            val segment = ByteArray(header.size + 2 + chunk.size)
            header.copyInto(segment, 0)
            segment[header.size] = chunkNumber
            segment[header.size + 1] = numChunks
            chunk.toByteArray().copyInto(segment, header.size + 2)
            bitWriter.writeSegment(APP2, segment)
        }
    }

    public companion object {
        /**
         * Creates a new encoder with default quality (75).
         */
        public fun new(w: IoWrite): JpegEncoder = JpegEncoder(w, 75u)

        /**
         * Creates a new encoder with the specified quality (1-100).
         */
        public fun newWithQuality(w: IoWrite, quality: UByte): JpegEncoder = JpegEncoder(w, quality)

        /**
         * Creates a new encoder with default quality writing to [w].
         */
        public fun new(w: BufferIoWrite): JpegEncoder = JpegEncoder(w, 75u)

        /**
         * Creates a new encoder with specified quality writing to [w].
         */
        public fun newWithQuality(w: BufferIoWrite, quality: UByte): JpegEncoder = JpegEncoder(w, quality)
    }
}
