package com.opennxt.api.stat

import com.opennxt.api.util.Cleanable
import com.opennxt.api.util.Initializable

interface StatContainer: Cleanable, Initializable {
    operator fun get(stat: Stat): StatData
    operator fun set(stat: Stat, data: StatData)

    fun addExperience(stat: Stat, amount: Double, source: ExperienceSource = ExperienceSource.Default): Int
    fun boostStat(stat: Stat, boostBy: Int)
    fun getLevel(stat: Stat, boosted: Boolean = true): Int
    fun hasLevel(stat: Stat, level: Int, boostAble: Boolean = true): Boolean
}