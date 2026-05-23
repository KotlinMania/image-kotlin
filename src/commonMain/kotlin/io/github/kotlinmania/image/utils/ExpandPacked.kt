// port-lint: source src/utils/mod.rs
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

    // The upstream iterator is `(0..entries).rev().flat_map(|idx|
    // (0..8/bit_depth).map(|i| i*bit_depth).zip(repeat(idx))).skip(extra)`,
    // i.e. for each source index counting down from `entries - 1` to 0,
    // yield `shiftStepsPerByte` pairs of `(shift, idx)`, then drop the
    // first `extra` pairs. Pair this with `j_inv` stepping by `channels`
    // from `channels` up to (but not including) `buf_len`, and stop when
    // either iterator is exhausted.
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
