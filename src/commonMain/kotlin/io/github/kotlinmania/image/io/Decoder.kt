// port-lint: source io/decoder.rs
package io.github.kotlinmania.image.io

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ExtendedColorType
import io.github.kotlinmania.image.metadata.Orientation

/**
 * The trait that all decoders implement.
 */
public interface ImageDecoder {
    /**
     * Returns a pair containing the width and height of the image.
     */
    public fun dimensions(): Pair<UInt, UInt>

    /**
     * Returns the color type of the image data produced by this decoder.
     */
    public fun colorType(): ColorType

    /**
     * Returns the color type of the image file before decoding.
     */
    public fun originalColorType(): ExtendedColorType = colorType().toExtendedColorType()

    /**
     * Returns the ICC color profile embedded in the image, or null if the image does not have one.
     */
    public fun iccProfile(): ByteArray? = null

    /**
     * Returns the raw Exif chunk, if it is present.
     */
    public fun exifMetadata(): ByteArray? = null

    /**
     * Returns the raw XMP chunk, if it is present.
     */
    public fun xmpMetadata(): ByteArray? = null

    /**
     * Returns the raw IPTC chunk, if it is present.
     */
    public fun iptcMetadata(): ByteArray? = null

    /**
     * Returns the orientation of the image.
     */
    public fun orientation(): Orientation =
        exifMetadata()?.let { Orientation.fromExifChunk(it) } ?: Orientation.NoTransforms

    /**
     * Returns the total number of bytes in the decoded image.
     */
    public fun totalBytes(): ULong {
        val (width, height) = dimensions()
        val totalPixels = width.toULong() * height.toULong()
        val bytesPerPixel = colorType().bytesPerPixel().toULong()
        return if (totalPixels != 0uL && ULong.MAX_VALUE / totalPixels < bytesPerPixel) {
            ULong.MAX_VALUE
        } else {
            totalPixels * bytesPerPixel
        }
    }

    /**
     * Returns all the bytes in the image into [buf].
     */
    public fun readImage(buf: ByteArray)

    /**
     * Set the decoder to have the specified limits.
     */
    public fun setLimits(limits: Limits) {
        limits.checkSupport(LimitSupport()).getOrThrow()
        val (width, height) = dimensions()
        limits.checkDimensions(width, height).getOrThrow()
    }
}

/**
 * Specialized image decoding supported by certain formats.
 */
public interface ImageDecoderRect : ImageDecoder {
    /**
     * Decode a rectangular section of the image into [buf].
     */
    public fun readRect(
        x: UInt,
        y: UInt,
        width: UInt,
        height: UInt,
        buf: ByteArray,
        rowPitch: Int,
    )
}

/**
 * Animation decoder producing a series of frames.
 */
public interface AnimationDecoder {
    /**
     * Consume the decoder producing a series of frames.
     */
    public fun intoFrames(): io.github.kotlinmania.image.Frames
}
