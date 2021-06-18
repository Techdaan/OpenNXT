package com.opennxt.model.entity

import com.opennxt.model.entity.movement.Movement
import com.opennxt.model.entity.rendering.EntityRenderer
import com.opennxt.model.world.TileLocation

class PlayerEntity(location: TileLocation): LivingEntity(location) {
    override val renderer: EntityRenderer = EntityRenderer(this)
    override val movement: Movement = Movement(this)

    override fun clean() {
        TODO("Clean")
    }
}