package com.opennxt.config

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.full.createInstance

abstract class TomlConfig {
    abstract fun save(map: MutableMap<String, Any>)

    abstract fun load(toml: Toml)

    companion object {
        inline fun <reified T : TomlConfig> load(
            path: Path,
            saveAfterLoad: Boolean = true,
            mustExist: Boolean = false
        ): T {
            if (mustExist && !Files.exists(path))
                throw FileNotFoundException(path.toString())

            if (!Files.exists(path.parent))
                Files.createDirectories(path.parent)

            val config = T::class.createInstance()
            if (!Files.exists(path)) {
                save(path, config)
                return config
            }

            val toml = Toml()
            toml.read(path.toFile())
            config.load(toml)

            if (saveAfterLoad)
                save(path, config)

            return config
        }

        fun save(path: Path, config: TomlConfig) {
            if (!Files.exists(path.parent))
                Files.createDirectories(path.parent)

            val map = Object2ObjectOpenHashMap<String, Any>()
            config.save(map)
            TomlWriter().write(map, path.toFile())
        }
    }
}