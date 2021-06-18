package com.opennxt.model.entity.rendering

import com.opennxt.model.entity.Entity
import com.opennxt.model.world.WorldPlayer
import com.opennxt.net.buf.GamePacketBuilder

abstract class UpdateBlock(val type: UpdateBlockType) {
    abstract fun encode(buffer: GamePacketBuilder, viewer: WorldPlayer, entity: Entity)

    /**
     * Whether if a player needs to see a certain update or not. This is not always true:
     *
     * For example, with mining, the player can occasionally get hitsplats, which are only visible to the player that is
     * mining. Any other player should not have to see this.
     */
    fun needsUpdate(viewer: WorldPlayer): Boolean = true
}