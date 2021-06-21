package com.opennxt.impl.stat

import com.opennxt.api.stat.ExperienceSource
import com.opennxt.api.stat.Stat
import com.opennxt.api.stat.StatContainer
import com.opennxt.api.stat.StatData
import com.opennxt.model.entity.BasePlayer
import com.opennxt.net.game.serverprot.UpdateStat
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import mu.KotlinLogging
import java.util.*
import kotlin.math.min

class PlayerStatContainer(val player: BasePlayer) : StatContainer {
    private val logger = KotlinLogging.logger {}
    private val stats = EnumMap<Stat, PlayerStatData>(Stat::class.java)
    private val dirty = ObjectOpenHashSet<Stat>()

    override fun init() {
        for (stat in Stat.values()) {
            refresh(stat)
        }
    }

    override fun markDirty() {
        dirty.addAll(Stat.values())
    }

    override fun isDirty(): Boolean = dirty.isNotEmpty()

    override fun clean() {
        for (stat in dirty) {
            refresh(stat)
        }

        dirty.clear()
    }

    init {
        Stat.values().forEach { stat ->
            val data = PlayerStatData(stat, 0.0, 1, 1)

            if (stat == Stat.CONSTITUTION) {
                data.experience = 1154.0
                data.actualLevel = 10
                data.boostedLevel = 10
            }

            stats[stat] = data
        }
    }

    override fun get(stat: Stat): StatData = stats.getValue(stat)

    override fun set(stat: Stat, data: StatData) {
        if (stat != data.stat) {
            throw IllegalArgumentException("'stat' and 'data#stat' do not match ($stat vs ${data.stat})")
        }

        if (data !is PlayerStatData) {
            stats[stat] = PlayerStatData(
                stat,
                data.experience,
                data.actualLevel,
                data.boostedLevel
            )
            return
        }

        stats[stat] = data
    }

    override fun addExperience(stat: Stat, amount: Double, source: ExperienceSource): Int {
        val data = stats.getValue(stat)

        var actualAmount = amount * source.boostFactor

        val bonusExp = data.bonusExp
        if (bonusExp > 0) {
            val toAdd = min(bonusExp, actualAmount)
            data.bonusExp -= toAdd
            actualAmount += toAdd
        }

        val levelsGained = data.addExperience(amount)

        refresh(data)

        if (levelsGained > 0) {
            logger.info { "TODO: Level gained pop-up" }
        }

        return levelsGained
    }

    fun refresh(stat: Stat) {
        refresh(stats.getValue(stat))
    }

    fun refresh(data: StatData) {
        val packet =
            UpdateStat(stat = data.stat.id, level = data.boostedLevel, experience = data.experience.toInt())

        player.client.write(packet)
    }

    override fun boostStat(stat: Stat, boostBy: Int) {
        TODO("Not yet implemented")
    }

    override fun hasLevel(stat: Stat, level: Int, boostAble: Boolean): Boolean {
        return getLevel(stat, boostAble) >= level
    }

    override fun getLevel(stat: Stat, boosted: Boolean): Int {
        val data = stats.getValue(stat)
        return if (boosted) {
            data.boostedLevel
        } else {
            data.actualLevel
        }
    }
}