package com.opennxt.resources.map

import com.opennxt.ext.readSmartShort
import io.netty.buffer.ByteBuf

class TerrainData {
    val tiles = Array(4) { Array(66) { Array(66) { TerrainTile() } } }

    fun hasTerrainData(plane: Int): Boolean = tiles[plane].any { a -> a.any { !it.isEmpty() || it.idkByte != 0 } }

    operator fun get(plane: Int, x: Int, y: Int): TerrainTile {
        return tiles[plane][x + 1][y + 1]
    }

    data class TerrainTile(
        var underlayId: Int = -1,
        var b: Int = -1,
        var overlayId: Int = -1,
        var d: Int = -1,
        var e: Int = -1,
        var height: Int = -1,
        var overlayShape: Int = -1,
        var overlayRotation: Int = -1,
        var mask: Int = 0,
        var idkByte: Int = 0 // This byte is not read/used by the client, but is not always 0.
    ) {
        fun isEmpty(): Boolean = (mask and 0x1) == 0
    }

    fun decode(buf: ByteBuf) {
        while (buf.isReadable) {
            val plane = buf.readUnsignedByte().toInt()

            for (xRemaining in 66 downTo 1) {
                for (yRemaining in 1 until 67) {
                    val tile = get(plane, 65 - xRemaining, 65 - yRemaining)
                    tile.mask = buf.readUnsignedByte().toInt()

                    if (tile.mask == 0) {
                        tile.idkByte = buf.readUnsignedByte().toInt()
                        continue
                    }

                    if ((tile.mask and 0x10) != 0) {
                        tile.height = buf.readUnsignedShort()
                    } else {
                        tile.height = buf.readUnsignedByte().toInt()
                    }

                    val underlayId = buf.readSmartShort() - 1
                    if (underlayId != -1) {
                        tile.underlayId = underlayId
                        tile.b = buf.readUnsignedShort()
                    }
                }
            }
        }
    }

}