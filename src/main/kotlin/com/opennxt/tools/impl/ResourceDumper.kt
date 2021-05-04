package com.opennxt.tools.impl

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.opennxt.Constants
import com.opennxt.resources.DefaultStateChecker
import com.opennxt.resources.DiskResourceCodec
import com.opennxt.resources.ResourceType
import com.opennxt.tools.Tool
import java.nio.file.Files
import kotlin.system.exitProcess

class ResourceDumper : Tool("resource-dumper", "Dumps resources from the filesystem") {
    val types by option(help = "A list of all resource types that should be dumped")
        .enum<ResourceType>()
        .multiple()

    val dumpAll by option(help = "Dumps all types regardless of the specified types in 'types'")
        .flag(default = false)

    override fun runTool() {
        if (types.isEmpty() && !dumpAll) {
            logger.error { "Please specify the resource types to dump, or use --dump-all (for help: --help)" }
            exitProcess(1)
        }

        val types = if (dumpAll) ResourceType.values() else this.types.toTypedArray()

        for (type in types) {
            val path = Constants.RESOURCE_PATH.resolve("dumps").resolve(type.identifier)
            logger.info { "Dumping type $type to $path" }

            if (!Files.exists(path))
                Files.createDirectories(path)

            @Suppress("UNCHECKED_CAST")
            val diskCodec = resources.getDiskCodec(type.kclass) as DiskResourceCodec<Any>
            resources.list(type.kclass).forEach { (id, resource) ->
                if (resource is DefaultStateChecker && resource.isDefault())
                    return@forEach

                val extension = diskCodec.getFileExtension(resource)
                if (extension == null) {
                    diskCodec.store(path.resolve(id.toString()), resource)
                } else {
                    diskCodec.store(path.resolve("$id.$extension"), resource)
                }
            }
        }
    }
}