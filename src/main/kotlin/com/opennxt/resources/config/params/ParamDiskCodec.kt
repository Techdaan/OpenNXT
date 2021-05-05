package com.opennxt.resources.config.params

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import com.opennxt.resources.DiskResourceCodec
import com.opennxt.resources.config.vars.ScriptVarType
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.nio.file.Files
import java.nio.file.Path

object ParamDiskCodec : DiskResourceCodec<ParamDefinition> {
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

    override fun load(path: Path): ParamDefinition? {
        if (!Files.exists(path)) return null

        val reader = Toml()
        reader.read(path.toFile())

        val definition = ParamDefinition()

        definition.type = ScriptVarType.valueOf(reader.getString("type"))

        if(reader.contains("defaultInt")) {
            definition.defaultInt = definition.type.readableToRaw(reader.getLong("defaultInt").toString()) as Int
        }

        if (reader.contains("defaultString")) {
            definition.defaultString = reader.getString("defaultString")
        }

        if (reader.contains("membersOnly")) {
            definition.membersOnly = reader.getBoolean("membersOnly")
        }

        return definition
    }

    override fun store(path: Path, data: ParamDefinition) {
        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)

        val map = mutableMapOf<Any, Any>(
            "type" to data.type,
            "defaultInt" to data.type.rawToReadable(data.defaultInt)
        )

        if (data.membersOnly) map["membersOnly"] = true

        if (data.defaultString != "null")
            map["defaultString"] = data.defaultString

        TomlWriter().write(map, path.toFile())
    }

    override fun getFileExtension(resource: ParamDefinition): String? = "toml"
}