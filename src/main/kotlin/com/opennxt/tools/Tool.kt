package com.opennxt.tools

import com.github.ajalt.clikt.core.CliktCommand
import mu.KotlinLogging

abstract class Tool(name: String, help: String) : CliktCommand(name = name, help = help) {
    protected val logger = KotlinLogging.logger { }

    override fun run() {
        logger.info { "Executing tool ${this::class.simpleName}" }
    }

    abstract fun runTool()

}