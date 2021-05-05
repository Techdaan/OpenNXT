package com.opennxt.model.lobby

import com.opennxt.OpenNXT
import com.opennxt.model.InterfaceHash
import com.opennxt.model.tick.Tickable
import com.opennxt.net.ConnectedClient
import com.opennxt.net.game.serverprot.ifaces.IfOpenSub
import com.opennxt.net.game.serverprot.ifaces.IfOpenTop
import com.opennxt.net.game.serverprot.variables.ResetClientVarcache
import com.opennxt.resources.config.enums.EnumDefinition
import mu.KotlinLogging

class LobbyPlayer(var client: ConnectedClient) : Tickable {
    private val logger = KotlinLogging.logger { }

    fun handleIncomingPackets() {
        val queue = client.incomingQueue
        while (true) {
            val packet = queue.poll() ?: return

            logger.info { "todo: handle incoming $packet" }
        }
    }

    fun added() {
        client.write(ResetClientVarcache)
        client.write(IfOpenTop(906))

        client.write(IfOpenSub(907, flag = true, parent = InterfaceHash(parent=906, component=65)))
        client.write(IfOpenSub(910, flag = true, parent = InterfaceHash(parent=906, component=66)))
        client.write(IfOpenSub(909, flag = true, parent = InterfaceHash(parent=906, component=67)))
        client.write(IfOpenSub(912, flag = true, parent = InterfaceHash(parent=906, component=69)))
        client.write(IfOpenSub(589, flag = true, parent = InterfaceHash(parent=906, component=68)))
        client.write(IfOpenSub(911, flag = true, parent = InterfaceHash(parent=906, component=70)))
        client.write(IfOpenSub(914, flag = true, parent = InterfaceHash(parent=906, component=127)))
        client.write(IfOpenSub(915, flag = true, parent = InterfaceHash(parent=906, component=128)))
        client.write(IfOpenSub(913, flag = true, parent = InterfaceHash(parent=906, component=129)))
        client.write(IfOpenSub(815, flag = true, parent = InterfaceHash(parent=906, component=136)))
        client.write(IfOpenSub(803, flag = true, parent = InterfaceHash(parent=906, component=131)))
        client.write(IfOpenSub(822, flag = true, parent = InterfaceHash(parent=906, component=132)))
        client.write(IfOpenSub(825, flag = true, parent = InterfaceHash(parent=906, component=114)))
        client.write(IfOpenSub(821, flag = true, parent = InterfaceHash(parent=906, component=115)))
        client.write(IfOpenSub(808, flag = true, parent = InterfaceHash(parent=906, component=113)))
        client.write(IfOpenSub(820, flag = true, parent = InterfaceHash(parent=906, component=133)))
        client.write(IfOpenSub(811, flag = true, parent = InterfaceHash(parent=906, component=130)))
        client.write(IfOpenSub(826, flag = true, parent = InterfaceHash(parent=906, component=82)))
        client.write(IfOpenSub(801, flag = true, parent = InterfaceHash(parent=906, component=36)))
    }

    override fun tick() {
        // TODO Do lobby players even need to be ticked?
    }
}