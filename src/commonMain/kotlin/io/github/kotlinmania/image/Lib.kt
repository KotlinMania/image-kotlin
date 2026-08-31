// port-lint: source image/src/lib.rs
package io.github.kotlinmania.image

public typealias FlatSamples<Buffer> = io.github.kotlinmania.image.images.FlatSamples<Buffer>
public typealias DynamicImage = io.github.kotlinmania.image.images.DynamicImage
public typealias GenericImage<P> = io.github.kotlinmania.image.images.GenericImage<P>
public typealias GenericImageView<P> = io.github.kotlinmania.image.images.GenericImageView<P>
public typealias SubImage<I> = io.github.kotlinmania.image.images.SubImage<I>
public typealias ImageBuffer<P, Container> = io.github.kotlinmania.image.images.ImageBuffer<P, Container>
public typealias GrayImage = io.github.kotlinmania.image.images.GrayImage
public typealias GrayAlphaImage = io.github.kotlinmania.image.images.GrayAlphaImage
public typealias RgbImage = io.github.kotlinmania.image.images.RgbImage
public typealias RgbaImage = io.github.kotlinmania.image.images.RgbaImage
public typealias Rgb32FImage = io.github.kotlinmania.image.images.Rgb32FImage
public typealias Rgba32FImage = io.github.kotlinmania.image.images.Rgba32FImage

public typealias Limits = io.github.kotlinmania.image.io.Limits
public typealias LimitSupport = io.github.kotlinmania.image.io.LimitSupport
public typealias ImageFormat = io.github.kotlinmania.image.io.ImageFormat
public typealias ImageReader = io.github.kotlinmania.image.io.ImageReader
public typealias ImageDecoder = io.github.kotlinmania.image.io.ImageDecoder
public typealias ImageDecoderRect = io.github.kotlinmania.image.io.ImageDecoderRect
public typealias AnimationDecoder = io.github.kotlinmania.image.io.AnimationDecoder
public typealias ImageEncoder = io.github.kotlinmania.image.io.ImageEncoder

/**
 * Image library top level declarations.
 */
public object ImageMania {
    public const val VERSION: String = "0.25.10"
}
