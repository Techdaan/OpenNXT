package com.opennxt.net.game.handlers

import com.opennxt.OpenNXT
import com.opennxt.model.commands.CommandException
import com.opennxt.model.commands.CommandSender
import com.opennxt.model.messages.Message
import com.opennxt.net.game.clientprot.ClientCheat
import com.opennxt.net.game.pipeline.GamePacketHandler

object ClientCheatHandler : GamePacketHandler<CommandSender, ClientCheat> {
    override fun handle(context: CommandSender, packet: ClientCheat) {
        try {
            if (packet.tabbed) {
                val completions = OpenNXT.commands.complete(context, packet.cheat)

                if (completions.size == 1) {
                    context.message(Message.ConsoleAutocomplete(completions.first()))
                    return
                }

                context.console("TODO: CONSOLE_FEEDBACK packet support")
                // TODO Send CONSOLE_FEEDBACK packet here
            } else {
                OpenNXT.commands.execute(context, packet.cheat)
            }
        } catch (e: CommandException) {
            context.error("Command exception occurred: ${e.message}")
        } catch (e: Exception) {
            context.error("Uncaught internal exception occurred: ${e.message}")
            e.printStackTrace()
        }
    }
}