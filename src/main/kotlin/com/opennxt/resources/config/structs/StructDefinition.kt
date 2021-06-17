package com.opennxt.resources.config.structs

import com.opennxt.OpenNXT
import com.opennxt.resources.DefaultStateChecker
import com.opennxt.resources.config.params.ParamDefinition
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

class StructDefinition(val values: Int2ObjectOpenHashMap<Any> = Int2ObjectOpenHashMap()) : DefaultStateChecker {
    override fun isDefault(): Boolean = values.isEmpty()

    fun getInt(
        key: Int,
        default: Int = OpenNXT.resources.get<ParamDefinition>(key)?.defaultInt
            ?: throw NullPointerException("couldn't find param $key")
    ): Int = values.getOrDefault(key, default) as Int

    fun getString(
        key: Int,
        default: String = OpenNXT.resources.get<ParamDefinition>(key)?.defaultString
            ?: throw NullPointerException("couldn't find param $key")
    ): String = values.getOrDefault(key, default) as String

}