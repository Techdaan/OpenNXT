package com.opennxt.net.buf

/**
 * Represents the different simple data types.
 *
 * @author Graham
 */
enum class DataType(
    /**
     * The number of bytes this type occupies.
     */
    val bytes: Int
) {

    /**
     * A byte.
     */
    BYTE(1),

    /**
     * A short.
     */
    SHORT(2),

    /**
     * A medium - a group of three bytes.
     */
    MEDIUM(3),

    /**
     * An integer.
     */
    INT(4),

    /**
     * A long.
     */
    LONG(8)

}