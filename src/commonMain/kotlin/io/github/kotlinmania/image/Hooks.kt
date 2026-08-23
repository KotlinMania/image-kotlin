// port-lint: source hooks.rs
package io.github.kotlinmania.image

import io.github.kotlinmania.image.io.ImageDecoder
import io.github.kotlinmania.image.io.IoErrorKind
import io.github.kotlinmania.image.io.IoException
import io.github.kotlinmania.image.io.IoRead
import io.github.kotlinmania.image.io.IoSeek
import io.github.kotlinmania.image.io.SeekFrom

/**
 * Trait combining [IoRead] and [IoSeek].
 */
public interface ReadSeek :
    IoRead,
    IoSeek

/**
 * A wrapper around a reader that implements [IoRead] and [IoSeek].
 */
public class GenericReader(
    public val reader: ReadSeek,
) : IoRead,
    IoSeek {
    override fun read(buffer: ByteArray, offset: Int, count: Int): Int =
        reader.read(buffer, offset, count)

    public fun readVectored(bufs: Array<ByteArray>): Int {
        var total = 0
        for (buf in bufs) {
            val n = read(buf)
            if (n == 0) break
            total += n
            if (n < buf.size) break
        }
        return total
    }

    public fun readToEnd(buf: MutableList<Byte>): Int {
        val temp = ByteArray(4096)
        var total = 0
        while (true) {
            val n = read(temp)
            if (n <= 0) break
            for (i in 0 until n) {
                buf.add(temp[i])
            }
            total += n
        }
        return total
    }

    public fun readToString(buf: StringBuilder): Int {
        val bytes = mutableListOf<Byte>()
        val n = readToEnd(bytes)
        buf.append(bytes.toByteArray().decodeToString())
        return n
    }

    public fun readExact(buf: ByteArray) {
        var readCount = 0
        while (readCount < buf.size) {
            val n = read(buf, readCount, buf.size - readCount)
            if (n == 0) {
                throw IoException(IoErrorKind.UnexpectedEof, "failed to fill whole buffer")
            }
            readCount += n
        }
    }

    public fun fillBuf(): ByteArray =
        ByteArray(0)

    public fun consume(amt: Int) {
        if (amt > 0) {
            seek(SeekFrom.Current(amt.toLong()))
        }
    }

    public fun readUntil(byte: Byte, buf: MutableList<Byte>): Int {
        val temp = ByteArray(1)
        var count = 0
        while (true) {
            val n = read(temp)
            if (n == 0) break
            buf.add(temp[0])
            count++
            if (temp[0] == byte) break
        }
        return count
    }

    public fun readLine(buf: StringBuilder): Int {
        val lineBytes = mutableListOf<Byte>()
        val n = readUntil('\n'.code.toByte(), lineBytes)
        if (n > 0) {
            buf.append(lineBytes.toByteArray().decodeToString())
        }
        return n
    }

    override fun seek(pos: SeekFrom): Long =
        reader.seek(pos)

    override fun rewind() {
        reader.rewind()
    }

    override fun streamPosition(): Long =
        reader.streamPosition()
}

/**
 * A function to produce an [ImageDecoder] for a given image format.
 */
public typealias DecodingHook = (GenericReader) -> ImageDecoder

/**
 * Hook representation for guessing an image format from its byte signature and optional mask.
 */
public data class DetectionHook(
    public val signature: ByteArray,
    public val mask: ByteArray,
    public val extension: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DetectionHook) return false
        return signature.contentEquals(other.signature) &&
            mask.contentEquals(other.mask) &&
            extension == other.extension
    }

    override fun hashCode(): Int {
        var result = signature.contentHashCode()
        result = 31 * result + mask.contentHashCode()
        result = 31 * result + extension.hashCode()
        return result
    }
}

private val decodingHooks: MutableMap<String, DecodingHook> = mutableMapOf()

private val guessFormatHooks: MutableList<DetectionHook> = mutableListOf()

/**
 * Register a new decoding hook or returns false if one already exists for the given format.
 */
public fun registerDecodingHook(extension: String, hook: DecodingHook): Boolean {
    if (decodingHooks.containsKey(extension)) {
        return false
    }
    decodingHooks[extension] = hook
    return true
}

/**
 * Returns whether a decoding hook has been registered for the given format.
 */
public fun decodingHookRegistered(extension: String): Boolean =
    decodingHooks.containsKey(extension)

/**
 * Registers a format detection hook.
 *
 * The [signature] holds the magic bytes from the start of the file that must be matched to
 * detect the format. The [mask] can specify which bytes in the signature should be ignored.
 */
public fun registerFormatDetectionHook(
    extension: String,
    signature: ByteArray,
    mask: ByteArray? = null,
) {
    val hook =
        DetectionHook(
            signature = signature,
            mask = mask ?: ByteArray(0),
            extension = extension,
        )
    guessFormatHooks.add(hook)
}

internal fun guessFormatWithHooks(data: ByteArray): String? {
    for (hook in guessFormatHooks) {
        if (data.size < hook.signature.size) continue
        var matched = true
        for (i in hook.signature.indices) {
            val maskByte = if (i < hook.mask.size) (hook.mask[i].toInt() and 0xFF) else 0xFF
            val dataByte = data[i].toInt() and 0xFF
            val sigByte = hook.signature[i].toInt() and 0xFF
            if ((dataByte and maskByte) != (sigByte and maskByte)) {
                matched = false
                break
            }
        }
        if (matched) return hook.extension
    }
    return null
}
