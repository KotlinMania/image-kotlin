// port-lint: source codecs/avif/decoder.rs
package io.github.kotlinmania.image.codecs.avif

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.DecodingError
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.ParameterError
import io.github.kotlinmania.image.ParameterErrorKind
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.io.BufferIoRead
import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.Limits
import io.github.kotlinmania.image.io.readExact

/**
 * AVIF decoder.
 */
public class AvifDecoder internal constructor(
    private val reader: IoRead,
) : ImageDecoder {
    private var width: UInt = 1u
    private var height: UInt = 1u
    private var colorType: ColorType = ColorType.Rgba8
    private var originalColorType: ExtendedColorType = ExtendedColorType.Rgba8
    private var limits: Limits = Limits.noLimits()
    private var iccProfileData: ByteArray? = null
    private var rawData: ByteArray = ByteArray(0)

    public constructor(bytes: ByteArray) : this(BufferIoRead(bytes))

    init {
        // Parse ftyp box for avif / avis brand
        val header = ByteArray(12)
        try {
            reader.readExact(header)
        } catch (e: Exception) {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Avif),
                    "Failed to read AVIF header: ${e.message}",
                ),
            )
        }

        val boxType = header.decodeToString(4, 8)
        if (boxType != "ftyp") {
            throw ImageError.Decoding(
                DecodingError(
                    ImageFormatHint.Exact(ImageFormat.Avif),
                    "Invalid AVIF container: expected ftyp box",
                ),
            )
        }
    }

    override fun dimensions(): Pair<UInt, UInt> = Pair(width, height)

    override fun colorType(): ColorType = colorType

    override fun originalColorType(): ExtendedColorType = originalColorType

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

    override fun iccProfile(): ByteArray? = iccProfileData

    public companion object {
        public fun new(bytes: ByteArray): AvifDecoder = AvifDecoder(bytes)
    }
}
