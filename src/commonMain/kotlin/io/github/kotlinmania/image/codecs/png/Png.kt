// port-lint: source codecs/png.rs
package io.github.kotlinmania.image.codecs.png

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.images.DynamicImage
import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.MethodSealedToImage
import io.github.kotlinmania.image.io.writeAll

/**
 * The first eight bytes of a PNG file always contain the following values:
 * 137, 80, 78, 71, 13, 10, 26, 10
 */
public val PNG_SIGNATURE: ByteArray = byteArrayOf(
    137.toByte(), 80.toByte(), 78.toByte(), 71.toByte(),
    13.toByte(), 10.toByte(), 26.toByte(), 10.toByte(),
)

/**
 * DEFLATE compression level of a PNG encoder.
 */
public sealed class CompressionType {
    /** Default compression level */
    public data object Default : CompressionType()

    /** Fast, minimal compression */
    public data object Fast : CompressionType()

    /** High compression level */
    public data object Best : CompressionType()

    /** No compression whatsoever */
    public data object Uncompressed : CompressionType()

    /** Detailed compression level between 1 and 9 */
    public data class Level(public val level: UByte) : CompressionType()
}

/**
 * Filter algorithms used to process image data to improve compression.
 */
public enum class FilterType {
    /** No processing done */
    NoFilter,

    /** Filters based on previous pixel in the same scanline */
    Sub,

    /** Filters based on the scanline above */
    Up,

    /** Filters based on the average of left and right neighbor pixels */
    Avg,

    /** Paeth filter algorithm */
    Paeth,

    /** Heuristic selection of filter per scanline */
    Adaptive,
}

/**
 * PNG encoder representation.
 */
public class PngEncoder internal constructor(
    private val writer: IoWrite,
    private val compression: CompressionType = CompressionType.Fast,
    private val filter: FilterType = FilterType.Adaptive,
) : ImageEncoder {
    public constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite)

    public constructor(
        writeBuffer: BufferIoWrite,
        compression: CompressionType,
        filter: FilterType,
    ) : this(writeBuffer as IoWrite, compression, filter)

    private var iccProfile: ByteArray = ByteArray(0)
    private var exifMetadata: ByteArray = ByteArray(0)

    override fun setIccProfile(iccProfile: ByteArray) {
        this.iccProfile = iccProfile
    }

    override fun setExifMetadata(exif: ByteArray) {
        this.exifMetadata = exif
    }

    /**
     * Encodes the image buffer to PNG format.
     */
    public fun encode(
        data: ByteArray,
        width: UInt,
        height: UInt,
        color: ExtendedColorType,
    ) {
        val expectedBufferLen = color.bufferSize(width, height)
        require(expectedBufferLen == data.size.toULong()) {
            "Invalid buffer length: expected $expectedBufferLen got ${data.size} for ${width}x$height image"
        }

        when (color) {
            ExtendedColorType.L8,
            ExtendedColorType.L16,
            ExtendedColorType.La8,
            ExtendedColorType.La16,
            ExtendedColorType.Rgb8,
            ExtendedColorType.Rgb16,
            ExtendedColorType.Rgba8,
            ExtendedColorType.Rgba16,
            -> {
                // Write PNG signature
                writer.writeAll(PNG_SIGNATURE)
                // Write minimal uncompressed PNG chunks (IHDR, IDAT, IEND) or delegate
                val w = width.toInt()
                val h = height.toInt()
                writeIhdr(w, h, color)
                writeIdat(data, w, h, color)
                writeIend()
            }
            else -> throw ImageError.Unsupported(
                UnsupportedError(
                    ImageFormatHint.Exact(ImageFormat.Png),
                    UnsupportedErrorKind.Color(color),
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
    ): DynamicImage? = null

    private fun writeIhdr(width: Int, height: Int, color: ExtendedColorType) {
        val (bitDepth, colorTypeByte) = when (color) {
            ExtendedColorType.L8 -> 8 to 0
            ExtendedColorType.L16 -> 16 to 0
            ExtendedColorType.La8 -> 8 to 4
            ExtendedColorType.La16 -> 16 to 4
            ExtendedColorType.Rgb8 -> 8 to 2
            ExtendedColorType.Rgb16 -> 16 to 2
            ExtendedColorType.Rgba8 -> 8 to 6
            ExtendedColorType.Rgba16 -> 16 to 6
            else -> 8 to 6
        }

        val chunkData = ByteArray(13)
        chunkData[0] = ((width shr 24) and 0xFF).toByte()
        chunkData[1] = ((width shr 16) and 0xFF).toByte()
        chunkData[2] = ((width shr 8) and 0xFF).toByte()
        chunkData[3] = (width and 0xFF).toByte()

        chunkData[4] = ((height shr 24) and 0xFF).toByte()
        chunkData[5] = ((height shr 16) and 0xFF).toByte()
        chunkData[6] = ((height shr 8) and 0xFF).toByte()
        chunkData[7] = (height and 0xFF).toByte()

        chunkData[8] = bitDepth.toByte()
        chunkData[9] = colorTypeByte.toByte()
        chunkData[10] = 0 // Compression method: deflate
        chunkData[11] = 0 // Filter method: standard
        chunkData[12] = 0 // Interlace method: none

        writeChunk("IHDR", chunkData)
    }

    private fun writeIdat(data: ByteArray, width: Int, height: Int, color: ExtendedColorType) {
        val bytesPerPixel = (color.bitsPerPixel().toInt() + 7) / 8
        val scanlineLength = width * bytesPerPixel
        // Add 1 filter byte (0 = NoFilter) per scanline
        val rawData = ByteArray(height * (1 + scanlineLength))
        var srcPos = 0
        var dstPos = 0
        for (y in 0 until height) {
            rawData[dstPos++] = 0 // Filter type 0 (None)
            data.copyInto(rawData, dstPos, srcPos, srcPos + scanlineLength)
            dstPos += scanlineLength
            srcPos += scanlineLength
        }

        // Deflate uncompressed blocks (stored blocks)
        val deflated = deflateStore(rawData)
        writeChunk("IDAT", deflated)
    }

    private fun writeIend() {
        writeChunk("IEND", ByteArray(0))
    }

    private fun writeChunk(type: String, data: ByteArray) {
        val typeBytes = type.encodeToByteArray()
        val len = data.size
        val lenBytes = byteArrayOf(
            ((len shr 24) and 0xFF).toByte(),
            ((len shr 16) and 0xFF).toByte(),
            ((len shr 8) and 0xFF).toByte(),
            (len and 0xFF).toByte(),
        )
        writer.writeAll(lenBytes)
        writer.writeAll(typeBytes)
        if (data.isNotEmpty()) {
            writer.writeAll(data)
        }

        // CRC-32 over type + data
        val crc = crc32(typeBytes, data)
        val crcBytes = byteArrayOf(
            ((crc shr 24) and 0xFF).toByte(),
            ((crc shr 16) and 0xFF).toByte(),
            ((crc shr 8) and 0xFF).toByte(),
            (crc and 0xFF).toByte(),
        )
        writer.writeAll(crcBytes)
    }

    private fun deflateStore(data: ByteArray): ByteArray {
        val out = mutableListOf<Byte>()
        // zlib header: CMF = 0x78 (deflate, 32K window), FLG = 0x01 (check bits)
        out.add(0x78.toByte())
        out.add(0x01.toByte())

        // Max uncompressed block size in Deflate is 65535
        val maxBlockSize = 65535
        var pos = 0
        while (pos < data.size) {
            val remaining = data.size - pos
            val blockSize = remaining.coerceAtMost(maxBlockSize)
            val isFinal = (pos + blockSize >= data.size)
            val bheader = if (isFinal) 0x01 else 0x00
            out.add(bheader.toByte())

            val len = blockSize
            val nlen = blockSize.inv() and 0xFFFF
            out.add((len and 0xFF).toByte())
            out.add(((len shr 8) and 0xFF).toByte())
            out.add((nlen and 0xFF).toByte())
            out.add(((nlen shr 8) and 0xFF).toByte())

            for (i in 0 until blockSize) {
                out.add(data[pos + i])
            }
            pos += blockSize
        }

        // Adler-32 checksum of uncompressed data
        val adler = adler32(data)
        out.add(((adler shr 24) and 0xFF).toByte())
        out.add(((adler shr 16) and 0xFF).toByte())
        out.add(((adler shr 8) and 0xFF).toByte())
        out.add((adler and 0xFF).toByte())

        return out.toByteArray()
    }

    private fun adler32(data: ByteArray): Int {
        var s1 = 1
        var s2 = 0
        for (b in data) {
            s1 = (s1 + (b.toInt() and 0xFF)) % 65521
            s2 = (s2 + s1) % 65521
        }
        return (s2 shl 16) or s1
    }

    private fun crc32(type: ByteArray, data: ByteArray): Int {
        var crc = -1
        for (b in type) {
            crc = CRC_TABLE[(crc xor (b.toInt() and 0xFF)) and 0xFF] xor (crc ushr 8)
        }
        for (b in data) {
            crc = CRC_TABLE[(crc xor (b.toInt() and 0xFF)) and 0xFF] xor (crc ushr 8)
        }
        return crc.inv()
    }

    private companion object {
        val CRC_TABLE: IntArray = IntArray(256) { i ->
            var c = i
            for (j in 0 until 8) {
                c = if ((c and 1) != 0) -0x124774cd xor (c ushr 1) else c ushr 1
            }
            c
        }
    }
}
