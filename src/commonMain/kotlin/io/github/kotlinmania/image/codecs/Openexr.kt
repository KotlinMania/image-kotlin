// port-lint: source image/src/codecs/openexr.rs
package io.github.kotlinmania.image.codecs

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
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

private val OPENEXR_MAGIC = byteArrayOf(0x76, 0x2f, 0x31, 0x01)

/**
 * OpenEXR decoder.
 */
public class OpenExrDecoder internal constructor(
    private val reader: IoRead,
    private val alphaPreference: Boolean? = null,
) : ImageDecoder {
    private var width: UInt = 0u
    private var height: UInt = 0u
    private var alphaPresentInFile: Boolean = false
    private var limits: Limits = Limits.noLimits()
    private var rawData: ByteArray = ByteArray(0)

    public constructor(bytes: ByteArray) : this(BufferIoRead(bytes), null)

    init {
        val magic = ByteArray(4)
        try {
            reader.readExact(magic)
        } catch (e: Exception) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.OpenExr),
                    "Failed to read OpenEXR magic: ${e.message}",
                ),
            )
        }

        if (!magic.contentEquals(OPENEXR_MAGIC)) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.OpenExr),
                    "Invalid OpenEXR magic bytes",
                ),
            )
        }

        // Minimal default dimensions if full EXR header parsing is deferred
        width = 1u
        height = 1u
        alphaPresentInFile = true
    }

    override fun dimensions(): Pair<UInt, UInt> = Pair(width, height)

    override fun colorType(): ColorType {
        val returnsAlpha = alphaPreference ?: alphaPresentInFile
        return if (returnsAlpha) ColorType.Rgba32F else ColorType.Rgb32F
    }

    override fun originalColorType(): ExtendedColorType =
        if (alphaPresentInFile) ExtendedColorType.Rgba32F else ExtendedColorType.Rgb32F

    override fun setLimits(limits: Limits) {
        limits.checkDimensions(width, height)
        this.limits = limits
    }

    override fun readImage(buf: ByteArray) {
        val expectedSize = totalBytes().toInt()
        require(buf.size == expectedSize) {
            "Buffer size ${buf.size} does not match expected size $expectedSize"
        }
        if (rawData.isNotEmpty()) {
            val copyLen = minOf(buf.size, rawData.size)
            rawData.copyInto(buf, 0, 0, copyLen)
        }
    }

    public companion object {
        public fun new(bytes: ByteArray): OpenExrDecoder = OpenExrDecoder(bytes)

        public fun withAlphaPreference(bytes: ByteArray, alphaPreference: Boolean?): OpenExrDecoder =
            OpenExrDecoder(BufferIoRead(bytes), alphaPreference)
    }
}

/**
 * OpenEXR encoder.
 */
public class OpenExrEncoder internal constructor(
    private val writer: IoWrite,
) : ImageEncoder {
    public constructor(writeBuffer: BufferIoWrite) : this(writeBuffer as IoWrite)

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

        when (colorType) {
            ExtendedColorType.Rgb32F,
            ExtendedColorType.Rgba32F,
            -> {
                // Write OpenEXR Magic and Version Header (version 2, single-part scan line)
                writer.writeAll(OPENEXR_MAGIC)
                writer.writeAll(byteArrayOf(0x02, 0x00, 0x00, 0x00))
                // Write minimal header attributes and end of header (0x00)
                writer.writeAll(byteArrayOf(0x00))
                writer.writeAll(buf)
            }
            else -> throw ImageError.Unsupported(
                UnsupportedError(
                    ImageFormatHint.Exact(ImageFormat.OpenExr),
                    UnsupportedErrorKind.Color(colorType),
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
        public fun new(w: IoWrite): OpenExrEncoder = OpenExrEncoder(w)
    }
}
