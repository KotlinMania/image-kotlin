// port-lint: tests traits.rs
package io.github.kotlinmania.image

import kotlin.test.Test
import kotlin.test.assertEquals

class TraitsTest {
    @Test
    fun testPrimitiveDefaults() {
        assertEquals(255u.toUByte(), UBytePrimitive.defaultMaxValue)
        assertEquals(0u.toUByte(), UBytePrimitive.defaultMinValue)

        assertEquals(65535u.toUShort(), UShortPrimitive.defaultMaxValue)
        assertEquals(0u.toUShort(), UShortPrimitive.defaultMinValue)

        assertEquals(1.0f, FloatPrimitive.defaultMaxValue)
        assertEquals(0.0f, FloatPrimitive.defaultMinValue)
    }

    @Test
    fun testEnlargeable() {
        assertEquals(255u.toUByte(), UByteEnlargeable.clampFrom(300u))
        assertEquals(100u.toUByte(), UByteEnlargeable.clampFrom(100u))
        assertEquals(100u, UByteEnlargeable.toLarger(100u.toUByte()))

        assertEquals(65535u.toUShort(), UShortEnlargeable.clampFrom(70000u))
        assertEquals(500u.toUShort(), UShortEnlargeable.clampFrom(500u))
    }

    @Test
    fun testLerp() {
        assertEquals(50u.toUByte(), UByteLerp.lerp(0u.toUByte(), 100u.toUByte(), 0.5f))
        assertEquals(100u.toUByte(), UByteLerp.lerp(0u.toUByte(), 100u.toUByte(), 1.0f))
        assertEquals(0u.toUByte(), UByteLerp.lerp(0u.toUByte(), 100u.toUByte(), 0.0f))
    }
}
