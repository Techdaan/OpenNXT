package com.opennxt.model.world

import com.opennxt.model.entity.BasePlayer
import com.opennxt.model.entity.PlayerEntity
import com.opennxt.net.ConnectedClient
import com.opennxt.net.GenericResponse
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.DynamicPacketHandler
import com.opennxt.net.game.pipeline.GamePacketEncoder
import com.opennxt.net.game.pipeline.GamePacketFraming
import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.login.LoginPacket
import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import mu.KotlinLogging
import kotlin.reflect.KClass

class WorldPlayer(client: ConnectedClient, val entity: PlayerEntity) : BasePlayer(client) {
    private val handlers =
        Object2ObjectOpenHashMap<KClass<out GamePacket>, GamePacketHandler<in BasePlayer, out GamePacket>>()
    private val logger = KotlinLogging.logger { }

    fun handleIncomingPackets() {
        val queue = client.incomingQueue
        while (true) {
            val packet = queue.poll() ?: return

            val handler = handlers[packet::class] as? GamePacketHandler<in BasePlayer, GamePacket>
            if (handler != null) {
                handler.handle(this, packet)
            } else {
                logger.info { "TODO: Handle incoming $packet" }
            }
        }
    }

    fun added() {
        client.channel.write(Unpooled.buffer(1).writeByte(GenericResponse.SUCCESSFUL.id))
        client.channel.writeAndFlush(
            LoginPacket.GameLoginResponse(
                byte0 = 0,
                rights = 2,
                byte2 = 0,
                byte3 = 0,
                byte4 = 0,
                byte5 = 0,
                byte6 = 0,
                playerIndex = entity.index,
                byte8 = 1,
                medium9 = 0,
                isMember = 1,
                username = "usernametodo",
                short12 = 0,
                int13 = 0
            )
        )

        client.channel.pipeline().replace("login-decoder", "game-decoder", GamePacketFraming())
        client.channel.pipeline().replace("login-encoder", "game-encoder", GamePacketEncoder())
        client.channel.pipeline().replace("login-handler", "game-handler", DynamicPacketHandler())

        // TODO Rebuild [region | dynamic] packet
    }
}