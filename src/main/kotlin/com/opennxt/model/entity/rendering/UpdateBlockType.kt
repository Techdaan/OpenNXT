package com.opennxt.model.entity.rendering

enum class UpdateBlockType(
    val playerMask: Int,
    val playerPos: Int,
    val npcMask: Int = 0,
    val npcPos: Int = -1
) {
    APPEARANCE(0x1, 3)
}