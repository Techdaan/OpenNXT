package com.opennxt.tools

import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands
import io.github.classgraph.ClassGraph
import mu.KotlinLogging
import kotlin.system.exitProcess

object ToolExecutor : NoRunCliktCommand(
    name = "run-tool",
    help = "Executes a tool bundled in the server"
) {
    private val logger = KotlinLogging.logger {}

    init {
        val result = ClassGraph()
            .enableClassInfo()
            .acceptPackages("com.opennxt.tools.impl")
            .scan()

        val classes = result.getSubclasses("com.opennxt.tools.Tool")

        val tools = classes.map { it.loadClass().newInstance() as Tool }

        if (tools.isEmpty()) {
            logger.error { "No bundled tools found" }
            exitProcess(1)
        }

        subcommands(tools)
    }

}