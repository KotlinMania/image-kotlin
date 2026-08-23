// port-lint: source io/encoder.rs
package io.github.kotlinmania.image.io

import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind

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
}
