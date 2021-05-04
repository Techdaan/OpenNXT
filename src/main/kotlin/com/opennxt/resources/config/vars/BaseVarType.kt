package com.opennxt.resources.config.vars

import kotlin.reflect.KClass

enum class BaseVarType(
    val id: Int,
    val clazz: KClass<*>
) {
    INTEGER(0, Int::class),
    LONG(1, Long::class),
    STRING(2, String::class),
    COORDFINE(3, Unit::class); // TODO Coordfine

    companion object {
        private val values = values()

        fun byId(id: Int): BaseVarType {
            return values[id]
        }
    }
}