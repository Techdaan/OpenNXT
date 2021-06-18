package com.opennxt.net.proxy

import com.opennxt.model.entity.BasePlayer
import com.opennxt.model.entity.player.InterfaceManager
import com.opennxt.net.Side
import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.PacketRegistry
import com.opennxt.net.game.clientprot.ClientCheat
import com.opennxt.net.game.handlers.ClientCheatHandler
import com.opennxt.net.game.pipeline.GamePacketHandler
import com.opennxt.net.game.serverprot.RebuildNormal
import com.opennxt.net.game.serverprot.RunClientScript
import com.opennxt.net.game.serverprot.SetMapFlag
import com.opennxt.net.game.serverprot.ifaces.*
import com.opennxt.net.game.serverprot.variables.VarpLarge
import com.opennxt.net.game.serverprot.variables.VarpSmall
import com.opennxt.net.proxy.handler.*
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import mu.KotlinLogging
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.KClass

class ProxyPlayer(val proxyClient: ConnectedProxyClient, name: String) : BasePlayer(proxyClient.connection, name) {
    private val handlers =
        Object2ObjectOpenHashMap<KClass<out GamePacket>, GamePacketHandler<in BasePlayer, out GamePacket>>()
    private val logger = KotlinLogging.logger { }

    override val interfaces: InterfaceManager = InterfaceManager(this)

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
            (handler as GamePacketHandler<in BasePlayer, GamePacket>)?.handle(this, packet)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        plaintextDumpFile.flush()
        return true
    }
}