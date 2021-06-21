package com.opennxt.model.entity

import com.opennxt.model.entity.movement.Movement
import com.opennxt.model.entity.rendering.EntityRenderer
import com.opennxt.model.world.TileLocation

/**
 * Anything that has a position is an entity.
 *
 * Entities that can take damage are LivingEntity s.
 *
 * Ground items, objects etc... are also entities.
 */
abstract class Entity(var location: TileLocation) {
    var index: Int = -1
    var previousLocation: TileLocation = location

    abstract val renderer: EntityRenderer
    abstract val movement: Movement

    abstract fun clean()
}