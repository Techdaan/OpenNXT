package com.opennxt.model.entity.movement

import com.opennxt.model.entity.Entity
import com.opennxt.model.entity.PlayerEntity
import com.opennxt.model.world.TileLocation
import java.util.*

class Movement(val entity: Entity) {
    private val queue = LinkedList<TileLocation>()
    private val isPlayer = entity is PlayerEntity
    var speed = MovementSpeed.RUN
    var currentSpeed = MovementSpeed.STATIONARY
    var nextWalkDirection: CompassPoint? = null
    var nextRunDirection: CompassPoint? = null
    var teleportLocation: TileLocation? = null

    fun process() {

    }
}