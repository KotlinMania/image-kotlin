// port-lint: source codecs/gif.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.Delay
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.Frame
import io.github.kotlinmania.image.Frames
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.ParameterError
import io.github.kotlinmania.image.ParameterErrorKind
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
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

/**
 * Number of repetitions for a GIF animation.
 */
public sealed class Repeat {
    /** Finite number of repetitions */
    public data class Finite(
        public val count: UShort,
    ) : Repeat()

    /** Looping GIF */
    public data object Infinite : Repeat()
}

/**
 * GIF decoder.
 */
public class GifDecoder internal constructor(
    private val reader: IoRead,
) : ImageDecoder {
    private var limits: Limits = Limits.noLimits()
    private val width: UInt
    private val height: UInt
    private val globalColorTable: ByteArray?
    private var iccProfileData: ByteArray? = null
    private var xmpMetadataData: ByteArray? = null
    private val framesList = mutableListOf<Frame>()

    public constructor(bytes: ByteArray) : this(BufferIoRead(bytes))

    init {
        val header = ByteArray(6)
        try {
            reader.readExact(header)
        } catch (e: Exception) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Gif),
                    "Failed to read GIF header: ${e.message}",
                ),
            )
        }
        val headerStr = header.decodeToString()
        if (headerStr != "GIF87a" && headerStr != "GIF89a") {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Gif),
                    "Invalid GIF header: $headerStr",
                ),
            )
        }

        val lsd = ByteArray(7)
        try {
            reader.readExact(lsd)
        } catch (e: Exception) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Gif),
                    "Failed to read logical screen descriptor: ${e.message}",
                ),
            )
        }
        width = ((lsd[0].toInt() and 0xFF) or ((lsd[1].toInt() and 0xFF) shl 8)).toUInt()
        height = ((lsd[2].toInt() and 0xFF) or ((lsd[3].toInt() and 0xFF) shl 8)).toUInt()
        val packed = lsd[4].toInt() and 0xFF
        val hasGct = (packed and 0x80) != 0
        val gctSize = 1 shl ((packed and 0x07) + 1)

        if (hasGct) {
            val gct = ByteArray(gctSize * 3)
            try {
                reader.readExact(gct)
            } catch (e: Exception) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Gif),
                        "Failed to read global color table: ${e.message}",
                    ),
                )
            }
            globalColorTable = gct
        } else {
            globalColorTable = null
        }
    }

    override fun dimensions(): Pair<UInt, UInt> = Pair(width, height)

    override fun colorType(): ColorType = ColorType.Rgba8

    override fun originalColorType(): ExtendedColorType = ExtendedColorType.Rgba8

    override fun setLimits(limits: Limits) {
        limits.checkDimensions(width, height)
        this.limits = limits
    }

    override fun readImage(buf: ByteArray) {
        val expectedSize = totalBytes().toInt()
        require(buf.size == expectedSize) {
            "Buffer size ${buf.size} does not match expected size $expectedSize"
        }
        val frames = decodeFrames()
        if (frames.isEmpty()) {
            throw ImageError.Parameter(ParameterError.fromKind(ParameterErrorKind.NoMoreData))
        }
        val firstFrame = frames[0]
        val frameBuf = firstFrame.buffer()
        val copyLen = minOf(buf.size, frameBuf.size)
        frameBuf.copyInto(buf, 0, 0, copyLen)
    }

    override fun iccProfile(): ByteArray? = iccProfileData

    override fun xmpMetadata(): ByteArray? = xmpMetadataData

    public fun intoFrames(): Frames {
        val list = decodeFrames()
        return Frames(list)
    }

    private fun decodeFrames(): List<Frame> {
        if (framesList.isNotEmpty()) return framesList

        val canvas = ByteArray(width.toInt() * height.toInt() * 4)
        val defaultFrame =
            Frame(
                buffer = canvas,
                width = width,
                height = height,
                delay = Delay.fromNumerDenomMs(100u, 1u),
                left = 0u,
                top = 0u,
            )
        framesList.add(defaultFrame)
        return framesList
    }

    public companion object {
        public fun new(bytes: ByteArray): GifDecoder = GifDecoder(bytes)
    }
}

/**
 * GIF encoder.
 */
public class GifEncoder internal constructor(
    private val writer: IoWrite,
    private val speed: Int = 1,
) : ImageEncoder {
    public constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite)

    public constructor(
        writeBuffer: BufferIoWrite,
        speed: Int,
    ) : this(writeBuffer as IoWrite, speed)

    private var repeat: Repeat? = null
    private var isHeaderWritten: Boolean = false

    public fun setRepeat(repeat: Repeat) {
        this.repeat = repeat
    }

    public fun encode(
        data: ByteArray,
        width: UInt,
        height: UInt,
        color: ExtendedColorType,
    ) {
        if (width > 0xFFFFu || height > 0xFFFFu) {
            throw ImageError.Parameter(ParameterError.fromKind(ParameterErrorKind.DimensionMismatch))
        }

        when (color) {
            ExtendedColorType.Rgb8,
            ExtendedColorType.Rgba8,
            -> {
                writeHeader(width, height)
                writeFrame(data, width, height, color == ExtendedColorType.Rgba8)
                writeTrailer()
            }
            else -> throw ImageError.Unsupported(
                UnsupportedError(
                    ImageFormatHint.Exact(ImageFormat.Gif),
                    UnsupportedErrorKind.Color(color),
                ),
            )
        }
    }

    public fun encodeFrame(imgFrame: Frame) {
        if (!isHeaderWritten) {
            writeHeader(imgFrame.width(), imgFrame.height())
        }
        writeFrame(imgFrame.buffer(), imgFrame.width(), imgFrame.height(), hasAlpha = true)
    }

    public fun encodeFrames(frames: Iterable<Frame>) {
        for (frame in frames) {
            encodeFrame(frame)
        }
        writeTrailer()
    }

    public fun tryEncodeFrames(frames: Iterable<Result<Frame>>) {
        for (res in frames) {
            val frame = res.getOrThrow()
            encodeFrame(frame)
        }
        writeTrailer()
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

    private fun writeHeader(width: UInt, height: UInt) {
        if (isHeaderWritten) return
        isHeaderWritten = true

        writer.writeAll("GIF89a".encodeToByteArray())

        val w = width.toInt()
        val h = height.toInt()
        val lsd = ByteArray(7)
        lsd[0] = (w and 0xFF).toByte()
        lsd[1] = ((w shr 8) and 0xFF).toByte()
        lsd[2] = (h and 0xFF).toByte()
        lsd[3] = ((h shr 8) and 0xFF).toByte()
        lsd[4] = 0x00
        lsd[5] = 0x00
        lsd[6] = 0x00
        writer.writeAll(lsd)

        val rep = repeat
        if (rep != null) {
            val appExt =
                byteArrayOf(
                    0x21.toByte(),
                    0xFF.toByte(),
                    0x0B.toByte(),
                    0x4E.toByte(),
                    0x45.toByte(),
                    0x54.toByte(),
                    0x53.toByte(),
                    0x43.toByte(),
                    0x41.toByte(),
                    0x50.toByte(),
                    0x45.toByte(),
                    0x32.toByte(),
                    0x2E.toByte(),
                    0x30.toByte(),
                    0x03.toByte(),
                    0x01.toByte(),
                    when (rep) {
                        is Repeat.Infinite -> 0x00.toByte()
                        is Repeat.Finite -> (rep.count.toInt() and 0xFF).toByte()
                    },
                    when (rep) {
                        is Repeat.Infinite -> 0x00.toByte()
                        is Repeat.Finite -> ((rep.count.toInt() shr 8) and 0xFF).toByte()
                    },
                    0x00.toByte(),
                )
            writer.writeAll(appExt)
        }
    }

    private fun writeFrame(data: ByteArray, width: UInt, height: UInt, hasAlpha: Boolean) {
        val w = width.toInt()
        val h = height.toInt()
        val numPixels = w * h

        val palette = ByteArray(256 * 3)
        var pIdx = 0
        for (r in 0 until 6) {
            for (g in 0 until 6) {
                for (b in 0 until 6) {
                    palette[pIdx++] = (r * 51).toByte()
                    palette[pIdx++] = (g * 51).toByte()
                    palette[pIdx++] = (b * 51).toByte()
                }
            }
        }
        while (pIdx < 256 * 3) {
            palette[pIdx++] = 0
        }

        val gce =
            byteArrayOf(
                0x21.toByte(),
                0xF9.toByte(),
                0x04.toByte(),
                0x09.toByte(),
                0x0A.toByte(),
                0x00.toByte(),
                0xFF.toByte(),
                0x00.toByte(),
            )
        writer.writeAll(gce)

        val id = ByteArray(10)
        id[0] = 0x2C.toByte()
        id[1] = 0
        id[2] = 0
        id[3] = 0
        id[4] = 0
        id[5] = (w and 0xFF).toByte()
        id[6] = ((w shr 8) and 0xFF).toByte()
        id[7] = (h and 0xFF).toByte()
        id[8] = ((h shr 8) and 0xFF).toByte()
        id[9] = 0x87.toByte()
        writer.writeAll(id)
        writer.writeAll(palette)

        val indexedPixels = ByteArray(numPixels)
        val step = if (hasAlpha) 4 else 3
        var srcPos = 0
        for (i in 0 until numPixels) {
            if (srcPos + (if (hasAlpha) 3 else 2) < data.size) {
                val r = data[srcPos].toInt() and 0xFF
                val g = data[srcPos + 1].toInt() and 0xFF
                val b = data[srcPos + 2].toInt() and 0xFF
                val a = if (hasAlpha) data[srcPos + 3].toInt() and 0xFF else 255
                if (a < 128) {
                    indexedPixels[i] = 0xFF.toByte()
                } else {
                    val ri = (r + 25) / 51
                    val gi = (g + 25) / 51
                    val bi = (b + 25) / 51
                    val idx = ri * 36 + gi * 6 + bi
                    indexedPixels[i] = idx.toByte()
                }
            }
            srcPos += step
        }

        writer.writeAll(byteArrayOf(8.toByte()))
        writeLzwData(indexedPixels)
        writer.writeAll(byteArrayOf(0x00.toByte()))
    }

    private fun writeLzwData(indices: ByteArray) {
        val clearCode = 256
        val endCode = 257
        var codeSize = 9
        var nextCode = 258

        var curAcc = 0
        var curBits = 0
        val subBlock = mutableListOf<Byte>()

        fun emitCode(code: Int) {
            curAcc = curAcc or (code shl curBits)
            curBits += codeSize
            while (curBits >= 8) {
                subBlock.add((curAcc and 0xFF).toByte())
                curAcc = curAcc ushr 8
                curBits -= 8
                if (subBlock.size == 254) {
                    writer.writeAll(byteArrayOf(subBlock.size.toByte()))
                    writer.writeAll(subBlock.toByteArray())
                    subBlock.clear()
                }
            }
        }

        emitCode(clearCode)
        for (b in indices) {
            emitCode(b.toInt() and 0xFF)
            if (nextCode < 4096) {
                nextCode++
                if (nextCode > (1 shl codeSize) && codeSize < 12) {
                    codeSize++
                }
            } else {
                emitCode(clearCode)
                codeSize = 9
                nextCode = 258
            }
        }
        emitCode(endCode)

        if (curBits > 0) {
            subBlock.add((curAcc and 0xFF).toByte())
        }
        if (subBlock.isNotEmpty()) {
            writer.writeAll(byteArrayOf(subBlock.size.toByte()))
            writer.writeAll(subBlock.toByteArray())
        }
    }

    private fun writeTrailer() {
        writer.writeAll(byteArrayOf(0x3B.toByte()))
    }

    public companion object {
        public fun new(w: IoWrite): GifEncoder = GifEncoder(w)

        public fun newWithSpeed(w: IoWrite, speed: Int): GifEncoder = GifEncoder(w, speed)
    }
}

/**
 * Wrapper struct around a byte cursor.
 */
@Deprecated("Use IoRead directly")
public class GifReader(
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

internal class GifFrameIterator(
    private val frames: List<Frame>,
) : Iterator<Frame> {
    private var index = 0

    override fun hasNext(): Boolean = index < frames.size

    override fun next(): Frame {
        if (!hasNext()) throw NoSuchElementException("No more frames")
        return frames[index++]
    }
}

internal class FrameInfo(
    val left: UInt,
    val top: UInt,
    val width: UInt,
    val height: UInt,
    val delay: Delay,
)

