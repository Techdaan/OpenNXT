package com.opennxt.util

import java.math.BigInteger
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.RSAKeyGenParameterSpec
import java.security.spec.RSAPrivateKeySpec

object RSAUtil {
    val PUBLIC_KEY = BigInteger("10001", 16)

    fun generateKeySpec(size: Int): RSAPrivateKeySpec {
        val factory = KeyFactory.getInstance("RSA")

        val generator = KeyPairGenerator.getInstance("RSA")
        val spec = RSAKeyGenParameterSpec(size, PUBLIC_KEY)
        generator.initialize(spec)

        val privateKey = generator.genKeyPair().private

        return factory.getKeySpec(privateKey, RSAPrivateKeySpec::class.java)
    }

    fun findRSAKey(data: ByteArray, bits: Int): BigInteger? {
        val size = bits / 4
        val buffer = ByteArray(size)

        for (i in 1 until data.size - buffer.size - 1) {
            // Ensure we don't run into any (previous) string terminators
            if (data[i].toInt() == 0) continue
            if (data[i - 1].toInt() != 0) continue

            // Ensure the key is followed by a string terminator
            if (data[i + size + 1].toInt() != 0) continue
            if (data[i + size + 2].toInt() != 0) continue

            // Copy the raw key into our buffer
            System.arraycopy(data, i, buffer, 0, size)

            try {
                // If it is not a valid BigInteger the instantiaton will throw an error
                return BigInteger(String(buffer, Charset.forName("ASCII")), 16)
            } catch (ignore: NumberFormatException) {
            }
        }

        return null
    }
}