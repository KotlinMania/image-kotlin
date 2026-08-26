// port-lint: source codecs/ico/encoder.rs
package io.github.kotlinmania.image.codecs.ico

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ParameterError
import io.github.kotlinmania.image.ParameterErrorKind
import io.github.kotlinmania.image.codecs.bmp.BmpEncoder
import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.writeAll

private const val ICO_IMAGE_TYPE: Int = 1
private const val ICO_ICONDIR_SIZE: UInt = 6u
private const val ICO_DIRENTRY_SIZE: UInt = 16u

/**
 * An ICO image entry.
 */
public class IcoFrame internal constructor(
    public val encodedImage: ByteArray,
    public val width: UByte,
    public val height: UByte,
    public val colorType: ExtendedColorType,
) {
    public companion object {
        /**
         * Construct a new [IcoFrame] using a pre-encoded PNG or BMP.
         *
         * The [width] and [height] must be between 1 and 256 (inclusive).
         */
        public fun withEncoded(
            encodedImage: ByteArray,
            width: UInt,
            height: UInt,
            colorType: ExtendedColorType,
        ): IcoFrame {
            if (width !in 1u..256u) {
                throw ImageError.Parameter(
                    ParameterError(
                        ParameterErrorKind.Generic(
                            "the image width must be `1..=256`, instead width $width was provided",
                        ),
                    ),
                )
            }
            if (height !in 1u..256u) {
                throw ImageError.Parameter(
                    ParameterError(
                        ParameterErrorKind.Generic(
                            "the image height must be `1..=256`, instead height $height was provided",
                        ),
                    ),
                )
            }
            val wByte = if (width == 256u) 0u.toUByte() else width.toUByte()
            val hByte = if (height == 256u) 0u.toUByte() else height.toUByte()
            return IcoFrame(encodedImage, wByte, hByte, colorType)
        }

        /**
         * Construct a new [IcoFrame] by encoding [buf] as a PNG image.
         */
        public fun asPng(
            buf: ByteArray,
            width: UInt,
            height: UInt,
            colorType: ExtendedColorType,
        ): IcoFrame {
            val sink = BufferIoWrite()
            val encoder = io.github.kotlinmania.image.codecs.png.PngEncoder(sink)
            encoder.writeImage(buf, width, height, colorType)
            return withEncoded(sink.toByteArray(), width, height, colorType)
        }

        /**
         * Construct a new [IcoFrame] by encoding [buf] as a BMP image.
         */
        public fun asBmp(
            buf: ByteArray,
            width: UInt,
            height: UInt,
            colorType: ExtendedColorType,
        ): IcoFrame {
            val sink = BufferIoWrite()
            val encoder = BmpEncoder(sink)
            encoder.writeImage(buf, width, height, colorType)
            return withEncoded(sink.toByteArray(), width, height, colorType)
        }
    }
}

/**
 * ICO encoder.
 */
public class IcoEncoder internal constructor(
    private val writer: IoWrite,
) : ImageEncoder {
    public constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite)

    public companion object {
        public fun new(w: IoWrite): IcoEncoder = IcoEncoder(w)
    }

    /**
     * Takes some [IcoFrame]s and encodes them into an ICO.
     *
     * [images] is a list of images, usually ordered by dimension, which
     * must be between 1 and 65535 (inclusive) in length.
     */
    public fun encodeImages(images: List<IcoFrame>) {
        if (images.isEmpty() || images.size > 65535) {
            throw ImageError.Parameter(
                ParameterError(
                    ParameterErrorKind.Generic(
                        "the number of images must be `1..=u16::MAX`, instead ${images.size} images were provided",
                    ),
                ),
            )
        }

        writeIcondir(writer, images.size)
        var offset = ICO_ICONDIR_SIZE + (ICO_DIRENTRY_SIZE * images.size.toUInt())

        for (image in images) {
            writeDirentry(
                writer,
                image.width,
                image.height,
                image.colorType,
                offset,
                image.encodedImage.size.toUInt(),
            )
            offset += image.encodedImage.size.toUInt()
        }

        for (image in images) {
            writer.writeAll(image.encodedImage)
        }
    }

    /**
     * Write an ICO image with the specified width, height, and color type.
     */
    override fun writeImage(
        buf: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ) {
        val expectedBufferLen = colorType.bufferSize(width, height)
        require(expectedBufferLen == buf.size.toULong()) {
            "Invalid buffer length: expected $expectedBufferLen got ${buf.size} for ${width}x$height image"
        }

        val image = IcoFrame.asBmp(buf, width, height, colorType)
        encodeImages(listOf(image))
    }
}

private fun writeIcondir(writer: IoWrite, numImages: Int) {
    // Reserved field (must be zero):
    writer.writeU16Le(0)
    // Image type (ICO = 1, CUR = 2):
    writer.writeU16Le(ICO_IMAGE_TYPE)
    // Number of images in the file:
    writer.writeU16Le(numImages)
}

private fun writeDirentry(
    writer: IoWrite,
    width: UByte,
    height: UByte,
    color: ExtendedColorType,
    dataStart: UInt,
    dataSize: UInt,
) {
    // Image dimensions:
    writer.writeU8(width.toInt())
    writer.writeU8(height.toInt())
    // Number of colors in palette (or zero for no palette):
    writer.writeU8(0)
    // Reserved field (must be zero):
    writer.writeU8(0)
    // Color planes:
    writer.writeU16Le(0)
    // Bits per pixel:
    writer.writeU16Le(color.bitsPerPixel().toInt())
    // Image data size, in bytes:
    writer.writeU32Le(dataSize.toLong())
    // Image data offset, in bytes:
    writer.writeU32Le(dataStart.toLong())
}

private fun IoWrite.writeU8(v: Int) {
    writeAll(byteArrayOf((v and 0xFF).toByte()))
}

private fun IoWrite.writeU16Le(v: Int) {
    val b0 = (v and 0xFF).toByte()
    val b1 = ((v shr 8) and 0xFF).toByte()
    writeAll(byteArrayOf(b0, b1))
}

private fun IoWrite.writeU32Le(v: Long) {
    val b0 = (v and 0xFF).toByte()
    val b1 = ((v shr 8) and 0xFF).toByte()
    val b2 = ((v shr 16) and 0xFF).toByte()
    val b3 = ((v shr 24) and 0xFF).toByte()
    writeAll(byteArrayOf(b0, b1, b2, b3))
}
