package com.opennxt.config

import com.moandjiezana.toml.Toml
import com.opennxt.Constants
import java.math.BigInteger

class RsaConfig : TomlConfig() {
    companion object {
        val DEFAULT_PATH = Constants.CONFIG_PATH.resolve("rsa.toml")
    }

    class RsaKeyPair(val exponent: BigInteger, val modulus: BigInteger)

    var login = RsaKeyPair(BigInteger.ZERO, BigInteger.ZERO)
    var js5 = RsaKeyPair(BigInteger.ZERO, BigInteger.ZERO)
    var launcher = RsaKeyPair(BigInteger.ZERO, BigInteger.ZERO)

    private fun Toml.getRsaKeyPair(
        path: String,
        default: RsaKeyPair = RsaKeyPair(BigInteger.ZERO, BigInteger.ZERO)
    ): RsaKeyPair {
        val table = getTable(path) ?: return default

        return RsaKeyPair(
            BigInteger(table.getString("exponent", "0"), 16),
            BigInteger(table.getString("modulus", "0"), 16)
        )
    }

    override fun load(toml: Toml) {
        js5 = toml.getRsaKeyPair("js5", js5)
        login = toml.getRsaKeyPair("login", login)
        launcher = toml.getRsaKeyPair("launcher", launcher)
    }

    override fun save(map: MutableMap<String, Any>) {
        map["js5"] = mapOf(
            "exponent" to js5.exponent.toString(16),
            "modulus" to js5.modulus.toString(16)
        )

        map["login"] = mapOf(
            "exponent" to login.exponent.toString(16),
            "modulus" to login.modulus.toString(16)
        )

        map["launcher"] = mapOf(
            "exponent" to launcher.exponent.toString(16),
            "modulus" to launcher.modulus.toString(16)
        )
    }
}