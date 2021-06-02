package com.opennxt.net.js5.packet

import com.opennxt.net.PacketCodec
import com.opennxt.net.buf.GamePacketBuilder
import com.opennxt.net.buf.GamePacketReader

object Js5PacketCodec {
    object ConnectionInitialized : PacketCodec<Js5Packet.ConnectionInitialized> {
        val opcode = 6

        override fun encode(packet: Js5Packet.ConnectionInitialized, buf: GamePacketBuilder) {
            buf.buffer.writeMedium(packet.value)
            buf.buffer.writeShort(0)
            buf.buffer.writeShort(packet.build)
            buf.buffer.writeShort(0)
        }

        override fun decode(buf: GamePacketReader): Js5Packet.ConnectionInitialized {
            val magic = buf.buffer.readMedium()
            buf.buffer.skipBytes(2)
            val build = buf.buffer.readUnsignedShort()
            buf.buffer.skipBytes(2)
            return Js5Packet.ConnectionInitialized(magic, build)
        }
    }

    object LoggedIn : PacketCodec<Js5Packet.LoggedIn> {
        val opcode = 2

        override fun encode(packet: Js5Packet.LoggedIn, buf: GamePacketBuilder) {
            buf.buffer.writeMedium(5)
            buf.buffer.writeShort(0)
            buf.buffer.writeShort(packet.build)
            buf.buffer.writeShort(0)
        }

        override fun decode(buf: GamePacketReader): Js5Packet.LoggedIn {
            buf.buffer.skipBytes(5)
            val build = buf.buffer.readUnsignedShort()
            buf.buffer.skipBytes(2)
            return Js5Packet.LoggedIn(build)
        }
    }

    object LoggedOut : PacketCodec<Js5Packet.LoggedOut> {
        val opcode = 3

        override fun encode(packet: Js5Packet.LoggedOut, buf: GamePacketBuilder) {
            buf.buffer.writeMedium(5)
            buf.buffer.writeShort(0)
            buf.buffer.writeShort(packet.build)
            buf.buffer.writeShort(0)
        }

        override fun decode(buf: GamePacketReader): Js5Packet.LoggedOut {
            buf.buffer.skipBytes(5)
            val build = buf.buffer.readUnsignedShort()
            buf.buffer.skipBytes(2)
            return Js5Packet.LoggedOut(build)
        }
    }

    object RequestTermination : PacketCodec<Js5Packet.RequestTermination> {
        val opcode = 7

        override fun encode(packet: Js5Packet.RequestTermination, buf: GamePacketBuilder) {
            buf.buffer.writeByte(0)
            buf.buffer.writeInt(0)
            buf.buffer.writeShort(packet.build)
            buf.buffer.writeShort(0)
        }

        override fun decode(buf: GamePacketReader): Js5Packet.RequestTermination {
            buf.buffer.skipBytes(5)
            val build = buf.buffer.readUnsignedShort()
            buf.buffer.skipBytes(2)
            return Js5Packet.RequestTermination(build)
        }
    }

    object RequestFile : PacketCodec<Js5Packet.RequestFile> {
        val opcodeNxtLow = 32
        val opcodeLow = 0
        val opcodeHigh = 1
        val opcodeNxtHigh1 = 17
        val opcodeNxtHigh2 = 33

        override fun decode(buf: GamePacketReader): Js5Packet.RequestFile {
            val packet = Js5Packet.RequestFile(
                false,
                buf.buffer.readUnsignedByte().toInt(),
                buf.buffer.readInt(),
                buf.buffer.readUnsignedShort()
            )
            buf.buffer.skipBytes(2)
            return packet
        }

        override fun encode(packet: Js5Packet.RequestFile, buf: GamePacketBuilder) {
            buf.buffer.writeByte(packet.index)
            buf.buffer.writeInt(packet.archive)
            buf.buffer.writeShort(packet.build)
            buf.buffer.writeShort(0)
        }
    }

    object Prefetches : PacketCodec<Js5Packet.Prefetches> {
        override fun encode(packet: Js5Packet.Prefetches, buf: GamePacketBuilder) {
            packet.prefetches.forEach { buf.buffer.writeInt(it) }
        }

        override fun decode(buf: GamePacketReader): Js5Packet.Prefetches {
            val prefetches = IntArray(31) { buf.buffer.readInt() }
            return Js5Packet.Prefetches(prefetches)
        }
    }

    object Handshake : PacketCodec<Js5Packet.Handshake> {
        override fun encode(packet: Js5Packet.Handshake, buf: GamePacketBuilder) {
            buf.buffer.writeByte(4 + 4 + packet.token.toByteArray(Charsets.US_ASCII).size + 1 + 1)
            buf.buffer.writeInt(packet.major)
            buf.buffer.writeInt(packet.minor)
            buf.buffer.writeBytes(packet.token.toByteArray(Charsets.US_ASCII))
            buf.buffer.writeByte(0) // c terminator for string
            buf.buffer.writeByte(packet.language)
        }

        override fun decode(buf: GamePacketReader): Js5Packet.Handshake {
            val size = buf.buffer.readUnsignedByte().toInt()

            val tokenArr = ByteArray(size - 10)

            val major = buf.buffer.readInt()
            val minor = buf.buffer.readInt()
            buf.buffer.readBytes(tokenArr)
            buf.buffer.skipBytes(1)
            val language = buf.buffer.readUnsignedByte().toInt()

            return Js5Packet.Handshake(major, minor, tokenArr.toString(Charsets.US_ASCII), language)
        }
    }

    object XorRequest : PacketCodec<Js5Packet.XorRequest> {
        val opcode = 4

        override fun encode(packet: Js5Packet.XorRequest, buf: GamePacketBuilder) {
            buf.buffer.writeByte(packet.xor)
            buf.buffer.writeInt(0) // TODO Figure out which offset is xor
            buf.buffer.writeInt(0)
        }

        override fun decode(buf: GamePacketReader): Js5Packet.XorRequest {
            val xor = buf.buffer.readUnsignedByte().toInt()
            buf.buffer.skipBytes(8)
            return Js5Packet.XorRequest(xor)
        }
    }

    object HandshakeResponse : PacketCodec<Js5Packet.HandshakeResponse> {
        override fun encode(packet: Js5Packet.HandshakeResponse, buf: GamePacketBuilder) {
            buf.buffer.writeByte(packet.code)
        }

        override fun decode(buf: GamePacketReader): Js5Packet.HandshakeResponse {
            return Js5Packet.HandshakeResponse(buf.buffer.readUnsignedByte().toInt())
        }
    }

}