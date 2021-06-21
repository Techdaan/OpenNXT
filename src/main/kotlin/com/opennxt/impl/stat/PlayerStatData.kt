package com.opennxt.impl.stat

import com.opennxt.api.stat.Stat
import com.opennxt.api.stat.StatData

data class PlayerStatData(
    override val stat: Stat,
    override var experience: Double = 0.0,
    override var actualLevel: Int = 1,
    override var boostedLevel: Int = 1,
    var bonusExp: Double = 0.0
) : StatData {
    fun calculateLevel(enforceMaxLevel: Boolean = true): Int = calculateLevel(experience.toInt(), enforceMaxLevel)

    fun calculateLevel(xp: Int, enforceMaxLevel: Boolean = true): Int {
        val level = stat.table.levelForXp(xp)
        if (enforceMaxLevel && level > stat.def.cap) return stat.def.cap
        return level
    }

    private fun validateXp() {
        if (experience < 0.0) experience = 0.0
        else if (experience > 200_000_000.0) experience = 200_000_000.0
    }

    fun addExperience(amount: Double): Int {
        experience += amount
        validateXp()

        val previousLevel = actualLevel
        val newLevel = calculateLevel()
        if (newLevel != previousLevel) {
            actualLevel = newLevel
        }

        return newLevel - previousLevel
    }
}