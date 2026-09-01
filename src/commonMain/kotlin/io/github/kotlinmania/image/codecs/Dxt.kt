// port-lint: source codecs/dxt.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ParameterError
import io.github.kotlinmania.image.ParameterErrorKind
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.readExact

/**
 * What version of DXT compression are we using?
 *
 * Note that DXT2 and DXT4 are omitted as they are just DXT3 and DXT5 with
 * premultiplied alpha.
 */
public enum class DxtVariant {
    /**
     * The DXT1 format. 48 bytes of RGB data in a 4x4 pixel square is
     * compressed into an 8 byte block of DXT1 data.
     */
    DXT1,

    /**
     * The DXT3 format. 64 bytes of RGBA data in a 4x4 pixel square is
     * compressed into an 16 byte block of DXT3 data.
     */
    DXT3,

    /**
     * The DXT5 format. 64 bytes of RGBA data in a 4x4 pixel square is
     * compressed into an 16 byte block of DXT5 data.
     */
    DXT5,
    ;

    /**
     * Returns the amount of bytes of raw image data that is encoded in a single DXTn block.
     */
    public fun decodedBytesPerBlock(): Long =
        when (this) {
            DXT1 -> 48L
            DXT3, DXT5 -> 64L
        }

    /**
     * Returns the amount of bytes per block of encoded DXTn data.
     */
    public fun encodedBytesPerBlock(): Long =
        when (this) {
            DXT1 -> 8L
            DXT3, DXT5 -> 16L
        }

    /**
     * Returns the color type that is stored in this DXT variant.
     */
    public fun colorType(): ColorType =
        when (this) {
            DXT1 -> ColorType.Rgb8
            DXT3, DXT5 -> ColorType.Rgba8
        }
}

/**
 * DXT decoder.
 */
public class DxtDecoder internal constructor(
    private val inner: IoRead,
    private val widthBlocks: UInt,
    private val heightBlocks: UInt,
    private val variant: DxtVariant,
    private var row: UInt = 0u,
) : ImageDecoder {
    public constructor(
        bytes: ByteArray,
        width: UInt,
        height: UInt,
        variant: DxtVariant,
    ) : this(
        inner = BufferIoRead(bytes),
        widthBlocks =
            if (width % 4u == 0u && height % 4u == 0u) {
                width / 4u
            } else {
                throw ImageError.Parameter(
                    ParameterError(ParameterErrorKind.DimensionMismatch),
                )
            },
        heightBlocks = height / 4u,
        variant = variant,
        row = 0u,
    )

    public companion object {
        /**
         * Create a new DXT decoder that decodes from [reader].
         *
         * As DXT is often stored as raw buffers with the width/height somewhere else,
         * the width and height of the image need to be passed in [width] and [height],
         * as well as the DXT variant in [variant].
         *
         * [width] and [height] are required to be multiples of 4; otherwise an error
         * will be thrown.
         */
        public fun create(
            reader: IoRead,
            width: UInt,
            height: UInt,
            variant: DxtVariant,
        ): DxtDecoder {
            if (width % 4u != 0u || height % 4u != 0u) {
                throw ImageError.Parameter(
                    ParameterError(ParameterErrorKind.DimensionMismatch),
                )
            }
            val widthBlocks = width / 4u
            val heightBlocks = height / 4u
            return DxtDecoder(reader, widthBlocks, heightBlocks, variant, 0u)
        }
        public fun new(
            reader: IoRead,
            width: UInt,
            height: UInt,
            variant: DxtVariant,
        ): DxtDecoder = create(reader, width, height, variant)
    }

    public fun scanlineBytes(): ULong =
        variant.decodedBytesPerBlock().toULong() * widthBlocks.toULong()

    public fun readScanline(buf: ByteArray): Int {
        val expected = scanlineBytes().toInt()
        require(buf.size == expected) {
            "Invalid buffer size: expected $expected, got ${buf.size}"
        }

        val len = (variant.encodedBytesPerBlock() * widthBlocks.toLong()).toInt()
        val src = ByteArray(len)
        inner.readExact(src)

        when (variant) {
            DxtVariant.DXT1 -> decodeDxt1Row(src, buf)
            DxtVariant.DXT3 -> decodeDxt3Row(src, buf)
            DxtVariant.DXT5 -> decodeDxt5Row(src, buf)
        }
        row++
        return buf.size
    }

    override fun dimensions(): Pair<UInt, UInt> =
        Pair(widthBlocks * 4u, heightBlocks * 4u)

    override fun colorType(): ColorType = variant.colorType()

    override fun readImage(buf: ByteArray) {
        val total = totalBytes().toInt()
        require(buf.size == total) {
            "Buffer size ${buf.size} does not match total bytes $total"
        }

        val scanlineSize = scanlineBytes().toInt().coerceAtLeast(1)
        var offset = 0
        while (offset < buf.size) {
            val end = (offset + scanlineSize).coerceAtMost(buf.size)
            val chunk = ByteArray(end - offset)
            readScanline(chunk)
            chunk.copyInto(buf, destinationOffset = offset)
            offset = end
        }
    }

    override fun readImageBoxed(buf: ByteArray) {
        readImage(buf)
    }
}

internal typealias Rgb = ByteArray

/**
 * Decodes a 5-bit R, 6-bit G, 5-bit B 16-bit packed color value into 8-bit RGB.
 *
 * Mapping is done so min/max range values are preserved. So for 5-bit values
 * `0x00` -> `0x00` and `0x1F` -> `0xFF`.
 */
internal fun enc565Decode(value: UShort): ByteArray {
    val v = value.toInt()
    val red = (v ushr 11) and 0x1F
    val green = (v ushr 5) and 0x3F
    val blue = v and 0x1F
    return byteArrayOf(
        ((red * 0xFF) / 0x1F).toByte(),
        ((green * 0xFF) / 0x3F).toByte(),
        ((blue * 0xFF) / 0x1F).toByte(),
    )
}

/**
 * Constructs the DXT5 alpha lookup table from the two alpha entries.
 *
 * If `alpha0 > alpha1`, constructs a table of `[a0, a1, 6 linearly interpolated values from a0 to a1]`.
 * If `alpha0 <= alpha1`, constructs a table of `[a0, a1, 4 linearly interpolated values from a0 to a1, 0, 0xFF]`.
 */
internal fun alphaTableDxt5(alpha0: UByte, alpha1: UByte): ByteArray {
    val a0 = alpha0.toInt()
    val a1 = alpha1.toInt()
    val table = ByteArray(8)
    table[0] = a0.toByte()
    table[1] = a1.toByte()
    table[2] = 0
    table[3] = 0
    table[4] = 0
    table[5] = 0
    table[6] = 0
    table[7] = 0xFF.toByte()

    if (a0 > a1) {
        for (i in 2..7) {
            table[i] = (((8 - i) * a0 + (i - 1) * a1) / 7).toByte()
        }
    } else {
        for (i in 2..5) {
            table[i] = (((6 - i) * a0 + (i - 1) * a1) / 5).toByte()
        }
    }
    return table
}

/**
 * Decodes an 8-byte DXT color block into the RGB channels of a 16xRGB or 16xRGBA block.
 *
 * [source] should have a length of at least [sourceOffset] + 8, and [destLen] must be 48 (RGB) or 64 (RGBA).
 */
internal fun decodeDxtColors(
    source: ByteArray,
    sourceOffset: Int,
    dest: ByteArray,
    destOffset: Int,
    destLen: Int,
    isDxt1: Boolean,
) {
    require(source.size >= sourceOffset + 8 && (destLen == 48 || destLen == 64)) {
        "Invalid source or destination buffer sizes"
    }
    val pitch = destLen / 16

    val color0 = (source[sourceOffset].toInt() and 0xFF) or ((source[sourceOffset + 1].toInt() and 0xFF) shl 8)
    val color1 = (source[sourceOffset + 2].toInt() and 0xFF) or ((source[sourceOffset + 3].toInt() and 0xFF) shl 8)
    val colorTable =
        (source[sourceOffset + 4].toLong() and 0xFFL) or
            ((source[sourceOffset + 5].toLong() and 0xFFL) shl 8) or
            ((source[sourceOffset + 6].toLong() and 0xFFL) shl 16) or
            ((source[sourceOffset + 7].toLong() and 0xFFL) shl 24)

    val c0 = enc565Decode(color0.toUShort())
    val c1 = enc565Decode(color1.toUShort())
    val c2 = ByteArray(3)
    val c3 = ByteArray(3)

    val c00 = c0[0].toInt() and 0xFF
    val c01 = c0[1].toInt() and 0xFF
    val c02 = c0[2].toInt() and 0xFF
    val c10 = c1[0].toInt() and 0xFF
    val c11 = c1[1].toInt() and 0xFF
    val c12 = c1[2].toInt() and 0xFF

    if (color0 > color1 || !isDxt1) {
        c2[0] = ((c00 * 2 + c10 + 1) / 3).toByte()
        c2[1] = ((c01 * 2 + c11 + 1) / 3).toByte()
        c2[2] = ((c02 * 2 + c12 + 1) / 3).toByte()

        c3[0] = ((c00 + c10 * 2 + 1) / 3).toByte()
        c3[1] = ((c01 + c11 * 2 + 1) / 3).toByte()
        c3[2] = ((c02 + c12 * 2 + 1) / 3).toByte()
    } else {
        c2[0] = ((c00 + c10 + 1) / 2).toByte()
        c2[1] = ((c01 + c11 + 1) / 2).toByte()
        c2[2] = ((c02 + c12 + 1) / 2).toByte()
        c3[0] = 0
        c3[1] = 0
        c3[2] = 0
    }

    val palette = arrayOf(c0, c1, c2, c3)

    for (i in 0 until 16) {
        val code = ((colorTable ushr (i * 2)) and 0x3L).toInt()
        val chosen = palette[code]
        val target = destOffset + i * pitch
        dest[target] = chosen[0]
        dest[target + 1] = chosen[1]
        dest[target + 2] = chosen[2]
    }
}

/**
 * Decodes an 8-byte block of DXT1 data to a 16xRGB block.
 */
internal fun decodeDxt1Block(source: ByteArray, sourceOffset: Int, dest: ByteArray, destOffset: Int) {
    decodeDxtColors(source, sourceOffset, dest, destOffset, 48, isDxt1 = true)
}

/**
 * Decodes a 16-byte block of DXT3 data to a 16xRGBA block.
 */
internal fun decodeDxt3Block(source: ByteArray, sourceOffset: Int, dest: ByteArray, destOffset: Int) {
    var alphaTable = 0L
    for (idx in 7 downTo 0) {
        val b = source[sourceOffset + idx].toLong() and 0xFFL
        alphaTable = (alphaTable shl 8) or b
    }

    for (i in 0 until 16) {
        val alphaNibble = ((alphaTable ushr (i * 4)) and 0xFL).toInt()
        dest[destOffset + i * 4 + 3] = (alphaNibble * 0x11).toByte()
    }

    decodeDxtColors(source, sourceOffset + 8, dest, destOffset, 64, isDxt1 = false)
}

/**
 * Decodes a 16-byte block of DXT5 data to a 16xRGBA block.
 */
internal fun decodeDxt5Block(source: ByteArray, sourceOffset: Int, dest: ByteArray, destOffset: Int) {
    var alphaTable = 0L
    for (idx in 7 downTo 2) {
        val b = source[sourceOffset + idx].toLong() and 0xFFL
        alphaTable = (alphaTable shl 8) or b
    }

    val alpha0 = source[sourceOffset].toUByte()
    val alpha1 = source[sourceOffset + 1].toUByte()
    val alphas = alphaTableDxt5(alpha0, alpha1)

    for (i in 0 until 16) {
        val alphaIndex = ((alphaTable ushr (i * 3)) and 0x7L).toInt()
        dest[destOffset + i * 4 + 3] = alphas[alphaIndex]
    }

    decodeDxtColors(source, sourceOffset + 8, dest, destOffset, 64, isDxt1 = false)
}

/**
 * Decode a row of DXT1 data to four rows of RGB data.
 */
internal fun decodeDxt1Row(source: ByteArray, dest: ByteArray) {
    require(source.size % 8 == 0) { "Source length must be a multiple of 8" }
    val blockCount = source.size / 8
    require(dest.size >= blockCount * 48) { "Destination buffer too small" }

    val decodedBlock = ByteArray(48)
    for (x in 0 until blockCount) {
        decodeDxt1Block(source, x * 8, decodedBlock, 0)
        for (line in 0 until 4) {
            val offset = (blockCount * line + x) * 12
            decodedBlock.copyInto(
                dest,
                destinationOffset = offset,
                startIndex = line * 12,
                endIndex = (line + 1) * 12,
            )
        }
    }
}

/**
 * Decode a row of DXT3 data to four rows of RGBA data.
 */
internal fun decodeDxt3Row(source: ByteArray, dest: ByteArray) {
    require(source.size % 16 == 0) { "Source length must be a multiple of 16" }
    val blockCount = source.size / 16
    require(dest.size >= blockCount * 64) { "Destination buffer too small" }

    val decodedBlock = ByteArray(64)
    for (x in 0 until blockCount) {
        decodeDxt3Block(source, x * 16, decodedBlock, 0)
        for (line in 0 until 4) {
            val offset = (blockCount * line + x) * 16
            decodedBlock.copyInto(
                dest,
                destinationOffset = offset,
                startIndex = line * 16,
                endIndex = (line + 1) * 16,
            )
        }
    }
}

/**
 * Decode a row of DXT5 data to four rows of RGBA data.
 */
internal fun decodeDxt5Row(source: ByteArray, dest: ByteArray) {
    require(source.size % 16 == 0) { "Source length must be a multiple of 16" }
    val blockCount = source.size / 16
    require(dest.size >= blockCount * 64) { "Destination buffer too small" }

    val decodedBlock = ByteArray(64)
    for (x in 0 until blockCount) {
        decodeDxt5Block(source, x * 16, decodedBlock, 0)
        for (line in 0 until 4) {
            val offset = (blockCount * line + x) * 16
            decodedBlock.copyInto(
                dest,
                destinationOffset = offset,
                startIndex = line * 16,
                endIndex = (line + 1) * 16,
            )
        }
    }
}
