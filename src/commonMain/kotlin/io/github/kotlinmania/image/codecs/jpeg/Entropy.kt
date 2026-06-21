// port-lint: source src/codecs/jpeg/entropy.rs
package io.github.kotlinmania.image.codecs.jpeg

/**
 * Given an array containing the number of codes of each code length,
 * this function generates the huffman codes lengths and their respective
 * code lengths as specified by the JPEG spec.
 */
private fun deriveCodesAndSizes(bits: UByteArray): Pair<UByteArray, UShortArray> {
    require(bits.size == 16) { "bits must have length 16" }
    val huffsize = UByteArray(256)
    val huffcode = UShortArray(256)

    var k = 0

    // Annex C.2
    // Figure C.1
    // Generate table of individual code lengths
    var i = 0
    while (i < 16) {
        var j: UByte = 0.toUByte()
        while (j < bits[i]) {
            huffsize[k] = (i + 1).toUByte()
            k += 1
            j = (j.toInt() + 1).toUByte()
        }
        i += 1
    }

    huffsize[k] = 0.toUByte()

    // Annex C.2
    // Figure C.2
    // Generate table of huffman codes
    k = 0
    var code: UShort = 0.toUShort()
    var size: UByte = huffsize[0]

    while (huffsize[k] != 0.toUByte()) {
        huffcode[k] = code
        code = (code.toInt() + 1).toUShort()
        k += 1

        if (huffsize[k] == size) {
            continue
        }

        // FIXME there is something wrong with this code
        val diff: UByte = (huffsize[k].toInt() - size.toInt()).toUByte()
        code =
            if (diff < 16.toUByte()) {
                (code.toInt() shl diff.toInt()).toUShort()
            } else {
                0.toUShort()
            }

        size = (size.toInt() + diff.toInt()).toUByte()
    }

    return huffsize to huffcode
}

internal fun buildHuffLutConst(bits: UByteArray, huffval: UByteArray): Array<Pair<UByte, UShort>> {
    require(bits.size == 16) { "bits must have length 16" }
    val lut = Array(256) { 17.toUByte() to 0.toUShort() }
    val (huffsize, huffcode) = deriveCodesAndSizes(bits)

    var i = 0
    while (i < huffval.size) {
        lut[huffval[i].toInt()] = huffsize[i] to huffcode[i]
        i += 1
    }

    return lut
}
