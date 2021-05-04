package com.opennxt.model.world

import kotlin.math.abs

class TileLocation(
    x: Int,
    y: Int,
    plane: Int = 0
) : Cloneable {

    var x: Int = x
        internal set
    var y: Int = y
        internal set
    var plane: Int = plane
        internal set

    val chunkX: Int get() = x shr 3
    val chunkY: Int get() = y shr 3
    val regionX: Int get() = x shr 6
    val regionY: Int get() = y shr 6
    val xInRegion: Int get() = x and 0x3f
    val yInRegion: Int get() = y and 0x3f
    val tileHash: Int get() = y + (x shl 14) + (plane shl 28)
    val regionHash: Int get() = regionY + (regionX shl 8) + (plane shl 16)
    val regionId: Int get() = regionY + (regionX shl 8)

    constructor(localX: Int, localY: Int, plane: Int, regionId: Int) : this(
        x = localX + (regionId shr 8 and 0xff shl 6),
        y = localY + (regionId and 0xff shl 6),
        plane = plane
    )

    fun getLocalX(other: TileLocation = this, size: MapSize = MapSize.SIZE_104): Int {
        return x - 8 * (other.chunkX - (size.size shr 4))
    }

    fun getLocalY(other: TileLocation = this, size: MapSize = MapSize.SIZE_104): Int {
        return y - 8 * (other.chunkY - (size.size shr 4))
    }

    fun withinDistance(other: TileLocation, distanceX: Int, distanceY: Int): Boolean {
        if (other.plane != plane) return false
        return abs(other.x - x) <= distanceX && abs(other.y - y) <= distanceY
    }

    fun withinDistance(other: TileLocation, distance: Int): Boolean = withinDistance(other, distance, distance)

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is TileLocation) return false
        if (other.x != x) return false
        if (other.y != y) return false
        if (other.plane != plane) return false
        return true
    }

    override fun hashCode(): Int = tileHash

    override fun toString(): String = "TileLocation(x=$x, y=$y, plane=$plane)"

    fun distSquared(other: TileLocation): Int {
        if (other.plane != plane)
            return Int.MAX_VALUE
        val dx = other.x - x
        val dy = other.y - y
        return (dx * dx) + (dy * dy)
    }
}