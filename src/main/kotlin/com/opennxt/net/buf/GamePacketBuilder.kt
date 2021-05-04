package com.opennxt.net.buf

import com.opennxt.ext.writeString
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

/**
 * A class which assists in creating a packet
 *
 * @author Graham
 */
class GamePacketBuilder(val buffer: ByteBuf = Unpooled.buffer()) {

    /**
     * The current bit index.
     */
    private var bitIndex: Int = 0

    /**
     * The current mode.
     */
    private var mode = AccessMode.BYTE_ACCESS

    /**
     * Gets the current length of the builder's buffer.
     *
     * @return The length of the buffer.
     */
    val length: Int
        get() {
            checkByteAccess()
            return buffer.writerIndex()
        }

    /**
     * Checks that this builder is in the bit access mode.
     *
     * @throws IllegalStateException If the builder is not in bit access mode.
     */
    private fun checkBitAccess() {
        if (mode !== AccessMode.BIT_ACCESS) {
            throw IllegalArgumentException("For bit-based calls to work, the mode must be bit access.")
        }
    }

    /**
     * Checks that this builder is in the byte access mode.
     *
     * @throws IllegalStateException If the builder is not in byte access mode.
     */
    private fun checkByteAccess() {
        if (mode !== AccessMode.BYTE_ACCESS) {
            throw IllegalArgumentException("For byte-based calls to work, the mode must be byte access.")
        }
    }

    /**
     * Puts a standard data type with the specified value, byte order and transformation.
     *
     * @param type           The data type.
     * @param order          The byte order.
     * @param transformation The transformation.
     * @param value          The value.
     * @throws IllegalArgumentException If the type, order, or transformation is unknown.
     */
    fun put(type: DataType, order: DataOrder, transformation: DataTransformation, value: Number) {
        checkByteAccess()
        val longValue = value.toLong()
        val length = type.bytes
        when (order) {
            DataOrder.BIG -> for (i in length - 1 downTo 0) {
                if (i == 0 && transformation !== DataTransformation.NONE) {
                    if (transformation === DataTransformation.ADD) {
                        buffer.writeByte((longValue + 128).toByte().toInt())
                    } else if (transformation === DataTransformation.NEGATE) {
                        buffer.writeByte((-longValue).toByte().toInt())
                    } else if (transformation === DataTransformation.SUBTRACT) {
                        buffer.writeByte((128 - longValue).toByte().toInt())
                    } else {
                        throw IllegalArgumentException("Unknown transformation.")
                    }
                } else {
                    buffer.writeByte((longValue shr i * 8).toByte().toInt())
                }
            }
            DataOrder.INVERSED_MIDDLE -> {
                if (transformation !== DataTransformation.NONE) {
                    throw IllegalArgumentException("Inversed middle endian cannot be transformed.")
                }

                if (type !== DataType.INT) {
                    throw IllegalArgumentException("Inversed middle endian can only be used with an integer.")
                }
                buffer.writeByte((longValue shr 16).toByte().toInt())
                buffer.writeByte((longValue shr 24).toByte().toInt())
                buffer.writeByte(longValue.toByte().toInt())
                buffer.writeByte((longValue shr 8).toByte().toInt())
            }
            DataOrder.LITTLE -> for (i in 0 until length) {
                if (i == 0 && transformation !== DataTransformation.NONE) {
                    if (transformation === DataTransformation.ADD) {
                        buffer.writeByte((longValue + 128).toByte().toInt())
                    } else if (transformation === DataTransformation.NEGATE) {
                        buffer.writeByte((-longValue).toByte().toInt())
                    } else if (transformation === DataTransformation.SUBTRACT) {
                        buffer.writeByte((128 - longValue).toByte().toInt())
                    } else {
                        throw IllegalArgumentException("Unknown transformation.")
                    }
                } else {
                    buffer.writeByte((longValue shr i * 8).toByte().toInt())
                }
            }
            DataOrder.MIDDLE -> {
                if (transformation !== DataTransformation.NONE) {
                    throw IllegalArgumentException("Middle endian cannot be transformed.")
                }

                if (type !== DataType.INT) {
                    throw IllegalArgumentException("Middle endian can only be used with an integer.")
                }

                buffer.writeByte((longValue shr 8).toByte().toInt())
                buffer.writeByte(longValue.toByte().toInt())
                buffer.writeByte((longValue shr 24).toByte().toInt())
                buffer.writeByte((longValue shr 16).toByte().toInt())
            }
            else -> throw IllegalArgumentException("Unknown order.")
        }
    }

    /**
     * Puts a standard data type with the specified value and byte order.
     *
     * @param type  The data type.
     * @param order The byte order.
     * @param value The value.
     */
    fun put(type: DataType, order: DataOrder, value: Number) {
        put(type, order, DataTransformation.NONE, value)
    }

    /**
     * Puts a standard data type with the specified value and transformation.
     *
     * @param type           The type.
     * @param transformation The transformation.
     * @param value          The value.
     */
    fun put(type: DataType, transformation: DataTransformation, value: Number) {
        put(type, DataOrder.BIG, transformation, value)
    }

    /**
     * Puts a standard data type with the specified value.
     *
     * @param type  The data type.
     * @param value The value.
     */
    fun put(type: DataType, value: Number) {
        put(type, DataOrder.BIG, DataTransformation.NONE, value)
    }

    /**
     * Puts a single bit into the buffer. If `flag` is `true`, the value of the bit is `1`. If `flag`
     * is `false`, the value of the bit is `0`.
     *
     * @param flag The flag.
     */
    fun putBit(flag: Boolean) {
        putBit(if (flag) 1 else 0)
    }

    /**
     * Puts a single bit into the buffer with the value `value`.
     *
     * @param value The value.
     */
    fun putBit(value: Int) {
        putBits(1, value)
    }

    /**
     * Puts `numBits` into the buffer with the value `value`.
     *
     * @param numBits The number of bits to put into the buffer.
     * @param value   The value.
     * @throws IllegalArgumentException If the number of bits is not between 1 and 31 inclusive.
     */
    fun putBits(numBits: Int, value: Int) {
        var numBits = numBits
        checkBitAccess()

        var bytePos = bitIndex shr 3
        var bitOffset = 8 - (bitIndex and 7)
        bitIndex += numBits

        var requiredSpace = bytePos - buffer.writerIndex() + 1
        requiredSpace += (numBits + 7) / 8
        buffer.ensureWritable(requiredSpace)

        while (numBits > bitOffset) {
            var tmp = buffer.getByte(bytePos).toInt()
            tmp = tmp and DataConstants.BIT_MASK[bitOffset].inv()
            tmp = tmp or (value shr numBits - bitOffset and DataConstants.BIT_MASK[bitOffset])
            buffer.setByte(bytePos++, tmp)
            numBits -= bitOffset
            bitOffset = 8
        }
        if (numBits == bitOffset) {
            var tmp = buffer.getByte(bytePos).toInt()
            tmp = tmp and DataConstants.BIT_MASK[bitOffset].inv()
            tmp = tmp or (value and DataConstants.BIT_MASK[bitOffset])
            buffer.setByte(bytePos, tmp)
        } else {
            var tmp = buffer.getByte(bytePos).toInt()
            tmp = tmp and (DataConstants.BIT_MASK[numBits] shl bitOffset - numBits).inv()
            tmp = tmp or (value and DataConstants.BIT_MASK[numBits] shl bitOffset - numBits)
            buffer.setByte(bytePos, tmp)
        }
    }

    /**
     * Puts the specified byte array into the buffer.
     *
     * @param bytes The byte array.
     */
    fun putBytes(bytes: ByteArray) {
        buffer.writeBytes(bytes)
    }

    /**
     * Puts the bytes from the specified buffer into this packet's buffer.
     *
     * @param buffer The source [ByteBuf].
     */
    fun putBytes(buffer: ByteBuf) {
        val bytes = ByteArray(buffer.readableBytes())
        buffer.markReaderIndex()
        try {
            buffer.readBytes(bytes)
        } finally {
            buffer.resetReaderIndex()
        }
        putBytes(bytes)
    }

    /**
     * Puts the bytes into the buffer with the specified transformation.
     *
     * @param transformation The transformation.
     * @param bytes          The byte array.
     */
    fun putBytes(transformation: DataTransformation, bytes: ByteArray) {
        if (transformation === DataTransformation.NONE) {
            putBytes(bytes)
        } else {
            for (b in bytes) {
                put(DataType.BYTE, transformation, b)
            }
        }
    }

    /**
     * Puts the specified byte array into the buffer in reverse.
     *
     * @param bytes The byte array.
     */
    fun putBytesReverse(bytes: ByteArray) {
        checkByteAccess()
        for (i in bytes.indices.reversed()) {
            buffer.writeByte(bytes[i].toInt())
        }
    }

    /**
     * Puts the bytes from the specified buffer into this packet's buffer, in reverse.
     *
     * @param buffer The source [ByteBuf].
     */
    fun putBytesReverse(buffer: ByteBuf) {
        val bytes = ByteArray(buffer.readableBytes())
        buffer.markReaderIndex()
        try {
            buffer.readBytes(bytes)
        } finally {
            buffer.resetReaderIndex()
        }
        putBytesReverse(bytes)
    }

    /**
     * Puts the specified byte array into the buffer in reverse with the specified transformation.
     *
     * @param transformation The transformation.
     * @param bytes          The byte array.
     */
    fun putBytesReverse(transformation: DataTransformation, bytes: ByteArray) {
        if (transformation === DataTransformation.NONE) {
            putBytesReverse(bytes)
        } else {
            for (i in bytes.indices.reversed()) {
                put(DataType.BYTE, transformation, bytes[i])
            }
        }
    }

    /**
     * Puts a smart into the buffer.
     *
     * @param value The value.
     */
    fun putSmart(value: Int) {
        checkByteAccess()
        if (value >= 128) {
            buffer.writeShort(value + 32768)
        } else {
            buffer.writeByte(value)
        }
    }

    /**
     * Puts a large smart into the buffer.
     *
     * @param value The value.
     */
    fun putLargeSmart(value: Int) {
        checkByteAccess()
        if (value >= java.lang.Short.MAX_VALUE) {
            buffer.writeInt(value - Integer.MAX_VALUE - 1)
        } else {
            buffer.writeShort(if (value >= 0) value else 32767)
        }
    }

    /**
     * Puts a prefixed string into the buffer.
     *
     * @param str The string.
     */
    fun putPrefixedString(str: String) {
        checkByteAccess()

        buffer.writeByte(0)
        putString(str)
    }

    /**
     * Puts a string into the buffer.
     *
     * @param str The string.
     */
    fun putString(str: String) {
        checkByteAccess()

        buffer.writeString(str)
    }

    /**
     * Switches this builder's mode to the bit access mode.
     *
     * @throws IllegalStateException If the builder is already in bit access mode.
     */
    fun switchToBitAccess() {
        if (mode === AccessMode.BIT_ACCESS) {
            return
        }

        mode = AccessMode.BIT_ACCESS
        bitIndex = buffer.writerIndex() * 8
    }

    /**
     * Switches this builder's mode to the byte access mode.
     *
     * @throws IllegalStateException If the builder is already in byte access mode.
     */
    fun switchToByteAccess() {
        if (mode === AccessMode.BYTE_ACCESS) {
            return
        }

        mode = AccessMode.BYTE_ACCESS
        buffer.writerIndex((bitIndex + 7) / 8)
    }

    fun writerIndex(): Int = buffer.writerIndex()

}