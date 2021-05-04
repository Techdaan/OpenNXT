package com.opennxt.resources.config.enums

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import com.opennxt.resources.DiskResourceCodec
import com.opennxt.resources.config.vars.ScriptVarType
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.nio.file.Files
import java.nio.file.Path

object EnumDiskCodec : DiskResourceCodec<EnumDefinition> {
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

        val keyType = ScriptVarType.valueOf(reader.getString("keyType"))
        if (keyType == ScriptVarType.STRING) throw IllegalStateException("string as key type")

        val valueType = ScriptVarType.valueOf(reader.getString("valueType"))
        val defaultInt = keyType.readableToRaw(reader.getLong("defaultInt").toString()) as Int
        val defaultString = reader.getString("defaultString")

        val values = Int2ObjectAVLTreeMap<Any>()
        reader.getTable("values").toMap().forEach { (k, v) ->
            val rawKey = keyType.readableToRaw(k) as Int
            val rawValue = valueType.readableToRaw(v.toString())
            values[rawKey] = rawValue
        }

        return EnumDefinition(keyType, valueType, defaultInt, defaultString, values)
    }

    override fun store(path: Path, data: EnumDefinition) {
        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)

        val values = HashMap<Any, Any>()
        data.values.forEach { (k, v) ->
            val readableKey = data.keyType.rawToReadable(k)
            val readableValue = data.valueType.rawToReadable(v)

            values[readableKey] = readableValue
        }

        val map = mutableMapOf<Any, Any>(
            "keyType" to data.keyType,
            "valueType" to data.valueType,
            "defaultInt" to data.defaultInt,
            "values" to values
        )

        if (data.defaultString != null)
            map["defaultString"] = data.defaultString!!

        TomlWriter().write(map, path.toFile())
    }

    override fun getFileExtension(resource: EnumDefinition): String? = "toml"
}