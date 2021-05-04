package com.opennxt.resources.config.enums

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import com.opennxt.resources.DiskResourceCodec
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.nio.file.Files
import java.nio.file.Path

object EnumDiskCodec: DiskResourceCodec<EnumDefinition> {
    override fun list(path: Path): Map<String, Path> {
        val result = Object2ObjectOpenHashMap<String, Path>()
        Files.list(path).forEach { file ->
            if (Files.isRegularFile(file)) {
                val name = file.fileName.toString().toLowerCase()
                if (name.endsWith(".toml")) {
                    result[name.substring(0, name.length - 5)] = file
                }
            } else if (Files.isDirectory(file)) {
                result.putAll(list(file))
            }
        }
        return result
    }

    override fun load(path: Path): EnumDefinition? {
        if (!Files.exists(path)) return null

        val reader = Toml()
        reader.read(path.toFile())
        return reader.to(EnumDefinition::class.java)
    }

    override fun store(path: Path, data: EnumDefinition) {
        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)

        TomlWriter().write(data, path.toFile())
    }

    override fun getFileExtension(): String? = "toml"
}