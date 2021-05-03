package com.opennxt.model.files

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.io.FileNotFoundException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

data class ClientConfig(val entries: MutableMap<String, String> = Object2ObjectOpenHashMap()) {
    companion object {
        private fun MutableMap<String, String>.readLine(line: String) {
            if (line.isBlank()) return

            var bits = line.split("=", limit = 2)

            var key = bits[0]
            var value = bits[1]

            if (bits[0] == "msg" || bits[0] == "param") {
                bits = line.split("=", limit = 3)

                key = "${bits[0]}=${bits[1]}"
                value = bits[2]
            }

            this[key] = value
        }

        fun download(base: String, binaryType: BinaryType): ClientConfig {
            val url = URL("$base?binaryType=${binaryType.id}")
            val config = ClientConfig()

            url.readText(Charsets.UTF_8).split("\n").forEach { line ->
                config.entries.readLine(line)
            }

            return config
        }

        fun load(path: Path): ClientConfig {
            if (!Files.exists(path))
                throw FileNotFoundException(path.toString())

            val config = ClientConfig()

            Files.readAllLines(path, Charsets.UTF_8).forEach { line ->
                config.entries.readLine(line)
            }

            return config
        }

        fun save(config: ClientConfig, path: Path) {
            if (!Files.exists(path.parent))
                Files.createDirectories(path.parent)

            Files.write(path, config.toString().toByteArray(Charsets.UTF_8))
        }
    }

    val highestParam: Int
        get() {
            var max = 100
            while (!entries.containsKey("param=$max")) max--
            return max
        }

    fun getParam(id: Int): String? = entries["param=$id"]

    fun getValue(key: String): String? = entries[key]

    fun getFiles(): Set<DownloadInformation> {
        val set = HashSet<DownloadInformation>()

        var download = 0
        while (entries.containsKey("download_name_$download")) {
            val name = entries.getValue("download_name_$download")
            val crc = entries.getValue("download_crc_$download").toLong()
            val hash = entries.getValue("download_hash_$download")

            set.add(DownloadInformation(download, name, crc, hash))

            download++
        }

        return set
    }

    fun getJs5Token(): String {
        for (i in 0 until highestParam) {
            val test = entries["param=$i"] ?: continue
            if (test.length == 32) return test
        }
        throw NullPointerException("no js5 token found")
    }

    operator fun set(key: String, value: String) {
        entries[key] = value
    }

    operator fun get(key: String): String? = entries[key]

    override fun toString(): String {
        val entries = ArrayList<String>()
        this.entries.forEach { (k, v) -> entries += "$k=$v" }
        entries.sort()

        val jnr = StringJoiner("\n")
        entries.forEach(jnr::add)

        return jnr.toString()
    }
}