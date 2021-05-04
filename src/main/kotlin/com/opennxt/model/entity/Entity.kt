package com.opennxt.model.entity

import com.opennxt.model.world.TileLocation

/**
 * Anything that has a position is an entity.
 *
 * Entities that can take damage are LivingEntity s.
 *
 * Ground items, objects etc... are also entities.
 */
abstract class Entity(var location: TileLocation)