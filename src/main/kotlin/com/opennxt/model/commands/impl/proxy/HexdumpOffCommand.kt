package com.opennxt.model.commands.impl.proxy

import com.opennxt.model.commands.CommandException
import com.opennxt.model.commands.CommandSender
import com.opennxt.model.commands.SimpleCommand
import com.opennxt.net.proxy.ProxyPlayer

object HexdumpOffCommand: SimpleCommand() {
    override fun execute(sender: CommandSender, alias: String, command: String) {
        if (sender !is ProxyPlayer)
            throw CommandException("This command can only be executed from a proxied connection")

        sender.console("Disabled hexdumps")
        sender.proxyClient.hexdump = false
    }
}