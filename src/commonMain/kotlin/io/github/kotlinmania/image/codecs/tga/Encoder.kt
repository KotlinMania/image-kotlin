// port-lint: source codecs/tga/encoder.rs
package io.github.kotlinmania.image.codecs.tga

import io.github.kotlinmania.image.EncodingError
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.writeAll

/**
 * Errors that can occur during encoding and saving of a TGA image.
 */
public sealed class EncoderError(
    message: String,
) : Exception(message) {
    public data class WidthInvalid(
        val width: UInt,
    ) : EncoderError("Invalid TGA width: $width")

    public data class HeightInvalid(
        val height: UInt,
    ) : EncoderError("Invalid TGA height: $height")

    public data class Empty(
        val width: UInt,
        val height: UInt,
    ) : EncoderError("Invalid TGA size: ${width}x$height")
}

private const val MAX_RUN_LENGTH: Int = 128

private enum class PacketType {
    Raw,
    Rle,
}

/**
 * TGA encoder.
 */
public class TgaEncoder internal constructor(
    private val writer: IoWrite,
    private var useRle: Boolean = true,
) : ImageEncoder {
    internal constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite)

    /**
     * Disables run-length encoding.
     */
    public fun disableRle(): TgaEncoder {
        useRle = false
        return this
    }

    private fun writeRawPacket(pixels: ByteArray, counter: Int) {
        val header = ((counter - 1) and 0x7F).toByte()
        writer.writeAll(byteArrayOf(header))
        writer.writeAll(pixels)
    }

    private fun writeRleEncodedPacket(pixel: ByteArray, counter: Int) {
        val header = (0x80 or ((counter - 1) and 0x7F)).toByte()
        writer.writeAll(byteArrayOf(header))
        writer.writeAll(pixel)
    }

    private fun runLengthEncode(image: ByteArray, colorType: ExtendedColorType) {
        val bpp = (colorType.bitsPerPixel().toInt() / 8)
        val tempBuf = ArrayList<Byte>(MAX_RUN_LENGTH * bpp)
        var counter = 0
        var prevPixel: ByteArray? = null
        var packetType = PacketType.Rle

        var offset = 0
        while (offset < image.size) {
            val pixel = image.copyOfRange(offset, offset + bpp)
            val prev = prevPixel
            if (prev != null) {
                if (pixel.contentEquals(prev)) {
                    if (packetType == PacketType.Raw && counter > 0) {
                        writeRawPacket(tempBuf.toByteArray(), counter)
                        counter = 0
                        tempBuf.clear()
                    }
                    packetType = PacketType.Rle
                } else if (packetType == PacketType.Rle && counter > 0) {
                    writeRleEncodedPacket(prev, counter)
                    counter = 0
                    packetType = PacketType.Raw
                    tempBuf.clear()
                }
            }

            counter += 1
            for (b in pixel) {
                tempBuf.add(b)
            }

            if (counter == MAX_RUN_LENGTH) {
                when (packetType) {
                    PacketType.Rle -> writeRleEncodedPacket(prevPixel!!, counter)
                    PacketType.Raw -> writeRawPacket(tempBuf.toByteArray(), counter)
                }
                counter = 0
                packetType = PacketType.Rle
                tempBuf.clear()
            }

            prevPixel = pixel
            offset += bpp
        }

        if (counter > 0) {
            when (packetType) {
                PacketType.Rle -> writeRleEncodedPacket(prevPixel!!, counter)
                PacketType.Raw -> writeRawPacket(tempBuf.toByteArray(), counter)
            }
        }
    }

    /**
     * Encodes the image [buf] that has dimensions [width] and [height] and [ExtendedColorType] [colorType].
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

        if (width == 0u || height == 0u) {
            val err = EncoderError.Empty(width, height)
            throw ImageError.Encoding(EncodingError(ImageFormatHint.Exact(ImageFormat.Tga), err))
        }

        if (width > 65535u) {
            val err = EncoderError.WidthInvalid(width)
            throw ImageError.Encoding(EncodingError(ImageFormatHint.Exact(ImageFormat.Tga), err))
        }

        if (height > 65535u) {
            val err = EncoderError.HeightInvalid(height)
            throw ImageError.Encoding(EncodingError(ImageFormatHint.Exact(ImageFormat.Tga), err))
        }

        val header = Header.fromPixelInfo(colorType, width.toUShort(), height.toUShort(), useRle).getOrThrow()
        header.writeTo(writer).getOrThrow()

        val imageType = ImageType.fromValue(header.imageType)
        when (imageType) {
            ImageType.RunTrueColor, ImageType.RunGrayScale -> {
                when (colorType) {
                    ExtendedColorType.Rgb8, ExtendedColorType.Rgba8 -> {
                        val image = buf.copyOf()
                        val bpp = colorType.bitsPerPixel().toInt() / 8
                        var off = 0
                        while (off + bpp <= image.size) {
                            val tmp = image[off]
                            image[off] = image[off + 2]
                            image[off + 2] = tmp
                            off += bpp
                        }
                        runLengthEncode(image, colorType)
                    }
                    else -> {
                        runLengthEncode(buf, colorType)
                    }
                }
            }
            else -> {
                when (colorType) {
                    ExtendedColorType.Rgb8, ExtendedColorType.Rgba8 -> {
                        val image = buf.copyOf()
                        val bpp = colorType.bitsPerPixel().toInt() / 8
                        var off = 0
                        while (off + bpp <= image.size) {
                            val tmp = image[off]
                            image[off] = image[off + 2]
                            image[off + 2] = tmp
                            off += bpp
                        }
                        writer.writeAll(image)
                    }
                    else -> {
                        writer.writeAll(buf)
                    }
                }
            }
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
}
