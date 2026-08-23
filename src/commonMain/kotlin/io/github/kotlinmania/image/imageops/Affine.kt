// port-lint: source imageops/affine.rs
package io.github.kotlinmania.image.imageops

/**
 * Rotates an image 90 degrees clockwise. Output has dimensions (height, width).
 */
public fun rotate90(image: ByteArray, width: Int, height: Int, channels: Int): ByteArray {
    val out = ByteArray(image.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val srcIdx = (y * width + x) * channels
            val dstX = height - y - 1
            val dstY = x
            val dstIdx = (dstY * height + dstX) * channels
            image.copyInto(out, destinationOffset = dstIdx, startIndex = srcIdx, endIndex = srcIdx + channels)
        }
    }
    return out
}

/**
 * Rotates an image 180 degrees clockwise.
 */
public fun rotate180(image: ByteArray, width: Int, height: Int, channels: Int): ByteArray {
    val out = ByteArray(image.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val srcIdx = (y * width + x) * channels
            val dstX = width - x - 1
            val dstY = height - y - 1
            val dstIdx = (dstY * width + dstX) * channels
            image.copyInto(out, destinationOffset = dstIdx, startIndex = srcIdx, endIndex = srcIdx + channels)
        }
    }
    return out
}

/**
 * Rotates an image 270 degrees clockwise (90 counter-clockwise). Output has dimensions (height, width).
 */
public fun rotate270(image: ByteArray, width: Int, height: Int, channels: Int): ByteArray {
    val out = ByteArray(image.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val srcIdx = (y * width + x) * channels
            val dstX = y
            val dstY = width - x - 1
            val dstIdx = (dstY * height + dstX) * channels
            image.copyInto(out, destinationOffset = dstIdx, startIndex = srcIdx, endIndex = srcIdx + channels)
        }
    }
    return out
}

/**
 * Flips an image horizontally.
 */
public fun flipHorizontal(image: ByteArray, width: Int, height: Int, channels: Int): ByteArray {
    val out = ByteArray(image.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val srcIdx = (y * width + x) * channels
            val dstX = width - x - 1
            val dstIdx = (y * width + dstX) * channels
            image.copyInto(out, destinationOffset = dstIdx, startIndex = srcIdx, endIndex = srcIdx + channels)
        }
    }
    return out
}

/**
 * Flips an image vertically.
 */
public fun flipVertical(image: ByteArray, width: Int, height: Int, channels: Int): ByteArray {
    val out = ByteArray(image.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val srcIdx = (y * width + x) * channels
            val dstY = height - 1 - y
            val dstIdx = (dstY * width + x) * channels
            image.copyInto(out, destinationOffset = dstIdx, startIndex = srcIdx, endIndex = srcIdx + channels)
        }
    }
    return out
}
