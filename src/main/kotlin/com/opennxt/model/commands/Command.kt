package com.opennxt.model.commands

abstract class Command {
    abstract fun autocomplete(sender: CommandSender, alias: String, command: String): List<String>

    abstract fun execute(sender: CommandSender, alias: String, command: String)
}