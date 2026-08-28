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
 *
 * This high level enum allows, by variant matching, a rough separation of concerns between
 * underlying IO, the caller, format specifications, and the `image` implementation.
 */
public sealed class ImageError(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * An error was encountered while decoding.
     *
     * This means that the input data did not conform to the specification of some image format,
     * or that no format could be determined, or that it did not match format specific
     * requirements set by the caller.
     */
    public data class Decoding(
        public val error: DecodingError,
    ) : ImageError(error.toString(), error.underlying)

    /**
     * An error was encountered while encoding.
     *
     * The input image can not be encoded with the chosen format, for example because the
     * specification has no representation for its color space or because a necessary conversion
     * is ambiguous. In some cases it might also happen that the dimensions can not be used with
     * the format.
     */
    public data class Encoding(
        public val error: EncodingError,
    ) : ImageError(error.toString(), error.underlying)

    /**
     * An error was encountered in input arguments.
     *
     * This is a catch-all case for strictly internal operations such as scaling, conversions,
     * etc. that involve no external format specifications.
     */
    public data class Parameter(
        public val error: ParameterError,
    ) : ImageError(error.toString(), error.underlying)

    /**
     * Completing the operation would have required more resources than allowed.
     *
     * Errors of this type are limits set by the user or environment, *not* inherent in a specific
     * format or operation that was executed.
     */
    public data class Limits(
        public val error: LimitError,
    ) : ImageError(error.toString(), null)

    /**
     * An operation can not be completed by the chosen abstraction.
     *
     * This means that it might be possible for the operation to succeed in general but
     * * it requires a disabled feature,
     * * the implementation does not yet exist, or
     * * no abstraction for a lower level could be found.
     */
    public data class Unsupported(
        public val error: UnsupportedError,
    ) : ImageError(error.toString(), null)

    /**
     * An error occurred while interacting with the environment.
     */
    public data class IoError(
        public val error: Throwable,
    ) : ImageError(error.message, error)

    /**
     * Returns the underlying source exception if available.
     */
    public fun source(): Throwable? =
        when (this) {
            is Decoding -> error.source()
            is Encoding -> error.source()
            is Parameter -> error.source()
            is Limits -> error.source()
            is Unsupported -> error.source()
            is IoError -> error
        }

    /**
     * Formats this error into a human-readable display string.
     */
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
        /**
         * Wraps a generic [Throwable] as an [ImageError].
         */
        public fun fromThrowable(t: Throwable): ImageError =
            when (t) {
                is ImageError -> t
                else -> IoError(t)
            }

        /**
         * Wraps a generic [Throwable] as an [ImageError].
         */
        public fun from(t: Throwable): ImageError = fromThrowable(t)

        /**
         * Thread safety validation assertion marker.
         */
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
     * Returns the corresponding [UnsupportedErrorKind] of the error.
     */
    public fun kind(): UnsupportedErrorKind = kind

    /**
     * Returns the image format associated with this error.
     */
    public fun formatHint(): ImageFormatHint = format

    /**
     * Returns the underlying source exception if available.
     */
    public fun source(): Throwable? = null

    /**
     * Formats the unsupported error into a human-readable description.
     */
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

        /**
         * Create an [UnsupportedError] for an unsupported image format hint.
         */
        public fun fromFormatHint(hint: ImageFormatHint): UnsupportedError =
            UnsupportedError(hint, UnsupportedErrorKind.Format(hint))

        /**
         * Create an [UnsupportedError] from an image format hint.
         */
        public fun from(hint: ImageFormatHint): UnsupportedError = fromFormatHint(hint)
    }
}

/**
 * Details what feature is not supported.
 */
public sealed interface UnsupportedErrorKind {
    /**
     * The required color type can not be handled.
     */
    public data class Color(
        public val color: ExtendedColorType,
    ) : UnsupportedErrorKind

    /**
     * Dealing with an intricate layout is not implemented for an algorithm.
     */
    public data class ColorLayout(
        public val layout: ExtendedColorType,
    ) : UnsupportedErrorKind

    /**
     * The colors or transfer function of the CICP are not supported.
     */
    public data class ColorspaceCicp(
        public val cicp: Cicp,
    ) : UnsupportedErrorKind

    /**
     * An image format is not supported.
     */
    public data class Format(
        public val hint: ImageFormatHint,
    ) : UnsupportedErrorKind

    /**
     * Some feature specified by string.
     * This is discouraged and is likely to get deprecated (but not removed).
     */
    public data class GenericFeature(
        public val message: String,
    ) : UnsupportedErrorKind
}

/**
 * An error was encountered while decoding an image.
 *
 * This is used as an opaque representation for the [ImageError.Decoding] variant. See its
 * documentation for more information.
 */
public data class DecodingError(
    public val format: ImageFormatHint,
    public val underlying: Throwable? = null,
) {
    public constructor(format: ImageFormatHint, message: String) : this(format, Exception(message))

    /**
     * Returns the image format associated with this error.
     */
    public fun formatHint(): ImageFormatHint = format

    /**
     * Returns the underlying source exception if available.
     */
    public fun source(): Throwable? = underlying

    /**
     * Formats the decoding error into a human-readable description.
     */
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
        /**
         * Create a [DecodingError] that stems from an arbitrary error of an underlying decoder.
         */
        public fun new(format: ImageFormatHint, err: Throwable): DecodingError = DecodingError(format, err)

        /**
         * Create a [DecodingError] for an image format.
         *
         * The error will not contain any further information but is very easy to create.
         */
        public fun fromFormatHint(format: ImageFormatHint): DecodingError = DecodingError(format, null)
    }
}

/**
 * An error was encountered while encoding an image.
 *
 * This is used as an opaque representation for the [ImageError.Encoding] variant. See its
 * documentation for more information.
 */
public data class EncodingError(
    public val format: ImageFormatHint,
    public val underlying: Throwable? = null,
) {
    public constructor(format: ImageFormatHint, message: String) : this(format, Exception(message))

    /**
     * Return the image format associated with this error.
     */
    public fun formatHint(): ImageFormatHint = format

    /**
     * Returns the underlying source exception if available.
     */
    public fun source(): Throwable? = underlying

    /**
     * Formats the encoding error into a human-readable description.
     */
    public fun fmt(): String =
        when (underlying) {
            null -> "Format error encoding $format"
            else -> "Format error encoding $format:\n${underlying.message ?: underlying.toString()}"
        }

    override fun toString(): String = fmt()

    public companion object {
        /**
         * Create an [EncodingError] that stems from an arbitrary error of an underlying encoder.
         */
        public fun new(format: ImageFormatHint, err: Throwable): EncodingError = EncodingError(format, err)

        /**
         * Create an [EncodingError] for an image format.
         *
         * The error will not contain any further information but is very easy to create.
         */
        public fun fromFormatHint(format: ImageFormatHint): EncodingError = EncodingError(format, null)
    }
}

/**
 * An error was encountered in inputs arguments.
 *
 * This is used as an opaque representation for the [ImageError.Parameter] variant. See its
 * documentation for more information.
 */
public data class ParameterError(
    public val kind: ParameterErrorKind,
    public val underlying: Throwable? = null,
) {
    /**
     * Returns the corresponding [ParameterErrorKind] of the error.
     */
     public fun kind(): ParameterErrorKind = kind

    /**
     * Returns the underlying source exception if available.
     */
    public fun source(): Throwable? = underlying

    /**
     * Formats the parameter error into a human-readable description.
     */
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

        /** Construct a [ParameterError] directly from a corresponding kind. */
        public fun from(kind: ParameterErrorKind): ParameterError = ParameterError(kind, null)
    }
}

/**
 * Details how a parameter is malformed.
 */
public sealed interface ParameterErrorKind {
    /** The dimensions passed are wrong. */
    public data object DimensionMismatch : ParameterErrorKind

    /** Repeated an operation for which error that could not be cloned was emitted already. */
    public data object FailedAlready : ParameterErrorKind

    /** The cicp is required to be RGB-like but had other matrix transforms or narrow range. */
    public data class RgbCicpRequired(
        public val cicp: Cicp,
    ) : ParameterErrorKind

    /**
     * A string describing the parameter.
     * This is discouraged and is likely to get deprecated (but not removed).
     */
    public data class Generic(
        public val message: String,
    ) : ParameterErrorKind

    /** The end of the image has been reached. */
    public data object NoMoreData : ParameterErrorKind

    /** An operation expected a concrete color space but another was found. */
    public data class CicpMismatch(
        /** The cicp that was expected. */
        public val expected: Cicp,
        /** The cicp that was found. */
        public val found: Cicp,
    ) : ParameterErrorKind
}

/**
 * Completing the operation would have required more resources than allowed.
 *
 * This is used as an opaque representation for the [ImageError.Limits] variant. See its
 * documentation for more information.
 */
public data class LimitError(
    public val kind: LimitErrorKind,
) {
    /**
     * Returns the corresponding [LimitErrorKind] of the error.
     */
    public fun kind(): LimitErrorKind = kind

    /**
     * Returns the underlying source exception if available.
     */
    public fun source(): Throwable? = null

    /**
     * Formats the limit error into a human-readable description.
     */
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

        /** Construct a generic [LimitError] directly from a corresponding kind. */
        public fun from(kind: LimitErrorKind): LimitError = LimitError(kind)
    }
}

/**
 * Indicates the limit that prevented an operation from completing.
 *
 * Note that this enumeration is not exhaustive and may in the future be extended to provide more
 * detailed information or to incorporate other resources types.
 */
public sealed interface LimitErrorKind {
    /** The resulting image exceed dimension limits in either direction. */
    public data object DimensionError : LimitErrorKind

    /** The operation would have performed an allocation larger than allowed. */
    public data object InsufficientMemory : LimitErrorKind

    /** The specified strict limits are not supported for this operation. */
    public data class Unsupported(
        /** The given limits. */
        public val limits: Limits,
        /** The supported strict limits. */
        public val supported: LimitSupport,
    ) : LimitErrorKind
}

/**
 * A best effort representation for image formats.
 */
public sealed interface ImageFormatHint {
    /**
     * Formats this format hint into a display string.
     */
    public fun fmt(): String =
        when (this) {
            is Exact -> "$format"
            is Name -> "`$name`"
            is PathExtension -> "`.$ext`"
            Unknown -> "`Unknown`"
        }

    /** The format is known exactly. */
    public data class Exact(
        public val format: ImageFormat,
    ) : ImageFormatHint

    /** The format can be identified by a name. */
    public data class Name(
        public val name: String,
    ) : ImageFormatHint

    /** A common path extension for the format is known. */
    public data class PathExtension(
        public val ext: String,
    ) : ImageFormatHint

    /** The format is not known or could not be determined. */
    public data object Unknown : ImageFormatHint

    public companion object {
        /** Create an [ImageFormatHint.Exact] from an [ImageFormat]. */
        public fun fromFormat(format: ImageFormat): ImageFormatHint = Exact(format)

        /** Create an [ImageFormatHint.Exact] from an [ImageFormat]. */
        public fun from(format: ImageFormat): ImageFormatHint = Exact(format)

        /** Create an [ImageFormatHint] from a file path by looking at its extension. */
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

/**
 * Converting [ExtendedColorType] to [ColorType] failed.
 *
 * This type is convertible to [ImageError] as [ImageError.Unsupported].
 */
public data class TryFromExtendedColorError(
    public val was: ExtendedColorType,
) : Exception("The pixel layout $was is not supported as a buffer ColorType") {
    public fun fmt(): String = "The pixel layout $was is not supported as a buffer ColorType"

    public fun fmt(format: Any?): String = fmt()

    /**
     * Converts this error into an [ImageError.Unsupported].
     */
    public fun toImageError(): ImageError =
        ImageError.Unsupported(
            UnsupportedError.fromFormatAndKind(
                ImageFormatHint.Unknown,
                UnsupportedErrorKind.Color(was),
            ),
        )

    public companion object {
        /** Converts a [TryFromExtendedColorError] into an [ImageError]. */
        public fun from(err: TryFromExtendedColorError): ImageError = err.toImageError()
    }
}
