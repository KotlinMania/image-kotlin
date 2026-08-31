// port-lint: source image/src/codecs/pnm/decoder.rs
package io.github.kotlinmania.image.codecs.pnm

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoErrorKind
import io.github.kotlinmania.image.io.IoException
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.readExact
import io.github.kotlinmania.image.utils.checkDimensionOverflow
import io.github.kotlinmania.image.utils.expandBits
import kotlin.math.roundToInt

/**
 * Single-value lines in a PNM header.
 */
public enum class PnmHeaderLine {
    Height,
    Width,
    Depth,
    Maxval,
    ;

    override fun toString(): String =
        when (this) {
            Height -> "HEIGHT"
            Width -> "WIDTH"
            Depth -> "DEPTH"
            Maxval -> "MAXVAL"
        }
}

/**
 * Source of data for PNM errors.
 */
public sealed class ErrorDataSource {
    public data class Line(
        val line: PnmHeaderLine,
    ) : ErrorDataSource() {
        override fun toString(): String = line.toString()
    }

    public data object Preamble : ErrorDataSource() {
        override fun toString(): String = "number in preamble"
    }

    public data object Sample : ErrorDataSource() {
        override fun toString(): String = "sample"
    }
}

/**
 * Errors that can occur when decoding PNM images.
 */
public sealed class DecoderError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    public data class NonAsciiByteInHeader(
        val byte: Byte,
    ) : DecoderError("Non-ASCII byte in header: 0x${(byte.toInt() and 0xFF).toString(16).padStart(2, '0')}")

    public data object NonAsciiLineInPamHeader :
        DecoderError("Non-ASCII line in PAM header")

    public data class PnmMagicInvalid(
        val magic: ByteArray,
    ) : DecoderError("PNM magic invalid: [${magic.joinToString(", ") { "0x" + (it.toInt() and 0xFF).toString(16).padStart(2, '0') }}]") {
        override fun equals(other: Any?): Boolean =
            other is PnmMagicInvalid && magic.contentEquals(other.magic)

        override fun hashCode(): Int = magic.contentHashCode()
    }

    public data class InvalidDigit(
        val source: ErrorDataSource,
    ) : DecoderError("Invalid digit in $source")

    public data class UnparsableValue(
        val source: ErrorDataSource,
        val value: String,
        val err: Throwable,
    ) : DecoderError("Unparsable value in $source: '$value'", err)

    public data class NotNewlineAfterP7Magic(
        val byte: Byte,
    ) : DecoderError("Expected newline after P7 magic, got 0x${(byte.toInt() and 0xFF).toString(16).padStart(2, '0')}")

    public data object UnexpectedPnmHeaderEnd :
        DecoderError("Unexpected end of PNM header")

    public data class HeaderLineDuplicated(
        val line: PnmHeaderLine,
    ) : DecoderError("Duplicate $line line")

    public data class HeaderLineUnknown(
        val identifier: String,
    ) : DecoderError("Unknown header line with identifier '$identifier'")

    public data class HeaderLineMissing(
        val height: UInt?,
        val width: UInt?,
        val depth: UInt?,
        val maxval: UInt?,
    ) : DecoderError("Missing header line: have height=$height, width=$width, depth=$depth, maxval=$maxval")

    public data object InputTooShort :
        DecoderError("Not enough data was provided to the Decoder to decode the image")

    public data class UnexpectedByteInRaster(
        val byte: Byte,
    ) : DecoderError("Unexpected character 0x${(byte.toInt() and 0xFF).toString(16).padStart(2, '0')} within sample raster")

    public data class SampleOutOfBounds(
        val value: UByte,
    ) : DecoderError("Sample value $value outside of bounds")

    public data object MaxvalZero :
        DecoderError("Image MAXVAL is zero")

    public data class MaxvalTooBig(
        val maxval: UInt,
    ) : DecoderError("Image MAXVAL exceeds 65535: $maxval")

    public data class InvalidDepthOrMaxval(
        val tupleType: ArbitraryTuplType,
        val depth: UInt,
        val maxval: UInt,
    ) : DecoderError("Invalid depth ($depth) or maxval ($maxval) for tuple type ${tupleType.name()}")

    public data class InvalidDepth(
        val tupleType: ArbitraryTuplType,
        val depth: UInt,
    ) : DecoderError("Invalid depth ($depth) for tuple type ${tupleType.name()}")

    public data object TupleTypeUnrecognised :
        DecoderError("Tuple type not recognized")

    public data class Overflow(
        val source: ErrorDataSource,
    ) : DecoderError("Overflow when parsing integer in $source")
}

public fun DecoderError.toImageError(): ImageError =
    ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Pnm), this))

/**
 * Dynamic representation of all decodable tuple types.
 */
public enum class TupleType {
    PbmBit,
    BWBit,
    BWAlphaBit,
    GrayU8,
    GrayAlphaU8,
    GrayU16,
    GrayAlphaU16,
    RGBU8,
    RGBAlphaU8,
    RGBU16,
    RGBAlphaU16,
}

/**
 * PNM decoder supporting PBM, PGM, PPM, and PAM.
 */
public class PnmDecoder(
    private val reader: IoRead,
) : ImageDecoder {
    private val header: PnmHeader
    private val tuple: TupleType

    public constructor(bytes: ByteArray) : this(BufferIoRead(bytes))

    init {
        val magic = ByteArray(2)
        try {
            reader.readExact(magic)
        } catch (e: Exception) {
            throw ImageError.IoError(IoException(IoErrorKind.UnexpectedEof, "Failed to read magic constant: ${e.message}", e))
        }

        val subtype =
            when {
                magic[0] == 'P'.code.toByte() && magic[1] == '1'.code.toByte() -> PnmSubtype.Bitmap(SampleEncoding.Ascii)
                magic[0] == 'P'.code.toByte() && magic[1] == '2'.code.toByte() -> PnmSubtype.Graymap(SampleEncoding.Ascii)
                magic[0] == 'P'.code.toByte() && magic[1] == '3'.code.toByte() -> PnmSubtype.Pixmap(SampleEncoding.Ascii)
                magic[0] == 'P'.code.toByte() && magic[1] == '4'.code.toByte() -> PnmSubtype.Bitmap(SampleEncoding.Binary)
                magic[0] == 'P'.code.toByte() && magic[1] == '5'.code.toByte() -> PnmSubtype.Graymap(SampleEncoding.Binary)
                magic[0] == 'P'.code.toByte() && magic[1] == '6'.code.toByte() -> PnmSubtype.Pixmap(SampleEncoding.Binary)
                magic[0] == 'P'.code.toByte() && magic[1] == '7'.code.toByte() -> PnmSubtype.ArbitraryMap
                else -> throw DecoderError.PnmMagicInvalid(magic).toImageError()
            }

        when (subtype) {
            is PnmSubtype.Bitmap -> {
                val bitmapHeader = readBitmapHeader(subtype.encoding)
                header = PnmHeader(bitmapHeader)
                tuple = TupleType.PbmBit
            }
            is PnmSubtype.Graymap -> {
                val graymapHeader = readGraymapHeader(subtype.encoding)
                header = PnmHeader(graymapHeader)
                tuple = graymapTupleType(graymapHeader)
            }
            is PnmSubtype.Pixmap -> {
                val pixmapHeader = readPixmapHeader(subtype.encoding)
                header = PnmHeader(pixmapHeader)
                tuple = pixmapTupleType(pixmapHeader)
            }
            PnmSubtype.ArbitraryMap -> {
                val arbitraryHeader = readArbitraryHeader()
                header = PnmHeader(arbitraryHeader)
                tuple = arbitraryTupleType(arbitraryHeader)
            }
        }

        if (checkDimensionOverflow(header.width(), header.height(), colorType().bytesPerPixel())) {
            throw ImageError.Unsupported(
                UnsupportedError(
                    ImageFormatHint.Exact(ImageFormat.Pnm),
                    UnsupportedErrorKind.GenericFeature("Image dimensions (${header.width()}x${header.height()}) are too large"),
                ),
            )
        }
    }

    public fun header(): PnmHeader = header

    public fun subtype(): PnmSubtype = header.subtype()

    public fun intoInner(): Pair<IoRead, PnmHeader> = Pair(reader, header)

    override fun dimensions(): Pair<UInt, UInt> = Pair(header.width(), header.height())

    override fun colorType(): ColorType =
        when (tuple) {
            TupleType.PbmBit, TupleType.BWBit, TupleType.GrayU8 -> ColorType.L8
            TupleType.BWAlphaBit, TupleType.GrayAlphaU8 -> ColorType.La8
            TupleType.GrayU16 -> ColorType.L16
            TupleType.GrayAlphaU16 -> ColorType.La16
            TupleType.RGBU8 -> ColorType.Rgb8
            TupleType.RGBAlphaU8 -> ColorType.Rgba8
            TupleType.RGBU16 -> ColorType.Rgb16
            TupleType.RGBAlphaU16 -> ColorType.Rgba16
        }

    override fun originalColorType(): ExtendedColorType =
        when (tuple) {
            TupleType.PbmBit, TupleType.BWBit -> ExtendedColorType.L1
            TupleType.BWAlphaBit -> ExtendedColorType.La1
            TupleType.GrayU8 -> ExtendedColorType.L8
            TupleType.GrayAlphaU8 -> ExtendedColorType.La8
            TupleType.GrayU16 -> ExtendedColorType.L16
            TupleType.GrayAlphaU16 -> ExtendedColorType.La16
            TupleType.RGBU8 -> ExtendedColorType.Rgb8
            TupleType.RGBAlphaU8 -> ExtendedColorType.Rgba8
            TupleType.RGBU16 -> ExtendedColorType.Rgb16
            TupleType.RGBAlphaU16 -> ExtendedColorType.Rgba16
        }

    override fun readImage(buf: ByteArray) {
        val expected = totalBytes().toLong()
        require(buf.size.toLong() == expected) {
            "Invalid buffer size: expected $expected, got ${buf.size}"
        }

        when (tuple) {
            TupleType.PbmBit -> readPbmBitSamples(buf)
            TupleType.BWBit -> readBWBitSamples(1, buf)
            TupleType.BWAlphaBit -> readBWBitSamples(2, buf)
            TupleType.RGBU8 -> readU8Samples(3, buf)
            TupleType.RGBAlphaU8 -> readU8Samples(4, buf)
            TupleType.RGBU16 -> readU16Samples(3, buf)
            TupleType.RGBAlphaU16 -> readU16Samples(4, buf)
            TupleType.GrayU8 -> readU8Samples(1, buf)
            TupleType.GrayAlphaU8 -> readU8Samples(2, buf)
            TupleType.GrayU16 -> readU16Samples(1, buf)
            TupleType.GrayAlphaU16 -> readU16Samples(2, buf)
        }
    }

    private fun readBitmapHeader(encoding: SampleEncoding): BitmapHeader {
        val width = readNextU32()
        val height = readNextU32()
        return BitmapHeader(encoding, height, width)
    }

    private fun readGraymapHeader(encoding: SampleEncoding): GraymapHeader {
        val pixmap = readPixmapHeader(encoding)
        return GraymapHeader(encoding, pixmap.height, pixmap.width, pixmap.maxval)
    }

    private fun readPixmapHeader(encoding: SampleEncoding): PixmapHeader {
        val width = readNextU32()
        val height = readNextU32()
        val maxval = readNextU32()
        return PixmapHeader(encoding, height, width, maxval)
    }

    private fun readArbitraryHeader(): ArbitraryHeader {
        val firstByte = ByteArray(1)
        val n = reader.read(firstByte, 0, 1)
        if (n == 0) {
            throw ImageError.IoError(IoException(IoErrorKind.UnexpectedEof, "Unexpected EOF after P7 magic"))
        }
        if (firstByte[0] != '\n'.code.toByte()) {
            throw DecoderError.NotNewlineAfterP7Magic(firstByte[0]).toImageError()
        }

        var height: UInt? = null
        var width: UInt? = null
        var depth: UInt? = null
        var maxval: UInt? = null
        var tupltype: String? = null

        fun parseSingleValueLine(current: UInt?, rest: String, line: PnmHeaderLine): UInt {
            if (current != null) {
                throw DecoderError.HeaderLineDuplicated(line).toImageError()
            }
            val trimmed = rest.trim()
            return try {
                trimmed.toUInt()
            } catch (e: Exception) {
                throw DecoderError.UnparsableValue(ErrorDataSource.Line(line), trimmed, e).toImageError()
            }
        }

        while (true) {
            val line = readNextLine()
            if (line.isEmpty()) {
                throw DecoderError.UnexpectedPnmHeaderEnd.toImageError()
            }
            if (line.startsWith("#")) {
                continue
            }
            if (line.any { it.code > 127 }) {
                throw DecoderError.NonAsciiLineInPamHeader.toImageError()
            }
            val trimmed = line.trimStart()
            val wsIdx = trimmed.indexOfFirst { it.isWhitespace() }
            val (identifier, rest) =
                if (wsIdx == -1) {
                    Pair(trimmed, "")
                } else {
                    Pair(trimmed.substring(0, wsIdx), trimmed.substring(wsIdx))
                }

            when (identifier) {
                "ENDHDR" -> break
                "HEIGHT" -> height = parseSingleValueLine(height, rest, PnmHeaderLine.Height)
                "WIDTH" -> width = parseSingleValueLine(width, rest, PnmHeaderLine.Width)
                "DEPTH" -> depth = parseSingleValueLine(depth, rest, PnmHeaderLine.Depth)
                "MAXVAL" -> maxval = parseSingleValueLine(maxval, rest, PnmHeaderLine.Maxval)
                "TUPLTYPE" -> {
                    val ident = rest.trim()
                    tupltype = if (tupltype == null) ident else "$tupltype $ident"
                }
                else -> throw DecoderError.HeaderLineUnknown(identifier).toImageError()
            }
        }

        if (height == null || width == null || depth == null || maxval == null) {
            throw DecoderError.HeaderLineMissing(height, width, depth, maxval).toImageError()
        }

        val arbitraryTuplType =
            when (tupltype) {
                null -> null
                "BLACKANDWHITE" -> ArbitraryTuplType.BlackAndWhite
                "BLACKANDWHITE_ALPHA" -> ArbitraryTuplType.BlackAndWhiteAlpha
                "GRAYSCALE" -> ArbitraryTuplType.Grayscale
                "GRAYSCALE_ALPHA" -> ArbitraryTuplType.GrayscaleAlpha
                "RGB" -> ArbitraryTuplType.RGB
                "RGB_ALPHA" -> ArbitraryTuplType.RGBAlpha
                else -> ArbitraryTuplType.Custom(tupltype)
            }

        return ArbitraryHeader(height, width, depth, maxval, arbitraryTuplType)
    }

    private fun graymapTupleType(header: GraymapHeader): TupleType =
        when {
            header.maxwhite == 0u -> throw DecoderError.MaxvalZero.toImageError()
            header.maxwhite <= 0xFFu -> TupleType.GrayU8
            header.maxwhite <= 0xFFFFu -> TupleType.GrayU16
            else -> throw DecoderError.MaxvalTooBig(header.maxwhite).toImageError()
        }

    private fun pixmapTupleType(header: PixmapHeader): TupleType =
        when {
            header.maxval == 0u -> throw DecoderError.MaxvalZero.toImageError()
            header.maxval <= 0xFFu -> TupleType.RGBU8
            header.maxval <= 0xFFFFu -> TupleType.RGBU16
            else -> throw DecoderError.MaxvalTooBig(header.maxval).toImageError()
        }

    private fun arbitraryTupleType(header: ArbitraryHeader): TupleType {
        if (header.maxval == 0u) {
            throw DecoderError.MaxvalZero.toImageError()
        }
        val tupltype = header.tupltype
        return when {
            tupltype == null && header.depth == 1u -> TupleType.GrayU8
            tupltype == null && header.depth == 2u -> TupleType.GrayAlphaU8
            tupltype == null && header.depth == 3u -> TupleType.RGBU8
            tupltype == null && header.depth == 4u -> TupleType.RGBAlphaU8

            tupltype == ArbitraryTuplType.BlackAndWhite && header.maxval == 1u && header.depth == 1u ->
                TupleType.BWBit
            tupltype == ArbitraryTuplType.BlackAndWhite ->
                throw DecoderError.InvalidDepthOrMaxval(ArbitraryTuplType.BlackAndWhite, header.depth, header.maxval).toImageError()

            tupltype == ArbitraryTuplType.Grayscale && header.depth == 1u && header.maxval <= 0xFFu ->
                TupleType.GrayU8
            tupltype == ArbitraryTuplType.Grayscale && header.depth <= 1u && header.maxval <= 0xFFFFu ->
                TupleType.GrayU16
            tupltype == ArbitraryTuplType.Grayscale ->
                throw DecoderError.InvalidDepthOrMaxval(ArbitraryTuplType.Grayscale, header.depth, header.maxval).toImageError()

            tupltype == ArbitraryTuplType.RGB && header.depth == 3u && header.maxval <= 0xFFu ->
                TupleType.RGBU8
            tupltype == ArbitraryTuplType.RGB && header.depth == 3u && header.maxval <= 0xFFFFu ->
                TupleType.RGBU16
            tupltype == ArbitraryTuplType.RGB ->
                throw DecoderError.InvalidDepth(ArbitraryTuplType.RGB, header.depth).toImageError()

            tupltype == ArbitraryTuplType.BlackAndWhiteAlpha && header.depth == 2u && header.maxval == 1u ->
                TupleType.BWAlphaBit
            tupltype == ArbitraryTuplType.BlackAndWhiteAlpha ->
                throw DecoderError.InvalidDepthOrMaxval(ArbitraryTuplType.BlackAndWhiteAlpha, header.depth, header.maxval).toImageError()

            tupltype == ArbitraryTuplType.GrayscaleAlpha && header.depth == 2u && header.maxval <= 0xFFu ->
                TupleType.GrayAlphaU8
            tupltype == ArbitraryTuplType.GrayscaleAlpha && header.depth == 2u && header.maxval <= 0xFFFFu ->
                TupleType.GrayAlphaU16
            tupltype == ArbitraryTuplType.GrayscaleAlpha ->
                throw DecoderError.InvalidDepth(ArbitraryTuplType.GrayscaleAlpha, header.depth).toImageError()

            tupltype == ArbitraryTuplType.RGBAlpha && header.depth == 4u && header.maxval <= 0xFFu ->
                TupleType.RGBAlphaU8
            tupltype == ArbitraryTuplType.RGBAlpha && header.depth == 4u && header.maxval <= 0xFFFFu ->
                TupleType.RGBAlphaU16
            tupltype == ArbitraryTuplType.RGBAlpha ->
                throw DecoderError.InvalidDepth(ArbitraryTuplType.RGBAlpha, header.depth).toImageError()

            tupltype is ArbitraryTuplType.Custom ->
                throw ImageError.Unsupported(
                    UnsupportedError(
                        ImageFormatHint.Exact(ImageFormat.Pnm),
                        UnsupportedErrorKind.GenericFeature("Tuple type ${tupltype.custom}"),
                    ),
                )
            else -> throw DecoderError.TupleTypeUnrecognised.toImageError()
        }
    }

    private fun readNextU32(): UInt {
        var value = 0u
        var foundDigit = false
        var inComment = false

        val oneByte = ByteArray(1)
        while (true) {
            val count = reader.read(oneByte, 0, 1)
            if (count == 0) break
            val b = oneByte[0]

            if (inComment) {
                if (b == '\r'.code.toByte() || b == '\n'.code.toByte()) {
                    inComment = false
                }
                continue
            }

            if (b == '#'.code.toByte()) {
                inComment = true
                continue
            }

            when (b) {
                '\t'.code.toByte(), '\n'.code.toByte(), 0x0B.toByte(), 0x0C.toByte(), '\r'.code.toByte(), ' '.code.toByte() -> {
                    if (foundDigit) {
                        return value
                    }
                }
                else -> {
                    val bInt = b.toInt() and 0xFF
                    if (bInt > 127) {
                        throw DecoderError.NonAsciiByteInHeader(b).toImageError()
                    }
                    if (b in '0'.code.toByte()..'9'.code.toByte()) {
                        val digit = (b - '0'.code.toByte()).toUInt()
                        if (value > (UInt.MAX_VALUE - digit) / 10u) {
                            throw DecoderError.Overflow(ErrorDataSource.Preamble).toImageError()
                        }
                        value = value * 10u + digit
                        foundDigit = true
                    } else {
                        throw DecoderError.InvalidDigit(ErrorDataSource.Preamble).toImageError()
                    }
                }
            }
        }

        if (!foundDigit) {
            throw ImageError.IoError(IoException(IoErrorKind.UnexpectedEof, "Unexpected EOF while reading integer"))
        }

        return value
    }

    private fun readNextLine(): String {
        val bytes = mutableListOf<Byte>()
        val oneByte = ByteArray(1)
        while (true) {
            val count = reader.read(oneByte, 0, 1)
            if (count == 0 || oneByte[0] == '\n'.code.toByte()) {
                break
            }
            bytes.add(oneByte[0])
        }
        return bytes.toByteArray().decodeToString()
    }

    private fun readU8Samples(components: Int, buf: ByteArray) {
        require(components > 0)
        when (subtype().sampleEncoding()) {
            SampleEncoding.Binary -> {
                reader.readExact(buf)
            }
            SampleEncoding.Ascii -> {
                for (i in buf.indices) {
                    val sample = readSeparatedAsciiU16()
                    buf[i] = sample.toByte()
                }
            }
        }
        scaleSamples(1, buf)
    }

    private fun readU16Samples(components: Int, buf: ByteArray) {
        require(components > 0)
        when (subtype().sampleEncoding()) {
            SampleEncoding.Binary -> {
                val temp = ByteArray(buf.size)
                reader.readExact(temp)
                for (i in 0 until buf.size step 2) {
                    val v = ((temp[i].toInt() and 0xFF) shl 8) or (temp[i + 1].toInt() and 0xFF)
                    buf[i] = (v and 0xFF).toByte()
                    buf[i + 1] = ((v ushr 8) and 0xFF).toByte()
                }
            }
            SampleEncoding.Ascii -> {
                for (i in 0 until buf.size step 2) {
                    val v = readSeparatedAsciiU16()
                    buf[i] = (v and 0xFF).toByte()
                    buf[i + 1] = ((v ushr 8) and 0xFF).toByte()
                }
            }
        }
        scaleSamples(2, buf)
    }

    private fun readPbmBitSamples(buf: ByteArray) {
        val width = header.width().toInt()
        val height = header.height().toInt()
        when (subtype().sampleEncoding()) {
            SampleEncoding.Binary -> {
                val lineLen = (width / 8) + if (width % 8 != 0) 1 else 0
                val byteCount = lineLen * height
                val bytes = ByteArray(byteCount)
                reader.readExact(bytes)
                val expanded = expandBits(1, width.toUInt(), bytes)
                for (i in buf.indices) {
                    val v = expanded[i].toInt() and 0xFF
                    buf[i] = if (v == 0) 255.toByte() else 0.toByte()
                }
            }
            SampleEncoding.Ascii -> {
                val oneByte = ByteArray(1)
                for (i in buf.indices) {
                    while (true) {
                        val count = reader.read(oneByte, 0, 1)
                        if (count == 0) {
                            throw DecoderError.InputTooShort.toImageError()
                        }
                        val b = oneByte[0]
                        when (b) {
                            '\t'.code.toByte(), '\n'.code.toByte(), 0x0B.toByte(), 0x0C.toByte(), '\r'.code.toByte(), ' '.code.toByte() -> continue
                            '0'.code.toByte() -> {
                                buf[i] = 255.toByte()
                                break
                            }
                            '1'.code.toByte() -> {
                                buf[i] = 0.toByte()
                                break
                            }
                            else -> throw DecoderError.UnexpectedByteInRaster(b).toImageError()
                        }
                    }
                }
            }
        }
    }

    private fun readBWBitSamples(components: Int, buf: ByteArray) {
        require(components > 0)
        reader.readExact(buf)
        for (i in buf.indices) {
            val v = buf[i].toInt() and 0xFF
            if (v > 1) {
                throw DecoderError.SampleOutOfBounds(v.toUByte()).toImageError()
            }
            buf[i] = if (v == 1) 255.toByte() else 0.toByte()
        }
    }

    private fun readSeparatedAsciiU16(): Int {
        var v = 0
        var hadAny = false
        val oneByte = ByteArray(1)

        while (true) {
            val count = reader.read(oneByte, 0, 1)
            if (count == 0) break
            val b = oneByte[0]
            val isSep =
                b == '\t'.code.toByte() ||
                    b == '\n'.code.toByte() ||
                    b == 0x0B.toByte() ||
                    b == 0x0C.toByte() ||
                    b == '\r'.code.toByte() ||
                    b == ' '.code.toByte()
            if (!hadAny) {
                if (isSep) continue
            } else {
                if (isSep) return v
            }

            if (b in '0'.code.toByte()..'9'.code.toByte()) {
                val digit = b - '0'.code.toByte()
                if (v > (0xFFFF - digit) / 10) {
                    throw DecoderError.Overflow(ErrorDataSource.Sample).toImageError()
                }
                v = v * 10 + digit
                hadAny = true
            } else {
                throw DecoderError.InvalidDigit(ErrorDataSource.Sample).toImageError()
            }
        }

        if (!hadAny) {
            throw DecoderError.InputTooShort.toImageError()
        }

        return v
    }

    private fun scaleSamples(sampleSize: Int, buf: ByteArray) {
        val currentMax = header.maximalSample().toFloat()
        val targetMax = if (sampleSize == 1) 255.0f else 65535.0f
        if (currentMax == targetMax || currentMax <= 0f) return

        val factor = targetMax / currentMax
        if (sampleSize == 1) {
            for (i in buf.indices) {
                val raw = buf[i].toInt() and 0xFF
                buf[i] = (raw.toFloat() * factor).roundToInt().coerceIn(0, 255).toByte()
            }
        } else if (sampleSize == 2) {
            for (i in 0 until buf.size step 2) {
                val raw = (buf[i].toInt() and 0xFF) or ((buf[i + 1].toInt() and 0xFF) shl 8)
                val scaled = (raw.toFloat() * factor).roundToInt().coerceIn(0, 65535)
                buf[i] = (scaled and 0xFF).toByte()
                buf[i + 1] = ((scaled ushr 8) and 0xFF).toByte()
            }
        }
    }
}

internal class PbmBit
internal class BWBit

