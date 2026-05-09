// port-lint: source math/rect.rs
package io.github.kotlinmania.image.math

/**
 * A Rectangle defined by its top left corner, width and height.
 */
public data class Rect(
    /** The x coordinate of the top left corner. */
    public val x: UInt,
    /** The y coordinate of the top left corner. */
    public val y: UInt,
    /** The rectangle's width. */
    public val width: UInt,
    /** The rectangle's height. */
    public val height: UInt,
)
