// port-lint: tests codecs/pnm/autobreak.rs
package io.github.kotlinmania.image.codecs.pnm

import io.github.kotlinmania.image.io.BufferIoWrite
import io.github.kotlinmania.image.io.writeAll
import kotlin.test.Test
import kotlin.test.assertContentEquals

class AutoBreakTest {
    @Test
    fun testAlignedWrites() {
        val output = BufferIoWrite()
        AutoBreak(output, lineCapacity = 10).use { writer ->
            writer.writeAll("0123456789".encodeToByteArray())
            writer.writeAll("0123456789".encodeToByteArray())
        }
        assertContentEquals("0123456789\n0123456789".encodeToByteArray(), output.toByteArray())
    }

    @Test
    fun testGreaterWrites() {
        val output = BufferIoWrite()
        AutoBreak(output, lineCapacity = 10).use { writer ->
            writer.writeAll("012".encodeToByteArray())
            writer.writeAll("345".encodeToByteArray())
            writer.writeAll("0123456789".encodeToByteArray())
            writer.writeAll("012345678910".encodeToByteArray())
            writer.writeAll("_".encodeToByteArray())
        }
        assertContentEquals(
            "012345\n0123456789\n012345678910\n_".encodeToByteArray(),
            output.toByteArray(),
        )
    }
}
