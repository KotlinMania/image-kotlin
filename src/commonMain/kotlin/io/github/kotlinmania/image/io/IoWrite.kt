package io.github.kotlinmania.image.io

/**
 * Minimal byte-oriented sink modelling [`std::io::Write`][std-io-Write] in
 * Kotlin Multiplatform commonMain. Crate-internal until the broader IO
 * layer arrives — public consumers of `image-kotlin` should not depend on
 * this surface.
 *
 * [std-io-Write]: https://doc.rust-lang.org/std/io/trait.Write.html
 */
internal interface IoWrite {
    /**
     * Writes up to [count] bytes from [buffer], starting at [offset], to
     * this sink and returns the number of bytes actually consumed.
     *
     * Implementations may return fewer bytes than requested. A return
     * value of `0` indicates the sink is unable to accept further writes
     * for reasons unrelated to the input (a full buffer, a closed pipe,
     * etc.) and callers should treat it as a terminal short write rather
     * than retry indefinitely.
     */
    fun write(buffer: ByteArray, offset: Int = 0, count: Int = buffer.size - offset): Int

    /**
     * Forces any buffered bytes downstream. May be a no-op on sinks that
     * do not buffer.
     */
    fun flush()
}

/**
 * Failure raised by [IoWrite] implementations and by intermediate writers
 * built on top of them. Mirrors `std::io::Error` for the slice of cases
 * the `image` crate actually surfaces — a kind discriminator plus an
 * optional cause and human-readable message.
 *
 * Crate-internal: this type extends [RuntimeException] for ergonomic
 * throw-and-catch within commonMain, which means it must not appear on a
 * public API. The Swift Export bridge gate forbids public `Throwable`
 * subclasses; keep this `internal`.
 */
internal class IoException(
    val kind: IoErrorKind,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Discriminator for the [IoException] cases that the `image` crate
 * routes on. The variants mirror the matching `std::io::ErrorKind`
 * entries, restricted to the ones actually consumed by ported
 * encoders/decoders.
 */
internal enum class IoErrorKind {
    /** A write returned zero bytes when bytes were requested. */
    WriteZero,

    /** End-of-stream reached before the requested byte count. */
    UnexpectedEof,

    /** Any other failure surfaced by the underlying sink. */
    Other,
}

/**
 * Convenience writer backed by an in-memory [MutableList] of bytes —
 * useful for tests that need to inspect the byte stream produced by an
 * [IoWrite] consumer.
 */
internal class BufferIoWrite : IoWrite {
    private val storage: MutableList<Byte> = ArrayList()

    /** Snapshot of bytes written so far. */
    fun toByteArray(): ByteArray = storage.toByteArray()

    override fun write(buffer: ByteArray, offset: Int, count: Int): Int {
        require(offset >= 0 && count >= 0 && offset + count <= buffer.size) {
            "offset/count out of bounds: offset=$offset, count=$count, size=${buffer.size}"
        }
        for (index in 0 until count) {
            storage.add(buffer[offset + index])
        }
        return count
    }

    override fun flush() {
        // Nothing to flush; the backing list is the buffer.
    }
}

/**
 * Writes every byte of [buffer] to this sink, looping on short writes,
 * and throws [IoException] of kind [IoErrorKind.WriteZero] if the sink
 * cannot accept further bytes. Mirrors the standard library's
 * `Write::write_all` convenience.
 */
internal fun IoWrite.writeAll(buffer: ByteArray, offset: Int = 0, count: Int = buffer.size - offset) {
    var written = 0
    while (written < count) {
        val n = write(buffer, offset + written, count - written)
        if (n == 0) {
            throw IoException(IoErrorKind.WriteZero, "failed to write whole buffer")
        }
        written += n
    }
}
