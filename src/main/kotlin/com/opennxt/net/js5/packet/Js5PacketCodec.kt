package com.opennxt.net.js5.packet

import com.opennxt.net.PacketCodec
import com.opennxt.net.buf.BitBuf

object Js5PacketCodec {
    object Magic : PacketCodec<Js5Packet.Magic> {
        override fun encode(packet: Js5Packet.Magic, buf: BitBuf) {
            buf.buffer.writeMedium(packet.value)
            buf.buffer.writeShort(packet.build)
            buf.buffer.writeShort(0)
            buf.buffer.writeShort(0)
        }

        override fun decode(buf: BitBuf): Js5Packet.Magic {
            return Js5Packet.Magic(
                buf.buffer.readMedium(),
                buf.buffer.readUnsignedShort()
            )
        }
    }

    object LoggedIn : PacketCodec<Js5Packet.LoggedIn> {
        override fun encode(packet: Js5Packet.LoggedIn, buf: BitBuf) {
            buf.buffer.writeByte(0)
            buf.buffer.writeInt(0)
            buf.buffer.writeShort(packet.build)
            buf.buffer.writeShort(0)
        }

        override fun decode(buf: BitBuf): Js5Packet.LoggedIn {
            buf.buffer.skipBytes(5)
            return Js5Packet.LoggedIn(buf.buffer.readUnsignedShort())
        }
    }

    object LoggedOut : PacketCodec<Js5Packet.LoggedOut> {
        override fun encode(packet: Js5Packet.LoggedOut, buf: BitBuf) {
            buf.buffer.writeByte(0)
            buf.buffer.writeInt(0)
            buf.buffer.writeShort(packet.build)
            buf.buffer.writeShort(0)
        }

        override fun decode(buf: BitBuf): Js5Packet.LoggedOut {
            buf.buffer.skipBytes(5)
            return Js5Packet.LoggedOut(buf.buffer.readUnsignedShort())
        }
    }

    object RequestTermination : PacketCodec<Js5Packet.LoggedIn> {
        override fun encode(packet: Js5Packet.LoggedIn, buf: BitBuf) {
            buf.buffer.writeByte(0)
            buf.buffer.writeInt(0)
            buf.buffer.writeShort(packet.build)
            buf.buffer.writeShort(0)
        }

        override fun decode(buf: BitBuf): Js5Packet.LoggedIn {
            buf.buffer.skipBytes(5)
            return Js5Packet.LoggedIn(buf.buffer.readUnsignedShort())
        }
    }

    object RequestFile : PacketCodec<Js5Packet.RequestFile> {
        override fun decode(buf: BitBuf): Js5Packet.RequestFile {
            return Js5Packet.RequestFile(
                false,
                buf.buffer.readUnsignedByte().toInt(),
                buf.buffer.readInt(),
                buf.buffer.readUnsignedShort()
            )
        }

        override fun encode(packet: Js5Packet.RequestFile, buf: BitBuf) {
            buf.buffer.writeByte(packet.index)
            buf.buffer.writeInt(packet.archive)
            buf.buffer.writeShort(packet.build)
            buf.buffer.writeShort(0)
        }
    }

    object Prefetches : PacketCodec<Js5Packet.Prefetches> {
        override fun encode(packet: Js5Packet.Prefetches, buf: BitBuf) {
            packet.prefetches.forEach { buf.buffer.writeInt(it) }
        }

        override fun decode(buf: BitBuf): Js5Packet.Prefetches {
            val prefetches = IntArray(30) { buf.buffer.readInt() }
            return Js5Packet.Prefetches(prefetches)
        }
    }

    object Handshake : PacketCodec<Js5Packet.Handshake> {
        override fun encode(packet: Js5Packet.Handshake, buf: BitBuf) {
            buf.buffer.writeByte(4 + 4 + packet.token.toByteArray(Charsets.US_ASCII).size + 1 + 1)
            buf.buffer.writeInt(packet.major)
            buf.buffer.writeInt(packet.minor)
            buf.buffer.writeBytes(packet.token.toByteArray(Charsets.US_ASCII))
            buf.buffer.writeByte(0) // c terminator for string
            buf.buffer.writeByte(packet.language)
        }

        override fun decode(buf: BitBuf): Js5Packet.Handshake {
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

    object HandshakeResponse : PacketCodec<Js5Packet.HandshakeResponse> {
        override fun encode(packet: Js5Packet.HandshakeResponse, buf: BitBuf) {
            buf.buffer.writeByte(packet.code)
        }

        override fun decode(buf: BitBuf): Js5Packet.HandshakeResponse {
            return Js5Packet.HandshakeResponse(buf.buffer.readUnsignedByte().toInt())
        }
    }

}