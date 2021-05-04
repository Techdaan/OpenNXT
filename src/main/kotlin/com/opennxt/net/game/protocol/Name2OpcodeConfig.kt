package com.opennxt.net.game.protocol

import com.moandjiezana.toml.Toml
import com.opennxt.config.TomlConfig
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap

class Name2OpcodeConfig : TomlConfig() {
    val values = Object2IntOpenHashMap<String>()

    fun reversedValues(): Int2ObjectMap<String> {
        val map = Int2ObjectOpenHashMap<String>()
        values.forEach { (k, v) -> map[v] = k }
        return map
    }

    override fun save(map: MutableMap<String, Any>) {
        val out = HashMap<Int, String>()
        values.forEach { (k, v) -> out[v] = k }
        map["values"] = out
    }

    override fun load(toml: Toml) {
        if (toml.contains("values"))
            toml.getTable("values")
                .toMap()
                .forEach { (k, v) -> values[v.toString()] = k.toInt() }
    }
}