package com.opennxt.model.commands

abstract class SimpleCommand: Command() {
    override fun autocomplete(sender: CommandSender, alias: String, command: String): Collection<String> = emptyList()
}