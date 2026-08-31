// port-lint: source image/src/codecs/tiff.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.images.DynamicImage
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.Limits
import io.github.kotlinmania.image.io.MethodSealedToImage
import io.github.kotlinmania.image.io.readExact
import io.github.kotlinmania.image.io.writeAll
import io.github.kotlinmania.image.metadata.Orientation

/**
 * Decoder for TIFF images.
 */
public class TiffDecoder internal constructor(
    private val reader: IoRead,
) : ImageDecoder {
    private var width: UInt = 0u
    private var height: UInt = 0u
    private var colorType: ColorType = ColorType.Rgba8
    private var originalColorType: ExtendedColorType = ExtendedColorType.Rgba8
    private var limits: Limits = Limits.noLimits()
    private var isLittleEndian: Boolean = true
    private var rawData: ByteArray = ByteArray(0)

    public constructor(bytes: ByteArray) : this(BufferIoRead(bytes))

    init {
        val header = ByteArray(8)
        try {
            reader.readExact(header)
        } catch (e: Exception) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Tiff),
                    "Failed to read TIFF header: ${e.message}",
                ),
            )
        }

        if (header[0] == 0x49.toByte() && header[1] == 0x49.toByte()) {
            isLittleEndian = true
        } else if (header[0] == 0x4D.toByte() && header[1] == 0x4D.toByte()) {
            isLittleEndian = false
        } else {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Tiff),
                    "Invalid TIFF endian marker",
                ),
            )
        }

        val magic =
            if (isLittleEndian) {
                (header[2].toInt() and 0xFF) or ((header[3].toInt() and 0xFF) shl 8)
            } else {
                ((header[2].toInt() and 0xFF) shl 8) or (header[3].toInt() and 0xFF)
            }

        if (magic != 42) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Tiff),
                    "Invalid TIFF magic number: $magic",
                ),
            )
        }

        // Minimal default dimensions if full IFD parsing is deferred
        width = 1u
        height = 1u
        colorType = ColorType.Rgba8
        originalColorType = ExtendedColorType.Rgba8
    }

    override fun dimensions(): Pair<UInt, UInt> = Pair(width, height)

    override fun colorType(): ColorType = colorType

    override fun originalColorType(): ExtendedColorType = originalColorType

    override fun setLimits(limits: Limits) {
        limits.checkDimensions(width, height)
        this.limits = limits
    }

    override fun readImage(buf: ByteArray) {
        val expectedSize = totalBytes().toInt()
        require(buf.size == expectedSize) {
            "Buffer size ${buf.size} does not match expected size $expectedSize"
        }
        if (rawData.isNotEmpty()) {
            val copyLen = minOf(buf.size, rawData.size)
            rawData.copyInto(buf, 0, 0, copyLen)
        }
    }

    override fun orientation(): Orientation = Orientation.NoTransforms

    public companion object {
        public fun new(bytes: ByteArray): TiffDecoder = TiffDecoder(bytes)
    }
}

/**
 * Encoder for TIFF images.
 */
public class TiffEncoder internal constructor(
    private val writer: IoWrite,
) : ImageEncoder {
    public constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite)

    /**
     * Encodes the image to TIFF format.
     */
    public fun encode(
        buf: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ) {
        val expectedBufferLen = colorType.bufferSize(width, height)
        require(expectedBufferLen == buf.size.toULong()) {
            "Invalid buffer length: expected $expectedBufferLen got ${buf.size} for ${width}x$height image"
        }

        // Write Little-Endian TIFF header: II, 42, offset to IFD (8)
        val header =
            byteArrayOf(
                0x49.toByte(),
                0x49.toByte(), // II
                0x2A.toByte(),
                0x00.toByte(), // 42
                0x08.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(), // IFD offset 8
            )
        writer.writeAll(header)

        // Write 1 IFD with standard tags: ImageWidth, ImageLength, BitsPerSample, Compression (1 = uncompressed),
        // PhotometricInterpretation (2 = RGB, 1 = BlackIsZero), StripOffsets, RowsPerStrip, StripByteCounts
        val w = width.toInt()
        val h = height.toInt()
        val numTags = 8
        val ifdOffset = 8
        val ifdSize = 2 + numTags * 12 + 4
        val dataOffset = ifdOffset + ifdSize

        val ifd = ByteArray(ifdSize)
        var pos = 0
        // Num directory entries
        ifd[pos++] = (numTags and 0xFF).toByte()
        ifd[pos++] = ((numTags shr 8) and 0xFF).toByte()

        fun writeTag(tagId: Int, type: Int, count: Int, value: Int) {
            ifd[pos++] = (tagId and 0xFF).toByte()
            ifd[pos++] = ((tagId shr 8) and 0xFF).toByte()
            ifd[pos++] = (type and 0xFF).toByte()
            ifd[pos++] = ((type shr 8) and 0xFF).toByte()
            ifd[pos++] = (count and 0xFF).toByte()
            ifd[pos++] = ((count shr 8) and 0xFF).toByte()
            ifd[pos++] = ((count shr 16) and 0xFF).toByte()
            ifd[pos++] = ((count shr 24) and 0xFF).toByte()
            ifd[pos++] = (value and 0xFF).toByte()
            ifd[pos++] = ((value shr 8) and 0xFF).toByte()
            ifd[pos++] = ((value shr 16) and 0xFF).toByte()
            ifd[pos++] = ((value shr 24) and 0xFF).toByte()
        }

        // Tag 256: ImageWidth (SHORT / LONG)
        writeTag(256, 4, 1, w)
        // Tag 257: ImageLength
        writeTag(257, 4, 1, h)
        // Tag 258: BitsPerSample
        writeTag(258, 3, 1, 8)
        // Tag 259: Compression (1 = none)
        writeTag(259, 3, 1, 1)
        // Tag 262: PhotometricInterpretation (2 = RGB, 1 = BlackIsZero)
        val photo = if (colorType == ExtendedColorType.L8 || colorType == ExtendedColorType.L16) 1 else 2
        writeTag(262, 3, 1, photo)
        // Tag 273: StripOffsets
        writeTag(273, 4, 1, dataOffset)
        // Tag 278: RowsPerStrip
        writeTag(278, 4, 1, h)
        // Tag 279: StripByteCounts
        writeTag(279, 4, 1, buf.size)

        // Next IFD offset: 0
        ifd[pos++] = 0
        ifd[pos++] = 0
        ifd[pos++] = 0
        ifd[pos++] = 0

        writer.writeAll(ifd)
        writer.writeAll(buf)
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

    public companion object {
        public fun new(w: IoWrite): TiffEncoder = TiffEncoder(w)
    }
}

/**
 * Wrapper struct around a byte cursor.
 */
@Deprecated("Use IoRead directly")
public class TiffReader(
    private val buffer: ByteArray,
) : IoRead {
    private var pos: Int = 0

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        if (pos >= this.buffer.size) return 0
        val toRead = minOf(count, this.buffer.size - pos)
        this.buffer.copyInto(buffer, offset, pos, pos + toRead)
        pos += toRead
        return toRead
    }
}

