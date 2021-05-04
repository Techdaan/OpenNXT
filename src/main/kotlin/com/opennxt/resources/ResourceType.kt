package com.opennxt.resources

import com.opennxt.resources.config.enums.EnumDefinition
import kotlin.reflect.KClass

enum class ResourceType(val identifier: String, val kclass: KClass<*>) {
    ENUM("enum", EnumDefinition::class)
    ;

    companion object {
        private val values = values()

        fun getArchive(id: Int, size: Int): Int = id.ushr(size)

        fun getFile(id: Int, size: Int): Int = (id and (1 shl size) - 1)

        fun forClass(kclass: KClass<*>): ResourceType? = values.firstOrNull { it.kclass == kclass }
    }
}