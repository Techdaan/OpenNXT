package com.opennxt.resources.config.structs

import com.moandjiezana.toml.Toml
import com.moandjiezana.toml.TomlWriter
import com.opennxt.OpenNXT
import com.opennxt.resources.DiskResourceCodec
import com.opennxt.resources.FilesystemResources
import com.opennxt.resources.config.params.ParamDefinition
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import java.nio.file.Files
import java.nio.file.Path

object StructDiskCodec : DiskResourceCodec<StructDefinition> {
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

    override fun load(path: Path): StructDefinition? {
        if (!Files.exists(path)) return null

        val reader = Toml()
        reader.read(path.toFile())

        val definition = StructDefinition()
        val raw = reader.getTable("values").toMap()
        raw.forEach { (key, value) ->
            val id = key.toInt()

            val param = FilesystemResources.instance.get<ParamDefinition>(id)

            definition.values[id] = param!!.type.readableToRaw(value.toString())
        }

        return definition
    }

    override fun store(path: Path, data: StructDefinition) {
        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)

        val values = HashMap<Any, Any>()
        data.values.forEach { (k, v) ->
            val param = FilesystemResources.instance.get<ParamDefinition>(k)

            values[k] = param!!.type.rawToReadable(v)
        }

        TomlWriter().write(mapOf("values" to values), path.toFile())
    }

    override fun getFileExtension(resource: StructDefinition): String? = "toml"
}