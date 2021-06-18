package com.opennxt.model.entity.movement

enum class MovementSpeed(val id: Int) {
    STATIONARY(-1),
    CRAWL(0),
    WALK(1),
    RUN(2),
    INSTANT(3);

    companion object {
        private val VALUES = values()

        fun getById(id: Int): MovementSpeed? = VALUES.firstOrNull { it.id == id }
    }
}