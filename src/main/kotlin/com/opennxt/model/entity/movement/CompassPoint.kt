package com.opennxt.model.entity.movement

enum class CompassPoint(val id: Int, val dx: Int, val dy: Int, val diagonal: Boolean) {
    NORTH(0, 0, 1, false),
    NORTHEAST(1, 1, 1, true),
    EAST(2, 1, 0, false),
    SOUTHEAST(3, 1, -1, true),
    SOUTH(4, 0, -1, false),
    SOUTHWEST(5, -1, -1, true),
    WEST(6, -1, 0, false),
    NORTHWEST(7, -1, 1, true);

    companion object {
        fun getById(id: Int): CompassPoint? {
            return when (id) {
                0 -> NORTH
                1 -> NORTHEAST
                2 -> EAST
                3 -> SOUTHEAST
                4 -> SOUTH
                5 -> SOUTHWEST
                6 -> WEST
                7 -> NORTHWEST
                else -> EAST
            }
        }

        fun forDelta(dx: Int, dy: Int): CompassPoint? {
            return when {
                dy >= 1 && dx >= 1 -> NORTHEAST
                dy <= -1 && dx >= 1 -> SOUTHEAST
                dy <= -1 && dx <= -1 -> SOUTHWEST
                dy >= 1 && dx <= -1 -> NORTHWEST
                dy >= 1 -> NORTH
                dx >= 1 -> EAST
                dy <= -1 -> SOUTH
                dx <= -1 -> WEST
                else -> null
            }
        }
    }
}