// port-lint: source image/src/codecs/bmp/encoder.rs
package io.github.kotlinmania.image.codecs.bmp

import io.github.kotlinmania.image.EncodingError
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.ParameterError
import io.github.kotlinmania.image.ParameterErrorKind
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.writeAll

private const val BITMAPFILEHEADER_SIZE: UInt = 14u
private const val BITMAPINFOHEADER_SIZE: UInt = 40u
private const val BITMAPV4HEADER_SIZE: UInt = 108u

/**
 * An RGB palette color entry.
 */
public data class PaletteColor(
    public val r: UByte,
    public val g: UByte,
    public val b: UByte,
)

/**
 * The representation of a BMP encoder.
 */
public class BmpEncoder(
    private val writer: IoWrite,
) : ImageEncoder {
    internal constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite)

    public companion object {
        /** Create a new encoder that writes its output to [writer]. */
        public fun new(writer: IoWrite): BmpEncoder = BmpEncoder(writer)
    }

    /**
     * Encodes the image that has dimensions [width] and [height] and [ExtendedColorType] [colorType].
     */
    public fun encode(
        image: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ) {
        encodeWithPalette(image, width, height, colorType, null)
    }

    /**
     * Same as [encode], but allow a palette to be passed in.
     */
    public fun encodeWithPalette(
        image: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
        palette: List<PaletteColor>?,
    ) {
        if (palette != null && colorType != ExtendedColorType.L8 && colorType != ExtendedColorType.La8) {
            throw ImageError.Parameter(
                ParameterError(
                    ParameterErrorKind.Generic("Palette given which must only be used with L8 or La8 color types"),
                ),
            )
        }

        val expectedBufferLen = colorType.bufferSize(width, height)
        if (expectedBufferLen != image.size.toULong()) {
            throw IllegalArgumentException(
                "Invalid buffer length: expected $expectedBufferLen got ${image.size} for ${width}x$height image",
            )
        }

        val bmpHeaderSize = BITMAPFILEHEADER_SIZE
        val (dibHeaderSize, writtenPixelSize, paletteColorCount) = writtenPixelInfo(colorType, palette)

        val rowBytes = width.toULong() * writtenPixelSize.toULong()
        val paddedRow = (rowBytes + 3uL) and 3uL.inv()
        val imageSize = paddedRow * height.toULong()

        if (width > 0x7FFFFFFFu || height > 0x7FFFFFFFu || imageSize > UInt.MAX_VALUE.toULong()) {
            throw ImageError.Parameter(ParameterError(ParameterErrorKind.DimensionMismatch))
        }

        val rowPadding = (paddedRow - rowBytes).toUInt()
        val paletteSize = paletteColorCount.toULong() * 4uL

        val fileSize = bmpHeaderSize.toULong() + dibHeaderSize.toULong() + paletteSize + imageSize
        val imageDataOffset = bmpHeaderSize.toULong() + dibHeaderSize.toULong() + paletteSize

        if (fileSize > UInt.MAX_VALUE.toULong() || imageDataOffset > UInt.MAX_VALUE.toULong()) {
            throw ImageError.Encoding(
                EncodingError(
                    ImageFormatHint.Exact(ImageFormat.Bmp),
                    "calculated BMP size larger than 2^32",
                ),
            )
        }

        // write BMP header
        writer.writeU8('B'.code)
        writer.writeU8('M'.code)
        writer.writeU32Le(fileSize.toLong())
        writer.writeU16Le(0)
        writer.writeU16Le(0)
        writer.writeU32Le(imageDataOffset.toLong())

        // write DIB header
        writer.writeU32Le(dibHeaderSize.toLong())
        writer.writeI32Le(width.toInt())
        writer.writeI32Le(height.toInt())
        writer.writeU16Le(1) // color planes
        writer.writeU16Le((writtenPixelSize * 8u).toInt()) // bits per pixel
        if (dibHeaderSize >= BITMAPV4HEADER_SIZE) {
            writer.writeU32Le(3L) // compression method - bitfields
        } else {
            writer.writeU32Le(0L) // compression method - no compression
        }
        writer.writeU32Le(imageSize.toLong())
        writer.writeI32Le(0) // horizontal ppm
        writer.writeI32Le(0) // vertical ppm
        writer.writeU32Le(paletteColorCount.toLong())
        writer.writeU32Le(0L) // all colors are important

        if (dibHeaderSize >= BITMAPV4HEADER_SIZE) {
            writer.writeU32Le(0xFF0000L) // red mask
            writer.writeU32Le(0x00FF00L) // green mask
            writer.writeU32Le(0x0000FFL) // blue mask
            writer.writeU32Le(0xFF000000L) // alpha mask
            writer.writeU32Le(0x73524742L) // colorspace - sRGB

            for (i in 0 until 12) {
                writer.writeU32Le(0L)
            }
        }

        when (colorType) {
            ExtendedColorType.Rgb8 -> encodeRgb(image, width, height, rowPadding, 3u)
            ExtendedColorType.Rgba8 -> encodeRgba(image, width, height, rowPadding, 4u)
            ExtendedColorType.L8 -> encodeGray(image, width, height, rowPadding, 1u, palette)
            ExtendedColorType.La8 -> encodeGray(image, width, height, rowPadding, 2u, palette)
            else -> throw ImageError.Unsupported(
                UnsupportedError(
                    ImageFormatHint.Exact(ImageFormat.Bmp),
                    UnsupportedErrorKind.Color(colorType),
                ),
            )
        }
    }

    private fun encodeRgb(
        image: ByteArray,
        width: UInt,
        height: UInt,
        rowPadding: UInt,
        bytesPerPixel: UInt,
    ) {
        val w = width.toInt()
        val h = height.toInt()
        val xStride = bytesPerPixel.toInt()
        val yStride = w * xStride
        val pad = ByteArray(rowPadding.toInt())

        for (row in (h - 1) downTo 0) {
            val rowStart = row * yStride
            for (col in 0 until w) {
                val px = rowStart + col * xStride
                val r = image[px]
                val g = image[px + 1]
                val b = image[px + 2]
                writer.writeAll(byteArrayOf(b, g, r))
            }
            if (pad.isNotEmpty()) {
                writer.writeAll(pad)
            }
        }
    }

    private fun encodeRgba(
        image: ByteArray,
        width: UInt,
        height: UInt,
        rowPadding: UInt,
        bytesPerPixel: UInt,
    ) {
        val w = width.toInt()
        val h = height.toInt()
        val xStride = bytesPerPixel.toInt()
        val yStride = w * xStride
        val pad = ByteArray(rowPadding.toInt())

        for (row in (h - 1) downTo 0) {
            val rowStart = row * yStride
            for (col in 0 until w) {
                val px = rowStart + col * xStride
                val r = image[px]
                val g = image[px + 1]
                val b = image[px + 2]
                val a = image[px + 3]
                writer.writeAll(byteArrayOf(b, g, r, a))
            }
            if (pad.isNotEmpty()) {
                writer.writeAll(pad)
            }
        }
    }

    private fun encodeGray(
        image: ByteArray,
        width: UInt,
        height: UInt,
        rowPadding: UInt,
        bytesPerPixel: UInt,
        palette: List<PaletteColor>?,
    ) {
        if (palette != null) {
            for (item in palette) {
                writer.writeAll(byteArrayOf(item.b.toByte(), item.g.toByte(), item.r.toByte(), 0))
            }
        } else {
            for (v in 0..255) {
                val b = v.toByte()
                writer.writeAll(byteArrayOf(b, b, b, 0))
            }
        }

        val w = width.toInt()
        val h = height.toInt()
        val xStride = bytesPerPixel.toInt()
        val yStride = w * xStride
        val pad = ByteArray(rowPadding.toInt())

        for (row in (h - 1) downTo 0) {
            val rowStart = row * yStride
            if (xStride == 1) {
                writer.writeAll(image, rowStart, yStride)
            } else {
                for (col in 0 until w) {
                    val px = rowStart + col * xStride
                    writer.writeAll(byteArrayOf(image[px]))
                }
            }
            if (pad.isNotEmpty()) {
                writer.writeAll(pad)
            }
        }
    }

    private fun writeRowPad(rowPadSize: UInt) {
        if (rowPadSize > 0u) {
            writer.writeAll(ByteArray(rowPadSize.toInt()))
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

private data class WrittenPixelInfo(
    val dibHeaderSize: UInt,
    val writtenPixelSize: UInt,
    val paletteColorCount: UInt,
)

private fun writtenPixelInfo(
    c: ExtendedColorType,
    palette: List<PaletteColor>?,
): WrittenPixelInfo =
    when (c) {
        ExtendedColorType.Rgb8 -> WrittenPixelInfo(BITMAPINFOHEADER_SIZE, 3u, 0u)
        ExtendedColorType.Rgba8 -> WrittenPixelInfo(BITMAPV4HEADER_SIZE, 4u, 0u)
        ExtendedColorType.L8 -> WrittenPixelInfo(BITMAPINFOHEADER_SIZE, 1u, palette?.size?.toUInt() ?: 256u)
        ExtendedColorType.La8 -> WrittenPixelInfo(BITMAPINFOHEADER_SIZE, 1u, palette?.size?.toUInt() ?: 256u)
        else -> throw ImageError.Unsupported(
            UnsupportedError(
                ImageFormatHint.Exact(ImageFormat.Bmp),
                UnsupportedErrorKind.Color(c),
            ),
        )
    }

private fun IoWrite.writeU8(value: Int) {
    writeAll(byteArrayOf((value and 0xFF).toByte()))
}

private fun IoWrite.writeU16Le(value: Int) {
    writeAll(byteArrayOf((value and 0xFF).toByte(), ((value ushr 8) and 0xFF).toByte()))
}

private fun IoWrite.writeU32Le(value: Long) {
    writeAll(
        byteArrayOf(
            (value and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte(),
            ((value ushr 24) and 0xFF).toByte(),
        ),
    )
}

private fun IoWrite.writeI32Le(value: Int) {
    writeU32Le(value.toLong() and 0xFFFFFFFFL)
}
