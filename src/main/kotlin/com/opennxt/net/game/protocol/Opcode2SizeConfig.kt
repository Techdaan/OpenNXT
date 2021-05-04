package com.opennxt.net.game.protocol

import com.moandjiezana.toml.Toml
import com.opennxt.config.TomlConfig
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap

class Opcode2SizeConfig : TomlConfig() {
    val values = Int2IntOpenHashMap()

    override fun save(map: MutableMap<String, Any>) {
        map["values"] = values
    }

    override fun load(toml: Toml) {
        toml.getTable("values")
            .toMap()
            .forEach { (k, v) -> values[k.toInt()] = v.toString().toInt() }
    }
}