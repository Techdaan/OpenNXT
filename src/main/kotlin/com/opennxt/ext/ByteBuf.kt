package com.opennxt.ext

import com.opennxt.model.Build
import com.opennxt.util.ISAACCipher
import io.netty.buffer.ByteBuf

private val CHARSET = charset("windows-1252")

fun ByteBuf.readBuild(): Build = Build(readInt(), readInt())

fun ByteBuf.readString(): String {
    val origPos = readerIndex()
    var length = 0
    while (readByte() != 0.toByte()) length++
    if (length == 0) return ""
    val byteArray = ByteArray(length)
    readerIndex(origPos)
    readBytes(byteArray)
    readerIndex(readerIndex() + 1)
    return String(byteArray, CHARSET)
}

fun ByteBuf.readNullCircumfixedString(): String {
    if (readUnsignedByte().toInt() != 0)
        throw IllegalArgumentException("byte != 0 infront of null-circumfixed string")
    return readString()
}

fun ByteBuf.writeNullCircumfixedString(string: String) {
    writeByte(0)
    writeString(string)
}

fun ByteBuf.writeString(string: String) {
    val bytes = string.toByteArray(CHARSET)
    writeBytes(bytes)
    writeByte(0)
}

// I don't want to talk about it. I don't want to improve it. I don't want anything to do with this shit.
fun ByteBuf.encipherXtea(keys: IntArray, start: Int = readerIndex(), end: Int = readerIndex() + readableBytes()) {
    val readerIndex = readerIndex()
    val writerIndex = writerIndex()

    readerIndex(start)
    val stopAt = (end - start) / 8
    for (i in 0 until stopAt) {
        var int1 = readInt()
        var int2 = readInt()

        var a = 0
        val b = -1640531527
        var c = 32
        while (c-- > 0) {
            int1 += (int2 + (int2 shl 4 xor int2.ushr(5)) xor a + keys[a and 0x3])
            a += b
            int2 += (int1 + (int1 shl 4 xor int1.ushr(5)) xor a + keys[a.ushr(11) and 0x3])
        }

        val originalReaderIndex = readerIndex()
        readerIndex(0)

        writerIndex(originalReaderIndex - 8)
        writeInt(int1)
        writeInt(int2)

        readerIndex(originalReaderIndex)
        writerIndex(writerIndex)
    }

    writerIndex(writerIndex)
    readerIndex(readerIndex)
}

fun ByteBuf.decipherXtea(keys: IntArray, start: Int = readerIndex(), end: Int = readerIndex() + readableBytes()) {
    val readerIndex = readerIndex()
    val writerIndex = writerIndex()

    readerIndex(start)
    val i1 = (end - start) / 8
    for (j1 in 0 until i1) {
        var k1 = readInt()
        var l1 = readInt()
        var sum = -0x3910c8e0
        val delta = -0x61c88647
        var k2 = 32
        while (k2-- > 0) {
            l1 -= keys[(sum and 0x1c84).ushr(11)] + sum xor (k1.ushr(5) xor (k1 shl 4)) + k1
            sum -= delta
            k1 -= (l1.ushr(5) xor (l1 shl 4)) + l1 xor keys[sum and 3] + sum
        }

        val oldReaderIndex = readerIndex()
        readerIndex(0)

        writerIndex(oldReaderIndex - 8)
        writeInt(k1)
        writeInt(l1)

        readerIndex(oldReaderIndex)
        writerIndex(writerIndex)
    }

    writerIndex(writerIndex)
    readerIndex(readerIndex)
}

fun ByteBuf.readOpcode(): Int {
    val value = readByte().toInt() and 0xff
    if (value < 128) return value
    return (value - 128 shl 8) + (readByte().toInt() and 0xff)
}

fun ByteBuf.writeOpcode(opcode: Int) {
    if (opcode >= 128) writeByte(((opcode shr 8) + 128))
    writeByte(opcode)
}

fun ByteBuf.readUnsignedSmartInt(): Int {
    if (getByte(readerIndex()).toInt() and 0xff < 128)
        return readUnsignedByte().toInt()
    return readUnsignedShort() - 0x8000
}

fun ByteBuf.isBigOpcode(isaac: ISAACCipher): Boolean {
    val value = getByte(readerIndex()).toInt() - isaac.currentValue and 0xff
    return value >= 128
}

fun ByteBuf.readOpcode(isaac: ISAACCipher): Int {
    val value = readByte().toInt() - isaac.nextValue and 0xff
    if (value < 128) return value
    return (value - 128 shl 8) + (readByte().toInt() - isaac.nextValue and 0xff)
}

fun ByteBuf.writeOpcode(isaac: ISAACCipher, opcode: Int) {
    if (opcode >= 128) writeByte(((opcode shr 8) + 128) + isaac.nextValue)
    writeByte(opcode + isaac.nextValue)
}

fun ByteBuf.readSmartShort(): Int {
    val n = getByte(readerIndex()).toInt() and 0xff
    if (n < 128) return readUnsignedByte().toInt()
    return readUnsignedShort() - 32768
}

fun ByteBuf.writeSmartShort(value: Int) {
    when (value) {
        in 0..127 -> writeByte(value)
        in 0..32767 -> writeShort(value + 32768)
        else -> throw IllegalArgumentException("Value cannot be greater than 32767")
    }
}

fun ByteBuf.readSmartInt(): Int {
    if (getByte(readerIndex()) < 0)
        return readInt() and 0x7fffffff
    val short = readUnsignedShort()
    return if (short == 32767) -1 else short
}