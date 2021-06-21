package com.opennxt.model.entity.rendering

import com.opennxt.model.entity.Entity
import com.opennxt.model.entity.PlayerEntity
import com.opennxt.model.world.WorldPlayer

/**
 * The [EntityRenderer] keeps track of the update blocks of the [Entity] that are used in the player and npc sync
 * packets. Update blocks can be added to an entity through a readable interface using the [EntityRenderer].
 */
class EntityRenderer(val entity: Entity) {
    val blocks = arrayOfNulls<UpdateBlock>(30)
    private val isPlayer = entity is PlayerEntity

    /**
     * Checks if this renderer should be updated for a certain viewer (see [UpdateBlock.needsUpdate])
     */
    fun needsUpdate(viewer: WorldPlayer): Boolean {
        for (block in blocks) {
            if (block == null) continue
            if (block.needsUpdate(viewer)) return true
        }
        return false
    }
}