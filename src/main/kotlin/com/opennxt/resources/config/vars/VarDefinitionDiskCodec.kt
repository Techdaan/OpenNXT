package com.opennxt.resources.config.vars

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import com.opennxt.api.vars.VarDomain
import com.opennxt.resources.DiskResourceCodec
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.nio.file.Files
import java.nio.file.Path

class VarDefinitionDiskCodec<T: VarDefinition>(val emptyProvider: () -> T): DiskResourceCodec<T> {
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

    override fun load(path: Path): T? {
        if (!Files.exists(path)) return null

        val reader = Toml()
        reader.read(path.toFile())

        val definition = emptyProvider()
        val raw = reader.getTable("values").toMap()
        definition.forceDefault = raw["forceDefault"] as Boolean
        definition.lifetime = raw["lifetime"] as Int
        definition.type = ScriptVarType.valueOf(raw["type"] as String)

        return definition
    }

    override fun store(path: Path, data: T) {
        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)

        val values = HashMap<Any, Any>()
        values["type"] = data.type.name
        values["lifetime"] = data.lifetime
        values["forceDefault"] = data.forceDefault

        TomlWriter().write(mapOf("values" to values), path.toFile())
    }

    override fun getFileExtension(resource: T): String? = "toml"
}