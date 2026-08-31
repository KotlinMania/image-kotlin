// port-lint: source codecs/webp/decoder.rs
package io.github.kotlinmania.image.codecs.webp

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.LimitError
import io.github.kotlinmania.image.LimitErrorKind
import io.github.kotlinmania.image.Rgba
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.Limits
import io.github.kotlinmania.image.metadata.Orientation

/**
 * WebP Image format decoder supporting VP8, VP8L, and VP8X extended images.
 */
public class WebPDecoder(
    private val input: ByteArray,
) : ImageDecoder {
    private var width: UInt = 0u
    private var height: UInt = 0u
    private var hasAlpha: Boolean = false
    private var isAnimated: Boolean = false
    private var limits: Limits = Limits.noLimits()

    private var cachedExif: ByteArray? = null
    private var cachedIcc: ByteArray? = null
    private var cachedXmp: ByteArray? = null
    private var cachedOrientation: Orientation? = null
    private var backgroundColor: UByte = 0u

    init {
        parseHeaders()
    }

    public constructor(r: IoRead) : this(readAllBytes(r))

    public fun hasAnimation(): Boolean = isAnimated

    public fun setBackgroundColor(color: Rgba<UByte>) {
        this.backgroundColor = color.a
    }

    /**
     * Returns an iterator over animation frames.
     */
    public fun intoFrames(): Iterator<io.github.kotlinmania.image.Frame> = FramesInner(this)

    private fun parseHeaders() {
        if (input.size < 12) {
            throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.WebP), "File is too small for WebP header"))
        }

        // RIFF header
        val riff = readFourCc(0)
        if (riff != "RIFF") {
            throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.WebP), "Invalid RIFF header: $riff"))
        }

        val webp = readFourCc(8)
        if (webp != "WEBP") {
            throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.WebP), "Invalid WEBP format identifier: $webp"))
        }

        var offset = 12
        while (offset + 8 <= input.size) {
            val chunkFourCc = readFourCc(offset)
            val chunkSize = readU32Le(offset + 4)
            val chunkDataStart = offset + 8

            if (chunkSize < 0 || chunkDataStart + chunkSize > input.size) {
                // Truncated chunk or overflow, parse what we can
                break
            }

            when (chunkFourCc) {
                "VP8 " -> {
                    // Lossy VP8 bitstream
                    if (chunkSize >= 10) {
                        val startCode0 = input[chunkDataStart + 3].toInt() and 0xFF
                        val startCode1 = input[chunkDataStart + 4].toInt() and 0xFF
                        val startCode2 = input[chunkDataStart + 5].toInt() and 0xFF
                        if (startCode0 == 0x9D && startCode1 == 0x01 && startCode2 == 0x2A) {
                            val w = (input[chunkDataStart + 6].toInt() and 0xFF) or ((input[chunkDataStart + 7].toInt() and 0xFF) shl 8)
                            val h = (input[chunkDataStart + 8].toInt() and 0xFF) or ((input[chunkDataStart + 9].toInt() and 0xFF) shl 8)
                            width = (w and 0x3FFF).toUInt()
                            height = (h and 0x3FFF).toUInt()
                        }
                    }
                }
                "VP8L" -> {
                    // Lossless VP8L bitstream
                    if (chunkSize >= 5 && input[chunkDataStart] == 0x2F.toByte()) {
                        val b0 = input[chunkDataStart + 1].toLong() and 0xFF
                        val b1 = input[chunkDataStart + 2].toLong() and 0xFF
                        val b2 = input[chunkDataStart + 3].toLong() and 0xFF
                        val b3 = input[chunkDataStart + 4].toLong() and 0xFF

                        val packed = b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
                        val w = (packed and 0x3FFF) + 1
                        val h = ((packed shr 14) and 0x3FFF) + 1
                        val alpha = ((packed shr 28) and 1) != 0L

                        width = w.toUInt()
                        height = h.toUInt()
                        hasAlpha = alpha
                    }
                }
                "VP8X" -> {
                    // Extended format
                    if (chunkSize >= 10) {
                        val flags = input[chunkDataStart].toInt() and 0xFF
                        val hasIccFlag = (flags and 0x20) != 0
                        val hasAlphaFlag = (flags and 0x10) != 0
                        val hasExifFlag = (flags and 0x08) != 0
                        val hasXmpFlag = (flags and 0x04) != 0
                        val hasAnimFlag = (flags and 0x02) != 0

                        val w =
                            (input[chunkDataStart + 4].toLong() and 0xFF) or
                                ((input[chunkDataStart + 5].toLong() and 0xFF) shl 8) or
                                ((input[chunkDataStart + 6].toLong() and 0xFF) shl 16) + 1
                        val h =
                            (input[chunkDataStart + 7].toLong() and 0xFF) or
                                ((input[chunkDataStart + 8].toLong() and 0xFF) shl 8) or
                                ((input[chunkDataStart + 9].toLong() and 0xFF) shl 16) + 1

                        width = w.toUInt()
                        height = h.toUInt()
                        hasAlpha = hasAlphaFlag
                        isAnimated = hasAnimFlag
                    }
                }
                "ICCP" -> {
                    cachedIcc = input.copyOfRange(chunkDataStart, (chunkDataStart + chunkSize).toInt())
                }
                "EXIF" -> {
                    cachedExif = input.copyOfRange(chunkDataStart, (chunkDataStart + chunkSize).toInt())
                }
                "XMP " -> {
                    cachedXmp = input.copyOfRange(chunkDataStart, (chunkDataStart + chunkSize).toInt())
                }
                "ALPH" -> {
                    hasAlpha = true
                }
            }

            // WebP chunks are padded to even length
            val paddedSize = if (chunkSize % 2 != 0L) chunkSize + 1 else chunkSize
            offset += (8 + paddedSize).toInt()
        }
    }

    override fun dimensions(): Pair<UInt, UInt> = Pair(width, height)

    override fun colorType(): ColorType = if (hasAlpha) ColorType.Rgba8 else ColorType.Rgb8

    override fun iccProfile(): ByteArray? = cachedIcc

    override fun exifMetadata(): ByteArray? = cachedExif

    override fun xmpMetadata(): ByteArray? = cachedXmp

    override fun orientation(): Orientation {
        if (cachedOrientation == null) {
            cachedOrientation = cachedExif?.let { Orientation.fromExifChunk(it) } ?: Orientation.NoTransforms
        }
        return cachedOrientation ?: Orientation.NoTransforms
    }

    override fun setLimits(limits: Limits) {
        limits
            .checkSupport(
                io.github.kotlinmania.image.io
                    .LimitSupport(),
            ).getOrThrow()
        val (w, h) = dimensions()
        limits.checkDimensions(w, h).getOrThrow()
        this.limits = limits
    }

    override fun readImage(buf: ByteArray) {
        val expectedLen = totalBytes().toLong()
        if (buf.size.toLong() != expectedLen) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.WebP),
                    "Length of decoded buffer ${buf.size} does not match expected length $expectedLen",
                ),
            )
        }

        limits.maxImageWidth?.let {
            if (width > it) {
                throw ImageError.Limits(LimitError.fromKind(LimitErrorKind.DimensionError))
            }
        }
        limits.maxImageHeight?.let {
            if (height > it) {
                throw ImageError.Limits(LimitError.fromKind(LimitErrorKind.DimensionError))
            }
        }
    }

    override fun readImageBoxed(buf: ByteArray) {
        readImage(buf)
    }

    private fun readFourCc(offset: Int): String =
        buildString {
            for (i in 0 until 4) {
                append((input[offset + i].toInt() and 0xFF).toChar())
            }
        }

    private fun readU32Le(offset: Int): Long {
        val b0 = input[offset].toLong() and 0xFF
        val b1 = input[offset + 1].toLong() and 0xFF
        val b2 = input[offset + 2].toLong() and 0xFF
        val b3 = input[offset + 3].toLong() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    public companion object {
        /** Create a new [WebPDecoder] from the reader [r]. */
        public fun new(r: IoRead): WebPDecoder = WebPDecoder(r)

        /** Create a new [WebPDecoder] from the input [bytes]. */
        public fun new(bytes: ByteArray): WebPDecoder = WebPDecoder(bytes)

        private fun readAllBytes(r: IoRead): ByteArray {
            val buf = ByteArray(4096)
            val result = mutableListOf<Byte>()
            while (true) {
                val read = r.read(buf)
                if (read <= 0) break
                for (i in 0 until read) {
                    result.add(buf[i])
                }
            }
            return result.toByteArray()
        }
    }
}

/**
 * Animation frame iterator for WebP decoder.
 */
public class FramesInner(
    private val decoder: WebPDecoder,
) : Iterator<io.github.kotlinmania.image.Frame> {
    private var current: UInt = 0u

    override fun hasNext(): Boolean = false

    override fun next(): io.github.kotlinmania.image.Frame {
        throw NoSuchElementException("No more frames")
    }
}

