package com.opennxt.resources.defaults.stats

import kotlin.math.pow

class StatExperienceTable {

    companion object {
        val DEFAULT = StatExperienceTable()
    }

    private val table: IntArray

    constructor() {
        table = IntArray(120)
        var xp = 0
        for (level in 1..120) {
            val difference = (level.toDouble() + 300.0 * 2.0.pow(level.toDouble() / 7.0)).toInt()
            xp += difference
            table[level - 1] = xp / 4
        }
        validate()
    }

    constructor(values: IntArray) {
        this.table = values
    }

    /**
     * Validates the xp table
     */
    private fun validate() {
        for (pos in 1 until table.size) {
            if (table[pos - 1] < 0) {
                throw IllegalArgumentException("Negative XP at pos:" + (pos - 1))
            }
            if (table[pos] < table[pos - 1]) {
                throw IllegalArgumentException("XP goes backwards at pos:$pos")
            }
        }
    }

    /**
     * Gets the level based on the experience
     */
    fun levelForXp(xp: Int): Int {
        var level = 0
        var pos = 0
        while (pos < table.size && xp >= table[pos]) {
            level = 1 + pos
            pos++
        }
        return level
    }

    /**
     * Gets the xp required for a certain level
     */
    fun xpForLevel(level: Int): Int {
        var level = level
        if (level < 1) {
            return 0
        }
        if (level > table.size) {
            level = table.size
        }
        return table[level - 1]
    }

    override fun toString(): String {
        return "StatExperienceTable[table=${table.contentToString()}]"
    }
}