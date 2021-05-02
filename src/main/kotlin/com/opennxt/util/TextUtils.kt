package com.opennxt.util

object TextUtils {

    private val cp1252 = charArrayOf(
        '\u20ac', '\u0000', '\u201a', '\u0192', '\u201e', '\u2026', '\u2020', '\u2021', '\u02c6', '\u2030', '\u0160',
        '\u2039', '\u0152', '\u0000', '\u017d', '\u0000', '\u0000', '\u2018', '\u2019', '\u201c', '\u201d', '\u2022',
        '\u2013', '\u2014', '\u02dc', '\u2122', '\u0161', '\u203a', '\u0153', '\u0000', '\u017e', '\u0178'
    )

    fun cp1252ToChar(i: Byte): Char {
        var cp1252 = i.toInt() and 0xff
        require(0 != cp1252) { "Non cp1252 character 0x" + cp1252.toString(16) + " provided" }
        if (cp1252 in 128..159) {
            var translated = this.cp1252[cp1252 - 128].toInt()
            if (translated == 0) {
                translated = 63
            }
            cp1252 = translated
        }
        return cp1252.toChar()
    }

    fun charToCp1252(c: Char): Byte {
        if (c.toInt() > 0 && c < '\u0080' || c in '\u00a0'..'\u00ff')
            return c.toByte()

        return when (c) {
            '\u20ac' -> (-128).toByte()
            '\u201a' -> (-126).toByte()
            '\u0192' -> (-125).toByte()
            '\u201e' -> (-124).toByte()
            '\u2026' -> (-123).toByte()
            '\u2020' -> (-122).toByte()
            '\u2021' -> (-121).toByte()
            '\u02c6' -> (-120).toByte()
            '\u2030' -> (-119).toByte()
            '\u0160' -> (-118).toByte()
            '\u2039' -> (-117).toByte()
            '\u0152' -> (-116).toByte()
            '\u017d' -> (-114).toByte()
            '\u2018' -> (-111).toByte()
            '\u2019' -> (-110).toByte()
            '\u201c' -> (-109).toByte()
            '\u201d' -> (-108).toByte()
            '\u2022' -> (-107).toByte()
            '\u2013' -> (-106).toByte()
            '\u2014' -> (-105).toByte()
            '\u02dc' -> (-104).toByte()
            '\u2122' -> (-103).toByte()
            '\u0161' -> (-102).toByte()
            '\u203a' -> (-101).toByte()
            '\u0153' -> (-100).toByte()
            '\u017e' -> (-98).toByte()
            '\u0178' -> (-97).toByte()
            else -> 63.toByte()
        }
    }
}