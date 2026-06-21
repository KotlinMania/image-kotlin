// port-lint: source codecs/pnm/autobreak.rs
package io.github.kotlinmania.image.codecs.pnm

import io.github.kotlinmania.image.io.IoErrorKind
import io.github.kotlinmania.image.io.IoException
import io.github.kotlinmania.image.io.IoWrite

/**
 * Insert line breaks between written buffers when they would overflow the line length.
 *
 * The PNM standard says to insert line breaks after 70 characters. Assumes that no line breaks
 * are actually written. We have to be careful to fully commit buffers or not commit them at all,
 * otherwise we might insert a newline in the middle of a token.
 */
internal class AutoBreak(
    private val wrapped: IoWrite,
    private val lineCapacity: Int,
) : IoWrite,
    AutoCloseable {
    private val line: MutableList<Byte> = ArrayList(lineCapacity + 1)
    private var hasNewline: Boolean = false

    /**
     * Tracks whether the wrapped sink threw on the most recent write. If it did,
     * [close] skips its automatic flush — guarding against re-entering a
     * failing writer while the caller is already unwinding. Once set,
     * the consumer is expected to discard this instance.
     */
    private var panicked: Boolean = false

    /**
     * Drain the buffered line to the wrapped sink, looping over short
     * writes. A partial commit leaves the remaining bytes in [line]
     * rather than dropping them.
     */
    private fun flushBuf() {
        var written = 0
        val len = line.size
        var failure: IoException? = null
        while (written < len) {
            val remaining = ByteArray(len - written) { line[written + it] }
            panicked = true
            val written0: Int =
                try {
                    wrapped.write(remaining, 0, remaining.size)
                } catch (e: IoException) {
                    panicked = false
                    // Common-Kotlin sinks do not surface OS-signal-interrupted
                    // retries the way the upstream platform's IO layer can, so
                    // every IoException propagates straight to the caller.
                    failure = e
                    break
                }
            panicked = false
            if (written0 == 0) {
                failure = IoException(IoErrorKind.WriteZero, "failed to write the buffered data")
                break
            }
            written += written0
        }
        if (written > 0) {
            // Drop the bytes that did make it downstream from the front
            // of the line buffer.
            repeat(written) { line.removeAt(0) }
        }
        failure?.let { throw it }
    }

    override fun write(buffer: ByteArray, offset: Int, count: Int): Int {
        require(offset >= 0 && count >= 0 && offset + count <= buffer.size) {
            "offset/count out of bounds: offset=$offset, count=$count, size=${buffer.size}"
        }
        if (hasNewline) {
            flush()
            hasNewline = false
        }

        if (line.isNotEmpty() && line.size + count > lineCapacity) {
            line.add('\n'.code.toByte())
            hasNewline = true
            flush()
            hasNewline = false
        }

        for (index in 0 until count) {
            line.add(buffer[offset + index])
        }
        return count
    }

    override fun flush() {
        flushBuf()
        wrapped.flush()
    }

    /**
     * Best-effort flush the buffered line to the wrapped sink, but only
     * when the writer is in a clean state. Failures during this final
     * flush are intentionally swallowed — the upstream auto-drop hook
     * cannot return errors either, and surfacing them from [close] would
     * mask the original failure that brought the caller here.
     */
    override fun close() {
        if (!panicked) {
            runCatching { flushBuf() }
        }
    }
}
