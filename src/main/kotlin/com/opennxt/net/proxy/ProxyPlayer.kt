package com.opennxt.net.proxy

import com.opennxt.model.commands.CommandSender
import com.opennxt.model.messages.Message
import com.opennxt.model.tick.Tickable
import com.opennxt.net.Side
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.PacketRegistry
import com.opennxt.net.game.clientprot.ClientCheat
import com.opennxt.net.game.handlers.ClientCheatHandler
import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.RunClientScript
import com.opennxt.net.game.serverprot.SetMapFlag
import com.opennxt.net.game.serverprot.ifaces.*
import com.opennxt.net.game.serverprot.variables.VarpLarge
import com.opennxt.net.game.serverprot.variables.VarpSmall
import com.opennxt.net.proxy.handler.*
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import mu.KotlinLogging
import java.io.BufferedWriter
import java.io.FileWriter
import java.nio.file.Files
import kotlin.reflect.KClass

class ProxyPlayer(val proxyClient: ConnectedProxyClient) : CommandSender, Tickable {
    private val handlers =
        Object2ObjectOpenHashMap<KClass<out GamePacket>, GamePacketHandler<*, out GamePacket>>()
    private val logger = KotlinLogging.logger { }

    val plaintextDumpFile: BufferedWriter

    init {
        handlers[ClientCheat::class] = ClientCheatHandler

        handlers[IfOpenTop::class] = IfOpenTopProxyHandler
        handlers[IfOpenSub::class] = IfOpenSubProxyHandler
        handlers[IfSetevents::class] = IfSetEventsHandler
        handlers[IfSettext::class] = IfSettextHandler
        handlers[IfSethide::class] = IfSethideHandler
        handlers[VarpSmall::class] = VarpSmallHandler
        handlers[VarpLarge::class] = VarpLargeHandler
        handlers[RunClientScript::class] = RunClientScriptHandler
        handlers[SetMapFlag::class] = SetMapFlagHandler

        val path = proxyClient.connection.dumper!!.file.parent.resolve("plaintext.log")
        if (!Files.exists(path.parent))
            Files.createDirectories(path.parent)
        if (!Files.exists(path))
            Files.createFile(path)
        plaintextDumpFile = BufferedWriter(FileWriter(path.toFile()))
    }

    fun handlePacket(packet: GamePacket): Boolean {
        val handler = handlers[packet::class]
        if (handler == null) {
            if (packet is UnidentifiedPacket) {
                val name = PacketRegistry.getRegistration(Side.SERVER, packet.packet.opcode)?.name ?: "null"
                plaintextDumpFile.appendLine("// $name - Unhandled/not decoded")
            } else {
                plaintextDumpFile.appendLine("// $packet")
            }
            plaintextDumpFile.flush()
            return false
        }

        try {
            (handler as GamePacketHandler<ProxyPlayer, GamePacket>)?.handle(this, packet)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        plaintextDumpFile.flush()
        return true
    }

    override fun hasPermissions(node: String): Boolean = false

    override fun message(message: Message) {
        proxyClient.connection.write(message.createPacket())
    }

    override fun message(message: String) {
        proxyClient.connection.write(Message.ConsoleMessage(message).createPacket())
    }

    override fun console(message: String) {
        proxyClient.connection.write(Message.ConsoleMessage(message).createPacket())
    }

    override fun error(message: String) {
        proxyClient.connection.write(Message.ConsoleError(message).createPacket())
    }

    override fun tick() {

    }
}