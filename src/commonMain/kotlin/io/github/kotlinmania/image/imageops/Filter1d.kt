// port-lint: source imageops/filter_1d.rs
package io.github.kotlinmania.image.imageops

import io.github.kotlinmania.image.ImageError
import io.github.kotlinmania.image.LimitError
import io.github.kotlinmania.image.LimitErrorKind
import kotlin.math.max

public interface SafeMul<S> {
    public fun safeMul(rhs: S): S
}

public interface SafeAdd<S> {
    public fun safeAdd(rhs: S): S
}

internal fun Int.safeMul(rhs: Int): Int {
    val res = this.toLong() * rhs.toLong()
    if (res > Int.MAX_VALUE || res < Int.MIN_VALUE) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }
    return res.toInt()
}

internal fun Int.safeAdd(rhs: Int): Int {
    val res = this.toLong() + rhs.toLong()
    if (res > Int.MAX_VALUE || res < Int.MIN_VALUE) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }
    return res.toInt()
}


public data class KernelShape(
    public val width: Int,
    public val height: Int,
)

public data class FilterImageSize(
    public val width: Int,
    public val height: Int,
)

public data class ArenaColumns<T>(
    public val topPad: T,
    public val bottomPad: T,
)

public interface ToStorage<S, T> {
    public fun to(input: S): T
    public fun to_(input: S): T = to(input)
}

public interface KernelTransformer<F, I> {
    public fun transform(input: F): I
}

internal class U32ToU8Storage : ToStorage<Int, Byte> {
    override fun to(input: Int): Byte {
        val res = ((input + (1 shl (Q0_15 - 1))) shr Q0_15).coerceIn(0, 255)
        return res.toByte()
    }
}

internal class U8KernelTransformer : KernelTransformer<Float, Int> {
    override fun transform(input: Float): Int {
        val scaled = (input * SCALE_U8).coerceIn(0f, ((1 shl Q0_15) - 1).toFloat())
        return scaled.toInt()
    }
}

internal fun makeColumnsArenas(
    image: ByteArray,
    imageSize: FilterImageSize,
    kernelSize: KernelShape,
    n: Int,
): ArenaColumns<ByteArray> {
    val totalRequired = imageSize.width.safeMul(imageSize.height).safeMul(n)
    if (image.size != totalRequired) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }
    val padH = kernelSize.height / 2
    val topPadStride = imageSize.width * n
    val topPad = ByteArray(padH * topPadStride)
    val bottomPad = ByteArray(padH * topPadStride)

    for (ky in 0 until padH) {
        val y = (ky - padH).coerceAtLeast(0).coerceAtMost(imageSize.height - 1)
        val srcOffset = y * topPadStride
        val dstOffset = ky * topPadStride
        image.copyInto(topPad, destinationOffset = dstOffset, startIndex = srcOffset, endIndex = srcOffset + topPadStride)
    }

    val lastRowOffset = (imageSize.height - 1) * topPadStride
    for (ky in 0 until padH) {
        val dstOffset = ky * topPadStride
        image.copyInto(bottomPad, destinationOffset = dstOffset, startIndex = lastRowOffset, endIndex = lastRowOffset + topPadStride)
    }

    return ArenaColumns(topPad, bottomPad)
}

internal fun makeColumnsArenasF32(
    image: FloatArray,
    imageSize: FilterImageSize,
    kernelSize: KernelShape,
    n: Int,
): ArenaColumns<FloatArray> {
    val totalRequired = imageSize.width.safeMul(imageSize.height).safeMul(n)
    if (image.size != totalRequired) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }
    val padH = kernelSize.height / 2
    val topPadStride = imageSize.width * n
    val topPad = FloatArray(padH * topPadStride)
    val bottomPad = FloatArray(padH * topPadStride)

    for (ky in 0 until padH) {
        val y = (ky - padH).coerceAtLeast(0).coerceAtMost(imageSize.height - 1)
        val srcOffset = y * topPadStride
        val dstOffset = ky * topPadStride
        image.copyInto(topPad, destinationOffset = dstOffset, startIndex = srcOffset, endIndex = srcOffset + topPadStride)
    }

    val lastRowOffset = (imageSize.height - 1) * topPadStride
    for (ky in 0 until padH) {
        val dstOffset = ky * topPadStride
        image.copyInto(bottomPad, destinationOffset = dstOffset, startIndex = lastRowOffset, endIndex = lastRowOffset + topPadStride)
    }

    return ArenaColumns(topPad, bottomPad)
}

internal fun makeColumnsArenasU16(
    image: ShortArray,
    imageSize: FilterImageSize,
    kernelSize: KernelShape,
    n: Int,
): ArenaColumns<ShortArray> {
    val totalRequired = imageSize.width.safeMul(imageSize.height).safeMul(n)
    if (image.size != totalRequired) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }
    val padH = kernelSize.height / 2
    val topPadStride = imageSize.width * n
    val topPad = ShortArray(padH * topPadStride)
    val bottomPad = ShortArray(padH * topPadStride)

    for (ky in 0 until padH) {
        val y = (ky - padH).coerceAtLeast(0).coerceAtMost(imageSize.height - 1)
        val srcOffset = y * topPadStride
        val dstOffset = ky * topPadStride
        image.copyInto(topPad, destinationOffset = dstOffset, startIndex = srcOffset, endIndex = srcOffset + topPadStride)
    }

    val lastRowOffset = (imageSize.height - 1) * topPadStride
    for (ky in 0 until padH) {
        val dstOffset = ky * topPadStride
        image.copyInto(bottomPad, destinationOffset = dstOffset, startIndex = lastRowOffset, endIndex = lastRowOffset + topPadStride)
    }

    return ArenaColumns(topPad, bottomPad)
}


private const val Q0_15: Int = 15
private const val SCALE_U8: Float = (1 shl Q0_15).toFloat()

internal fun transformKernelU8(kernel: FloatArray): IntArray {
    val out = IntArray(kernel.size)
    for (i in kernel.indices) {
        val scaled = (kernel[i] * SCALE_U8).coerceIn(0f, ((1 shl Q0_15) - 1).toFloat())
        out[i] = scaled.toInt()
    }
    return out
}

internal fun prepareSymmetricKernel(kernel: IntArray): IntArray {
    var start = 0
    var end = kernel.size
    while (end - start > 2 && kernel[start] == 0 && kernel[end - 1] == 0) {
        start++
        end--
    }
    return kernel.copyOfRange(start, end)
}

internal fun prepareSymmetricKernel(kernel: FloatArray): FloatArray {
    var start = 0
    var end = kernel.size
    while (end - start > 2 && kernel[start] == 0f && kernel[end - 1] == 0f) {
        start++
        end--
    }
    return kernel.copyOfRange(start, end)
}

internal fun makeArenaRow(
    image: ByteArray,
    rowBuffer: ByteArray,
    sourceY: Int,
    imageSize: FilterImageSize,
    kernelSize: KernelShape,
    n: Int,
) {
    val totalExpected = n * imageSize.width * imageSize.height
    if (image.size != totalExpected) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }
    val padW = max(kernelSize.width / 2, 1)
    val arenaWidth = imageSize.width.safeMul(n).safeAdd(padW.safeMul(2 * n))
    if (rowBuffer.size != arenaWidth) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }

    val sourceOffset = sourceY * imageSize.width * n
    val rowDstOffset = padW * n
    val rowWidthBytes = imageSize.width * n

    image.copyInto(
        rowBuffer,
        destinationOffset = rowDstOffset,
        startIndex = sourceOffset,
        endIndex = sourceOffset + rowWidthBytes,
    )

    for (x in 0 until padW) {
        val oldX = (x - padW).coerceAtLeast(0).coerceAtMost(imageSize.width - 1)
        val oldPx = sourceOffset + oldX * n
        val dstPx = x * n
        for (c in 0 until n) {
            rowBuffer[dstPx + c] = image[oldPx + c]
        }
    }

    val lastPixelOffset = sourceOffset + (imageSize.width - 1) * n
    for (x in 0 until padW) {
        val dstPx = rowDstOffset + rowWidthBytes + x * n
        for (c in 0 until n) {
            rowBuffer[dstPx + c] = image[lastPixelOffset + c]
        }
    }
}

internal fun makeArenaRowF32(
    image: FloatArray,
    rowBuffer: FloatArray,
    sourceY: Int,
    imageSize: FilterImageSize,
    kernelSize: KernelShape,
    n: Int,
) {
    val totalExpected = n * imageSize.width * imageSize.height
    if (image.size != totalExpected) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }
    val padW = max(kernelSize.width / 2, 1)
    val arenaWidth = imageSize.width.safeMul(n).safeAdd(padW.safeMul(2 * n))
    if (rowBuffer.size != arenaWidth) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }

    val sourceOffset = sourceY * imageSize.width * n
    val rowDstOffset = padW * n
    val rowWidthFloats = imageSize.width * n

    image.copyInto(
        rowBuffer,
        destinationOffset = rowDstOffset,
        startIndex = sourceOffset,
        endIndex = sourceOffset + rowWidthFloats,
    )

    for (x in 0 until padW) {
        val oldX = (x - padW).coerceAtLeast(0).coerceAtMost(imageSize.width - 1)
        val oldPx = sourceOffset + oldX * n
        val dstPx = x * n
        for (c in 0 until n) {
            rowBuffer[dstPx + c] = image[oldPx + c]
        }
    }

    val lastPixelOffset = sourceOffset + (imageSize.width - 1) * n
    for (x in 0 until padW) {
        val dstPx = rowDstOffset + rowWidthFloats + x * n
        for (c in 0 until n) {
            rowBuffer[dstPx + c] = image[lastPixelOffset + c]
        }
    }
}

internal fun makeArenaRowU16(
    image: ShortArray,
    rowBuffer: ShortArray,
    sourceY: Int,
    imageSize: FilterImageSize,
    kernelSize: KernelShape,
    n: Int,
) {
    val totalExpected = n * imageSize.width * imageSize.height
    if (image.size != totalExpected) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }
    val padW = max(kernelSize.width / 2, 1)
    val arenaWidth = imageSize.width.safeMul(n).safeAdd(padW.safeMul(2 * n))
    if (rowBuffer.size != arenaWidth) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }

    val sourceOffset = sourceY * imageSize.width * n
    val rowDstOffset = padW * n
    val rowWidthShorts = imageSize.width * n

    image.copyInto(
        rowBuffer,
        destinationOffset = rowDstOffset,
        startIndex = sourceOffset,
        endIndex = sourceOffset + rowWidthShorts,
    )

    for (x in 0 until padW) {
        val oldX = (x - padW).coerceAtLeast(0).coerceAtMost(imageSize.width - 1)
        val oldPx = sourceOffset + oldX * n
        val dstPx = x * n
        for (c in 0 until n) {
            rowBuffer[dstPx + c] = image[oldPx + c]
        }
    }

    val lastPixelOffset = sourceOffset + (imageSize.width - 1) * n
    for (x in 0 until padW) {
        val dstPx = rowDstOffset + rowWidthShorts + x * n
        for (c in 0 until n) {
            rowBuffer[dstPx + c] = image[lastPixelOffset + c]
        }
    }
}

internal fun filterSymmetricRow(
    arena: ByteArray,
    dstRow: ByteArray,
    dstOffset: Int,
    scannedKernel: IntArray,
    n: Int,
    width: Int,
) {
    val length = scannedKernel.size
    val halfLen = length / 2
    val hc = scannedKernel[halfLen]
    val totalElements = width * n

    for (x in 0 until totalElements) {
        val centerByte = arena[x + halfLen * n].toInt() and 0xFF
        var sum = centerByte * hc
        for (i in 0 until halfLen) {
            val otherSide = length - i - 1
            val fw = arena[x + i * n].toInt() and 0xFF
            val bw = arena[x + otherSide * n].toInt() and 0xFF
            sum += (fw + bw) * scannedKernel[i]
        }
        val outVal = ((sum + (1 shl 14)) shr 15).coerceIn(0, 255)
        dstRow[dstOffset + x] = outVal.toByte()
    }
}

internal fun filterSymmetricRowF32(
    arena: FloatArray,
    dstRow: FloatArray,
    dstOffset: Int,
    scannedKernel: FloatArray,
    n: Int,
    width: Int,
) {
    val length = scannedKernel.size
    val halfLen = length / 2
    val hc = scannedKernel[halfLen]
    val totalElements = width * n

    for (x in 0 until totalElements) {
        val centerVal = arena[x + halfLen * n]
        var sum = centerVal * hc
        for (i in 0 until halfLen) {
            val otherSide = length - i - 1
            val fw = arena[x + i * n]
            val bw = arena[x + otherSide * n]
            sum += (fw + bw) * scannedKernel[i]
        }
        dstRow[dstOffset + x] = sum
    }
}

internal fun filterSymmetricRowU16(
    arena: ShortArray,
    dstRow: ShortArray,
    dstOffset: Int,
    scannedKernel: IntArray,
    n: Int,
    width: Int,
) {
    val length = scannedKernel.size
    val halfLen = length / 2
    val hc = scannedKernel[halfLen]
    val totalElements = width * n

    for (x in 0 until totalElements) {
        val centerVal = arena[x + halfLen * n].toInt() and 0xFFFF
        var sum = centerVal.toLong() * hc.toLong()
        for (i in 0 until halfLen) {
            val otherSide = length - i - 1
            val fw = arena[x + i * n].toInt() and 0xFFFF
            val bw = arena[x + otherSide * n].toInt() and 0xFFFF
            sum += (fw + bw).toLong() * scannedKernel[i].toLong()
        }
        val outVal = (((sum + (1L shl 14)) shr 15).coerceIn(0L, 65535L)).toInt()
        dstRow[dstOffset + x] = outVal.toShort()
    }
}

internal fun filterSymmetricColumn(
    ringBuffer: ByteArray,
    ringOffsets: IntArray,
    dst: ByteArray,
    dstOffset: Int,
    imageSize: FilterImageSize,
    kernel: IntArray,
    n: Int,
) {
    val length = kernel.size
    val halfLen = length / 2
    val coeff = kernel[halfLen]
    val fullWidth = imageSize.width * n
    val centerOffset = ringOffsets[halfLen]

    for (cx in 0 until fullWidth) {
        val centerVal = ringBuffer[centerOffset + cx].toInt() and 0xFF
        var sum = centerVal * coeff
        for (i in 0 until halfLen) {
            val otherSide = length - i - 1
            val fwOffset = ringOffsets[i]
            val bwOffset = ringOffsets[otherSide]
            val fw = ringBuffer[fwOffset + cx].toInt() and 0xFF
            val bw = ringBuffer[bwOffset + cx].toInt() and 0xFF
            sum += (fw + bw) * kernel[i]
        }
        val outVal = ((sum + (1 shl 14)) shr 15).coerceIn(0, 255)
        dst[dstOffset + cx] = outVal.toByte()
    }
}

internal fun filterSymmetricColumnF32(
    ringBuffer: FloatArray,
    ringOffsets: IntArray,
    dst: FloatArray,
    dstOffset: Int,
    imageSize: FilterImageSize,
    kernel: FloatArray,
    n: Int,
) {
    val length = kernel.size
    val halfLen = length / 2
    val coeff = kernel[halfLen]
    val fullWidth = imageSize.width * n
    val centerOffset = ringOffsets[halfLen]

    for (cx in 0 until fullWidth) {
        val centerVal = ringBuffer[centerOffset + cx]
        var sum = centerVal * coeff
        for (i in 0 until halfLen) {
            val otherSide = length - i - 1
            val fwOffset = ringOffsets[i]
            val bwOffset = ringOffsets[otherSide]
            val fw = ringBuffer[fwOffset + cx]
            val bw = ringBuffer[bwOffset + cx]
            sum += (fw + bw) * kernel[i]
        }
        dst[dstOffset + cx] = sum
    }
}

internal fun filterSymmetricColumnU16(
    ringBuffer: ShortArray,
    ringOffsets: IntArray,
    dst: ShortArray,
    dstOffset: Int,
    imageSize: FilterImageSize,
    kernel: IntArray,
    n: Int,
) {
    val length = kernel.size
    val halfLen = length / 2
    val coeff = kernel[halfLen]
    val fullWidth = imageSize.width * n
    val centerOffset = ringOffsets[halfLen]

    for (cx in 0 until fullWidth) {
        val centerVal = ringBuffer[centerOffset + cx].toInt() and 0xFFFF
        var sum = centerVal.toLong() * coeff.toLong()
        for (i in 0 until halfLen) {
            val otherSide = length - i - 1
            val fwOffset = ringOffsets[i]
            val bwOffset = ringOffsets[otherSide]
            val fw = ringBuffer[fwOffset + cx].toInt() and 0xFFFF
            val bw = ringBuffer[bwOffset + cx].toInt() and 0xFFFF
            sum += (fw + bw).toLong() * kernel[i].toLong()
        }
        val outVal = (((sum + (1L shl 14)) shr 15).coerceIn(0L, 65535L)).toInt()
        dst[dstOffset + cx] = outVal.toShort()
    }
}

internal fun filter2dSeparableRingQueue(
    image: ByteArray,
    destination: ByteArray,
    imageSize: FilterImageSize,
    rowKernel: IntArray,
    columnKernel: IntArray,
    n: Int,
) {
    val padW = max(rowKernel.size / 2, 1)
    val arenaWidth = imageSize.width.safeMul(n).safeAdd(padW.safeMul(2 * n))
    val rowBuffer = ByteArray(arenaWidth)
    val fullWidth = imageSize.width * n
    val columnKernelLen = columnKernel.size
    val halfKernel = columnKernelLen / 2

    val buffer = ByteArray(fullWidth.safeMul(columnKernelLen))

    makeArenaRow(
        image,
        rowBuffer,
        0,
        imageSize,
        KernelShape(rowKernel.size, 0),
        n,
    )

    filterSymmetricRow(rowBuffer, buffer, 0, rowKernel, n, imageSize.width)

    for (k in 1..halfKernel) {
        buffer.copyInto(
            buffer,
            destinationOffset = k * fullWidth,
            startIndex = 0,
            endIndex = fullWidth,
        )
    }

    var startKy = (columnKernelLen / 2 + 1) % columnKernelLen
    val ringOffsets = IntArray(columnKernelLen)

    for (y in 1 until (imageSize.height + halfKernel)) {
        val newY = if (y < imageSize.height) y else (imageSize.height - 1)

        makeArenaRow(
            image,
            rowBuffer,
            newY,
            imageSize,
            KernelShape(rowKernel.size, 0),
            n,
        )

        filterSymmetricRow(
            rowBuffer,
            buffer,
            startKy * fullWidth,
            rowKernel,
            n,
            imageSize.width,
        )

        if (y >= halfKernel) {
            for (i in 0 until columnKernelLen) {
                val ky = (i + startKy + 1) % columnKernelLen
                ringOffsets[i] = ky * fullWidth
            }

            val dy = y - halfKernel
            val dstOffset = dy * fullWidth

            filterSymmetricColumn(
                buffer,
                ringOffsets,
                destination,
                dstOffset,
                imageSize,
                columnKernel,
                n,
            )
        }

        startKy = (startKy + 1) % columnKernelLen
    }
}

internal fun filter2dSeparableRingQueueF32(
    image: FloatArray,
    destination: FloatArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
    n: Int,
) {
    val padW = max(rowKernel.size / 2, 1)
    val arenaWidth = imageSize.width.safeMul(n).safeAdd(padW.safeMul(2 * n))
    val rowBuffer = FloatArray(arenaWidth)
    val fullWidth = imageSize.width * n
    val columnKernelLen = columnKernel.size
    val halfKernel = columnKernelLen / 2

    val buffer = FloatArray(fullWidth.safeMul(columnKernelLen))

    makeArenaRowF32(
        image,
        rowBuffer,
        0,
        imageSize,
        KernelShape(rowKernel.size, 0),
        n,
    )

    filterSymmetricRowF32(rowBuffer, buffer, 0, rowKernel, n, imageSize.width)

    for (k in 1..halfKernel) {
        buffer.copyInto(
            buffer,
            destinationOffset = k * fullWidth,
            startIndex = 0,
            endIndex = fullWidth,
        )
    }

    var startKy = (columnKernelLen / 2 + 1) % columnKernelLen
    val ringOffsets = IntArray(columnKernelLen)

    for (y in 1 until (imageSize.height + halfKernel)) {
        val newY = if (y < imageSize.height) y else (imageSize.height - 1)

        makeArenaRowF32(
            image,
            rowBuffer,
            newY,
            imageSize,
            KernelShape(rowKernel.size, 0),
            n,
        )

        filterSymmetricRowF32(
            rowBuffer,
            buffer,
            startKy * fullWidth,
            rowKernel,
            n,
            imageSize.width,
        )

        if (y >= halfKernel) {
            for (i in 0 until columnKernelLen) {
                val ky = (i + startKy + 1) % columnKernelLen
                ringOffsets[i] = ky * fullWidth
            }

            val dy = y - halfKernel
            val dstOffset = dy * fullWidth

            filterSymmetricColumnF32(
                buffer,
                ringOffsets,
                destination,
                dstOffset,
                imageSize,
                columnKernel,
                n,
            )
        }

        startKy = (startKy + 1) % columnKernelLen
    }
}

internal fun filter2dSeparableRingQueueU16(
    image: ShortArray,
    destination: ShortArray,
    imageSize: FilterImageSize,
    rowKernel: IntArray,
    columnKernel: IntArray,
    n: Int,
) {
    val padW = max(rowKernel.size / 2, 1)
    val arenaWidth = imageSize.width.safeMul(n).safeAdd(padW.safeMul(2 * n))
    val rowBuffer = ShortArray(arenaWidth)
    val fullWidth = imageSize.width * n
    val columnKernelLen = columnKernel.size
    val halfKernel = columnKernelLen / 2

    val buffer = ShortArray(fullWidth.safeMul(columnKernelLen))

    makeArenaRowU16(
        image,
        rowBuffer,
        0,
        imageSize,
        KernelShape(rowKernel.size, 0),
        n,
    )

    filterSymmetricRowU16(rowBuffer, buffer, 0, rowKernel, n, imageSize.width)

    for (k in 1..halfKernel) {
        buffer.copyInto(
            buffer,
            destinationOffset = k * fullWidth,
            startIndex = 0,
            endIndex = fullWidth,
        )
    }

    var startKy = (columnKernelLen / 2 + 1) % columnKernelLen
    val ringOffsets = IntArray(columnKernelLen)

    for (y in 1 until (imageSize.height + halfKernel)) {
        val newY = if (y < imageSize.height) y else (imageSize.height - 1)

        makeArenaRowU16(
            image,
            rowBuffer,
            newY,
            imageSize,
            KernelShape(rowKernel.size, 0),
            n,
        )

        filterSymmetricRowU16(
            rowBuffer,
            buffer,
            startKy * fullWidth,
            rowKernel,
            n,
            imageSize.width,
        )

        if (y >= halfKernel) {
            for (i in 0 until columnKernelLen) {
                val ky = (i + startKy + 1) % columnKernelLen
                ringOffsets[i] = ky * fullWidth
            }

            val dy = y - halfKernel
            val dstOffset = dy * fullWidth

            filterSymmetricColumnU16(
                buffer,
                ringOffsets,
                destination,
                dstOffset,
                imageSize,
                columnKernel,
                n,
            )
        }

        startKy = (startKy + 1) % columnKernelLen
    }
}

public fun filter2dSeparable(
    image: ByteArray,
    destination: ByteArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
    n: Int,
) {
    val totalRequired = imageSize.width.safeMul(imageSize.height).safeMul(n)
    if (image.size != totalRequired || destination.size != totalRequired) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }
    require(rowKernel.size % 2 != 0) { "Row kernel length must be odd" }
    require(columnKernel.size % 2 != 0) { "Column kernel length must be odd" }

    var scannedRowKernel = transformKernelU8(rowKernel)
    var scannedColumnKernel = transformKernelU8(columnKernel)

    scannedRowKernel = prepareSymmetricKernel(scannedRowKernel)
    scannedColumnKernel = prepareSymmetricKernel(scannedColumnKernel)

    if (scannedRowKernel.size <= 1 && scannedColumnKernel.size <= 1) {
        image.copyInto(destination)
        return
    }

    filter2dSeparableRingQueue(image, destination, imageSize, scannedRowKernel, scannedColumnKernel, n)
}

public fun filter2dSeparableF32(
    image: FloatArray,
    destination: FloatArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
    n: Int,
) {
    val totalRequired = imageSize.width.safeMul(imageSize.height).safeMul(n)
    if (image.size != totalRequired || destination.size != totalRequired) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }
    require(rowKernel.size % 2 != 0) { "Row kernel length must be odd" }
    require(columnKernel.size % 2 != 0) { "Column kernel length must be odd" }

    var scannedRowKernel = prepareSymmetricKernel(rowKernel)
    var scannedColumnKernel = prepareSymmetricKernel(columnKernel)

    if (scannedRowKernel.size <= 1 && scannedColumnKernel.size <= 1) {
        image.copyInto(destination)
        return
    }

    filter2dSeparableRingQueueF32(image, destination, imageSize, scannedRowKernel, scannedColumnKernel, n)
}

public fun filter2dSeparableU16(
    image: ShortArray,
    destination: ShortArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
    n: Int,
) {
    val totalRequired = imageSize.width.safeMul(imageSize.height).safeMul(n)
    if (image.size != totalRequired || destination.size != totalRequired) {
        throw ImageError.Limits(LimitError(LimitErrorKind.DimensionError))
    }
    require(rowKernel.size % 2 != 0) { "Row kernel length must be odd" }
    require(columnKernel.size % 2 != 0) { "Column kernel length must be odd" }

    var scannedRowKernel = transformKernelU8(rowKernel)
    var scannedColumnKernel = transformKernelU8(columnKernel)

    scannedRowKernel = prepareSymmetricKernel(scannedRowKernel)
    scannedColumnKernel = prepareSymmetricKernel(scannedColumnKernel)

    if (scannedRowKernel.size <= 1 && scannedColumnKernel.size <= 1) {
        image.copyInto(destination)
        return
    }

    filter2dSeparableRingQueueU16(image, destination, imageSize, scannedRowKernel, scannedColumnKernel, n)
}

public fun filter2dSepPlane(
    image: ByteArray,
    destination: ByteArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
) {
    filter2dSeparable(image, destination, imageSize, rowKernel, columnKernel, 1)
}

public fun filter2dSepLa(
    image: ByteArray,
    destination: ByteArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
) {
    filter2dSeparable(image, destination, imageSize, rowKernel, columnKernel, 2)
}

public fun filter2dSepRgb(
    image: ByteArray,
    destination: ByteArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
) {
    filter2dSeparable(image, destination, imageSize, rowKernel, columnKernel, 3)
}

public fun filter2dSepRgba(
    image: ByteArray,
    destination: ByteArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
) {
    filter2dSeparable(image, destination, imageSize, rowKernel, columnKernel, 4)
}

public fun filter2dSepPlaneF32(
    image: FloatArray,
    destination: FloatArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
) {
    filter2dSeparableF32(image, destination, imageSize, rowKernel, columnKernel, 1)
}

public fun filter2dSepLaF32(
    image: FloatArray,
    destination: FloatArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
) {
    filter2dSeparableF32(image, destination, imageSize, rowKernel, columnKernel, 2)
}

public fun filter2dSepRgbF32(
    image: FloatArray,
    destination: FloatArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
) {
    filter2dSeparableF32(image, destination, imageSize, rowKernel, columnKernel, 3)
}

public fun filter2dSepRgbaF32(
    image: FloatArray,
    destination: FloatArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
) {
    filter2dSeparableF32(image, destination, imageSize, rowKernel, columnKernel, 4)
}

public fun filter2dSepPlaneU16(
    image: ShortArray,
    destination: ShortArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
) {
    filter2dSeparableU16(image, destination, imageSize, rowKernel, columnKernel, 1)
}

public fun filter2dSepLaU16(
    image: ShortArray,
    destination: ShortArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
) {
    filter2dSeparableU16(image, destination, imageSize, rowKernel, columnKernel, 2)
}

public fun filter2dSepRgbU16(
    image: ShortArray,
    destination: ShortArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
) {
    filter2dSeparableU16(image, destination, imageSize, rowKernel, columnKernel, 3)
}

public fun filter2dSepRgbaU16(
    image: ShortArray,
    destination: ShortArray,
    imageSize: FilterImageSize,
    rowKernel: FloatArray,
    columnKernel: FloatArray,
) {
    filter2dSeparableU16(image, destination, imageSize, rowKernel, columnKernel, 4)
}

/**
 * Applies horizontal 1D convolution on byte array buffer.
 */
public fun filter1dHorizontal(
    src: ByteArray,
    dst: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    kernel: FloatArray,
) {
    val kRadius = kernel.size / 2
    for (y in 0 until height) {
        val rowOffset = y * width * channels
        for (x in 0 until width) {
            for (c in 0 until channels) {
                var sum = 0f
                for (k in kernel.indices) {
                    val kx = (x + k - kRadius).coerceIn(0, width - 1)
                    val px = (src[rowOffset + kx * channels + c].toInt() and 0xFF).toFloat()
                    sum += px * kernel[k]
                }
                dst[rowOffset + x * channels + c] = sum.coerceIn(0f, 255f).toInt().toByte()
            }
        }
    }
}

/**
 * Applies vertical 1D convolution on byte array buffer.
 */
public fun filter1dVertical(
    src: ByteArray,
    dst: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    kernel: FloatArray,
) {
    val kRadius = kernel.size / 2
    for (y in 0 until height) {
        for (x in 0 until width) {
            for (c in 0 until channels) {
                var sum = 0f
                for (k in kernel.indices) {
                    val ky = (y + k - kRadius).coerceIn(0, height - 1)
                    val px = (src[ky * width * channels + x * channels + c].toInt() and 0xFF).toFloat()
                    sum += px * kernel[k]
                }
                dst[y * width * channels + x * channels + c] = sum.coerceIn(0f, 255f).toInt().toByte()
            }
        }
    }
}

