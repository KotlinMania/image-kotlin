// port-lint: source src/utils/mod.rs
package io.github.kotlinmania.image.utils

/**
 * Expand a buffer of packed 1, 2, or 4 bits integers into u8's. Assumes that
 * every [rowSize] entries there are padding bits up to the next byte boundary.
 */
internal fun expandBits(bitDepth: Int, rowSize: UInt, buf: ByteArray): ByteArray {
    // Note: this conversion assumes that the scanlines begin on byte boundaries.
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
            // skip the pixels that can be neglected because scanlines should
            // start at byte boundaries
            if ((i.toUInt() % rowLen) < rowSize) {
                val pixel = (vInt and (mask shl shift)) ushr shift
                p.add((pixel * scalingFactor).toByte())
            }
            i += 1
        }
    }
    return p.toByteArray()
}
