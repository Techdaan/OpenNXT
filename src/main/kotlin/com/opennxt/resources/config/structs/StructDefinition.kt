package com.opennxt.resources.config.structs

import com.opennxt.resources.DefaultStateChecker
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

class StructDefinition(val values: Int2ObjectOpenHashMap<Any> = Int2ObjectOpenHashMap()): DefaultStateChecker {
    override fun isDefault(): Boolean = values.isEmpty()
}