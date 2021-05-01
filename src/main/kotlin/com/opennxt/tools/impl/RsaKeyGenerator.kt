package com.opennxt.tools.impl

import com.opennxt.Constants
import com.opennxt.config.RsaConfig
import com.opennxt.config.TomlConfig
import com.opennxt.tools.Tool
import com.opennxt.util.RSAUtil
import java.nio.file.Files

class RsaKeyGenerator : Tool("rsa-key-generator", "Generates RSA keys for the launcher and js5/login servers") {
    override fun runTool() {
        if (Files.exists(RsaConfig.DEFAULT_PATH)) {
            val renamed = Constants.CONFIG_PATH.resolve("rsa.toml_${System.currentTimeMillis()}")
            logger.info { "A RSA config file already exists at ${RsaConfig.DEFAULT_PATH}. Renaming existing config to ${renamed.fileName}" }
            Files.move(RsaConfig.DEFAULT_PATH, renamed)
        }

        logger.info { "Generating new js5 keys (size=4096)" }
        val js5 = RSAUtil.generateKeySpec(4096)

        logger.info { "Generating new login keys (size=1024)" }
        val login = RSAUtil.generateKeySpec(1024)

        logger.info { "Generating new launcher keys (size=4096)" }
        val launcher = RSAUtil.generateKeySpec(4096)

        val config = TomlConfig.load<RsaConfig>(RsaConfig.DEFAULT_PATH)
        config.js5 = RsaConfig.RsaKeyPair(js5.privateExponent, js5.modulus)
        config.login = RsaConfig.RsaKeyPair(login.privateExponent, login.modulus)
        config.launcher = RsaConfig.RsaKeyPair(launcher.privateExponent, launcher.modulus)

        TomlConfig.save(RsaConfig.DEFAULT_PATH, config)
        logger.info { "Saved newly generated RSA keys to ${RsaConfig.DEFAULT_PATH}" }
    }
}