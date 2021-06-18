package com.opennxt.api.stat

import com.opennxt.resources.FilesystemResources
import com.opennxt.resources.config.enums.EnumDefinition
import com.opennxt.resources.defaults.stats.StatDefaults
import com.opennxt.resources.defaults.stats.StatDefinition
import com.opennxt.resources.defaults.stats.StatExperienceTable

enum class Stat(val id: Int) {
    ATTACK(0),
    DEFENCE(1),
    STRENGTH(2),
    CONSTITUTION(3),
    RANGED(4),
    PRAYER(5),
    MAGIC(6),
    COOKING(7),
    WOODCUTTING(8),
    FLETCHING(9),
    FISHING(10),
    FIREMAKING(11),
    CRAFTING(12),
    SMITHING(13),
    MINING(14),
    HERBLORE(15),
    AGILITY(16),
    THIEVING(17),
    SLAYER(18),
    FARMING(19),
    RUNECRAFTING(20),
    HUNTER(21),
    CONSTRUCTION(22),
    SUMMONING(23),
    DUNGEONEERING(24),
    DIVINATION(25),
    INVENTION(26),
    ARCHAEOLOGY(27),
    ;

    lateinit var def: StatDefinition
    lateinit var table: StatExperienceTable
    lateinit var display: String

    companion object {
        private val VALUES = values()

        fun reload() {
            val defaults = FilesystemResources.instance.defaults.get<StatDefaults>()
            val names = FilesystemResources.instance.get<EnumDefinition>(680)!!

            defaults.stats.forEach { def ->
                val enum = VALUES[def.id]
                enum.def = def
                enum.table = def.table
                enum.display = ((names.values[def.id] as? String) ?: names.defaultString) ?: "null"
            }
        }
    }
}