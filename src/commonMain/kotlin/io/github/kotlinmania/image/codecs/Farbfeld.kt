// port-lint: source codecs/farbfeld.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.readExact
import io.github.kotlinmania.image.io.writeAll
import io.github.kotlinmania.image.utils.checkDimensionOverflow

private val FARBFELD_MAGIC =
    byteArrayOf(
        0x66,
        0x61,
        0x72,
        0x62,
        0x66,
        0x65,
        0x6c,
        0x64, // "farbfeld"
    )

/**
 * Farbfeld image decoder.
 */
public class FarbfeldDecoder internal constructor(
    private val reader: IoRead,
) : io.github.kotlinmania.image.io.ImageDecoderRect {
    private val width: UInt
    private val height: UInt

    public constructor(bytes: ByteArray) : this(BufferIoRead(bytes))

    init {
        val magic = ByteArray(8)
        try {
            reader.readExact(magic)
        } catch (e: Exception) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Farbfeld),
                    "Failed to read farbfeld magic: ${e.message}",
                ),
            )
        }

        if (!magic.contentEquals(FARBFELD_MAGIC)) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Farbfeld),
                    "Invalid magic",
                ),
            )
        }

        val dimBuf = ByteArray(8)
        try {
            reader.readExact(dimBuf)
        } catch (e: Exception) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Farbfeld),
                    "Failed to read farbfeld dimensions: ${e.message}",
                ),
            )
        }

        val w =
            (
                (dimBuf[0].toLong() and 0xFF shl 24) or
                    (dimBuf[1].toLong() and 0xFF shl 16) or
                    (dimBuf[2].toLong() and 0xFF shl 8) or
                    (dimBuf[3].toLong() and 0xFF)
            ).toUInt()

        val h =
            (
                (dimBuf[4].toLong() and 0xFF shl 24) or
                    (dimBuf[5].toLong() and 0xFF shl 16) or
                    (dimBuf[6].toLong() and 0xFF shl 8) or
                    (dimBuf[7].toLong() and 0xFF)
            ).toUInt()

        if (checkDimensionOverflow(w, h, 8u)) {
            throw ImageError.Unsupported(
                UnsupportedError(
                    ImageFormatHint.Exact(ImageFormat.Farbfeld),
                    UnsupportedErrorKind.GenericFeature("Image dimensions (${w}x$h) are too large"),
                ),
            )
        }

        width = w
        height = h
    }

    override fun dimensions(): Pair<UInt, UInt> = Pair(width, height)

    override fun colorType(): ColorType = ColorType.Rgba16

    override fun readImage(buf: ByteArray) {
        val expected = totalBytes().toLong()
        require(buf.size.toLong() == expected) {
            "Invalid buffer size: expected $expected, got ${buf.size}"
        }
        reader.readExact(buf)
    }

    override fun readRect(
        x: UInt,
        y: UInt,
        width: UInt,
        height: UInt,
        buf: ByteArray,
        rowPitch: Int,
    ) {
        val totalBytes = width.toLong() * height.toLong() * 8L
        require(buf.size >= totalBytes) {
            "output buffer too short: expected $totalBytes, provided ${buf.size}"
        }
        val fullData = ByteArray((this.width.toLong() * this.height.toLong() * 8L).toInt())
        readImage(fullData)
        val fullRowPitch = this.width.toInt() * 8
        for (r in 0 until height.toInt()) {
            val srcRow = (y.toInt() + r) * fullRowPitch + (x.toInt() * 8)
            val dstRow = r * rowPitch
            fullData.copyInto(buf, destinationOffset = dstRow, startIndex = srcRow, endIndex = srcRow + width.toInt() * 8)
        }
    }
}

/**
 * Farbfeld image encoder.
 */
public class FarbfeldEncoder internal constructor(
    private val writer: IoWrite,
) : ImageEncoder {
    internal constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite)

    /**
     * Encodes the image data (native/big-endian) that has dimensions width and height.
     */
    public fun encode(data: ByteArray, width: UInt, height: UInt) {
        val expectedLen = (width.toULong() * height.toULong() * 8uL).toLong()
        require(data.size.toLong() == expectedLen) {
            "Invalid buffer length: expected $expectedLen got ${data.size} for ${width}x$height image"
        }

        writer.writeAll(FARBFELD_MAGIC)

        val header = ByteArray(8)
        val w = width.toLong()
        header[0] = (w ushr 24).toByte()
        header[1] = (w ushr 16).toByte()
        header[2] = (w ushr 8).toByte()
        header[3] = w.toByte()

        val h = height.toLong()
        header[4] = (h ushr 24).toByte()
        header[5] = (h ushr 16).toByte()
        header[6] = (h ushr 8).toByte()
        header[7] = h.toByte()

        writer.writeAll(header)
        writer.writeAll(data)
    }

    override fun writeImage(
        buf: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ) {
        if (colorType != ExtendedColorType.Rgba16) {
            throw ImageError.Unsupported(
                UnsupportedError(
                    ImageFormatHint.Exact(ImageFormat.Farbfeld),
                    UnsupportedErrorKind.Color(colorType),
                ),
            )
        }
        encode(buf, width, height)
    }
}

/**
 * Farbfeld reader.
 */
public class FarbfeldReader(
    public val width: UInt,
    public val height: UInt,
    private val inner: IoRead,
) {
    public companion object {
        public fun new(reader: IoRead): FarbfeldReader {
            val magic = ByteArray(8)
            reader.readExact(magic)
            if (!magic.contentEquals(FARBFELD_MAGIC)) {
                throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Farbfeld), "Invalid farbfeld magic"))
            }
            val wBuf = ByteArray(4)
            reader.readExact(wBuf)
            val hBuf = ByteArray(4)
            reader.readExact(hBuf)
            val w = ((wBuf[0].toInt() and 0xFF) shl 24) or ((wBuf[1].toInt() and 0xFF) shl 16) or ((wBuf[2].toInt() and 0xFF) shl 8) or (wBuf[3].toInt() and 0xFF)
            val h = ((hBuf[0].toInt() and 0xFF) shl 24) or ((hBuf[1].toInt() and 0xFF) shl 16) or ((hBuf[2].toInt() and 0xFF) shl 8) or (hBuf[3].toInt() and 0xFF)
            return FarbfeldReader(w.toUInt(), h.toUInt(), reader)
        }
    }
}

