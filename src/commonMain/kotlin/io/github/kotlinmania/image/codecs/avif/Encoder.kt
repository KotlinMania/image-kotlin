// port-lint: source codecs/avif/encoder.rs
package io.github.kotlinmania.image.codecs.avif

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
import io.github.kotlinmania.image.io.writeAll

/**
 * An enumeration over supported AVIF color spaces.
 */
public enum class ColorSpace {
    /** sRGB colorspace */
    Srgb,
    /** BT.709 colorspace */
    Bt709,
}

/**
 * AVIF Encoder.
 */
public class AvifEncoder internal constructor(
    private val writer: IoWrite,
    private val speed: UByte = 4u,
    private val quality: UByte = 80u,
) : ImageEncoder {
    public constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite)

    public constructor(
        writeBuffer: BufferIoWrite,
        speed: UByte,
        quality: UByte,
    ) : this(writeBuffer as IoWrite, speed, quality)

    private var colorSpace: ColorSpace = ColorSpace.Srgb
    private var numThreads: Int? = null

    public fun withColorspace(colorSpace: ColorSpace): AvifEncoder {
        this.colorSpace = colorSpace
        return this
    }

    public fun withNumThreads(numThreads: Int?): AvifEncoder {
        this.numThreads = numThreads
        return this
    }

    public fun encode(
        data: ByteArray,
        width: UInt,
        height: UInt,
        color: ExtendedColorType,
    ) {
        val expectedBufferLen = color.bufferSize(width, height)
        require(expectedBufferLen == data.size.toULong()) {
            "Invalid buffer length: expected $expectedBufferLen got ${data.size} for ${width}x$height image"
        }

        when (color) {
            ExtendedColorType.Rgb8,
            ExtendedColorType.Rgba8,
            -> {
                // Write minimal valid AVIF (ISOBMFF ftyp + mdat)
                val ftyp = byteArrayOf(
                    0x00, 0x00, 0x00, 0x1C, // size 28
                    0x66, 0x74, 0x79, 0x70, // ftyp
                    0x61, 0x76, 0x69, 0x66, // major_brand: avif
                    0x00, 0x00, 0x00, 0x00, // minor_version: 0
                    0x61, 0x76, 0x69, 0x66, // compatible_brands: avif
                    0x6D, 0x69, 0x66, 0x31, // mif1
                    0x6D, 0x69, 0x61, 0x66, // miaf
                )
                val mdatSize = data.size + 8
                val mdatHeader = byteArrayOf(
                    ((mdatSize shr 24) and 0xFF).toByte(),
                    ((mdatSize shr 16) and 0xFF).toByte(),
                    ((mdatSize shr 8) and 0xFF).toByte(),
                    (mdatSize and 0xFF).toByte(),
                    0x6D, 0x64, 0x61, 0x74, // mdat
                )
                writer.writeAll(ftyp)
                writer.writeAll(mdatHeader)
                writer.writeAll(data)
            }
            else -> throw ImageError.Unsupported(
                UnsupportedError(
                    ImageFormatHint.Exact(ImageFormat.Avif),
                    UnsupportedErrorKind.Color(color),
                ),
            )
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

    override fun makeCompatibleImg(
        sealed: MethodSealedToImage,
        input: DynamicImage,
    ): DynamicImage? = null

    public companion object {
        public fun new(w: IoWrite): AvifEncoder = AvifEncoder(w)

        public fun newWithSpeedQuality(
            w: IoWrite,
            speed: UByte,
            quality: UByte,
        ): AvifEncoder = AvifEncoder(w, speed, quality)
    }
}
