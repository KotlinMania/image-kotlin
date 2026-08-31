// port-lint: source codecs/pnm/header.rs
package io.github.kotlinmania.image.codecs.pnm

import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.writeAll

/**
 * The kind of encoding used to store sample values.
 */
public enum class SampleEncoding {
    /** Samples are unsigned binary integers in big endian. */
    Binary,

    /** Samples are encoded as decimal ascii strings separated by whitespace. */
    Ascii,
}

/**
 * Denotes the category of the magic number.
 */
public sealed interface PnmSubtype {
    /** Magic numbers P1 and P4. */
    public data class Bitmap(
        val encoding: SampleEncoding,
    ) : PnmSubtype

    /** Magic numbers P2 and P5. */
    public data class Graymap(
        val encoding: SampleEncoding,
    ) : PnmSubtype

    /** Magic numbers P3 and P6. */
    public data class Pixmap(
        val encoding: SampleEncoding,
    ) : PnmSubtype

    /** Magic number P7. */
    public data object ArbitraryMap : PnmSubtype

    /**
     * Get the two magic constant bytes corresponding to this format subtype.
     */
    public fun magicConstant(): ByteArray =
        when (this) {
            is Bitmap ->
                if (encoding == SampleEncoding.Ascii) {
                    byteArrayOf('P'.code.toByte(), '1'.code.toByte())
                } else {
                    byteArrayOf('P'.code.toByte(), '4'.code.toByte())
                }
            is Graymap ->
                if (encoding == SampleEncoding.Ascii) {
                    byteArrayOf('P'.code.toByte(), '2'.code.toByte())
                } else {
                    byteArrayOf('P'.code.toByte(), '5'.code.toByte())
                }
            is Pixmap ->
                if (encoding == SampleEncoding.Ascii) {
                    byteArrayOf('P'.code.toByte(), '3'.code.toByte())
                } else {
                    byteArrayOf('P'.code.toByte(), '6'.code.toByte())
                }
            ArbitraryMap -> byteArrayOf('P'.code.toByte(), '7'.code.toByte())
        }

    /**
     * Whether samples are stored as binary or as decimal ascii.
     */
    public fun sampleEncoding(): SampleEncoding =
        when (this) {
            ArbitraryMap -> SampleEncoding.Binary
            is Bitmap -> encoding
            is Graymap -> encoding
            is Pixmap -> encoding
        }
}

/**
 * Standardized tuple type specifiers in the header of a `pam`.
 */
public sealed interface ArbitraryTuplType {
    public data object BlackAndWhite : ArbitraryTuplType

    public data object BlackAndWhiteAlpha : ArbitraryTuplType

    public data object Grayscale : ArbitraryTuplType

    public data object GrayscaleAlpha : ArbitraryTuplType

    public data object RGB : ArbitraryTuplType

    public data object RGBAlpha : ArbitraryTuplType

    public data class Custom(
        val custom: String,
    ) : ArbitraryTuplType

    public fun name(): String =
        when (this) {
            BlackAndWhite -> "BLACKANDWHITE"
            BlackAndWhiteAlpha -> "BLACKANDWHITE_ALPHA"
            Grayscale -> "GRAYSCALE"
            GrayscaleAlpha -> "GRAYSCALE_ALPHA"
            RGB -> "RGB"
            RGBAlpha -> "RGB_ALPHA"
            is Custom -> custom
        }
}

/**
 * Header produced by a `pbm` file ("Portable Bit Map").
 */
public data class BitmapHeader(
    public val encoding: SampleEncoding,
    public val height: UInt,
    public val width: UInt,
)

/**
 * Header produced by a `pgm` file ("Portable Gray Map").
 */
public data class GraymapHeader(
    public val encoding: SampleEncoding,
    public val height: UInt,
    public val width: UInt,
    public val maxwhite: UInt,
)

/**
 * Header produced by a `ppm` file ("Portable Pixel Map").
 */
public data class PixmapHeader(
    public val encoding: SampleEncoding,
    public val height: UInt,
    public val width: UInt,
    public val maxval: UInt,
)

/**
 * Header produced by a `pam` file ("Portable Arbitrary Map").
 */
public data class ArbitraryHeader(
    public val height: UInt,
    public val width: UInt,
    public val depth: UInt,
    public val maxval: UInt,
    public val tupltype: ArbitraryTuplType? = null,
)

public sealed interface HeaderRecord {
    public data class Bitmap(
        val header: BitmapHeader,
    ) : HeaderRecord

    public data class Graymap(
        val header: GraymapHeader,
    ) : HeaderRecord

    public data class Pixmap(
        val header: PixmapHeader,
    ) : HeaderRecord

    public data class Arbitrary(
        val header: ArbitraryHeader,
    ) : HeaderRecord
}

/**
 * Stores the complete header data of a file.
 */
public class PnmHeader(
    public val decoded: HeaderRecord,
    public val encoded: ByteArray? = null,
) {
    public constructor(header: BitmapHeader) : this(HeaderRecord.Bitmap(header), null)

    public constructor(header: GraymapHeader) : this(HeaderRecord.Graymap(header), null)

    public constructor(header: PixmapHeader) : this(HeaderRecord.Pixmap(header), null)

    public constructor(header: ArbitraryHeader) : this(HeaderRecord.Arbitrary(header), null)

    public fun subtype(): PnmSubtype =
        when (decoded) {
            is HeaderRecord.Bitmap -> PnmSubtype.Bitmap(decoded.header.encoding)
            is HeaderRecord.Graymap -> PnmSubtype.Graymap(decoded.header.encoding)
            is HeaderRecord.Pixmap -> PnmSubtype.Pixmap(decoded.header.encoding)
            is HeaderRecord.Arbitrary -> PnmSubtype.ArbitraryMap
        }

    public fun width(): UInt =
        when (decoded) {
            is HeaderRecord.Bitmap -> decoded.header.width
            is HeaderRecord.Graymap -> decoded.header.width
            is HeaderRecord.Pixmap -> decoded.header.width
            is HeaderRecord.Arbitrary -> decoded.header.width
        }

    public fun height(): UInt =
        when (decoded) {
            is HeaderRecord.Bitmap -> decoded.header.height
            is HeaderRecord.Graymap -> decoded.header.height
            is HeaderRecord.Pixmap -> decoded.header.height
            is HeaderRecord.Arbitrary -> decoded.header.height
        }

    public fun maximalSample(): UInt =
        when (decoded) {
            is HeaderRecord.Bitmap -> 1u
            is HeaderRecord.Graymap -> decoded.header.maxwhite
            is HeaderRecord.Pixmap -> decoded.header.maxval
            is HeaderRecord.Arbitrary -> decoded.header.maxval
        }

    public fun asBitmap(): BitmapHeader? =
        (decoded as? HeaderRecord.Bitmap)?.header

    public fun asGraymap(): GraymapHeader? =
        (decoded as? HeaderRecord.Graymap)?.header

    public fun asPixmap(): PixmapHeader? =
        (decoded as? HeaderRecord.Pixmap)?.header

    public fun asArbitrary(): ArbitraryHeader? =
        (decoded as? HeaderRecord.Arbitrary)?.header

    internal fun write(writer: IoWrite) {
        writer.writeAll(subtype().magicConstant())
        if (encoded != null) {
            writer.writeAll(encoded)
            return
        }
        val text =
            when (decoded) {
                is HeaderRecord.Bitmap -> "\n${decoded.header.width} ${decoded.header.height}\n"
                is HeaderRecord.Graymap -> "\n${decoded.header.width} ${decoded.header.height} ${decoded.header.maxwhite}\n"
                is HeaderRecord.Pixmap -> "\n${decoded.header.width} ${decoded.header.height} ${decoded.header.maxval}\n"
                is HeaderRecord.Arbitrary -> {
                    val tuplStr = decoded.header.tupltype?.let { "TUPLTYPE ${it.name()}\n" } ?: ""
                    "\nWIDTH ${decoded.header.width}\nHEIGHT ${decoded.header.height}\nDEPTH ${decoded.header.depth}\nMAXVAL ${decoded.header.maxval}\n${tuplStr}ENDHDR\n"
                }
            }
        writer.writeAll(text.encodeToByteArray())
    }

    public companion object {
        public fun from(header: BitmapHeader): PnmHeader = PnmHeader(header)

        public fun from(header: GraymapHeader): PnmHeader = PnmHeader(header)

        public fun from(header: PixmapHeader): PnmHeader = PnmHeader(header)

        public fun from(header: ArbitraryHeader): PnmHeader = PnmHeader(header)
    }
}

internal class TupltypeWriter(
    private val tupltype: ArbitraryTuplType?,
) {
    override fun toString(): String =
        if (tupltype != null) "TUPLTYPE ${tupltype.name()}\n" else ""
}

