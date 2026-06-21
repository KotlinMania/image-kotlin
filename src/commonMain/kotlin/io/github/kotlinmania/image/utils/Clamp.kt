// port-lint: source src/utils/mod.rs
package io.github.kotlinmania.image.utils

internal fun <N : Comparable<N>> clamp(a: N, min: N, max: N): N =
    when {
        a < min -> min
        a > max -> max
        else -> a
    }
