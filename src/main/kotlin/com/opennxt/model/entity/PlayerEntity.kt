package com.opennxt.model.entity

import com.opennxt.model.entity.movement.Movement
import com.opennxt.model.entity.player.appearance.PlayerModel
import com.opennxt.model.entity.rendering.EntityRenderer
import com.opennxt.model.world.TileLocation
import com.opennxt.model.world.WorldPlayer

class PlayerEntity(location: TileLocation) : LivingEntity(location) {
    override val renderer = EntityRenderer(this)
    override val movement = Movement(this)

    val model = PlayerModel(this)

    var controllingPlayer: WorldPlayer? = null

    override fun clean() {
        TODO("Clean")
    }
}