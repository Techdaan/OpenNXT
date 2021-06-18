package com.opennxt.model.entity.updating

import com.opennxt.OpenNXT
import com.opennxt.model.entity.Entity
import com.opennxt.model.entity.PlayerEntity
import com.opennxt.model.entity.movement.CompassPoint
import com.opennxt.model.entity.movement.MovementSpeed
import com.opennxt.model.entity.player.Viewport
import com.opennxt.model.entity.rendering.UpdateBlock
import com.opennxt.model.entity.rendering.UpdateBlockType
import com.opennxt.model.entity.rendering.blocks.AppearanceUpdateBlock
import com.opennxt.model.world.WorldPlayer
import com.opennxt.net.buf.DataType
import com.opennxt.net.buf.GamePacketBuilder
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.security.MessageDigest
import kotlin.math.abs

object PlayerInfoEncoder {
    val MAX_PLAYERS_PER_ADD = 25

    fun createBufferFor(player: WorldPlayer): ByteBuf {
        val outBuf = Unpooled.buffer()
        val blockBuf = Unpooled.buffer()

        val buffer = GamePacketBuilder(outBuf)
        val block = GamePacketBuilder(blockBuf)
        val viewport = player.viewport

        processLocalPlayers(player, viewport, buffer, block, true)
        processLocalPlayers(player, viewport, buffer, block, false)
        processOutsidePlayers(player, viewport, buffer, block, true)
        processOutsidePlayers(player, viewport, buffer, block, false)

        outBuf.writeBytes(blockBuf)
        blockBuf.release()

        return outBuf
    }

    private fun processLocalPlayers(player: WorldPlayer,
                                    viewport: Viewport,
                                    buffer: GamePacketBuilder,
                                    block: GamePacketBuilder,
                                    nsn0: Boolean) {
        val viewingDist = viewport.playerViewingDistance

        buffer.switchToBitAccess()
        var skipCount = 0
        for (index in 0 until viewport.localPlayerIndicesCount) {
            val id = viewport.localPlayerIndices[index]
            if (if (nsn0) (viewport.slotFlags[id].toInt() and 0x1) != 0
                else (viewport.slotFlags[id].toInt() and 0x1) == 0) {
                continue
            }

            if (skipCount > 0) {
                skipCount--
                viewport.slotFlags[id] = (viewport.slotFlags[id].toInt() or 0x2).toByte()
                continue
            }

            val target = viewport.localPlayers[id]
                ?: throw NullPointerException("Player disappeared from local players")
            if (needsRemove(player, viewport, target)) {
                buffer.putBits(1, 1)
                buffer.putBits(1, 0)
                buffer.putBits(2, 0)

                val regionHash = target.location.regionHash
                if (regionHash == viewport.regionHashes[id]) {
                    buffer.putBits(1, 0)
                } else {
                    buffer.putBits(1, 1)
                    appendRegionHash(buffer, target, viewport.regionHashes[id], regionHash)
                    viewport.regionHashes[id] = regionHash
                }

                viewport.localPlayers[id] = null
            } else {
                viewport.regionHashes[id] = target.location.regionHash
                val needsUpdate = needsMaskUpdate(player, viewport, target, block.writerIndex())
                if (needsUpdate) {
                    packUpdateBlock(player, viewport, target, block)
                }

                val movement = target.movement
                val speed = movement.currentSpeed
                val walkDirection = movement.nextWalkDirection
                val runDirection = movement.nextRunDirection

                if (walkDirection != null) {
                    buffer.putBits(1, 1) // Needs update
                    buffer.putBits(1, if (needsUpdate) 1 else 0) // Mask update

                    var dx = walkDirection.dx + (runDirection?.dx ?: 0)
                    var dy = walkDirection.dy + (runDirection?.dy ?: 0)

                    if (speed.id.toByte() != viewport.movementTypes[id]
                        && !(abs(dx) < 2 && abs(dy) < 2 && runDirection != null)) {
                        buffer.putBits(2, 3) // Teleport
                        buffer.putBits(1, 0) // Local

                        if (dx < 0) dx += 32
                        if (dy < 0) dy += 32

                        buffer.putBits(15, dy + (dx shl 5) + (0 shl 10) + ((speed.id + 1) shl 12))
                        viewport.movementTypes[id] = speed.id.toByte()
                    } else if ((dx == 0 && dy == 0)) {
                        buffer.putBits(2, 0)
                    } else if (runDirection == null) {
                        buffer.putBits(2, 1) // Walk opcode
                        buffer.putBits(3, getWalkOpcode(walkDirection.dx, walkDirection.dy))
                        buffer.putBits(1, 0)
                    } else if (abs(dx) < 2 && abs(dy) < 2) {
                        if (runDirection.diagonal || walkDirection.diagonal)
                            throw IllegalStateException("HELP WHAT THE FUCK DO I DO NOW")

                        val addedDx = runDirection.dx + walkDirection.dx
                        val addedDy = runDirection.dy + walkDirection.dy

                        buffer.putBits(2, 1) // Walk
                        buffer.putBits(3, getWalkOpcode(addedDx, addedDy))
                        buffer.putBits(1, 1)
                        buffer.putBits(2, when (walkDirection) {
                            CompassPoint.NORTH -> 0
                            CompassPoint.WEST -> 1
                            CompassPoint.EAST -> 2
                            CompassPoint.SOUTH -> 3
                            else -> throw IllegalArgumentException("Hmm idk")
                        })
                    } else {
                        buffer.putBits(2, 2) // Run opcode
                        buffer.putBits(4, getRunOpcode(dx, dy))
                    }
                } else if (speed == MovementSpeed.INSTANT) {
                    buffer.putBits(1, 1) // Needs update
                    buffer.putBits(1, if (needsUpdate) 1 else 0) // Mask update
                    buffer.putBits(2, 3) // No other updates

                    var xOffset = target.location.x - target.previousLocation.x
                    var yOffset = target.location.y - target.previousLocation.y
                    val planeOffset = target.location.plane - target.previousLocation.plane
                    if (abs(xOffset) < 16 && abs(yOffset) < 16) {
                        buffer.putBits(1, 0)
                        if (xOffset < 0) xOffset += 32
                        if (yOffset < 0) yOffset += 32
                        buffer.putBits(15, yOffset + (xOffset shl 5) + ((planeOffset and 0x3) shl 10) + (4 shl 12))
                    } else {
                        buffer.putBits(1, 1)
                        buffer.putBits(3, 4)
                        buffer.putBits(30, (yOffset and 0x3fff) + ((xOffset and 0x3fff) shl 14) + ((planeOffset and 0x3) shl 28))
                    }

                    viewport.movementTypes[id] = MovementSpeed.INSTANT.id.toByte()
                } else if (needsUpdate) {
                    buffer.putBits(1, 1) // Needs update
                    buffer.putBits(1, 1) // Mask update
                    buffer.putBits(2, 0) // No other updates
                } else {
                    buffer.putBits(1, 0) // No update needed
                    inner@ for (idx in index + 1 until viewport.localPlayerIndicesCount) {
                        val p2Index = viewport.localPlayerIndices[idx]
                        if (if (nsn0) (viewport.slotFlags[p2Index].toInt() and 0x1) != 0
                            else ((viewport.slotFlags[p2Index].toInt() and 0x1) == 0))
                            continue

                        val p2 = viewport.localPlayers[p2Index]!!
                        if (needsRemove(player, viewport, p2) || needsMaskUpdate(player, viewport, p2, block.writerIndex())
                            || p2.movement.nextWalkDirection != null || p2.movement.currentSpeed == MovementSpeed.INSTANT)
                            break@inner
                        skipCount++
                    }
                    putSkip(buffer, skipCount)
                    viewport.slotFlags[id] = (viewport.slotFlags[id].toInt() or 0x2).toByte()
                }
            }
        }
        buffer.switchToByteAccess()
    }

    private fun getWalkOpcode(dx: Int, dy: Int): Int = when {
        dx == -1 && dy == -1 -> 0
        dx == 0 && dy == -1 -> 1
        dx == 1 && dy == -1 -> 2
        dx == -1 && dy == 0 -> 3
        dx == 1 && dy == 0 -> 4
        dx == -1 && dy == 1 -> 5
        dx == 0 && dy == 1 -> 6
        dx == 1 && dy == 1 -> 7
        else -> throw IllegalArgumentException("No walk opcode for delta: $dx $dy")
    }

    private fun getRunOpcode(dx: Int, dy: Int): Int = when {
        dx == -2 && dy == -2 -> 0
        dx == -1 && dy == -2 -> 1
        dx == 0 && dy == -2 -> 2
        dx == 1 && dy == -2 -> 3
        dx == 2 && dy == -2 -> 4
        dx == -2 && dy == -1 -> 5
        dx == 2 && dy == -1 -> 6
        dx == -2 && dy == 0 -> 7
        dx == 2 && dy == 0 -> 8
        dx == -2 && dy == 1 -> 9
        dx == 2 && dy == 1 -> 10
        dx == -2 && dy == 2 -> 11
        dx == -1 && dy == 2 -> 12
        dx == 0 && dy == 2 -> 13
        dx == 1 && dy == 2 -> 14
        dx == 2 && dy == 2 -> 15
        else -> throw IllegalArgumentException("No run opcode for delta $dx $dy")
    }

    private fun packUpdateBlock(player: WorldPlayer, viewport: Viewport, target: PlayerEntity, block: GamePacketBuilder) {
        var maskData = 0x0
        var appearanceBlock: AppearanceUpdateBlock? = null
        if (needsAppearanceUpdate(viewport, target.index, target.model.hash, block.writerIndex())) {
            appearanceBlock = AppearanceUpdateBlock()
            maskData = maskData or appearanceBlock.type.playerMask
            viewport.cachedAppearanceHashes[target.index] = target.model.hash
        }
        // TODO HeadIconUpdate

        val blocks = target.renderer.blocks
        for (pos in 0 until blocks.size) {
            val current: UpdateBlock = blocks[pos] ?: continue
            maskData = maskData or current.type.playerMask
        }

        if (maskData >= 0xff) maskData = maskData or 0x40
        if (maskData >= 0xffff) maskData = maskData or 0x400

        block.put(DataType.SHORT, 0) // TODO Size of the update block
        block.put(DataType.BYTE, maskData)

        if (maskData >= 0xff) block.put(DataType.BYTE, maskData shr 8)
        if (maskData >= 0xffff) block.put(DataType.BYTE, maskData shr 16)
        if (maskData >= 0xffffff) block.put(DataType.BYTE, maskData shr 24)

        for (pos in 0 until blocks.size) {
            val current = blocks[pos]
            if (current == null) {
                if (appearanceBlock != null && pos == UpdateBlockType.APPEARANCE.playerPos)
                    appearanceBlock.encode(block, player, target)
                continue
            }
            current.encode(block, player, target)
        }
    }

    private fun needsRemove(player: WorldPlayer, viewport: Viewport, target: PlayerEntity): Boolean {
        if (target.index <= 0) return true
        if (!player.entity.location.withinDistance(target.location, viewport.playerViewingDistance)) return true
//        if (player.entity.location.getRegionKey() != target.location.getRegionKey()) return true // TODO Re-add
        return false
    }

    private fun needsAdd(player: WorldPlayer, viewport: Viewport, target: PlayerEntity): Boolean {
        if (target.index <= 0) return false
        if (viewport.localAddedPlayers > MAX_PLAYERS_PER_ADD) return false
        if (!player.entity.location.withinDistance(target.location, viewport.playerViewingDistance)) return false
//        if (player.entity.location.getRegionKey() != target.location.getRegionKey()) return false // TODO Re-add
        return true
    }

    private fun needsMaskUpdate(player: WorldPlayer, viewport: Viewport, target: PlayerEntity, blockSize: Int): Boolean {
        val appearanceUpdate = needsAppearanceUpdate(viewport, target.index, target.model.hash, blockSize)
        // TODO Icon update
        return appearanceUpdate || target.renderer.needsUpdate(player)
    }

    private fun needsAppearanceUpdate(viewport: Viewport, index: Int, hash: ByteArray, blockSize: Int): Boolean {
        if (blockSize > ((7500 - 500) / 2) || hash.isEmpty()) return false
        val cachedHash = viewport.cachedAppearanceHashes[index]
        return cachedHash == null || !MessageDigest.isEqual(cachedHash, hash)
    }

    private fun putSkip(buffer: GamePacketBuilder, skipCount: Int) {
        val type = when {
            skipCount == 0 -> 0
            skipCount > 255 -> 3
            skipCount > 31 -> 2
            else -> 1
        }
        buffer.putBits(2, type)
        if (skipCount > 0) {
            val bits = when {
                skipCount > 255 -> 11
                skipCount > 31 -> 8
                else -> 5
            }

            buffer.putBits(bits, skipCount)
        }
    }

    private fun processOutsidePlayers(player: WorldPlayer,
                                      viewport: Viewport,
                                      buffer: GamePacketBuilder,
                                      block: GamePacketBuilder,
                                      nsn2: Boolean) {
        buffer.switchToBitAccess()
        var skipCount = 0
        for (index in 0 until viewport.outPlayerIndicesCount) {
            val id = viewport.outPlayerIndices[index]
            if (if (nsn2) (viewport.slotFlags[id].toInt() and 0x1) == 0
                else (viewport.slotFlags[id].toInt() and 0x1) != 0)
                continue
            if (skipCount > 0) {
                skipCount--
                viewport.slotFlags[id] = (viewport.slotFlags[id].toInt() or 2).toByte()
                continue
            }

            val target = OpenNXT.world.getPlayer(id)
//                    ?: throw NullPointerException("Player not found (but should be added) in index: $id")
            if (target != null && needsAdd(player, viewport, target)) {
                buffer.putBits(1, 1)
                buffer.putBits(2, 0)
                val hash = target.location.regionHash
                if (hash == viewport.regionHashes[id]) {
                    buffer.putBits(1, 0)
                } else {
                    buffer.putBits(1, 1)
                    appendRegionHash(buffer, target, viewport.regionHashes[id], hash)
                    viewport.regionHashes[id] = hash
                }
                buffer.putBits(6, target.location.xInRegion)
                buffer.putBits(6, target.location.yInRegion)
                packUpdateBlock(player, viewport, target, block)
                buffer.putBits(1, 1)
                viewport.localPlayers[id] = target
                viewport.slotFlags[id] = (viewport.slotFlags[id].toInt() or 2).toByte()
            } else {
                val hash = target?.location?.regionHash ?: viewport.regionHashes[index]
                if (target != null && hash != viewport.regionHashes[id]) {
                    buffer.putBits(1, 1)
                    appendRegionHash(buffer, target, viewport.regionHashes[id], hash)
                    viewport.regionHashes[id] = hash
                } else {
                    buffer.putBits(1, 0)
                    for (idx in index + 1 until viewport.outPlayerIndicesCount) {
                        val p2Index = viewport.outPlayerIndices[idx]
                        if (if (nsn2) (viewport.slotFlags[p2Index].toInt() and 0x1) == 0
                            else (viewport.slotFlags[p2Index].toInt() and 0x1) != 0)
                            continue

                        val p2 = OpenNXT.world.getPlayer(p2Index)
                        if (p2 != null && (needsAdd(player, viewport, p2) || p2.location.regionHash != viewport.regionHashes[p2Index]))
                            break
                        skipCount++
                    }
                    putSkip(buffer, skipCount)
                    viewport.slotFlags[id] = (viewport.slotFlags[id].toInt() or 0x2).toByte()
                }
            }
        }
        buffer.switchToByteAccess()
    }

    private fun appendRegionHash(buf: GamePacketBuilder, entity: Entity, lastHash: Int, currentHash: Int) {
        val lastRegionX = lastHash shr 8 and 0xff
        val lastRegionY = 0xff and lastHash
        val lastPlane = lastHash shr 16 and 0x3
        val currentRegionX = currentHash shr 8 and 0xff
        val currentRegionY = 0xff and currentHash
        val currentPlane = currentHash shr 16 and 0x3
        val planeOffset = currentPlane - lastPlane
        if (lastRegionX == currentRegionX && lastRegionY == currentRegionY) {
            buf.putBits(2, 1)
            buf.putBits(2, planeOffset and 0x3)
        } else if (Math.abs(currentRegionX - lastRegionX) <= 1 && Math.abs(currentRegionY - lastRegionY) <= 1) {
            val opcode: Int
            val dx = currentRegionX - lastRegionX
            val dy = currentRegionY - lastRegionY
            if (dx == -1 && dy == -1) {
                opcode = 0
            } else if (dx == 1 && dy == -1) {
                opcode = 2
            } else if (dx == -1 && dy == 1) {
                opcode = 5
            } else if (dx == 1 && dy == 1) {
                opcode = 7
            } else if (dy == -1) {
                opcode = 1
            } else if (dx == -1) {
                opcode = 3
            } else if (dx == 1) {
                opcode = 4
            } else if (dy == 1) {
                opcode = 6
            } else {
                throw RuntimeException("Invalid delta value for region hash!")
            }
            buf.putBits(2, 2)
            buf.putBits(5, (planeOffset and 0x3 shl 3) + (opcode and 0x7))
        } else {
            val xOffset = currentRegionX - lastRegionX
            val yOffset = currentRegionY - lastRegionY
            buf.putBits(2, 3)
            buf.putBits(20, (yOffset and 0xff) + (xOffset and 0xff shl 8)
                    + (planeOffset and 0x3 shl 16) + ((entity.movement.speed.id + 1) shl 18))
        }
    }
}