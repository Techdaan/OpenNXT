package com.opennxt.tools.impl.cachedownloader

import com.opennxt.model.files.BinaryType
import com.opennxt.model.files.ClientConfig

data class Js5Credentials(val version: Int, val token: String){
    companion object {
        fun download(url: String = "https://world5.runescape.com/jav_config.ws"): Js5Credentials {
            val config = ClientConfig.download(url, BinaryType.WIN64)
            val version = config["server_version"] ?: throw NullPointerException("server_version not found")
            val token = config.getJs5Token()

            return Js5Credentials(version.toInt(), token)
        }
    }
}
