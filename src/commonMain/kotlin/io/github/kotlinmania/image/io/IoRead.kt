package io.github.kotlinmania.image.io

/**
 * Minimal byte-oriented source modelling [`std::io::Read`][std-io-Read] in
 * Kotlin Multiplatform commonMain.
 */
internal interface IoRead {
    /**
     * Reads up to [count] bytes into [buffer], starting at [offset], and returns
     * the number of bytes read, or 0 on end-of-stream.
     */
    fun read(buffer: ByteArray, offset: Int = 0, count: Int = buffer.size - offset): Int
}

/**
 * Convenience reader backed by a ByteArray.
 */
internal class BufferIoRead(
    private val data: ByteArray,
    private var position: Int = 0,
) : IoRead {
    val remaining: Int get() = data.size - position

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
        require(offset >= 0 && count >= 0 && offset + count <= buffer.size) {
            "offset/count out of bounds: offset=$offset, count=$count, size=${buffer.size}"
        }
        if (count == 0) return 0
        val available = data.size - position
        if (available <= 0) return 0
        val toRead = minOf(count, available)
        data.copyInto(buffer, destinationOffset = offset, startIndex = position, endIndex = position + toRead)
        position += toRead
        return toRead
    }
}

internal fun IoRead.readExact(buffer: ByteArray, offset: Int = 0, count: Int = buffer.size - offset) {
    var readCount = 0
    while (readCount < count) {
        val n = read(buffer, offset + readCount, count - readCount)
        if (n == 0) {
            throw IoException(IoErrorKind.UnexpectedEof, "failed to fill whole buffer")
        }
        readCount += n
    }
}

internal fun IoRead.readU8(): UByte {
    val buf = ByteArray(1)
    readExact(buf)
    return buf[0].toUByte()
}

internal fun IoRead.readU16Le(): UShort {
    val buf = ByteArray(2)
    readExact(buf)
    val b0 = buf[0].toInt() and 0xFF
    val b1 = buf[1].toInt() and 0xFF
    return ((b1 shl 8) or b0).toUShort()
}

internal fun IoWrite.writeU8(value: UByte) {
    writeAll(byteArrayOf(value.toByte()))
}

internal fun IoWrite.writeU16Le(value: UShort) {
    val v = value.toInt()
    writeAll(byteArrayOf((v and 0xFF).toByte(), ((v ushr 8) and 0xFF).toByte()))
}
