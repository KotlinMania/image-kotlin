// port-lint: tests metadata/cicp.rs
package io.github.kotlinmania.image.metadata

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CicpTest {
    @Test
    fun testDerivedLuminance() {
        val luminance = Cicp.SRGB.intoRgb().derivedLuminance()
        assertNotNull(luminance)
        val kr = luminance[0]
        val kg = luminance[1]
        val kb = luminance[2]
        assertTrue(abs(kr - 0.2126f) < 1e-4f)
        assertTrue(abs(kg - 0.7152f) < 1e-4f)
        assertTrue(abs(kb - 0.0722f) < 1e-4f)
    }

    @Test
    fun testQualifyStability() {
        assertTrue(Cicp.SRGB.qualifyStability())
        assertTrue(Cicp.SRGB_LINEAR.qualifyStability())
        assertTrue(Cicp.DISPLAY_P3.qualifyStability())
    }

    @Test
    fun testTryIntoRgb() {
        val srgb = Cicp.SRGB
        val res = srgb.tryIntoRgb()
        assertTrue(res.isSuccess)
        assertEquals(srgb, res.getOrThrow().toCicp())
    }

    @Test
    fun testCanCreateTransforms() {
        assertNotNull(CicpTransform.new(Cicp.SRGB, Cicp.SRGB))
        assertNotNull(CicpTransform.new(Cicp.SRGB, Cicp.DISPLAY_P3))
        assertNotNull(CicpTransform.new(Cicp.DISPLAY_P3, Cicp.SRGB))
        assertNotNull(CicpTransform.new(Cicp.DISPLAY_P3, Cicp.DISPLAY_P3))
    }
}
