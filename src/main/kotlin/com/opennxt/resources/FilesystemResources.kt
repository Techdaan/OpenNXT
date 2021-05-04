package com.opennxt.resources

import com.opennxt.filesystem.Filesystem
import com.opennxt.resources.config.enums.EnumFilesystemCodec
import com.opennxt.resources.config.enums.EnumDiskCodec
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KClass

class FilesystemResources(val filesystem: Filesystem, val path: Path) {
    private val fsCodices = EnumMap<ResourceType, FilesystemResourceCodec<*>>(ResourceType::class.java)
    private val diskCodices = EnumMap<ResourceType, DiskResourceCodec<*>>(ResourceType::class.java)

    init {
        if (!Files.exists(path))
            Files.createDirectories(path)

        fsCodices[ResourceType.ENUM] = EnumFilesystemCodec

        diskCodices[ResourceType.ENUM] = EnumDiskCodec
    }

    fun getFilesystemCodex(type: KClass<*>): FilesystemResourceCodec<*> {
        val resourceType =
            ResourceType.forClass(type) ?: throw NullPointerException("No resource type linked to type $type")

        return fsCodices[resourceType] ?: throw NullPointerException("No filesystem codex found for type $type")
    }

    fun getDiskCodec(type: KClass<*>): DiskResourceCodec<*> {
        val resourceType =
            ResourceType.forClass(type) ?: throw NullPointerException("No resource type linked to type $type")

        return diskCodices[resourceType] ?: throw NullPointerException("No disk codex found for type $type")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: KClass<out T>, id: Int): T? {
        return getFilesystemCodex(type).load(filesystem, id) as? T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> list(type: KClass<out T>): Map<Int, T> {
        return getFilesystemCodex(type).list(filesystem) as Map<Int, T>
    }
}