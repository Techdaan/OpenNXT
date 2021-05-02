package com.opennxt.ext

fun ByteArray.replaceFirst(needle: ByteArray, replacement: ByteArray): Boolean {
    val index = indexOf(needle)
    if (index == -1) {
        return false
    }

    // Patch binary data
    for (x in index until index + replacement.size) {
        this[x] = replacement[x - index]
    }

    return true
}

fun ByteArray.indexOf(needle: ByteArray): Int {
    outer@ for (i in 0 until this.size - needle.size + 1) {
        for (j in needle.indices) {
            if (this[i + j] != needle[j]) {
                continue@outer
            }
        }
        return i
    }
    return -1
}