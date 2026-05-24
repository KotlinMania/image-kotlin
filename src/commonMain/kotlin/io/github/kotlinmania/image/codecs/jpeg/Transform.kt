// port-lint: source src/codecs/jpeg/transform.rs
package io.github.kotlinmania.image.codecs.jpeg

/*
fdct is a Kotlin translation of jfdctint.c from the
Independent JPEG Group's libjpeg version 9a
obtained from http://www.ijg.org/files/jpegsr9a.zip
It comes with the following conditions of distribution and use:

    In plain English:

    1. We don't promise that this software works.  (But if you find any bugs,
        please let us know!)
    2. You can use this software for whatever you want.  You don't have to pay us.
    3. You may not pretend that you wrote this software.  If you use it in a
       program, you must acknowledge somewhere in your documentation that
       you've used the IJG code.

    In legalese:

    The authors make NO WARRANTY or representation, either express or implied,
    with respect to this software, its quality, accuracy, merchantability, or
    fitness for a particular purpose.  This software is provided "AS IS", and you,
    its user, assume the entire risk as to its quality and accuracy.

    This software is copyright (C) 1991-2014, Thomas G. Lane, Guido Vollbeding.
    All Rights Reserved except as specified below.

    Permission is hereby granted to use, copy, modify, and distribute this
    software (or portions thereof) for any purpose, without fee, subject to these
    conditions:
    (1) If any part of the source code for this software is distributed, then this
    README file must be included, with this copyright and no-warranty notice
    unaltered; and any additions, deletions, or changes to the original files
    must be clearly indicated in accompanying documentation.
    (2) If only executable code is distributed, then the accompanying
    documentation must state that "this software is based in part on the work of
    the Independent JPEG Group".
    (3) Permission for use of this software is granted only if the user accepts
    full responsibility for any undesirable consequences; the authors accept
    NO LIABILITY for damages of any kind.

    These conditions apply to any software derived from or based on the IJG code,
    not just to the unmodified library.  If you use our work, you ought to
    acknowledge us.

    Permission is NOT granted for the use of any IJG author's name or company name
    in advertising or publicity relating to this software or products derived from
    it.  This software may be referred to only as "the Independent JPEG Group's
    software".

    We specifically permit and encourage the use of this software as the basis of
    commercial products, provided that all warranty or liability claims are
    assumed by the product vendor.
*/

private const val CONST_BITS: Int = 13
private const val PASS1_BITS: Int = 2

private const val FIX_0_298631336: Int = 2446
private const val FIX_0_390180644: Int = 3196
private const val FIX_0_541196100: Int = 4433
private const val FIX_0_765366865: Int = 6270
private const val FIX_0_899976223: Int = 7373
private const val FIX_1_175875602: Int = 9633
private const val FIX_1_501321110: Int = 12_299
private const val FIX_1_847759065: Int = 15_137
private const val FIX_1_961570560: Int = 16_069
private const val FIX_2_053119869: Int = 16_819
private const val FIX_2_562915447: Int = 20_995
private const val FIX_3_072711026: Int = 25_172

internal fun fdct(samples: UByteArray, coeffs: IntArray) {
    require(samples.size == 64) { "samples must have length 64" }
    require(coeffs.size == 64) { "coeffs must have length 64" }

    // Pass 1: process rows.
    // Results are scaled by sqrt(8) compared to a true DCT
    // furthermore we scale the results by 2**PASS1_BITS
    for (y in 0 until 8) {
        val y0 = y * 8

        // Even part
        val sum0 = samples[y0].toInt() + samples[y0 + 7].toInt()
        val sum1 = samples[y0 + 1].toInt() + samples[y0 + 6].toInt()
        val sum2 = samples[y0 + 2].toInt() + samples[y0 + 5].toInt()
        val sum3 = samples[y0 + 3].toInt() + samples[y0 + 4].toInt()

        val t10 = sum0 + sum3
        val t12Even = sum0 - sum3
        val t11 = sum1 + sum2
        val t13Even = sum1 - sum2

        val diff0 = samples[y0].toInt() - samples[y0 + 7].toInt()
        val diff1 = samples[y0 + 1].toInt() - samples[y0 + 6].toInt()
        val diff2 = samples[y0 + 2].toInt() - samples[y0 + 5].toInt()
        val diff3 = samples[y0 + 3].toInt() - samples[y0 + 4].toInt()

        // Apply unsigned -> signed conversion
        coeffs[y0] = (t10 + t11 - 8 * 128) shl PASS1_BITS
        coeffs[y0 + 4] = (t10 - t11) shl PASS1_BITS

        var z1Even = (t12Even + t13Even) * FIX_0_541196100
        // Add fudge factor here for final descale
        z1Even += 1 shl (CONST_BITS - PASS1_BITS - 1)

        coeffs[y0 + 2] = (z1Even + t12Even * FIX_0_765366865) shr (CONST_BITS - PASS1_BITS)
        coeffs[y0 + 6] = (z1Even - t13Even * FIX_1_847759065) shr (CONST_BITS - PASS1_BITS)

        // Odd part
        val t12Odd = diff0 + diff2
        val t13Odd = diff1 + diff3

        var z1Odd = (t12Odd + t13Odd) * FIX_1_175875602
        // Add fudge factor here for final descale
        z1Odd += 1 shl (CONST_BITS - PASS1_BITS - 1)

        var t12Acc = t12Odd * (-FIX_0_390180644)
        var t13Acc = t13Odd * (-FIX_1_961570560)
        t12Acc += z1Odd
        t13Acc += z1Odd

        val z1Diff03 = (diff0 + diff3) * (-FIX_0_899976223)
        var t0 = diff0 * FIX_1_501321110
        var t3 = diff3 * FIX_0_298631336
        t0 += z1Diff03 + t12Acc
        t3 += z1Diff03 + t13Acc

        val z1Diff12 = (diff1 + diff2) * (-FIX_2_562915447)
        var t1 = diff1 * FIX_3_072711026
        var t2 = diff2 * FIX_2_053119869
        t1 += z1Diff12 + t13Acc
        t2 += z1Diff12 + t12Acc

        coeffs[y0 + 1] = t0 shr (CONST_BITS - PASS1_BITS)
        coeffs[y0 + 3] = t1 shr (CONST_BITS - PASS1_BITS)
        coeffs[y0 + 5] = t2 shr (CONST_BITS - PASS1_BITS)
        coeffs[y0 + 7] = t3 shr (CONST_BITS - PASS1_BITS)
    }

    // Pass 2: process columns
    // We remove the PASS1_BITS scaling but leave the results scaled up an
    // overall factor of 8
    for (x in 7 downTo 0) {
        // Even part
        val sum0 = coeffs[x] + coeffs[x + 8 * 7]
        val sum1 = coeffs[x + 8] + coeffs[x + 8 * 6]
        val sum2 = coeffs[x + 8 * 2] + coeffs[x + 8 * 5]
        val sum3 = coeffs[x + 8 * 3] + coeffs[x + 8 * 4]

        // Add fudge factor here for final descale
        val t10 = sum0 + sum3 + (1 shl (PASS1_BITS - 1))
        val t12Even = sum0 - sum3
        val t11 = sum1 + sum2
        val t13Even = sum1 - sum2

        val diff0 = coeffs[x] - coeffs[x + 8 * 7]
        val diff1 = coeffs[x + 8] - coeffs[x + 8 * 6]
        val diff2 = coeffs[x + 8 * 2] - coeffs[x + 8 * 5]
        val diff3 = coeffs[x + 8 * 3] - coeffs[x + 8 * 4]

        coeffs[x] = (t10 + t11) shr PASS1_BITS
        coeffs[x + 8 * 4] = (t10 - t11) shr PASS1_BITS

        var z1Even = (t12Even + t13Even) * FIX_0_541196100
        // Add fudge factor here for final descale
        z1Even += 1 shl (CONST_BITS + PASS1_BITS - 1)

        coeffs[x + 8 * 2] = (z1Even + t12Even * FIX_0_765366865) shr (CONST_BITS + PASS1_BITS)
        coeffs[x + 8 * 6] = (z1Even - t13Even * FIX_1_847759065) shr (CONST_BITS + PASS1_BITS)

        // Odd part
        val t12Odd = diff0 + diff2
        val t13Odd = diff1 + diff3

        var z1Odd = (t12Odd + t13Odd) * FIX_1_175875602
        // Add fudge factor here for final descale
        z1Odd += 1 shl (CONST_BITS - PASS1_BITS - 1)

        var t12Acc = t12Odd * (-FIX_0_390180644)
        var t13Acc = t13Odd * (-FIX_1_961570560)
        t12Acc += z1Odd
        t13Acc += z1Odd

        val z1Diff03 = (diff0 + diff3) * (-FIX_0_899976223)
        var t0 = diff0 * FIX_1_501321110
        var t3 = diff3 * FIX_0_298631336
        t0 += z1Diff03 + t12Acc
        t3 += z1Diff03 + t13Acc

        val z1Diff12 = (diff1 + diff2) * (-FIX_2_562915447)
        var t1 = diff1 * FIX_3_072711026
        var t2 = diff2 * FIX_2_053119869
        t1 += z1Diff12 + t13Acc
        t2 += z1Diff12 + t12Acc

        coeffs[x + 8] = t0 shr (CONST_BITS + PASS1_BITS)
        coeffs[x + 8 * 3] = t1 shr (CONST_BITS + PASS1_BITS)
        coeffs[x + 8 * 5] = t2 shr (CONST_BITS + PASS1_BITS)
        coeffs[x + 8 * 7] = t3 shr (CONST_BITS + PASS1_BITS)
    }
}
