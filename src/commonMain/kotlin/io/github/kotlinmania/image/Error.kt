// port-lint: source error.rs
package io.github.kotlinmania.image

import io.github.kotlinmania.image.io.ImageFormat
import io.github.kotlinmania.image.io.LimitSupport
import io.github.kotlinmania.image.io.Limits
import io.github.kotlinmania.image.metadata.Cicp

/**
 * Result of an image decoding/encoding process.
 */
public typealias ImageResult<T> = Result<T>

/**
 * The generic error type for image operations.
 */
public sealed class ImageError(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    public data class Decoding(
        public val error: DecodingError,
    ) : ImageError(error.toString(), error.underlying)

    public data class Encoding(
        public val error: EncodingError,
    ) : ImageError(error.toString(), error.underlying)

    public data class Parameter(
        public val error: ParameterError,
    ) : ImageError(error.toString(), error.underlying)

    public data class Limits(
        public val error: LimitError,
    ) : ImageError(error.toString(), null)

    public data class Unsupported(
        public val error: UnsupportedError,
    ) : ImageError(error.toString(), null)

    public data class IoError(
        public val error: Throwable,
    ) : ImageError(error.message, error)

    public fun source(): Throwable? =
        when (this) {
            is Decoding -> error.source()
            is Encoding -> error.source()
            is Parameter -> error.source()
            is Limits -> error.source()
            is Unsupported -> error.source()
            is IoError -> error
        }

    public fun fmt(): String =
        when (this) {
            is Decoding -> error.fmt()
            is Encoding -> error.fmt()
            is Parameter -> error.fmt()
            is Limits -> error.fmt()
            is Unsupported -> error.fmt()
            is IoError -> error.message ?: error.toString()
        }

    override fun toString(): String = fmt()

    public companion object {
        public fun fromThrowable(t: Throwable): ImageError =
            when (t) {
                is ImageError -> t
                else -> IoError(t)
            }

        public fun from(t: Throwable): ImageError = fromThrowable(t)

        public fun assertSendSync() {
            // Thread safety validation marker
        }
    }
}

/**
 * The implementation for an operation was not provided.
 *
 * See the variant [ImageError.Unsupported] for more documentation.
 */
public data class UnsupportedError(
    public val format: ImageFormatHint,
    public val kind: UnsupportedErrorKind,
) {

    /**
     * Returns the image format associated with this error.
     */
    public fun formatHint(): ImageFormatHint = format

    public fun source(): Throwable? = null

    public fun fmt(): String =
        when (kind) {
            is UnsupportedErrorKind.Format ->
                when (kind.hint) {
                    ImageFormatHint.Unknown -> "The image format could not be determined"
                    is ImageFormatHint.PathExtension -> "The file extension ${kind.hint.ext} was not recognized as an image format"
                    else -> "The image format ${kind.hint} is not supported"
                }
            is UnsupportedErrorKind.Color -> "The encoder or decoder for $format does not support the color type `${kind.color}`"
            is UnsupportedErrorKind.ColorLayout -> "Converting with the texel memory layout ${kind.layout} is not supported"
            is UnsupportedErrorKind.ColorspaceCicp -> "The colorimetric interpretation of a CICP color space is not supported for `${kind.cicp}`"
            is UnsupportedErrorKind.GenericFeature ->
                when (format) {
                    ImageFormatHint.Unknown -> "The decoder does not support the format feature ${kind.message}"
                    else -> "The decoder for $format does not support the format features ${kind.message}"
                }
        }

    override fun toString(): String = fmt()

    public companion object {
        /**
         * Create an [UnsupportedError] for an image with details on the unsupported feature.
         *
         * If the operation was not connected to a particular image format then the hint may be
         * [ImageFormatHint.Unknown].
         */
        public fun fromFormatAndKind(format: ImageFormatHint, kind: UnsupportedErrorKind): UnsupportedError =
            UnsupportedError(format, kind)

        public fun fromFormatHint(hint: ImageFormatHint): UnsupportedError =
            UnsupportedError(hint, UnsupportedErrorKind.Format(hint))

        public fun from(hint: ImageFormatHint): UnsupportedError = fromFormatHint(hint)
    }
}

public sealed interface UnsupportedErrorKind {
    public data class Color(
        public val color: ExtendedColorType,
    ) : UnsupportedErrorKind

    public data class ColorLayout(
        public val layout: ExtendedColorType,
    ) : UnsupportedErrorKind

    public data class ColorspaceCicp(
        public val cicp: Cicp,
    ) : UnsupportedErrorKind

    public data class Format(
        public val hint: ImageFormatHint,
    ) : UnsupportedErrorKind

    public data class GenericFeature(
        public val message: String,
    ) : UnsupportedErrorKind
}

public data class DecodingError(
    public val format: ImageFormatHint,
    public val underlying: Throwable? = null,
) {
    public constructor(format: ImageFormatHint, message: String) : this(format, Exception(message))

    public fun formatHint(): ImageFormatHint = format

    public fun source(): Throwable? = underlying

    public fun fmt(): String =
        when (underlying) {
            null ->
                when (format) {
                    ImageFormatHint.Unknown -> "Format error"
                    else -> "Format error decoding $format"
                }
            else -> "Format error decoding $format: ${underlying.message ?: underlying.toString()}"
        }

    override fun toString(): String = fmt()

    public companion object {
        public fun new(format: ImageFormatHint, err: Throwable): DecodingError = DecodingError(format, err)

        public fun fromFormatHint(format: ImageFormatHint): DecodingError = DecodingError(format, null)
    }
}

public data class EncodingError(
    public val format: ImageFormatHint,
    public val underlying: Throwable? = null,
) {
    public constructor(format: ImageFormatHint, message: String) : this(format, Exception(message))

    public fun formatHint(): ImageFormatHint = format

    public fun source(): Throwable? = underlying

    public fun fmt(): String =
        when (underlying) {
            null -> "Format error encoding $format"
            else -> "Format error encoding $format:\n${underlying.message ?: underlying.toString()}"
        }

    override fun toString(): String = fmt()

    public companion object {
        public fun new(format: ImageFormatHint, err: Throwable): EncodingError = EncodingError(format, err)

        public fun fromFormatHint(format: ImageFormatHint): EncodingError = EncodingError(format, null)
    }
}

/**
 * An error was encountered in inputs arguments.
 *
 * This is used as an opaque representation for the [ImageError.Parameter] variant.
 */
public data class ParameterError(
    public val kind: ParameterErrorKind,
    public val underlying: Throwable? = null,
) {

    public fun source(): Throwable? = underlying

    public fun fmt(): String {
        val base =
            when (kind) {
                ParameterErrorKind.DimensionMismatch -> "The Image's dimensions are either too small or too large"
                ParameterErrorKind.FailedAlready -> "The end the image stream has been reached due to a previous error"
                is ParameterErrorKind.RgbCicpRequired -> "The CICP ${kind.cicp} can not be used for RGB images"
                is ParameterErrorKind.Generic -> "The parameter is malformed: ${kind.message}"
                ParameterErrorKind.NoMoreData -> "The end of the image has been reached"
                is ParameterErrorKind.CicpMismatch -> "The color space ${kind.found} does not match the expected ${kind.expected}"
            }
        return if (underlying != null) "$base\n${underlying.message ?: underlying.toString()}" else base
    }

    override fun toString(): String = fmt()

    public companion object {
        /** Construct a [ParameterError] directly from a corresponding kind. */
        public fun fromKind(kind: ParameterErrorKind): ParameterError = ParameterError(kind, null)

        public fun from(kind: ParameterErrorKind): ParameterError = ParameterError(kind, null)
    }
}

/** Details how a parameter is malformed. */
public sealed interface ParameterErrorKind {
    /** The dimensions passed are wrong. */
    public data object DimensionMismatch : ParameterErrorKind

    /** Repeated an operation for which error that could not be cloned was emitted already. */
    public data object FailedAlready : ParameterErrorKind

    /** The cicp is required to be RGB-like but had other matrix transforms or narrow range. */
    public data class RgbCicpRequired(
        public val cicp: Cicp,
    ) : ParameterErrorKind

    /** A string describing the parameter. */
    public data class Generic(
        public val message: String,
    ) : ParameterErrorKind

    /** The end of the image has been reached. */
    public data object NoMoreData : ParameterErrorKind

    /** An operation expected a concrete color space but another was found. */
    public data class CicpMismatch(
        public val expected: Cicp,
        public val found: Cicp,
    ) : ParameterErrorKind
}

/**
 * Completing the operation would have required more resources than allowed.
 *
 * This is used as an opaque representation for the [ImageError.Limits] variant.
 */
public data class LimitError(
    public val kind: LimitErrorKind,
) {

    public fun source(): Throwable? = null

    public fun fmt(): String =
        when (kind) {
            LimitErrorKind.InsufficientMemory -> "Memory limit exceeded"
            LimitErrorKind.DimensionError -> "Image size exceeds limit"
            is LimitErrorKind.Unsupported -> "The following strict limits are specified but not supported by the operation: ${kind.limits}"
        }

    override fun toString(): String = fmt()

    public companion object {
        /** Construct a generic [LimitError] directly from a corresponding kind. */
        public fun fromKind(kind: LimitErrorKind): LimitError = LimitError(kind)

        public fun from(kind: LimitErrorKind): LimitError = LimitError(kind)
    }
}

/** Indicates the limit that prevented an operation from completing. */
public sealed interface LimitErrorKind {
    /** The resulting image exceed dimension limits in either direction. */
    public data object DimensionError : LimitErrorKind

    /** The operation would have performed an allocation larger than allowed. */
    public data object InsufficientMemory : LimitErrorKind

    /** The specified strict limits are not supported for this operation. */
    public data class Unsupported(
        public val limits: Limits,
        public val supported: LimitSupport,
    ) : LimitErrorKind
}

public sealed interface ImageFormatHint {
    public fun fmt(): String =
        when (this) {
            is Exact -> "$format"
            is Name -> "`$name`"
            is PathExtension -> "`.${ext}`"
            Unknown -> "`Unknown`"
        }

    public data class Exact(
        public val format: ImageFormat,
    ) : ImageFormatHint

    public data class Name(
        public val name: String,
    ) : ImageFormatHint

    public data class PathExtension(
        public val ext: String,
    ) : ImageFormatHint

    public data object Unknown : ImageFormatHint

    public companion object {
        public fun fromFormat(format: ImageFormat): ImageFormatHint = Exact(format)

        public fun from(format: ImageFormat): ImageFormatHint = Exact(format)

        public fun from(path: String): ImageFormatHint {
            val lastDot = path.lastIndexOf('.')
            return if (lastDot != -1 && lastDot < path.length - 1) {
                PathExtension(path.substring(lastDot + 1))
            } else {
                Unknown
            }
        }
    }
}

public data class TryFromExtendedColorError(
    public val was: ExtendedColorType,
) : Exception("The pixel layout $was is not supported as a buffer ColorType") {
    public fun fmt(): String = "The pixel layout $was is not supported as a buffer ColorType"

    public fun fmt(format: Any?): String = fmt()

    public fun toImageError(): ImageError =
        ImageError.Unsupported(
            UnsupportedError.fromFormatAndKind(
                ImageFormatHint.Unknown,
                UnsupportedErrorKind.Color(was),
            ),
        )

    public companion object {
        public fun from(err: TryFromExtendedColorError): ImageError = err.toImageError()
    }
}

