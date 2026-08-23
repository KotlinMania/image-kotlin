// port-lint: source codecs/tga/decoder.rs
package io.github.kotlinmania.image.codecs.tga

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
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.readExact
import io.github.kotlinmania.image.io.readU8

internal class ColorMap(
    val startOffset: Int,
    val entrySize: Int,
    val bytes: ByteArray,
) {
    fun get(index: Int): ByteArray? {
        val adjustedIndex = index - startOffset
        if (adjustedIndex < 0) return null
        val entry = entrySize * adjustedIndex
        if (entry + entrySize > bytes.size) return null
        return bytes.copyOfRange(entry, entry + entrySize)
    }
}

internal enum class TgaOrientation {
    TopLeft,
    TopRight,
    BottomRight,
    BottomLeft,
    ;

    companion object {
        fun fromImageDescByte(value: UByte): TgaOrientation {
            val v = value.toInt()
            val rightToLeft = (v and (1 shl 4)) != 0
            val topToBottom = (v and (1 shl 5)) != 0
            return if (!rightToLeft) {
                if (!topToBottom) BottomLeft else TopLeft
            } else {
                if (!topToBottom) BottomRight else TopRight
            }
        }
    }
}

/**
 * The representation of a TGA decoder.
 */
public class TgaDecoder internal constructor(
    private val reader: IoRead,
) : ImageDecoder {
    private val width: Int
    private val height: Int
    private val rawBytesPerPixel: Int
    private val imageType: ImageType
    private val colorType: ColorType
    private val origColorType: ExtendedColorType?
    private val header: Header
    private val colorMap: ColorMap?

    public constructor(bytes: ByteArray) : this(BufferIoRead(bytes))

    init {
        header = Header.fromReader(reader).getOrThrow()
        imageType = ImageType.fromValue(header.imageType)
        width = header.imageWidth.toInt()
        height = header.imageHeight.toInt()
        rawBytesPerPixel = (header.pixelDepth.toInt() + 7) / 8
        val numAlphaBits = header.imageDesc.toInt() and ALPHA_BIT_MASK.toInt()

        if (width == 0 || height == 0) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Tga),
                    "Invalid empty image",
                ),
            )
        }

        val depth = header.pixelDepth.toInt()
        if (depth !in setOf(8, 16, 24, 32) || numAlphaBits !in setOf(0, 8)) {
            throw ImageError.Unsupported(
                UnsupportedError(
                    ImageFormatHint.Exact(ImageFormat.Tga),
                    UnsupportedErrorKind.Color(ExtendedColorType.Unknown(header.pixelDepth)),
                ),
            )
        }

        if (imageType.isColorMapped()) {
            if (header.mapType.toInt() != 1) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Tga),
                        "Color map type must be 1 for color mapped images",
                    ),
                )
            } else if (depth !in setOf(8, 16)) {
                throw ImageError.Decoding(
                    DecodingError(
                        ImageFormatHint.Exact(ImageFormat.Tga),
                        "Color map must use 1 or 2 byte indexes",
                    ),
                )
            } else if (header.pixelDepth > header.mapEntrySize) {
                throw ImageError.Unsupported(
                    UnsupportedError(
                        ImageFormatHint.Exact(ImageFormat.Tga),
                        UnsupportedErrorKind.GenericFeature("Indices larger than pixel values"),
                    ),
                )
            }
        }

        // Read image ID and ignore it
        if (header.idLength.toInt() > 0) {
            val tmp = ByteArray(header.idLength.toInt())
            reader.readExact(tmp)
        }

        // Read color map
        var parsedColorMap: ColorMap? = null
        if (header.mapType.toInt() == 1) {
            val entrySize = (header.mapEntrySize.toInt() + 7) / 8
            if (entrySize !in setOf(2, 3, 4)) {
                throw ImageError.Unsupported(
                    UnsupportedError(
                        ImageFormatHint.Exact(ImageFormat.Tga),
                        UnsupportedErrorKind.GenericFeature("Unsupported color map entry size"),
                    ),
                )
            }
            val mapBytes = ByteArray(entrySize * header.mapLength.toInt())
            reader.readExact(mapBytes)

            if (imageType.isColorMapped()) {
                parsedColorMap =
                    ColorMap(
                        startOffset = header.mapOrigin.toInt(),
                        entrySize = entrySize,
                        bytes = mapBytes,
                    )
            }
        }
        colorMap = parsedColorMap

        // Compute output pixel depth
        val totalPixelBits =
            if (header.mapType.toInt() == 1) {
                header.mapEntrySize.toInt()
            } else {
                header.pixelDepth.toInt()
            }
        val numOtherBits = totalPixelBits - numAlphaBits
        if (numOtherBits < 0) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Tga),
                    "More alpha bits than pixel bits",
                ),
            )
        }

        // Determine color type
        val isColor = imageType.isColor()
        when {
            numAlphaBits == 0 && numOtherBits == 32 && isColor -> {
                colorType = ColorType.Rgba8
                origColorType = null
            }
            numAlphaBits == 8 && numOtherBits == 24 && isColor -> {
                colorType = ColorType.Rgba8
                origColorType = null
            }
            numAlphaBits == 0 && numOtherBits == 24 && isColor -> {
                colorType = ColorType.Rgb8
                origColorType = null
            }
            numAlphaBits == 8 && numOtherBits == 8 && !isColor -> {
                colorType = ColorType.La8
                origColorType = null
            }
            numAlphaBits == 0 && numOtherBits == 8 && !isColor -> {
                colorType = ColorType.L8
                origColorType = null
            }
            numAlphaBits == 8 && numOtherBits == 0 && !isColor -> {
                colorType = ColorType.L8
                origColorType = ExtendedColorType.A8
            }
            else -> {
                throw ImageError.Unsupported(
                    UnsupportedError(
                        ImageFormatHint.Exact(ImageFormat.Tga),
                        UnsupportedErrorKind.Color(ExtendedColorType.Unknown(header.pixelDepth)),
                    ),
                )
            }
        }
    }

    override fun dimensions(): Pair<UInt, UInt> = Pair(width.toUInt(), height.toUInt())

    override fun colorType(): ColorType = colorType

    override fun originalColorType(): ExtendedColorType =
        origColorType ?: colorType.toExtendedColorType()

    private fun readEncodedData(buf: ByteArray, length: Int) {
        val repeatBuf = ByteArray(rawBytesPerPixel)
        var index = 0
        while (index < length) {
            val runPacket = reader.readU8().toInt()
            if ((runPacket and 0x80) != 0) {
                val repeatCount = (runPacket and 0x7F) + 1
                reader.readExact(repeatBuf)
                for (r in 0 until repeatCount) {
                    val dest = index + r * rawBytesPerPixel
                    if (dest + rawBytesPerPixel <= length) {
                        repeatBuf.copyInto(buf, destinationOffset = dest)
                    }
                }
                index += repeatCount * rawBytesPerPixel
            } else {
                val numPixels = runPacket + 1
                val numRawBytes = minOf(numPixels * rawBytesPerPixel, length - index)
                reader.readExact(buf, offset = index, count = numRawBytes)
                index += numRawBytes
            }
        }
    }

    private fun expandColorMap(input: ByteArray, output: ByteArray, colorMap: ColorMap) {
        if (rawBytesPerPixel == 1) {
            for (i in 0 until width * height) {
                val index = input[i].toInt() and 0xFF
                val color =
                    colorMap.get(index)
                        ?: throw ImageError.Decoding(
                            DecodingError(
                                ImageFormatHint.Exact(ImageFormat.Tga),
                                "Invalid color map index",
                            ),
                        )
                color.copyInto(output, destinationOffset = i * colorMap.entrySize)
            }
        } else if (rawBytesPerPixel == 2) {
            for (i in 0 until width * height) {
                val b0 = input[i * 2].toInt() and 0xFF
                val b1 = input[i * 2 + 1].toInt() and 0xFF
                val index = (b1 shl 8) or b0
                val color =
                    colorMap.get(index)
                        ?: throw ImageError.Decoding(
                            DecodingError(
                                ImageFormatHint.Exact(ImageFormat.Tga),
                                "Invalid color map index",
                            ),
                        )
                color.copyInto(output, destinationOffset = i * colorMap.entrySize)
            }
        }
    }

    private fun reverseEncodingInOutput(pixels: ByteArray) {
        if (colorType == ColorType.Rgb8 || colorType == ColorType.Rgba8) {
            val bpp = colorType.bytesPerPixel().toInt()
            var offset = 0
            while (offset + bpp <= pixels.size) {
                val tmp = pixels[offset]
                pixels[offset] = pixels[offset + 2]
                pixels[offset + 2] = tmp
                offset += bpp
            }
        }
    }

    private fun flipHorizontal(pixels: ByteArray) {
        val bpp = colorType.bytesPerPixel().toInt()
        val rowSize = width * bpp
        val row = ByteArray(rowSize)
        for (y in 0 until height) {
            val rowOffset = y * rowSize
            pixels.copyInto(row, destinationOffset = 0, startIndex = rowOffset, endIndex = rowOffset + rowSize)
            for (x in 0 until width) {
                val srcOffset = (width - 1 - x) * bpp
                val destOffset = rowOffset + x * bpp
                row.copyInto(pixels, destinationOffset = destOffset, startIndex = srcOffset, endIndex = srcOffset + bpp)
            }
        }
    }

    private fun flipVertical(pixels: ByteArray) {
        val bpp = colorType.bytesPerPixel().toInt()
        val rowSize = width * bpp
        val row = ByteArray(rowSize)
        for (y in 0 until height / 2) {
            val topRowOffset = y * rowSize
            val bottomRowOffset = (height - 1 - y) * rowSize
            pixels.copyInto(row, destinationOffset = 0, startIndex = topRowOffset, endIndex = topRowOffset + rowSize)
            pixels.copyInto(pixels, destinationOffset = topRowOffset, startIndex = bottomRowOffset, endIndex = bottomRowOffset + rowSize)
            row.copyInto(pixels, destinationOffset = bottomRowOffset, startIndex = 0, endIndex = rowSize)
        }
    }

    override fun readImage(buf: ByteArray) {
        require(buf.size >= totalBytes().toInt()) {
            "output buffer too small: expected ${totalBytes()}, got ${buf.size}"
        }

        val orientation = TgaOrientation.fromImageDescByte(header.imageDesc)
        val cm = colorMap

        if (cm != null) {
            val rawBuf = ByteArray(rawBytesPerPixel * width * height)
            if (imageType.isEncoded()) {
                readEncodedData(rawBuf, rawBuf.size)
            } else {
                reader.readExact(rawBuf)
            }
            expandColorMap(rawBuf, buf, cm)
        } else {
            if (imageType.isEncoded()) {
                readEncodedData(buf, width * height * colorType.bytesPerPixel().toInt())
            } else {
                reader.readExact(buf, offset = 0, count = width * height * colorType.bytesPerPixel().toInt())
            }
        }

        reverseEncodingInOutput(buf)

        when (orientation) {
            TgaOrientation.TopLeft -> Unit
            TgaOrientation.TopRight -> flipHorizontal(buf)
            TgaOrientation.BottomLeft -> flipVertical(buf)
            TgaOrientation.BottomRight -> {
                flipHorizontal(buf)
                flipVertical(buf)
            }
        }
    }
}
