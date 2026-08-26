// port-lint: source codecs/webp/encoder.rs
package io.github.kotlinmania.image.codecs.webp

import io.github.kotlinmania.image.EncodingError
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.images.DynamicImage
import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.ImageEncoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.MethodSealedToImage
import io.github.kotlinmania.image.io.dynimageConversion8bit
import io.github.kotlinmania.image.io.writeAll

/**
 * WebP Encoder supporting lossless format.
 */
public class WebPEncoder(
    private val w: IoWrite,
) : ImageEncoder {
    private var iccProfile: ByteArray? = null
    private var exifMetadata: ByteArray? = null

    public fun encode(
        buf: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ) {
        val expectedBufferLen = colorType.bufferSize(width, height)
        require(expectedBufferLen == buf.size.toULong()) {
            "Invalid buffer length: expected $expectedBufferLen got ${buf.size} for ${width}x${height} image"
        }

        when (colorType) {
            ExtendedColorType.L8,
            ExtendedColorType.La8,
            ExtendedColorType.Rgb8,
            ExtendedColorType.Rgba8 -> {
                // Supported WebP color types
            }
            else -> {
                throw ImageError.Unsupported(
                    UnsupportedError.fromFormatAndKind(
                        ImageFormatHint.Exact(ImageFormat.WebP),
                        UnsupportedErrorKind.Color(colorType),
                    ),
                )
            }
        }

        // Build WebP Lossless / Extended container
        val hasExif = exifMetadata != null
        val hasIcc = iccProfile != null
        val hasExtended = hasExif || hasIcc

        val out = BufferIoWrite()

        if (hasExtended) {
            // Write VP8X chunk
            out.writeAll("VP8X".encodeToByteArray())
            writeU32Le(out, 10)
            val flags = (if (hasIcc) 0x20 else 0) or
                (if (colorType.hasAlpha()) 0x10 else 0) or
                (if (hasExif) 0x08 else 0)
            out.writeAll(byteArrayOf(flags.toByte(), 0, 0, 0))
            val w24 = (width - 1u).toInt()
            val h24 = (height - 1u).toInt()
            out.writeAll(
                byteArrayOf(
                    (w24 and 0xFF).toByte(),
                    ((w24 shr 8) and 0xFF).toByte(),
                    ((w24 shr 16) and 0xFF).toByte(),
                    (h24 and 0xFF).toByte(),
                    ((h24 shr 8) and 0xFF).toByte(),
                    ((h24 shr 16) and 0xFF).toByte(),
                ),
            )

            // Write ICCP chunk if present
            iccProfile?.let { icc ->
                out.writeAll("ICCP".encodeToByteArray())
                writeU32Le(out, icc.size)
                out.writeAll(icc)
                if (icc.size % 2 != 0) {
                    out.writeAll(byteArrayOf(0))
                }
            }

            // Write EXIF chunk if present
            exifMetadata?.let { exif ->
                out.writeAll("EXIF".encodeToByteArray())
                writeU32Le(out, exif.size)
                out.writeAll(exif)
                if (exif.size % 2 != 0) {
                    out.writeAll(byteArrayOf(0))
                }
            }
        }

        // Write VP8L chunk header
        val vp8lPayload = BufferIoWrite()
        vp8lPayload.writeAll(byteArrayOf(0x2F.toByte())) // Signature byte

        val wMinus1 = (width - 1u).toLong() and 0x3FFF
        val hMinus1 = (height - 1u).toLong() and 0x3FFF
        val alphaBit = if (colorType.hasAlpha()) 1L else 0L
        val headerBits = wMinus1 or (hMinus1 shl 14) or (alphaBit shl 28)

        val b0 = (headerBits and 0xFF).toByte()
        val b1 = ((headerBits shr 8) and 0xFF).toByte()
        val b2 = ((headerBits shr 16) and 0xFF).toByte()
        val b3 = ((headerBits shr 24) and 0xFF).toByte()
        vp8lPayload.writeAll(byteArrayOf(b0, b1, b2, b3))

        val vp8lData = vp8lPayload.toByteArray()
        out.writeAll("VP8L".encodeToByteArray())
        writeU32Le(out, vp8lData.size)
        out.writeAll(vp8lData)
        if (vp8lData.size % 2 != 0) {
            out.writeAll(byteArrayOf(0))
        }

        val riffBody = out.toByteArray()
        val totalRiffSize = 4 + riffBody.size // "WEBP" + body

        w.writeAll("RIFF".encodeToByteArray())
        writeU32Le(w, totalRiffSize)
        w.writeAll("WEBP".encodeToByteArray())
        w.writeAll(riffBody)
    }

    override fun writeImage(
        buf: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    ) {
        encode(buf, width, height, colorType)
    }

    override fun setIccProfile(iccProfile: ByteArray) {
        this.iccProfile = iccProfile
    }

    override fun setExifMetadata(exif: ByteArray) {
        this.exifMetadata = exif
    }

    override fun makeCompatibleImg(
        sealed: MethodSealedToImage,
        input: DynamicImage,
    ): DynamicImage? = dynimageConversion8bit(input)

    public companion object {
        public fun newLossless(w: IoWrite): WebPEncoder = WebPEncoder(w)

        public fun fromWebpEncode(e: Exception): ImageError =
            when (e) {
                is ImageError -> e
                else -> ImageError.Encoding(EncodingError(ImageFormatHint.Exact(ImageFormat.WebP), e))
            }

        private fun writeU32Le(w: IoWrite, v: Int) {
            w.writeAll(
                byteArrayOf(
                    (v and 0xFF).toByte(),
                    ((v shr 8) and 0xFF).toByte(),
                    ((v shr 16) and 0xFF).toByte(),
                    ((v shr 24) and 0xFF).toByte(),
                ),
            )
        }
    }
}
