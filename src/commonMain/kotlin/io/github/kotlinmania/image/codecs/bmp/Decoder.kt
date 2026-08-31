// port-lint: source image/src/codecs/bmp/decoder.rs
package io.github.kotlinmania.image.codecs.bmp

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.ParameterError
import io.github.kotlinmania.image.ParameterErrorKind
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.ImageDecoderRect
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.readExact
import kotlin.math.abs

private const val BITMAPCOREHEADER_SIZE: UInt = 12u
private const val BITMAPINFOHEADER_SIZE: UInt = 40u
private const val BITMAPV2HEADER_SIZE: UInt = 52u
private const val BITMAPV3HEADER_SIZE: UInt = 56u
private const val BITMAPV4HEADER_SIZE: UInt = 108u
private const val BITMAPV5HEADER_SIZE: UInt = 124u

private val LOOKUP_TABLE_3_BIT_TO_8_BIT: IntArray = intArrayOf(0, 36, 73, 109, 146, 182, 219, 255)
private val LOOKUP_TABLE_4_BIT_TO_8_BIT: IntArray =
    intArrayOf(0, 17, 34, 51, 68, 85, 102, 119, 136, 153, 170, 187, 204, 221, 238, 255)
private val LOOKUP_TABLE_5_BIT_TO_8_BIT: IntArray =
    intArrayOf(
        0, 8, 16, 25, 33, 41, 49, 58, 66, 74, 82, 90, 99, 107, 115, 123, 132, 140, 148, 156, 165, 173,
        181, 189, 197, 206, 214, 222, 230, 239, 247, 255,
    )
private val LOOKUP_TABLE_6_BIT_TO_8_BIT: IntArray =
    intArrayOf(
        0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 45, 49, 53, 57, 61, 65, 69, 73, 77, 81, 85, 89, 93,
        97, 101, 105, 109, 113, 117, 121, 125, 130, 134, 138, 142, 146, 150, 154, 158, 162, 166, 170,
        174, 178, 182, 186, 190, 194, 198, 202, 206, 210, 215, 219, 223, 227, 231, 235, 239, 243, 247,
        251, 255,
    )

private const val RLE_ESCAPE: Int = 0
private const val RLE_ESCAPE_EOL: Int = 0
private const val RLE_ESCAPE_EOF: Int = 1
private const val RLE_ESCAPE_DELTA: Int = 2

private enum class ImageType {
    Palette,
    RGB16,
    RGB24,
    RGB32,
    RGBA32,
    RLE8,
    RLE4,
    Bitfields16,
    Bitfields32,
}

internal data class Bitfield(
    val shift: Int,
    val len: Int,
) {
    fun read(data: UInt): Int {
        val d = (data.toLong() ushr shift).toInt()
        return when (len) {
            1 -> (d and 0b1) * 0xFF
            2 -> (d and 0b11) * 0x55
            3 -> LOOKUP_TABLE_3_BIT_TO_8_BIT[d and 0b00_0111]
            4 -> LOOKUP_TABLE_4_BIT_TO_8_BIT[d and 0b00_1111]
            5 -> LOOKUP_TABLE_5_BIT_TO_8_BIT[d and 0b01_1111]
            6 -> LOOKUP_TABLE_6_BIT_TO_8_BIT[d and 0b11_1111]
            7 -> (((d and 0x7F) shl 1) or ((d and 0x7F) ushr 6))
            8 -> (d and 0xFF)
            else -> 0
        }
    }

    companion object {
        fun fromMask(mask: Long, maxLen: Int = 32): Bitfield {
            if (mask == 0L) return Bitfield(0, 0)
            val umask = mask.toUInt()
            var shift = umask.countTrailingZeroBits()
            var len = (umask shr shift).inv().countTrailingZeroBits()
            if (len != umask.countOneBits()) {
                throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Bmp), "Bitfield mask non-contiguous"))
            }
            if (len + shift > maxLen) {
                throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Bmp), "Bitfield mask invalid"))
            }
            if (len > 8) {
                shift += len - 8
                len = 8
            }
            return Bitfield(shift = shift, len = len)
        }
    }
}

internal data class Bitfields(
    val r: Bitfield,
    val g: Bitfield,
    val b: Bitfield,
    val a: Bitfield,
) {
    companion object {
        fun fromMask(
            rMask: Long,
            gMask: Long,
            bMask: Long,
            aMask: Long,
            maxLen: Int = 32,
        ): Bitfields {
            val bitfields =
                Bitfields(
                    r = Bitfield.fromMask(rMask, maxLen),
                    g = Bitfield.fromMask(gMask, maxLen),
                    b = Bitfield.fromMask(bMask, maxLen),
                    a = Bitfield.fromMask(aMask, maxLen),
                )
            if (bitfields.r.len == 0 || bitfields.g.len == 0 || bitfields.b.len == 0) {
                throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Bmp), "Bitfield mask missing"))
            }
            return bitfields
        }
    }
}

private val R5_G5_B5_COLOR_MASK =
    Bitfields(
        r = Bitfield(shift = 10, len = 5),
        g = Bitfield(shift = 5, len = 5),
        b = Bitfield(shift = 0, len = 5),
        a = Bitfield(shift = 0, len = 0),
    )

private val R8_G8_B8_COLOR_MASK =
    Bitfields(
        r = Bitfield(shift = 16, len = 8),
        g = Bitfield(shift = 8, len = 8),
        b = Bitfield(shift = 0, len = 8),
        a = Bitfield(shift = 0, len = 0),
    )

private val R8_G8_B8_A8_COLOR_MASK =
    Bitfields(
        r = Bitfield(shift = 16, len = 8),
        g = Bitfield(shift = 8, len = 8),
        b = Bitfield(shift = 0, len = 8),
        a = Bitfield(shift = 24, len = 8),
    )

/**
 * A BMP decoder.
 */
public class BmpDecoder(
    private val reader: IoRead,
    private val isIco: Boolean = false,
    private val noFileHeader: Boolean = false,
) : ImageDecoderRect {
    public constructor(bytes: ByteArray) : this(BufferIoRead(bytes), false, false)
    public constructor(bytes: ByteArray, isIco: Boolean) : this(BufferIoRead(bytes), isIco, false)
    public constructor(bytes: ByteArray, isIco: Boolean, noFileHeader: Boolean) : this(BufferIoRead(bytes), isIco, noFileHeader)

    private var indexedColor: Boolean = false
    private val width: UInt
    private val height: UInt
    private val topDown: Boolean
    private val bitCount: Int
    private val imageType: ImageType
    private val bitfields: Bitfields?
    private val palette: ByteArray? // RGB palette
    private val rawImageData: ByteArray

    init {
        var dataOffset = 0
        var currentBytesRead: Int
        val dibHeaderSize: UInt
        val dibRest: ByteArray

        if (!isIco && !noFileHeader) {
            // Read 14-byte file header
            val fileHeader = ByteArray(14)
            reader.readExact(fileHeader)
            if (fileHeader[0] != 'B'.code.toByte() || fileHeader[1] != 'M'.code.toByte()) {
                throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Bmp), "BMP signature invalid"))
            }
            dataOffset = readU32Le(fileHeader, 10).toInt()

            // Read DIB header size (first 4 bytes of DIB header)
            val dibSizeBuf = ByteArray(4)
            reader.readExact(dibSizeBuf)
            dibHeaderSize = readU32Le(dibSizeBuf, 0).toUInt()

            dibRest = ByteArray((dibHeaderSize.toInt() - 4).coerceAtLeast(0))
            reader.readExact(dibRest)
            currentBytesRead = 14 + dibHeaderSize.toInt()
        } else {
            // Read DIB header size directly
            val dibSizeBuf = ByteArray(4)
            reader.readExact(dibSizeBuf)
            dibHeaderSize = readU32Le(dibSizeBuf, 0).toUInt()

            dibRest = ByteArray((dibHeaderSize.toInt() - 4).coerceAtLeast(0))
            reader.readExact(dibRest)
            currentBytesRead = dibHeaderSize.toInt()
            dataOffset = currentBytesRead
        }

        var w: Int
        var h: Int
        var planes: Int
        var bpp: Int
        var compression = 0L
        var colorsUsed = 0L

        if (dibHeaderSize == BITMAPCOREHEADER_SIZE) {
            w = readU16Le(dibRest, 0)
            val rawH = readU16Le(dibRest, 2)
            h = if (isIco) rawH / 2 else rawH
            planes = readU16Le(dibRest, 4)
            bpp = readU16Le(dibRest, 6)
            topDown = false
        } else {
            w = readI32Le(dibRest, 0)
            val rawH = readI32Le(dibRest, 4)
            topDown = rawH < 0
            val absH = abs(rawH)
            h = if (isIco) absH / 2 else absH
            planes = readU16Le(dibRest, 8)
            bpp = readU16Le(dibRest, 10)
            if (dibRest.size >= 16) {
                compression = readU32Le(dibRest, 12)
            }
            if (dibRest.size >= 32) {
                colorsUsed = readU32Le(dibRest, 28)
            }
        }

        if (!isIco && planes != 1) {
            throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Bmp), "More than one plane"))
        }
        if (w <= 0 || h <= 0) {
            throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Bmp), "Invalid dimensions: ${w}x$h"))
        }

        width = w.toUInt()
        height = h.toUInt()
        bitCount = bpp

        var parsedBitfields: Bitfields? = null

        imageType =
            when (bpp) {
                1, 2, 4, 8 ->
                    when (compression) {
                        0L -> ImageType.Palette
                        1L -> if (bpp == 8) ImageType.RLE8 else throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Bmp), "Invalid RLE"))
                        2L -> if (bpp == 4) ImageType.RLE4 else throw ImageError.Decoding(DecodingError(ImageFormatHint.Exact(ImageFormat.Bmp), "Invalid RLE"))
                        else -> throw ImageError.Unsupported(UnsupportedError(ImageFormatHint.Exact(ImageFormat.Bmp), UnsupportedErrorKind.GenericFeature("compression $compression")))
                    }
                16 ->
                    when (compression) {
                        0L -> {
                            parsedBitfields = R5_G5_B5_COLOR_MASK
                            ImageType.RGB16
                        }
                        3L -> ImageType.Bitfields16
                        else -> throw ImageError.Unsupported(UnsupportedError(ImageFormatHint.Exact(ImageFormat.Bmp), UnsupportedErrorKind.GenericFeature("compression $compression")))
                    }
                24 ->
                    when (compression) {
                        0L -> ImageType.RGB24
                        else -> throw ImageError.Unsupported(UnsupportedError(ImageFormatHint.Exact(ImageFormat.Bmp), UnsupportedErrorKind.GenericFeature("compression $compression")))
                    }
                32 ->
                    when (compression) {
                        0L -> {
                            parsedBitfields = R8_G8_B8_COLOR_MASK
                            ImageType.RGB32
                        }
                        3L -> ImageType.Bitfields32
                        else -> throw ImageError.Unsupported(UnsupportedError(ImageFormatHint.Exact(ImageFormat.Bmp), UnsupportedErrorKind.GenericFeature("compression $compression")))
                    }
                else -> throw ImageError.Unsupported(UnsupportedError(ImageFormatHint.Exact(ImageFormat.Bmp), UnsupportedErrorKind.GenericFeature("bpp $bpp")))
            }

        // Read bitfield masks if bitfields compression and not in V4+ header
        currentBytesRead = (if (!isIco && !noFileHeader) 14 else 0) + dibHeaderSize.toInt()
        if (imageType == ImageType.Bitfields16 || imageType == ImageType.Bitfields32) {
            if (dibHeaderSize < BITMAPV4HEADER_SIZE) {
                val maskBuf = ByteArray(12)
                reader.readExact(maskBuf)
                currentBytesRead += 12
                val rMask = readU32Le(maskBuf, 0)
                val gMask = readU32Le(maskBuf, 4)
                val bMask = readU32Le(maskBuf, 8)
                parsedBitfields = Bitfields.fromMask(rMask, gMask, bMask, 0L, maxLen = bpp)
            } else {
                val rMask = readU32Le(dibRest, 36)
                val gMask = readU32Le(dibRest, 40)
                val bMask = readU32Le(dibRest, 44)
                val aMask = readU32Le(dibRest, 48)
                parsedBitfields = Bitfields.fromMask(rMask, gMask, bMask, aMask, maxLen = bpp)
            }
        }
        bitfields = parsedBitfields

        // Read palette if needed
        if (imageType == ImageType.Palette || imageType == ImageType.RLE8 || imageType == ImageType.RLE4) {
            val numColors = if (colorsUsed > 0L) colorsUsed.toInt() else (1 shl bpp)
            val entrySize = if (dibHeaderSize == BITMAPCOREHEADER_SIZE) 3 else 4
            val palData = ByteArray(numColors * entrySize)
            reader.readExact(palData)
            currentBytesRead += palData.size

            val rgbPal = ByteArray(numColors * 3)
            for (i in 0 until numColors) {
                val b = palData[i * entrySize]
                val g = palData[i * entrySize + 1]
                val r = palData[i * entrySize + 2]
                rgbPal[i * 3] = r
                rgbPal[i * 3 + 1] = g
                rgbPal[i * 3 + 2] = b
            }
            palette = rgbPal
        } else {
            palette = null
        }

        // Skip any padding to reach dataOffset
        val skip = dataOffset - currentBytesRead
        if (skip > 0) {
            val skipBuf = ByteArray(skip)
            reader.readExact(skipBuf)
        }

        // Read remaining raw image bytes
        val byteList = ArrayList<Byte>()
        val temp = ByteArray(4096)
        var n: Int
        while (reader.read(temp).also { n = it } > 0) {
            for (i in 0 until n) {
                byteList.add(temp[i])
            }
        }
        rawImageData = byteList.toByteArray()
    }

    override fun dimensions(): Pair<UInt, UInt> = Pair(width, height)

    override fun colorType(): ColorType =
        if (isIco || bitfields?.a?.len ?: 0 > 0 || imageType == ImageType.RGBA32) {
            ColorType.Rgba8
        } else {
            ColorType.Rgb8
        }

    override fun readImage(buf: ByteArray) {
        val w = width.toInt()
        val h = height.toInt()
        val isRgba = colorType() == ColorType.Rgba8
        val outChannels = if (isRgba) 4 else 3

        val paddedRowBytes =
            when (imageType) {
                ImageType.Palette -> ((w * bitCount + 7) / 8 + 3) and 3.inv()
                ImageType.RGB24 -> (w * 3 + 3) and 3.inv()
                ImageType.RGB16, ImageType.Bitfields16 -> (w * 2 + 3) and 3.inv()
                ImageType.RGB32, ImageType.RGBA32, ImageType.Bitfields32 -> w * 4
                else -> 0
            }
        val expectedPixelBytes = paddedRowBytes * h
        if (imageType != ImageType.RLE8 && imageType != ImageType.RLE4) {
            if (rawImageData.size < expectedPixelBytes) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(if (isIco) ImageFormat.Ico else ImageFormat.Bmp),
                        "Truncated image data",
                    ),
                )
            }
        }

        if (isIco) {
            val maskRowBytes = ((w + 31) / 32) * 4
            val maskLength = maskRowBytes * h
            if (rawImageData.size > expectedPixelBytes && rawImageData.size < expectedPixelBytes + maskLength) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Ico),
                        "ICO image data size did not match expected size",
                    ),
                )
            }
        }

        when (imageType) {
            ImageType.Palette -> decodePalette(buf, w, h, outChannels)
            ImageType.RGB24 -> decodeRgb24(buf, w, h, outChannels)
            ImageType.RGB32, ImageType.RGBA32, ImageType.Bitfields32 -> decodeBitfields32(buf, w, h, outChannels)
            ImageType.RGB16, ImageType.Bitfields16 -> decodeBitfields16(buf, w, h, outChannels)
            ImageType.RLE8 -> decodeRle8(buf, w, h, outChannels)
            ImageType.RLE4 -> decodeRle4(buf, w, h, outChannels)
        }

        if (isIco) {
            applyIcoMask(buf, w, h)
        }
    }

    private fun applyIcoMask(buf: ByteArray, w: Int, h: Int) {
        val paddedRowBytes =
            when (imageType) {
                ImageType.Palette -> ((w * bitCount + 7) / 8 + 3) and 3.inv()
                ImageType.RGB24 -> (w * 3 + 3) and 3.inv()
                ImageType.RGB16, ImageType.Bitfields16 -> (w * 2 + 3) and 3.inv()
                ImageType.RGB32, ImageType.RGBA32, ImageType.Bitfields32 -> w * 4
                else -> return
            }
        val maskOffset = paddedRowBytes * h
        val maskRowBytes = ((w + 31) / 32) * 4
        if (rawImageData.size >= maskOffset + maskRowBytes * h) {
            for (y in 0 until h) {
                val maskRowStart = maskOffset + y * maskRowBytes
                var x = 0
                for (byteIdx in 0 until maskRowBytes) {
                    if (maskRowStart + byteIdx >= rawImageData.size) break
                    val maskByte = rawImageData[maskRowStart + byteIdx].toInt() and 0xFF
                    for (bit in 7 downTo 0) {
                        if (x >= w) break
                        if ((maskByte and (1 shl bit)) != 0) {
                            val targetRow = if (topDown) y else (h - 1 - y)
                            val dstIdx = (targetRow * w + x) * 4 + 3
                            if (dstIdx < buf.size) {
                                buf[dstIdx] = 0.toByte()
                            }
                        }
                        x++
                    }
                }
            }
        }
    }

    private fun decodePalette(buf: ByteArray, w: Int, h: Int, outChannels: Int) {
        val pal = palette ?: return
        val rowBits = w * bitCount
        val rowBytes = (rowBits + 7) / 8
        val paddedRowBytes = (rowBytes + 3) and 3.inv()

        for (row in 0 until h) {
            val srcRow = if (topDown) row else (h - 1 - row)
            val srcOffset = srcRow * paddedRowBytes
            val dstRowStart = row * w * outChannels

            var bitPos = srcOffset * 8
            for (col in 0 until w) {
                val byteIdx = bitPos / 8
                val bitOffset = 8 - bitCount - (bitPos % 8)
                val mask = (1 shl bitCount) - 1
                val colorIdx =
                    if (byteIdx < rawImageData.size) {
                        ((rawImageData[byteIdx].toInt() and 0xFF) ushr bitOffset) and mask
                    } else {
                        0
                    }
                bitPos += bitCount

                val dstIdx = dstRowStart + col * outChannels
                val palOffset = colorIdx * 3
                if (palOffset + 2 < pal.size) {
                    buf[dstIdx] = pal[palOffset]
                    buf[dstIdx + 1] = pal[palOffset + 1]
                    buf[dstIdx + 2] = pal[palOffset + 2]
                }
                if (outChannels == 4) {
                    buf[dstIdx + 3] = 255.toByte()
                }
            }
        }
    }

    private fun decodeRgb24(buf: ByteArray, w: Int, h: Int, outChannels: Int) {
        val rowBytes = w * 3
        val paddedRowBytes = (rowBytes + 3) and 3.inv()

        for (row in 0 until h) {
            val srcRow = if (topDown) row else (h - 1 - row)
            val srcOffset = srcRow * paddedRowBytes
            val dstRowStart = row * w * outChannels

            for (col in 0 until w) {
                val srcIdx = srcOffset + col * 3
                val dstIdx = dstRowStart + col * outChannels
                if (srcIdx + 2 < rawImageData.size) {
                    val b = rawImageData[srcIdx]
                    val g = rawImageData[srcIdx + 1]
                    val r = rawImageData[srcIdx + 2]
                    buf[dstIdx] = r
                    buf[dstIdx + 1] = g
                    buf[dstIdx + 2] = b
                }
                if (outChannels == 4) {
                    buf[dstIdx + 3] = 255.toByte()
                }
            }
        }
    }

    private fun decodeBitfields32(buf: ByteArray, w: Int, h: Int, outChannels: Int) {
        val bf = bitfields ?: R8_G8_B8_COLOR_MASK
        val paddedRowBytes = w * 4

        for (row in 0 until h) {
            val srcRow = if (topDown) row else (h - 1 - row)
            val srcOffset = srcRow * paddedRowBytes
            val dstRowStart = row * w * outChannels

            for (col in 0 until w) {
                val srcIdx = srcOffset + col * 4
                val dstIdx = dstRowStart + col * outChannels
                if (srcIdx + 3 < rawImageData.size) {
                    val px = readU32Le(rawImageData, srcIdx)
                    val r = extractChannel(px, bf.r)
                    val g = extractChannel(px, bf.g)
                    val b = extractChannel(px, bf.b)
                    val a = if (bf.a.len > 0) extractChannel(px, bf.a) else 255
                    buf[dstIdx] = r.toByte()
                    buf[dstIdx + 1] = g.toByte()
                    buf[dstIdx + 2] = b.toByte()
                    if (outChannels == 4) {
                        buf[dstIdx + 3] = a.toByte()
                    }
                }
            }
        }
    }

    private fun decodeBitfields16(buf: ByteArray, w: Int, h: Int, outChannels: Int) {
        val bf = bitfields ?: R5_G5_B5_COLOR_MASK
        val rowBytes = w * 2
        val paddedRowBytes = (rowBytes + 3) and 3.inv()

        for (row in 0 until h) {
            val srcRow = if (topDown) row else (h - 1 - row)
            val srcOffset = srcRow * paddedRowBytes
            val dstRowStart = row * w * outChannels

            for (col in 0 until w) {
                val srcIdx = srcOffset + col * 2
                val dstIdx = dstRowStart + col * outChannels
                if (srcIdx + 1 < rawImageData.size) {
                    val px = readU16Le(rawImageData, srcIdx).toLong()
                    val r = extractChannel(px, bf.r)
                    val g = extractChannel(px, bf.g)
                    val b = extractChannel(px, bf.b)
                    val a = if (bf.a.len > 0) extractChannel(px, bf.a) else 255
                    buf[dstIdx] = r.toByte()
                    buf[dstIdx + 1] = g.toByte()
                    buf[dstIdx + 2] = b.toByte()
                    if (outChannels == 4) {
                        buf[dstIdx + 3] = a.toByte()
                    }
                }
            }
        }
    }

    private fun decodeRle8(buf: ByteArray, w: Int, h: Int, outChannels: Int) {
        val pal = palette ?: return
        var x = 0
        var y = if (topDown) 0 else (h - 1)
        var srcIdx = 0

        while (srcIdx < rawImageData.size) {
            val b0 = rawImageData[srcIdx++].toInt() and 0xFF
            val b1 = if (srcIdx < rawImageData.size) rawImageData[srcIdx++].toInt() and 0xFF else 0

            if (b0 == RLE_ESCAPE) {
                when (b1) {
                    RLE_ESCAPE_EOL -> {
                        x = 0
                        y += if (topDown) 1 else -1
                    }
                    RLE_ESCAPE_EOF -> break
                    RLE_ESCAPE_DELTA -> {
                        if (srcIdx + 1 < rawImageData.size) {
                            val dx = rawImageData[srcIdx++].toInt() and 0xFF
                            val dy = rawImageData[srcIdx++].toInt() and 0xFF
                            x += dx
                            y += if (topDown) dy else -dy
                        }
                    }
                    else -> {
                        // Absolute mode: b1 pixels follow
                        for (i in 0 until b1) {
                            if (srcIdx < rawImageData.size) {
                                val colorIdx = rawImageData[srcIdx++].toInt() and 0xFF
                                setPixel(buf, w, h, x, y, outChannels, pal, colorIdx)
                                x++
                            }
                        }
                        if ((b1 and 1) != 0 && srcIdx < rawImageData.size) {
                            srcIdx++ // padding byte
                        }
                    }
                }
            } else {
                // Encoded mode: repeat pixel b1, b0 times
                for (i in 0 until b0) {
                    setPixel(buf, w, h, x, y, outChannels, pal, b1)
                    x++
                }
            }
        }
    }

    private fun decodeRle4(buf: ByteArray, w: Int, h: Int, outChannels: Int) {
        val pal = palette ?: return
        var x = 0
        var y = if (topDown) 0 else (h - 1)
        var srcIdx = 0

        while (srcIdx < rawImageData.size) {
            val b0 = rawImageData[srcIdx++].toInt() and 0xFF
            val b1 = if (srcIdx < rawImageData.size) rawImageData[srcIdx++].toInt() and 0xFF else 0

            if (b0 == RLE_ESCAPE) {
                when (b1) {
                    RLE_ESCAPE_EOL -> {
                        x = 0
                        y += if (topDown) 1 else -1
                    }
                    RLE_ESCAPE_EOF -> break
                    RLE_ESCAPE_DELTA -> {
                        if (srcIdx + 1 < rawImageData.size) {
                            val dx = rawImageData[srcIdx++].toInt() and 0xFF
                            val dy = rawImageData[srcIdx++].toInt() and 0xFF
                            x += dx
                            y += if (topDown) dy else -dy
                        }
                    }
                    else -> {
                        // Absolute mode: b1 4-bit pixels follow
                        val bytesToRead = (b1 + 1) / 2
                        var pixelCount = 0
                        for (i in 0 until bytesToRead) {
                            if (srcIdx < rawImageData.size) {
                                val byteVal = rawImageData[srcIdx++].toInt() and 0xFF
                                val c0 = (byteVal ushr 4) and 0x0F
                                val c1 = byteVal and 0x0F
                                if (pixelCount < b1) {
                                    setPixel(buf, w, h, x, y, outChannels, pal, c0)
                                    x++
                                    pixelCount++
                                }
                                if (pixelCount < b1) {
                                    setPixel(buf, w, h, x, y, outChannels, pal, c1)
                                    x++
                                    pixelCount++
                                }
                            }
                        }
                        if ((bytesToRead and 1) != 0 && srcIdx < rawImageData.size) {
                            srcIdx++ // padding byte
                        }
                    }
                }
            } else {
                // Encoded mode: repeat alternating nibbles b0 times
                val c0 = (b1 ushr 4) and 0x0F
                val c1 = b1 and 0x0F
                for (i in 0 until b0) {
                    val c = if (i % 2 == 0) c0 else c1
                    setPixel(buf, w, h, x, y, outChannels, pal, c)
                    x++
                }
            }
        }
    }

    private fun setPixel(
        buf: ByteArray,
        w: Int,
        h: Int,
        x: Int,
        y: Int,
        outChannels: Int,
        pal: ByteArray,
        colorIdx: Int,
    ) {
        if (x in 0 until w && y in 0 until h) {
            val dstIdx = (y * w + x) * outChannels
            val palOffset = colorIdx * 3
            if (palOffset + 2 < pal.size && dstIdx + 2 < buf.size) {
                buf[dstIdx] = pal[palOffset]
                buf[dstIdx + 1] = pal[palOffset + 1]
                buf[dstIdx + 2] = pal[palOffset + 2]
                if (outChannels == 4 && dstIdx + 3 < buf.size) {
                    buf[dstIdx + 3] = 255.toByte()
                }
            }
        }
    }

    public fun setIndexedColor(indexedColor: Boolean) {
        this.indexedColor = indexedColor
    }

    public fun getIndexedColor(): Boolean = indexedColor

    public fun getPalette(): ByteArray? = palette

    public fun readMetadataInIcoFormat() {
        // Metadata is loaded upon construction for ICO format
    }

    public fun readImageData(buf: ByteArray) {
        readImage(buf)
    }

    public fun rows(pixelData: ByteArray): RowIterator =
        RowIterator(pixelData, width.toInt() * colorType().bytesPerPixel().toInt(), height.toInt(), topDown)

    override fun readRect(
        x: UInt,
        y: UInt,
        width: UInt,
        height: UInt,
        buf: ByteArray,
        rowPitch: Int,
    ) {
        val totalDecoded = ByteArray(totalBytes().toInt())
        readImage(totalDecoded)
        val (imgW, imgH) = dimensions()
        val bpp = colorType().bytesPerPixel().toInt()
        val imgRowBytes = imgW.toInt() * bpp
        val w = width.toInt()
        val h = height.toInt()
        val x0 = x.toInt()
        val y0 = y.toInt()

        if (x0 + w > imgW.toInt() || y0 + h > imgH.toInt() || w == 0 || h == 0) {
            throw ImageError.Parameter(
                ParameterError(ParameterErrorKind.DimensionMismatch),
            )
        }

        for (r in 0 until h) {
            val srcOffset = (y0 + r) * imgRowBytes + x0 * bpp
            val dstOffset = r * rowPitch
            totalDecoded.copyInto(buf, destinationOffset = dstOffset, startIndex = srcOffset, endIndex = srcOffset + w * bpp)
        }
    }

    private fun extractChannel(px: Long, bf: Bitfield): Int =
        if (bf.len == 0) 0 else bf.read(px.toUInt())

    public companion object {
        public fun new(reader: IoRead): BmpDecoder = BmpDecoder(reader, isIco = false, noFileHeader = false)

        public fun newWithoutFileHeader(reader: IoRead): BmpDecoder = BmpDecoder(reader, isIco = false, noFileHeader = true)

        public fun newWithIcoFormat(reader: IoRead): BmpDecoder = BmpDecoder(reader, isIco = true, noFileHeader = false)
    }
}

private fun readU16Le(buf: ByteArray, offset: Int): Int {
    val b0 = buf[offset].toInt() and 0xFF
    val b1 = buf[offset + 1].toInt() and 0xFF
    return (b1 shl 8) or b0
}

private fun readU32Le(buf: ByteArray, offset: Int): Long {
    val b0 = buf[offset].toLong() and 0xFFL
    val b1 = buf[offset + 1].toLong() and 0xFFL
    val b2 = buf[offset + 2].toLong() and 0xFFL
    val b3 = buf[offset + 3].toLong() and 0xFFL
    return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
}

private fun readI32Le(buf: ByteArray, offset: Int): Int =
    readU32Le(buf, offset).toInt()

/**
 * Row iterator for BMP decoding.
 */
public class RowIterator(
    private val buffer: ByteArray,
    private val rowSize: Int,
    private val height: Int,
    private val topDown: Boolean,
) : Iterator<ByteArray> {
    private var cur = 0

    override fun hasNext(): Boolean = cur < height

    override fun next(): ByteArray {
        if (!hasNext()) throw NoSuchElementException("No more rows")
        val rowIndex = if (topDown) cur else (height - 1 - cur)
        cur++
        val offset = rowIndex * rowSize
        return buffer.copyOfRange(offset, offset + rowSize)
    }
}

