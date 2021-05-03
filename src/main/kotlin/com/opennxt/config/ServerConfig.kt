package com.opennxt.config

import com.moandjiezana.toml.Toml
import com.opennxt.Constants

class ServerConfig : TomlConfig() {
    companion object {
        val DEFAULT_PATH = Constants.CONFIG_PATH.resolve("server.toml")
    }

    data class Ports(var game: Int = 43594, var http: Int = 80, var https: Int = 443)

    var ports = Ports()
    var hostname = "127.0.0.1"
    var configUrl = "http://127.0.0.1/jav_config.ws?binaryType=2"

    var build = 918

    override fun save(map: MutableMap<String, Any>) {
        map["networking"] = mapOf(
            "ports" to mapOf(
                "game" to ports.game,
                "http" to ports.http,
                "https" to ports.https
            )
        )
        map["hostname"] = hostname
        map["configUrl"] = configUrl
        map["build"] = build
    }

    override fun load(toml: Toml) {
        hostname = toml.getString("hostname", hostname)
        configUrl = toml.getString("configUrl", configUrl)
        build = toml.getLong("build", build.toLong()).toInt()

        val networking = toml.getTable("networking")
        if (networking != null) {
            val ports = toml.getTable("ports")
            if (ports != null) {
                this.ports.game = ports.getLong("game", this.ports.game.toLong()).toInt()
                this.ports.http = ports.getLong("http", this.ports.http.toLong()).toInt()
                this.ports.https = ports.getLong("https", this.ports.https.toLong()).toInt()
            }
        }
    }
}