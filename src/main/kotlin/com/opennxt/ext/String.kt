package com.opennxt.ext

import com.opennxt.util.TextUtils

fun String.toFilesystemHash(): Int {
    val size = length
    var char = 0
    for (index in 0 until size)
        char = (char shl 5) - char + TextUtils.charToCp1252(this[index])
    return char
}