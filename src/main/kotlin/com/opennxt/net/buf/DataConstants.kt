package com.opennxt.net.buf

/**
 * A class holding data-related constants.
 *
 * @author Graham
 */
object DataConstants {

    /**
     * An array of bit masks. The element `n` is equal to `2<sup>n</sup> - 1`.
     */
    val BIT_MASK = IntArray(32)

    /**
     * Initializes the [.BIT_MASK] array.
     */
    init {
        for (i in BIT_MASK.indices) {
            BIT_MASK[i] = (1 shl i) - 1
        }
    }

}