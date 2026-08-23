package io.github.kotlinmania.image.io

/**
 * Position for seeking within an I/O stream.
 */
public sealed interface SeekFrom {
    /**
     * Sets the offset to the provided number of bytes from the start.
     */
    public data class Start(
        val offset: Long,
    ) : SeekFrom

    /**
     * Sets the offset to the size of this object plus the specified number of bytes.
     */
    public data class End(
        val offset: Long,
    ) : SeekFrom

    /**
     * Sets the offset to the current position plus the specified number of bytes.
     */
    public data class Current(
        val offset: Long,
    ) : SeekFrom
}

/**
 * Minimal seek capability modelling `std::io::Seek` in Kotlin Multiplatform commonMain.
 */
public interface IoSeek {
    /**
     * Seeks to an offset, in bytes, in a stream.
     */
    fun seek(pos: SeekFrom): Long

    /**
     * Rewind to the beginning of a stream.
     */
    fun rewind() {
        seek(SeekFrom.Start(0L))
    }

    /**
     * Returns the current seek position from the start of the stream.
     */
    fun streamPosition(): Long = seek(SeekFrom.Current(0L))
}
