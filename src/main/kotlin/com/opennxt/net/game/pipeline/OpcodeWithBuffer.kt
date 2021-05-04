package com.opennxt.net.game.pipeline

import io.netty.buffer.ByteBuf

// only used for framing -> handler, really
data class OpcodeWithBuffer(val opcode: Int, val buf: ByteBuf)