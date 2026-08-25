// port-lint: source io/free_functions.rs
package io.github.kotlinmania.image.io

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.LimitError
import io.github.kotlinmania.image.LimitErrorKind
import io.github.kotlinmania.image.ParameterError
import io.github.kotlinmania.image.ParameterErrorKind
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.codecs.DdsDecoder
import io.github.kotlinmania.image.codecs.FarbfeldDecoder
import io.github.kotlinmania.image.codecs.FarbfeldEncoder
import io.github.kotlinmania.image.codecs.QoiDecoder
import io.github.kotlinmania.image.codecs.QoiEncoder
import io.github.kotlinmania.image.codecs.bmp.BmpDecoder
import io.github.kotlinmania.image.codecs.bmp.BmpEncoder
import io.github.kotlinmania.image.codecs.hdr.HdrDecoder
import io.github.kotlinmania.image.codecs.hdr.HdrEncoder
import io.github.kotlinmania.image.codecs.ico.IcoDecoder
import io.github.kotlinmania.image.codecs.ico.IcoEncoder
import io.github.kotlinmania.image.codecs.pnm.PnmDecoder
import io.github.kotlinmania.image.codecs.pnm.PnmEncoder
import io.github.kotlinmania.image.codecs.tga.TgaDecoder
import io.github.kotlinmania.image.codecs.tga.TgaEncoder
import io.github.kotlinmania.image.images.DynamicImage

private data class MagicSpec(
    val signature: ByteArray,
    val mask: ByteArray,
    val format: ImageFormat,
)

private val MAGIC_BYTES: List<MagicSpec> =
    listOf(
        MagicSpec(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A), byteArrayOf(), ImageFormat.Png),
        MagicSpec(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), byteArrayOf(), ImageFormat.Jpeg),
        MagicSpec("GIF89a".encodeToByteArray(), byteArrayOf(), ImageFormat.Gif),
        MagicSpec("GIF87a".encodeToByteArray(), byteArrayOf(), ImageFormat.Gif),
        MagicSpec(
            byteArrayOf(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50),
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0, 0, 0, 0),
            ImageFormat.WebP,
        ),
        MagicSpec(byteArrayOf(0x4D, 0x4D, 0x00, 0x2A), byteArrayOf(), ImageFormat.Tiff),
        MagicSpec(byteArrayOf(0x49, 0x49, 0x2A, 0x00), byteArrayOf(), ImageFormat.Tiff),
        MagicSpec("DDS ".encodeToByteArray(), byteArrayOf(), ImageFormat.Dds),
        MagicSpec("BM".encodeToByteArray(), byteArrayOf(), ImageFormat.Bmp),
        MagicSpec(byteArrayOf(0, 0, 1, 0), byteArrayOf(), ImageFormat.Ico),
        MagicSpec("#?RADIANCE".encodeToByteArray(), byteArrayOf(), ImageFormat.Hdr),
        MagicSpec(
            byteArrayOf(0, 0, 0, 0, 0x66, 0x74, 0x79, 0x70, 0x61, 0x76, 0x69, 0x66),
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0, 0),
            ImageFormat.Avif,
        ),
        MagicSpec(byteArrayOf(0x76, 0x2F, 0x31, 0x01), byteArrayOf(), ImageFormat.OpenExr),
        MagicSpec("qoif".encodeToByteArray(), byteArrayOf(), ImageFormat.Qoi),
        MagicSpec("P1".encodeToByteArray(), byteArrayOf(), ImageFormat.Pnm),
        MagicSpec("P2".encodeToByteArray(), byteArrayOf(), ImageFormat.Pnm),
        MagicSpec("P3".encodeToByteArray(), byteArrayOf(), ImageFormat.Pnm),
        MagicSpec("P4".encodeToByteArray(), byteArrayOf(), ImageFormat.Pnm),
        MagicSpec("P5".encodeToByteArray(), byteArrayOf(), ImageFormat.Pnm),
        MagicSpec("P6".encodeToByteArray(), byteArrayOf(), ImageFormat.Pnm),
        MagicSpec("P7".encodeToByteArray(), byteArrayOf(), ImageFormat.Pnm),
        MagicSpec("farbfeld".encodeToByteArray(), byteArrayOf(), ImageFormat.Farbfeld),
    )

/**
 * Makes an educated guess about the image format based on magic bytes at the beginning.
 */
public fun guessFormat(buffer: ByteArray): ImageFormat =
    guessFormatImpl(buffer) ?: throw ImageError.Unsupported(
        UnsupportedError(
            ImageFormatHint.Unknown,
            UnsupportedErrorKind.Format(ImageFormatHint.Unknown),
        ),
    )

public fun guessFormatImpl(buffer: ByteArray): ImageFormat? {
    for (spec in MAGIC_BYTES) {
        val sig = spec.signature
        val mask = spec.mask
        if (mask.isEmpty()) {
            if (buffer.size >= sig.size && buffer.copyOfRange(0, sig.size).contentEquals(sig)) {
                return spec.format
            }
        } else if (buffer.size >= sig.size) {
            var matches = true
            for (i in sig.indices) {
                val m = if (i < mask.size) mask[i].toInt() and 0xFF else 0xFF
                if (((buffer[i].toInt() and 0xFF) and m) != (sig[i].toInt() and 0xFF)) {
                    matches = false
                    break
                }
            }
            if (matches) return spec.format
        }
    }
    return null
}

/**
 * Returns an encoder for the given format writing to [writer].
 */
internal fun encoderForFormat(format: ImageFormat, writer: IoWrite): ImageEncoder =
    when (format) {
        ImageFormat.Bmp -> BmpEncoder(writer)
        ImageFormat.Ico -> IcoEncoder(writer)
        ImageFormat.Tga -> TgaEncoder(writer)
        ImageFormat.Farbfeld -> FarbfeldEncoder(writer)
        ImageFormat.Qoi -> QoiEncoder(writer)
        ImageFormat.Pnm -> PnmEncoder(writer)
        ImageFormat.Hdr -> HdrEncoder.new(writer)
        else -> throw ImageError.Unsupported(
            UnsupportedError(
                ImageFormatHint.Exact(format),
                UnsupportedErrorKind.Format(ImageFormatHint.Name(format.name)),
            ),
        )
    }

/**
 * Returns a decoder for the given format reading from [reader].
 */
internal fun decoderForFormat(format: ImageFormat, reader: IoRead): ImageDecoder =
    when (format) {
        ImageFormat.Bmp -> BmpDecoder(reader)
        ImageFormat.Ico -> IcoDecoder(reader)
        ImageFormat.Tga -> TgaDecoder(reader)
        ImageFormat.Farbfeld -> FarbfeldDecoder(reader)
        ImageFormat.Qoi -> QoiDecoder(reader)
        ImageFormat.Pnm -> PnmDecoder(reader)
        ImageFormat.Dds -> DdsDecoder(reader)
        ImageFormat.Hdr -> HdrDecoder.new(reader)
        else -> throw ImageError.Unsupported(
            UnsupportedError(
                ImageFormatHint.Exact(format),
                UnsupportedErrorKind.Format(ImageFormatHint.Name(format.name)),
            ),
        )
    }

/**
 * Loads an image from the given reader and format.
 */
public fun load(reader: IoRead, format: ImageFormat): DynamicImage {
    val decoder = decoderForFormat(format, reader)
    return DynamicImage.fromDecoder(decoder)
}

/**
 * Saves the supplied buffer to a sink given the desired format.
 */
public fun saveBuffer(
    writer: IoWrite,
    buf: ByteArray,
    width: UInt,
    height: UInt,
    color: ExtendedColorType,
    format: ImageFormat,
) {
    saveBufferWithFormat(writer, buf, width, height, color, format)
}

/**
 * Saves the supplied buffer to a sink given the desired format.
 */
public fun saveBufferWithFormat(
    writer: IoWrite,
    buf: ByteArray,
    width: UInt,
    height: UInt,
    color: ExtendedColorType,
    format: ImageFormat,
) {
    val encoder = encoderForFormat(format, writer)
    encoder.writeImage(buf, width, height, color)
}

/**
 * Decodes all bytes from an [ImageDecoder] into a [ByteArray].
 */
public fun decoderToVec(decoder: ImageDecoder): ByteArray {
    val totalBytes = decoder.totalBytes()
    if (totalBytes > Int.MAX_VALUE.toULong()) {
        throw ImageError.Limits(LimitError(LimitErrorKind.InsufficientMemory))
    }
    val buf = ByteArray(totalBytes.toInt())
    decoder.readImage(buf)
    return buf
}

/**
 * Decodes a specific region of the image into [buf].
 */
public fun loadRect(
    x: UInt,
    y: UInt,
    width: UInt,
    height: UInt,
    buf: ByteArray,
    rowPitch: Int,
    decoder: ImageDecoder,
    scanlineBytes: Int,
    seekScanline: (decoder: ImageDecoder, n: ULong) -> Unit,
    readScanline: (decoder: ImageDecoder, buf: ByteArray) -> Unit,
) {
    val (imgWidth, imgHeight) = decoder.dimensions()
    val bytesPerPixel = decoder.colorType().bytesPerPixel().toInt()
    val rowBytes = bytesPerPixel * imgWidth.toInt()
    val totalBytes = width.toLong() * height.toLong() * bytesPerPixel

    require(buf.size >= totalBytes) {
        "output buffer too short: expected $totalBytes, provided ${buf.size}"
    }

    if (x + width > imgWidth || y + height > imgHeight || width == 0u || height == 0u) {
        throw ImageError.Parameter(ParameterError(ParameterErrorKind.DimensionMismatch))
    }

    var currentScanline = 0L
    val tmp = ByteArray(scanlineBytes)
    var tmpScanline: Long? = null

    fun readImageRange(startPos: Long, endPos: Long, output: ByteArray, outOffset: Int) {
        var start = startPos
        var currentOut = outOffset
        val targetScanline = start / scanlineBytes
        if (tmpScanline == targetScanline) {
            val position = targetScanline * scanlineBytes
            val offset = (start - position).coerceAtLeast(0L)
            val len = minOf(endPos - start, minOf(scanlineBytes.toLong() - offset, endPos - position))
            tmp.copyInto(output, destinationOffset = currentOut, startIndex = offset.toInt(), endIndex = (offset + len).toInt())
            start += len
            currentOut += len.toInt()
            if (start == endPos) return
        }

        val scanline = start / scanlineBytes
        if (scanline != currentScanline) {
            seekScanline(decoder, scanline.toULong())
            currentScanline = scanline
        }

        var position = currentScanline * scanlineBytes
        while (position < endPos) {
            if (position >= start && endPos - position >= scanlineBytes) {
                val slice = ByteArray(scanlineBytes)
                readScanline(decoder, slice)
                slice.copyInto(output, destinationOffset = currentOut, startIndex = 0, endIndex = scanlineBytes)
                currentOut += scanlineBytes
            } else {
                readScanline(decoder, tmp)
                tmpScanline = currentScanline
                val offset = (start - position).coerceAtLeast(0L)
                val len = minOf(endPos - start, minOf(scanlineBytes.toLong() - offset, endPos - position))
                tmp.copyInto(output, destinationOffset = currentOut, startIndex = offset.toInt(), endIndex = (offset + len).toInt())
                currentOut += len.toInt()
            }
            currentScanline += 1
            position += scanlineBytes
        }
    }

    if (x == 0u && width == imgWidth && rowPitch == rowBytes) {
        val start = (x.toLong() * bytesPerPixel) + (y.toLong() * rowBytes)
        val end = ((x + width).toLong() * bytesPerPixel) + ((y + height - 1u).toLong() * rowBytes)
        readImageRange(start, end, buf, 0)
    } else {
        for (r in 0 until height.toInt()) {
            val rowY = y.toInt() + r
            val start = (x.toLong() * bytesPerPixel) + (rowY.toLong() * rowBytes)
            val end = ((x + width).toLong() * bytesPerPixel) + (rowY.toLong() * rowBytes)
            val outOffset = r * rowPitch
            readImageRange(start, end, buf, outOffset)
        }
    }

    seekScanline(decoder, 0uL)
}
