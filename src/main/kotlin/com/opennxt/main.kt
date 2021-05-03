package com.opennxt

import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.opennxt.tools.ToolExecutor

fun main(args: Array<String>) {
    NoRunCliktCommand(name = "open-nxt", help = "Base command for the OpenNXT server")
        .subcommands(OpenNXT, ToolExecutor)
        .main(args)
}