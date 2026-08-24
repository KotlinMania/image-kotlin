// port-lint: source utils/mod.rs
package io.github.kotlinmania.image.utils

/**
 * Expands a buffer of packed pixels in place by walking the buffer from the
 * end and unpacking each source byte into channel sub-ranges via [func].
 *
 * [func] receives the unpacked pixel value, the backing buffer, and the
 * starting offset of the channel sub-range of length [channels]. Modelled on
 * the upstream `FnMut(u8, &mut [u8])` callback whose slice begins at that
 * offset and has length [channels].
 */
internal inline fun expandPacked(
    buf: ByteArray,
    channels: Int,
    bitDepth: Int,
    func: (pixel: UByte, buf: ByteArray, offset: Int) -> Unit,
) {
    val pixels = buf.size / channels * bitDepth
    val extra = pixels % 8
    val entries = pixels / 8 + if (extra == 0) 0 else 1
    val mask = (((1 shl bitDepth) - 1) and 0xFF)
    val shiftStepsPerByte = 8 / bitDepth
    val bufLen = buf.size

    var pairIndex = extra
    val totalPairs = entries * shiftStepsPerByte
    var jInv = channels
    while (pairIndex < totalPairs && jInv < bufLen) {
        val sourceIndex = entries - 1 - pairIndex / shiftStepsPerByte
        val shift = (pairIndex % shiftStepsPerByte) * bitDepth
        val j = bufLen - jInv
        val pixel = ((buf[sourceIndex].toInt() and 0xFF) and (mask shl shift)) ushr shift
        func(pixel.toUByte(), buf, j)
        pairIndex++
        jInv += channels
    }
}

/**
 * Expand a buffer of packed 1, 2, or 4 bits integers into u8's. Assumes that
 * every [rowSize] entries there are padding bits up to the next byte boundary.
 */
internal fun expandBits(bitDepth: Int, rowSize: UInt, buf: ByteArray): ByteArray {
    val mask = (1 shl bitDepth) - 1
    val scalingFactor = 255 / ((1 shl bitDepth) - 1)
    val bitWidth = rowSize * bitDepth.toUInt()
    val skip =
        if (bitWidth % 8u == 0u) {
            0u
        } else {
            (8u - bitWidth % 8u) / bitDepth.toUInt()
        }
    val rowLen = rowSize + skip
    val p = mutableListOf<Byte>()
    var i = 0
    for (v in buf) {
        val vInt = v.toInt() and 0xFF
        for (shiftInv in 1..(8 / bitDepth)) {
            val shift = 8 - bitDepth * shiftInv
            if ((i.toUInt() % rowLen) < rowSize) {
                val pixel = (vInt and (mask shl shift)) ushr shift
                p.add((pixel * scalingFactor).toByte())
            }
            i += 1
        }
    }
    return p.toByteArray()
}

/** Checks if the provided dimensions would cause an overflow. */
internal fun checkDimensionOverflow(width: UInt, height: UInt, bytesPerPixel: UByte): Boolean =
    width.toULong() * height.toULong() > ULong.MAX_VALUE / bytesPerPixel.toULong()

/**
 * Copies slice to byte array.
 */
internal fun vecCopyToU8(bytes: ByteArray): ByteArray = bytes.copyOf()

/**
 * Clamps a value [a] between [min] and [max].
 */
internal fun <N : Comparable<N>> clamp(a: N, min: N, max: N): N =
    when {
        a < min -> min
        a > max -> max
        else -> a
    }

/**
 * Allocate a [MutableList] with the requested initial [capacity].
 */
internal fun <T> vecTryWithCapacity(capacity: Int): MutableList<T> = ArrayList(capacity)
