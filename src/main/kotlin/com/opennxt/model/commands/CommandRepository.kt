package com.opennxt.model.commands

import com.opennxt.model.commands.impl.proxy.HexdumpOffCommand
import com.opennxt.model.commands.impl.proxy.HexdumpOnCommand
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import mu.KotlinLogging

class CommandRepository {
    private val logger = KotlinLogging.logger {  }

    val commands = Object2ObjectOpenHashMap<String, Command>()

    init {
        commands["hexdump-on"] = HexdumpOnCommand
        commands["hexdump-off"] = HexdumpOffCommand

        logger.info { "Registered ${commands.size} commands" }
    }

    fun complete(sender: CommandSender, input: String): Collection<String> {
        val split = input.split(" ", limit = 2)
        val commandName = split[0].toLowerCase()

        val match = commands[commandName]
        if (match != null) {
            return match.autocomplete(sender, split[0], if (split.size == 1) "" else split[1])
        }

        if (split.size == 1)
            return commands.keys.filter { it.startsWith(input.toLowerCase()) }

        throw CommandException("Could not find a command named '${commandName}'")
    }

    fun execute(sender: CommandSender, input: String) {
        val split = input.split(" ", limit = 2)
        val commandName = split[0].toLowerCase()

        val match = commands[commandName] ?: throw CommandException("Command not found: '$commandName'")

        match.execute(sender, split[0], if (split.size == 1) "" else split[1])
    }
}