// port-lint: source images/flat.rs
package io.github.kotlinmania.image.images

import io.github.kotlinmania.image.ColorType

/**
 * A description of a sample buffer layout.
 */
public data class SampleLayout(
    public val channels: UByte,
    public val channelStride: Int,
    public val width: UInt,
    public val widthStride: Int,
    public val height: UInt,
    public val heightStride: Int,
) {
    public fun stridesCwh(): Triple<Int, Int, Int> =
        Triple(channelStride, widthStride, heightStride)

    public fun extentsCwh(): Triple<Int, UInt, UInt> =
        Triple(channels.toInt(), width, height)

    public fun totalSamples(): Long =
        channels.toLong() * width.toLong() * height.toLong()

    public fun sampleIndex(channel: Int, x: UInt, y: UInt): Long =
        (channel * channelStride + x.toLong() * widthStride + y.toLong() * heightStride)

    companion object {
        public fun rowMajorPacked(channels: UByte, width: UInt, height: UInt): SampleLayout {
            val ch = channels.toInt()
            val w = width.toInt()
            val heightStride = ch * w
            return SampleLayout(
                channels = channels,
                channelStride = 1,
                width = width,
                widthStride = ch,
                height = height,
                heightStride = heightStride,
            )
        }

        public fun columnMajorPacked(channels: UByte, width: UInt, height: UInt): SampleLayout {
            val ch = channels.toInt()
            val h = height.toInt()
            val widthStride = ch * h
            return SampleLayout(
                channels = channels,
                channelStride = 1,
                width = width,
                widthStride = widthStride,
                height = height,
                heightStride = ch,
            )
        }
    }
}

/**
 * A flat buffer over a multi-channel image.
 */
public data class FlatSamples(
    public val samples: ByteArray,
    public val layout: SampleLayout,
    public val colorHint: ColorType? = null,
) {
    public fun getSample(channel: Int, x: UInt, y: UInt): Byte {
        val idx = layout.sampleIndex(channel, x, y).toInt()
        return samples[idx]
    }

    public fun setSample(channel: Int, x: UInt, y: UInt, value: Byte) {
        val idx = layout.sampleIndex(channel, x, y).toInt()
        samples[idx] = value
    }
}
