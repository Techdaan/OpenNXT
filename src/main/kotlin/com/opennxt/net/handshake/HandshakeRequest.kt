package com.opennxt.net.handshake

import com.opennxt.net.IncomingPacket
import com.opennxt.net.OutgoingPacket

data class HandshakeRequest(val type: HandshakeType): IncomingPacket, OutgoingPacket