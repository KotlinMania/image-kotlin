// port-lint: source io/encoder.rs
package io.github.kotlinmania.image.io

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.images.DynamicImage

/**
 * Nominally public marker for sealed encoder methods.
 */
public object MethodSealedToImage

/**
 * The trait all encoders implement.
 */
public interface ImageEncoder {
    /**
     * Writes all the bytes in an image to the encoder.
     */
    public fun writeImage(
        buf: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    )

    /**
     * Set the ICC profile to use for the image.
     */
    public fun setIccProfile(iccProfile: ByteArray): Unit = throw ImageError.Unsupported(
        UnsupportedError(
            ImageFormatHint.Unknown,
            UnsupportedErrorKind.GenericFeature("ICC profiles are not supported for this format"),
        ),
    )

    /**
     * Set the EXIF metadata to use for the image.
     */
    public fun setExifMetadata(exif: ByteArray): Unit = throw ImageError.Unsupported(
        UnsupportedError(
            ImageFormatHint.Unknown,
            UnsupportedErrorKind.GenericFeature("EXIF metadata is not supported for this format"),
        ),
    )

    /**
     * Convert the image to a compatible format for the encoder.
     */
    public fun makeCompatibleImg(
        sealed: MethodSealedToImage,
        input: DynamicImage,
    ): DynamicImage? = null
}

/**
 * Writes all the bytes in an image to the encoder with standard [ColorType].
 */
public fun ImageEncoder.writeImage(
    buf: ByteArray,
    width: UInt,
    height: UInt,
    colorType: ColorType,
) {
    writeImage(buf, width, height, ExtendedColorType.from(colorType))
}

/**
 * Boxed variant of the image encoder interface.
 */
public interface ImageEncoderBoxed : ImageEncoder {
    override fun writeImage(
        buf: ByteArray,
        width: UInt,
        height: UInt,
        colorType: ExtendedColorType,
    )
}

/**
 * Converts a dynamic image to an 8-bit representation if needed.
 */
internal fun dynimageConversion8bit(img: DynamicImage): DynamicImage? =
    when (img.color()) {
        ColorType.Rgb8, ColorType.Rgba8, ColorType.L8, ColorType.La8 -> null
        ColorType.L16 -> DynamicImage.ImageLuma8(img.toLuma8())
        ColorType.La16 -> DynamicImage.ImageLumaA8(img.toLumaAlpha8())
        ColorType.Rgb16, ColorType.Rgb32F -> DynamicImage.ImageRgb8(img.toRgb8())
        ColorType.Rgba16, ColorType.Rgba32F -> DynamicImage.ImageRgba8(img.toRgba8())
    }
