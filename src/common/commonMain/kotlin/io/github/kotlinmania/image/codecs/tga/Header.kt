// port-lint: source codecs/tga/header.rs
package io.github.kotlinmania.image.codecs.tga

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.IoWrite
import io.github.kotlinmania.image.io.readU16Le
import io.github.kotlinmania.image.io.readU8
import io.github.kotlinmania.image.io.writeU16Le
import io.github.kotlinmania.image.io.writeU8

internal const val ALPHA_BIT_MASK: UByte = 0x0Fu
internal const val SCREEN_ORIGIN_BIT_MASK: UByte = 0x20u

internal enum class ImageType(
    val value: UByte,
) {
    NoImageData(0u),

    /** Uncompressed images. */
    RawColorMap(1u),
    RawTrueColor(2u),
    RawGrayScale(3u),

    /** Run length encoded images. */
    RunColorMap(9u),
    RunTrueColor(10u),
    RunGrayScale(11u),
    Unknown(255u),
    ;

    companion object {
        fun new(imgType: UByte): ImageType = fromValue(imgType)

        fun new(imgType: Int): ImageType = fromValue(imgType.toUByte())

        fun fromValue(imgType: UByte): ImageType =
            when (imgType.toUInt()) {
                0u -> NoImageData
                1u -> RawColorMap
                2u -> RawTrueColor
                3u -> RawGrayScale
                9u -> RunColorMap
                10u -> RunTrueColor
                11u -> RunGrayScale
                else -> Unknown
            }
    }

    /** Check if the image format uses colors as opposed to grayscale. */
    fun isColor(): Boolean =
        when (this) {
            RawColorMap, RawTrueColor, RunTrueColor, RunColorMap -> true
            else -> false
        }

    /** Does the image use a color map. */
    fun isColorMapped(): Boolean = this == RawColorMap || this == RunColorMap

    /** Is the image run length encoded. */
    fun isEncoded(): Boolean = this == RunColorMap || this == RunTrueColor || this == RunGrayScale
}

/**
 * Header used by TGA image files.
 */
internal data class Header(
    var idLength: UByte = 0u,
    var mapType: UByte = 0u,
    var imageType: UByte = 0u,
    var mapOrigin: UShort = 0u,
    var mapLength: UShort = 0u,
    var mapEntrySize: UByte = 0u,
    var xOrigin: UShort = 0u,
    var yOrigin: UShort = 0u,
    var imageWidth: UShort = 0u,
    var imageHeight: UShort = 0u,
    var pixelDepth: UByte = 0u,
    var imageDesc: UByte = 0u,
) {
    companion object {
        /**
         * Load the header with values from pixel information.
         */
        fun fromPixelInfo(
            colorType: ExtendedColorType,
            width: UShort,
            height: UShort,
            useRle: Boolean,
        ): Result<Header> {
            val header = Header()

            if (width > 0u && height > 0u) {
                val (numAlphaBits, otherChannelBits, imgType) =
                    when {
                        colorType == ExtendedColorType.Rgba8 && useRle -> Triple(8u, 24u, ImageType.RunTrueColor)
                        colorType == ExtendedColorType.Rgb8 && useRle -> Triple(0u, 24u, ImageType.RunTrueColor)
                        colorType == ExtendedColorType.La8 && useRle -> Triple(8u, 8u, ImageType.RunGrayScale)
                        colorType == ExtendedColorType.L8 && useRle -> Triple(0u, 8u, ImageType.RunGrayScale)
                        colorType == ExtendedColorType.Rgba8 && !useRle -> Triple(8u, 24u, ImageType.RawTrueColor)
                        colorType == ExtendedColorType.Rgb8 && !useRle -> Triple(0u, 24u, ImageType.RawTrueColor)
                        colorType == ExtendedColorType.La8 && !useRle -> Triple(8u, 8u, ImageType.RawGrayScale)
                        colorType == ExtendedColorType.L8 && !useRle -> Triple(0u, 8u, ImageType.RawGrayScale)
                        else -> return Result.failure(
                            ImageError.Unsupported(
                                UnsupportedError.fromFormatAndKind(
                                    ImageFormatHint.Exact(ImageFormat.Tga),
                                    UnsupportedErrorKind.Color(colorType),
                                ),
                            ),
                        )
                    }

                header.imageType = imgType.value
                header.imageWidth = width
                header.imageHeight = height
                header.pixelDepth = (numAlphaBits + otherChannelBits).toUByte()
                header.imageDesc = (numAlphaBits.toUByte() and ALPHA_BIT_MASK) or SCREEN_ORIGIN_BIT_MASK
            }

            return Result.success(header)
        }

        /**
         * Load the header with values from the reader.
         */
        fun fromReader(r: IoRead): Result<Header> =
            runCatching {
                Header(
                    idLength = r.readU8(),
                    mapType = r.readU8(),
                    imageType = r.readU8(),
                    mapOrigin = r.readU16Le(),
                    mapLength = r.readU16Le(),
                    mapEntrySize = r.readU8(),
                    xOrigin = r.readU16Le(),
                    yOrigin = r.readU16Le(),
                    imageWidth = r.readU16Le(),
                    imageHeight = r.readU16Le(),
                    pixelDepth = r.readU8(),
                    imageDesc = r.readU8(),
                )
            }
    }

    /**
     * Write out the header values.
     */
    fun writeTo(w: IoWrite): Result<Unit> =
        runCatching {
            w.writeU8(idLength)
            w.writeU8(mapType)
            w.writeU8(imageType)
            w.writeU16Le(mapOrigin)
            w.writeU16Le(mapLength)
            w.writeU8(mapEntrySize)
            w.writeU16Le(xOrigin)
            w.writeU16Le(yOrigin)
            w.writeU16Le(imageWidth)
            w.writeU16Le(imageHeight)
            w.writeU8(pixelDepth)
            w.writeU8(imageDesc)
        }
}
