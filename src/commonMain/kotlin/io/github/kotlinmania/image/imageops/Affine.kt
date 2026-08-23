// port-lint: source imageops/affine.rs
package io.github.kotlinmania.image.imageops

/**
 * Rotates an image 90 degrees clockwise and puts the result into the destination buffer.
 */
public fun rotate90In(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    destination: ByteArray,
    dstWidth: Int,
    dstHeight: Int,
) {
    require(width == dstHeight && height == dstWidth) { "Dimension mismatch" }
    require(image.size >= width * height * channels) { "Source buffer too small" }
    require(destination.size >= dstWidth * dstHeight * channels) { "Destination buffer too small" }
    for (y in 0 until height) {
        for (x in 0 until width) {
            val srcIdx = (y * width + x) * channels
            val dstX = height - y - 1
            val dstY = x
            val dstIdx = (dstY * height + dstX) * channels
            image.copyInto(destination, destinationOffset = dstIdx, startIndex = srcIdx, endIndex = srcIdx + channels)
        }
    }
}

/**
 * Rotates an image 90 degrees clockwise. Output has dimensions (height, width).
 */
public fun rotate90(image: ByteArray, width: Int, height: Int, channels: Int): ByteArray {
    val out = ByteArray(image.size)
    rotate90In(image, width, height, channels, out, height, width)
    return out
}

/**
 * Rotates an image 180 degrees clockwise and puts the result into the destination buffer.
 */
public fun rotate180In(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    destination: ByteArray,
    dstWidth: Int,
    dstHeight: Int,
) {
    require(width == dstWidth && height == dstHeight) { "Dimension mismatch" }
    require(image.size >= width * height * channels) { "Source buffer too small" }
    require(destination.size >= dstWidth * dstHeight * channels) { "Destination buffer too small" }
    for (y in 0 until height) {
        for (x in 0 until width) {
            val srcIdx = (y * width + x) * channels
            val dstX = width - x - 1
            val dstY = height - y - 1
            val dstIdx = (dstY * width + dstX) * channels
            image.copyInto(destination, destinationOffset = dstIdx, startIndex = srcIdx, endIndex = srcIdx + channels)
        }
    }
}

/**
 * Rotates an image 180 degrees clockwise.
 */
public fun rotate180(image: ByteArray, width: Int, height: Int, channels: Int): ByteArray {
    val out = ByteArray(image.size)
    rotate180In(image, width, height, channels, out, width, height)
    return out
}

/**
 * Rotates an image 270 degrees clockwise and puts the result into the destination buffer.
 */
public fun rotate270In(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    destination: ByteArray,
    dstWidth: Int,
    dstHeight: Int,
) {
    require(width == dstHeight && height == dstWidth) { "Dimension mismatch" }
    require(image.size >= width * height * channels) { "Source buffer too small" }
    require(destination.size >= dstWidth * dstHeight * channels) { "Destination buffer too small" }
    for (y in 0 until height) {
        for (x in 0 until width) {
            val srcIdx = (y * width + x) * channels
            val dstX = y
            val dstY = width - x - 1
            val dstIdx = (dstY * height + dstX) * channels
            image.copyInto(destination, destinationOffset = dstIdx, startIndex = srcIdx, endIndex = srcIdx + channels)
        }
    }
}

/**
 * Rotates an image 270 degrees clockwise (90 counter-clockwise). Output has dimensions (height, width).
 */
public fun rotate270(image: ByteArray, width: Int, height: Int, channels: Int): ByteArray {
    val out = ByteArray(image.size)
    rotate270In(image, width, height, channels, out, height, width)
    return out
}

/**
 * Flips an image horizontally and puts the result into the destination buffer.
 */
public fun flipHorizontalIn(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    destination: ByteArray,
    dstWidth: Int,
    dstHeight: Int,
) {
    require(width == dstWidth && height == dstHeight) { "Dimension mismatch" }
    require(image.size >= width * height * channels) { "Source buffer too small" }
    require(destination.size >= dstWidth * dstHeight * channels) { "Destination buffer too small" }
    for (y in 0 until height) {
        for (x in 0 until width) {
            val srcIdx = (y * width + x) * channels
            val dstX = width - x - 1
            val dstIdx = (y * width + dstX) * channels
            image.copyInto(destination, destinationOffset = dstIdx, startIndex = srcIdx, endIndex = srcIdx + channels)
        }
    }
}

/**
 * Flips an image horizontally.
 */
public fun flipHorizontal(image: ByteArray, width: Int, height: Int, channels: Int): ByteArray {
    val out = ByteArray(image.size)
    flipHorizontalIn(image, width, height, channels, out, width, height)
    return out
}

/**
 * Flips an image vertically and puts the result into the destination buffer.
 */
public fun flipVerticalIn(
    image: ByteArray,
    width: Int,
    height: Int,
    channels: Int,
    destination: ByteArray,
    dstWidth: Int,
    dstHeight: Int,
) {
    require(width == dstWidth && height == dstHeight) { "Dimension mismatch" }
    require(image.size >= width * height * channels) { "Source buffer too small" }
    require(destination.size >= dstWidth * dstHeight * channels) { "Destination buffer too small" }
    for (y in 0 until height) {
        for (x in 0 until width) {
            val srcIdx = (y * width + x) * channels
            val dstY = height - 1 - y
            val dstIdx = (dstY * width + x) * channels
            image.copyInto(destination, destinationOffset = dstIdx, startIndex = srcIdx, endIndex = srcIdx + channels)
        }
    }
}

/**
 * Flips an image vertically.
 */
public fun flipVertical(image: ByteArray, width: Int, height: Int, channels: Int): ByteArray {
    val out = ByteArray(image.size)
    flipVerticalIn(image, width, height, channels, out, width, height)
    return out
}

/**
 * Rotates an image 180 degrees clockwise in place.
 */
public fun rotate180InPlace(image: ByteArray, width: Int, height: Int, channels: Int) {
    require(image.size >= width * height * channels) { "Buffer too small" }
    for (y in 0 until height / 2) {
        for (x in 0 until width) {
            val x2 = width - x - 1
            val y2 = height - y - 1
            val idx1 = (y * width + x) * channels
            val idx2 = (y2 * width + x2) * channels
            for (c in 0 until channels) {
                val tmp = image[idx1 + c]
                image[idx1 + c] = image[idx2 + c]
                image[idx2 + c] = tmp
            }
        }
    }
    if (height % 2 != 0) {
        val middle = height / 2
        for (x in 0 until width / 2) {
            val x2 = width - x - 1
            val idx1 = (middle * width + x) * channels
            val idx2 = (middle * width + x2) * channels
            for (c in 0 until channels) {
                val tmp = image[idx1 + c]
                image[idx1 + c] = image[idx2 + c]
                image[idx2 + c] = tmp
            }
        }
    }
}

/**
 * Flips an image horizontally in place.
 */
public fun flipHorizontalInPlace(image: ByteArray, width: Int, height: Int, channels: Int) {
    require(image.size >= width * height * channels) { "Buffer too small" }
    for (y in 0 until height) {
        for (x in 0 until width / 2) {
            val x2 = width - x - 1
            val idx1 = (y * width + x) * channels
            val idx2 = (y * width + x2) * channels
            for (c in 0 until channels) {
                val tmp = image[idx1 + c]
                image[idx1 + c] = image[idx2 + c]
                image[idx2 + c] = tmp
            }
        }
    }
}

/**
 * Flips an image vertically in place.
 */
public fun flipVerticalInPlace(image: ByteArray, width: Int, height: Int, channels: Int) {
    require(image.size >= width * height * channels) { "Buffer too small" }
    for (y in 0 until height / 2) {
        val y2 = height - y - 1
        for (x in 0 until width) {
            val idx1 = (y * width + x) * channels
            val idx2 = (y2 * width + x) * channels
            for (c in 0 until channels) {
                val tmp = image[idx1 + c]
                image[idx1 + c] = image[idx2 + c]
                image[idx2 + c] = tmp
            }
        }
    }
}
