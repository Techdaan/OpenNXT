package com.opennxt.net.game.pipeline

import com.opennxt.net.PacketCodec
import com.opennxt.net.game.GamePacket

interface GamePacketCodec<T : GamePacket> : PacketCodec<T>