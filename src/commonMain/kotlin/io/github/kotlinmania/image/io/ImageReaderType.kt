// port-lint: source io/image_reader_type.rs
package io.github.kotlinmania.image.io

import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.ImageFormatHint
import io.github.kotlinmania.image.UnsupportedError
import io.github.kotlinmania.image.UnsupportedErrorKind
import io.github.kotlinmania.image.codecs.FarbfeldDecoder
import io.github.kotlinmania.image.codecs.QoiDecoder
import io.github.kotlinmania.image.codecs.tga.TgaDecoder

/**
 * A multi-format image reader.
 *
 * Facilitates automatic detection of an image's format, appropriate decoding
 * method, and dispatches into the set of supported [ImageDecoder] implementations.
 */
public class ImageReader(
    private val data: ByteArray,
    private var format: ImageFormat? = null,
    private var limits: Limits = Limits(),
) {
    public fun format(): ImageFormat? = format

    public fun setFormat(format: ImageFormat) {
        this.format = format
    }

    public fun clearFormat() {
        this.format = null
    }

    public fun setLimits(limits: Limits) {
        this.limits = limits
    }

    public fun withGuessedFormat(): ImageReader {
        if (format == null) {
            format = guessFormatImpl(data)
        }
        return this
    }

    public fun intoDecoder(): ImageDecoder {
        val fmt = format ?: guessFormatImpl(data) ?: throw ImageError.Unsupported(
            UnsupportedError(
                ImageFormatHint.Unknown,
                UnsupportedErrorKind.Format(ImageFormatHint.Unknown),
            ),
        )

        val decoder: ImageDecoder = when (fmt) {
            ImageFormat.Tga -> TgaDecoder(data)
            ImageFormat.Farbfeld -> FarbfeldDecoder(data)
            ImageFormat.Qoi -> QoiDecoder(data)
            else -> throw ImageError.Unsupported(
                UnsupportedError(
                    ImageFormatHint.Exact(fmt),
                    UnsupportedErrorKind.Format(ImageFormatHint.Name(fmt.name)),
                ),
            )
        }

        decoder.setLimits(limits)
        return decoder
    }

    public fun dimensions(): Pair<UInt, UInt> {
        val decoder = intoDecoder()
        return decoder.dimensions()
    }

    public fun decode(outputBuf: ByteArray) {
        val decoder = intoDecoder()
        decoder.readImage(outputBuf)
    }

    companion object {
        public fun fromBytes(bytes: ByteArray): ImageReader = ImageReader(bytes)

        public fun withFormat(bytes: ByteArray, format: ImageFormat): ImageReader =
            ImageReader(bytes, format = format)
    }
}
