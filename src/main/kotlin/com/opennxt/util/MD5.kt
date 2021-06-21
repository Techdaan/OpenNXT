package com.opennxt.util

import java.security.MessageDigest

object MD5 {
    private val LOCK = Any()
    private val md5 = MessageDigest.getInstance("MD5")
    fun hash(data: ByteArray): ByteArray {
        synchronized(LOCK) {
            md5.update(data)
            val digest = md5.digest()
            md5.reset()
            return digest
        }
    }
}