// port-lint: source image/src/io.rs
package io.github.kotlinmania.image.io

/**
 * Deprecated re-export of [ImageReader] as [Reader].
 */
@Deprecated(
    message = "this type has been moved and renamed to ImageReader",
    replaceWith = ReplaceWith("ImageReader", "io.github.kotlinmania.image.io.ImageReader"),
)
public typealias Reader = ImageReader

/**
 * Interface providing extension functions for byte-oriented reading.
 */
internal interface ReadExt {
    /**
     * Reads exactly [len] bytes into the provided byte list.
     */
    fun readExactVec(vec: MutableList<Byte>, len: Int)
}

/**
 * Reads exactly [len] bytes from this [IoRead] into [vec].
 */
internal fun IoRead.readExactVec(vec: MutableList<Byte>, len: Int) {
    val initialLen = vec.size
    val buf = ByteArray(len)
    try {
        readExact(buf)
        for (i in 0 until len) {
            vec.add(buf[i])
        }
    } catch (e: Exception) {
        while (vec.size > initialLen) {
            vec.removeAt(vec.size - 1)
        }
        throw e
    }
}
