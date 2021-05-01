package com.opennxt.util

import java.math.BigInteger
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
}