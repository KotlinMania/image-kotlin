// port-lint: source io/limits.rs
package io.github.kotlinmania.image.io

import io.github.kotlinmania.image.ColorType
import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.LimitError
import io.github.kotlinmania.image.LimitErrorKind

/**
 * Set of supported strict limits for a decoder.
 */
public data class LimitSupport(
    public val maxImageWidth: Boolean = false,
    public val maxImageHeight: Boolean = false,
    public val maxAlloc: Boolean = false,
)

/**
 * Resource limits for decoding.
 */
public data class Limits(
    /** The maximum allowed image width. */
    public var maxImageWidth: UInt? = null,
    /** The maximum allowed image height. */
    public var maxImageHeight: UInt? = null,
    /** The maximum allowed sum of allocations allocated by the decoder. Default is 512MiB. */
    public var maxAlloc: ULong? = 512uL * 1024uL * 1024uL,
) {
    public companion object {
        public fun default(): Limits = Limits()

        public fun noLimits(): Limits =
            Limits(
                maxImageWidth = null,
                maxImageHeight = null,
                maxAlloc = null,
            )
    }

    /**
     * Checks that all currently set strict limits are supported.
     */
    public fun checkSupport(supported: LimitSupport): Result<Unit> {
        if (maxImageWidth != null && !supported.maxImageWidth) {
            return Result.failure(ImageError.Limits(LimitError(LimitErrorKind.Unsupported(this, supported))))
        }
        if (maxImageHeight != null && !supported.maxImageHeight) {
            return Result.failure(ImageError.Limits(LimitError(LimitErrorKind.Unsupported(this, supported))))
        }
        return Result.success(Unit)
    }

    /**
     * Checks the `maxImageWidth` and `maxImageHeight` limits given the image width and height.
     */
    public fun checkDimensions(width: UInt, height: UInt): Result<Unit> {
        val maxW = maxImageWidth
        if (maxW != null && width > maxW) {
            return Result.failure(ImageError.Limits(LimitError.fromKind(LimitErrorKind.DimensionError)))
        }
        val maxH = maxImageHeight
        if (maxH != null && height > maxH) {
            return Result.failure(ImageError.Limits(LimitError.fromKind(LimitErrorKind.DimensionError)))
        }
        return Result.success(Unit)
    }

    /**
     * Checks that the current limit allows reserving the set amount of bytes,
     * then reduces the limit accordingly.
     */
    public fun reserve(amount: ULong): Result<Unit> {
        val current = maxAlloc
        if (current != null) {
            if (current < amount) {
                return Result.failure(ImageError.Limits(LimitError.fromKind(LimitErrorKind.InsufficientMemory)))
            }
            maxAlloc = current - amount
        }
        return Result.success(Unit)
    }

    /**
     * Acts identically to [reserve], but accepts an [Int] for convenience.
     */
    public fun reserveUsize(amount: Int): Result<Unit> =
        if (amount < 0 && maxAlloc != null) {
            Result.failure(ImageError.Limits(LimitError.fromKind(LimitErrorKind.InsufficientMemory)))
        } else if (amount < 0) {
            Result.success(Unit)
        } else {
            reserve(amount.toULong())
        }

    /**
     * Acts identically to [reserve], but accepts a [Long] for convenience.
     */
    public fun reserveUsize(amount: Long): Result<Unit> =
        if (amount < 0 && maxAlloc != null) {
            Result.failure(ImageError.Limits(LimitError.fromKind(LimitErrorKind.InsufficientMemory)))
        } else if (amount < 0) {
            Result.success(Unit)
        } else {
            reserve(amount.toULong())
        }

    public fun reserveBuffer(width: UInt, height: UInt, colorType: ColorType): Result<Unit> {
        val dimCheck = checkDimensions(width, height)
        if (dimCheck.isFailure) return dimCheck

        val inMemorySize = width.toULong() * height.toULong() * colorType.bytesPerPixel().toULong()
        return reserve(inMemorySize)
    }

    /**
     * Increases the `maxAlloc` limit with amount.
     */
    public fun free(amount: ULong) {
        val current = maxAlloc
        if (current != null) {
            val next = current + amount
            maxAlloc = if (next < current) ULong.MAX_VALUE else next
        }
    }

    /**
     * Acts identically to [free], but accepts an [Int] for convenience.
     */
    public fun freeUsize(amount: Int) {
        if (amount > 0) {
            free(amount.toULong())
        }
    }

    /**
     * Acts identically to [free], but accepts a [Long] for convenience.
     */
    public fun freeUsize(amount: Long) {
        if (amount > 0) {
            free(amount.toULong())
        }
    }
}
