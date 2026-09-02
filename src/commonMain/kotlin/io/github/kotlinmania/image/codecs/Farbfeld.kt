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
import io.github.kotlinmania.image.io.ImageDecoderRect
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoErrorKind
import io.github.kotlinmania.image.io.IoException
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.IoSeek
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.SeekFrom
import io.github.kotlinmania.image.io.loadRect
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
 * Reads a 32-bit big-endian unsigned dimension from the given reader.
 */
public fun readDimm(from: IoRead): UInt {
    val buf = ByteArray(4)
    from.readExact(buf)
    return (
        ((buf[0].toLong() and 0xFF) shl 24) or
            ((buf[1].toLong() and 0xFF) shl 16) or
            ((buf[2].toLong() and 0xFF) shl 8) or
            (buf[3].toLong() and 0xFF)
    ).toUInt()
}

/**
 * Consumes a single 16-bit big-endian channel and writes it in native endian order.
 */
public fun consumeChannel(from: IoRead, to: ByteArray, toOffset: Int = 0) {
    val ibuf = ByteArray(2)
    from.readExact(ibuf)
    val v = ((ibuf[0].toInt() and 0xFF) shl 8) or (ibuf[1].toInt() and 0xFF)
    to[toOffset] = (v ushr 8).toByte()
    to[toOffset + 1] = v.toByte()
}

/**
 * Reads 2 bytes, stores the second in cachedByte, and returns the first.
 */
public fun cacheByte(from: IoRead, cachedByteOut: (Byte?) -> Unit): Byte {
    val obuf = ByteArray(2)
    consumeChannel(from, obuf)
    cachedByteOut(obuf[1])
    return obuf[0]
}

/**
 * Parses seek offset relative to current position.
 */
public fun parseOffset(
    originalOffset: ULong,
    endOffset: ULong,
    pos: SeekFrom,
): Long? =
    when (pos) {
        is SeekFrom.Start -> {
            val target = pos.offset
            if (target < 0) null else target - originalOffset.toLong()
        }
        is SeekFrom.End -> {
            val endSigned = endOffset.toLong()
            if (pos.offset < -endSigned) {
                null
            } else {
                (endOffset - originalOffset).toLong() + pos.offset
            }
        }
        is SeekFrom.Current -> {
            val origSigned = originalOffset.toLong()
            if (pos.offset < -origSigned) {
                null
            } else {
                pos.offset
            }
        }
    }

/**
 * farbfeld Reader.
 */
public class FarbfeldReader(
    public val width: UInt,
    public val height: UInt,
    public val inner: IoRead,
    private var currentOffset: ULong = 0uL,
    private var cachedByte: Byte? = null,
) : IoRead, IoSeek {
    public fun currentOffset(): ULong = currentOffset

    public fun cachedByte(): Byte? = cachedByte

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        if (count <= 0) return 0
        var bytesWritten = 0
        var curOffset = offset
        var remaining = count

        val cached = cachedByte
        if (cached != null) {
            buffer[curOffset] = cached
            cachedByte = null
            curOffset += 1
            remaining -= 1
            bytesWritten += 1
            currentOffset += 1uL
        }

        if (remaining == 1) {
            val b = cacheByte(inner) { cachedByte = it }
            buffer[curOffset] = b
            bytesWritten += 1
            currentOffset += 1uL
        } else {
            val pairs = remaining / 2
            for (i in 0 until pairs) {
                consumeChannel(inner, buffer, curOffset)
                curOffset += 2
                bytesWritten += 2
                currentOffset += 2uL
            }
            if (remaining % 2 == 1) {
                val b = cacheByte(inner) { cachedByte = it }
                buffer[curOffset] = b
                bytesWritten += 1
                currentOffset += 1uL
            }
        }

        return bytesWritten
    }

    override fun seek(pos: SeekFrom): Long {
        val seekable = inner as? IoSeek ?: throw IoException(IoErrorKind.Other, "Inner reader does not support seek")
        val originalOffset = this.currentOffset
        val endOffset = width.toULong() * height.toULong() * 8uL
        val offsetFromCurrent = parseOffset(originalOffset, endOffset, pos)
            ?: throw IoException(IoErrorKind.InvalidInput, "invalid seek to a negative or overflowing position")

        seekable.seek(SeekFrom.Current(offsetFromCurrent))
        this.currentOffset = if (offsetFromCurrent < 0) {
            originalOffset - (-offsetFromCurrent).toULong()
        } else {
            originalOffset + offsetFromCurrent.toULong()
        }

        if (this.currentOffset < endOffset && (this.currentOffset % 2uL) == 1uL) {
            val curr = seekable.seek(SeekFrom.Current(-1L))
            cacheByte(inner) { cachedByte = it }
            seekable.seek(SeekFrom.Start(curr))
        } else {
            this.cachedByte = null
        }

        return originalOffset.toLong()
    }

    public companion object {
        public fun new(bufferedRead: IoRead): FarbfeldReader {
            val magic = ByteArray(8)
            try {
                bufferedRead.readExact(magic)
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

            val width = try {
                readDimm(bufferedRead)
            } catch (e: Exception) {
                throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Farbfeld), e))
            }
            val height = try {
                readDimm(bufferedRead)
            } catch (e: Exception) {
                throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Farbfeld), e))
            }

            if (checkDimensionOverflow(width, height, 8u)) {
                throw ImageError.Unsupported(
                    UnsupportedError(
                        ImageFormatHint.Exact(ImageFormat.Farbfeld),
                        UnsupportedErrorKind.GenericFeature("Image dimensions (${width}x$height) are too large"),
                    ),
                )
            }

            return FarbfeldReader(width, height, bufferedRead)
        }
    }
}

/**
 * farbfeld decoder.
 */
public class FarbfeldDecoder(
    private val reader: FarbfeldReader,
) : ImageDecoderRect {
    public constructor(bufferedRead: IoRead) : this(FarbfeldReader.new(bufferedRead))
    public constructor(bytes: ByteArray) : this(BufferIoRead(bytes))

    public fun reader(): FarbfeldReader = reader

    override fun dimensions(): Pair<UInt, UInt> = Pair(reader.width, reader.height)

    override fun colorType(): ColorType = ColorType.Rgba16

    override fun readImage(buf: ByteArray) {
        val expected = totalBytes().toLong()
        require(buf.size.toLong() == expected) {
            "Invalid buffer size: expected $expected, got ${buf.size}"
        }
        reader.readExact(buf)
    }

    override fun readImageBoxed(buf: ByteArray) {
        readImage(buf)
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
        val start = (reader.inner as? IoSeek)?.streamPosition() ?: 0L
        loadRect(
            x = x,
            y = y,
            width = width,
            height = height,
            buf = buf,
            rowPitch = rowPitch,
            decoder = this,
            scanlineBytes = 2,
            seekScanline = { dec, scanline ->
                val fDec = dec as FarbfeldDecoder
                (fDec.reader as IoSeek).seek(SeekFrom.Start((scanline * 2uL).toLong()))
            },
            readScanline = { dec, dest ->
                val fDec = dec as FarbfeldDecoder
                fDec.reader.readExact(dest)
            },
        )
        if (reader.inner is IoSeek) {
            reader.seek(SeekFrom.Start(start))
        }
    }

    public companion object {
        public fun new(bufferedRead: IoRead): FarbfeldDecoder = FarbfeldDecoder(bufferedRead)
    }
}

/**
 * farbfeld encoder.
 */
public class FarbfeldEncoder(
    private val writer: IoWrite,
) : ImageEncoder {
    public constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite)

    /**
     * Encodes the image data (native/big-endian) that has dimensions width and height.
     */
    public fun encode(data: ByteArray, width: UInt, height: UInt) {
        val expectedLen = (width.toULong() * height.toULong() * 8uL).toLong()
        require(data.size.toLong() == expectedLen) {
            "Invalid buffer length: expected $expectedLen got ${data.size} for ${width}x$height image"
        }
        encodeImpl(data, width, height)
    }

    public fun encodeImpl(data: ByteArray, width: UInt, height: UInt) {
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

        for (i in 0 until data.size step 2) {
            val v = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            val channel = byteArrayOf((v ushr 8).toByte(), v.toByte())
            writer.writeAll(channel)
        }
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

    public companion object {
        public fun new(writer: IoWrite): FarbfeldEncoder = FarbfeldEncoder(writer)
    }
}
