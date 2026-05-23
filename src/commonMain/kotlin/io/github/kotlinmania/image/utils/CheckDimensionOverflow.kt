// port-lint: source src/utils/mod.rs
package io.github.kotlinmania.image.utils

/** Checks if the provided dimensions would cause an overflow. */
internal fun checkDimensionOverflow(width: UInt, height: UInt, bytesPerPixel: UByte): Boolean =
    width.toULong() * height.toULong() > ULong.MAX_VALUE / bytesPerPixel.toULong()
