// port-lint: source codecs/avif/mod.rs
package io.github.kotlinmania.image.codecs.avif

/**
 * AVIF codec module containing color transformations, encoder, and decoder components.
 */
public enum class AvifColorSpace {
    /** sRGB colorspace */
    Srgb,
    /** BT.709 colorspace */
    Bt709,
}
