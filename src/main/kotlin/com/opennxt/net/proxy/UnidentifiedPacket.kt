package com.opennxt.net.proxy

import com.opennxt.net.game.GamePacket
import com.opennxt.net.game.pipeline.OpcodeWithBuffer

data class UnidentifiedPacket(val packet: OpcodeWithBuffer): GamePacket