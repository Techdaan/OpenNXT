package com.opennxt.tools.impl.cachedownloader

import com.opennxt.net.buf.BitBuf
import com.opennxt.net.handshake.HandshakeType
import com.opennxt.net.js5.packet.Js5Packet
import com.opennxt.net.js5.packet.Js5PacketCodec
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.handler.traffic.GlobalTrafficShapingHandler
import mu.KotlinLogging
import java.util.concurrent.Executors

object Js5ClientPipeline {
    private val executor = Executors.newScheduledThreadPool(4)
    private val trafficCounter = GlobalTrafficShapingHandler(executor, 250)

    fun getReadThroughput(): Long {
        return trafficCounter.trafficCounter().lastReadThroughput()
    }

    class Js5ClientChannelInitializer : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel) {
            val credentials = Js5Credentials.download()
            val client = Js5Client(credentials.version, credentials.token)
            ch.attr(Js5Client.ATTR_KEY).set(client)

            ch.pipeline().addLast("traffic-counter", trafficCounter)
            ch.pipeline().addLast("encoder", Js5ClientEncoder())
            ch.pipeline().addLast("decoder", Js5ClientDecoder(client))
            ch.pipeline().addLast("handler", Js5ClientHandler(client))
        }
    }

    class Js5ClientDecoder(private val client: Js5Client) : ByteToMessageDecoder() {
        override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
            client.lastRead = System.currentTimeMillis()

            if (!ctx.channel().isOpen || !client.state.canRead) {
                buf.skipBytes(buf.readableBytes())
                return
            }

            if (client.state == Js5ClientState.HANDSHAKE) {
                if (buf.readableBytes() < 1) return
                out.add(Js5PacketCodec.HandshakeResponse.decode(BitBuf(buf)))
                return
            }

            if (client.state == Js5ClientState.PREFETCHES) {
                if (buf.readableBytes() < 30 * 4) return
                out.add(Js5PacketCodec.Prefetches.decode(BitBuf(buf)))
                return
            }

            if (client.state == Js5ClientState.ACTIVE) {
                var request = client.current

                if (request == null) {
                    if (buf.readableBytes() < 5) return

                    val index = buf.readUnsignedByte().toInt().toLong()

                    val hash = buf.readInt().toLong()
                    val archive = hash and 0x7fffffff
                    val priority = (hash and -0x80000000L) == 0L

                    request = client.getRequest(priority, index.toInt(), archive.toInt())
                        ?: throw IllegalStateException("server sent file we were not requesting: [$priority, $index, $archive]")

                    client.current = request
                    request.offset = 5
                } else if (request.buffer == null) {
                    if (buf.readableBytes() < 5) return

                    val compression = buf.readUnsignedByte().toInt()
                    val fileSize = buf.readInt()

                    val size = fileSize + (if (compression == 0) 5 else 9) + (if (request.index == 255) 0 else 2)
                    val buffer = request.allocateBuffer(size)
                    buffer.put(compression.toByte())
                    buffer.putInt(fileSize)

                    request.offset = 10
                } else {
                    val buffer = request.buffer ?: throw IllegalStateException("buffer not initialized")
                    val totalSize = buffer.capacity() - (if (request.index == 255) 0 else 2)
                    var blockSize = 102400 - request.offset
                    val remaining = totalSize - buffer.position()

                    if (remaining < blockSize)
                        blockSize = remaining
                    if (buf.readableBytes() < blockSize)
                        blockSize = buf.readableBytes()

                    val block = ByteArray(blockSize)
                    buf.readBytes(block)
                    buffer.put(block)

                    request.offset += blockSize
                    if (buffer.position() == totalSize) {
                        buffer.flip()

                        if (!client.removeRequest(request))
                            throw IllegalStateException("failed to remove completed download from client request list")

                        request.notifyCompleted()

                        client.current = null
                    } else if (request.offset == 102400) {
                        request.offset = 0
                        client.current = null
                    }
                }
            }
        }
    }

    class Js5ClientEncoder : MessageToByteEncoder<Js5Packet>() {
        private val logger = KotlinLogging.logger {  }
        override fun encode(ctx: ChannelHandlerContext, msg: Js5Packet, out: ByteBuf) {
            when (msg) {
                is Js5Packet.Handshake -> {
                    out.writeByte(HandshakeType.JS_5.id)
                    Js5PacketCodec.Handshake.encode(msg, BitBuf(out))
                }
                is Js5Packet.RequestFile -> {
                    val opcode = if (msg.nxt) {
                        if (msg.priority) 33 else 32
                    } else {
                        if (msg.priority) 1 else 0
                    }

                    out.writeByte(opcode)
                    Js5PacketCodec.RequestFile.encode(msg, BitBuf(out))
                }
                is Js5Packet.Magic -> {
                    out.writeByte(6)
                    Js5PacketCodec.Magic.encode(msg, BitBuf(out))
                }
                is Js5Packet.LoggedIn -> {
                    out.writeByte(2)
                    Js5PacketCodec.LoggedIn.encode(msg, BitBuf(out))
                }
                is Js5Packet.LoggedOut -> {
                    out.writeByte(3)
                    Js5PacketCodec.LoggedOut.encode(msg, BitBuf(out))
                }
                else -> throw RuntimeException("idk what to do: $msg")
            }
        }
    }

    class Js5ClientHandler(private val client: Js5Client) : SimpleChannelInboundHandler<Js5Packet>() {
        private val logger = KotlinLogging.logger { }

        override fun channelRead0(ctx: ChannelHandlerContext, msg: Js5Packet) {
            when (msg) {
                is Js5Packet.HandshakeResponse -> {
                    if (msg.code != 0) {
                        logger.error { "Received code ${msg.code}. Closing" }
                        client.state = Js5ClientState.CRASHED
                        ctx.channel().close().sync()
                        return
                    }
                    client.state = Js5ClientState.PREFETCHES
                }
                is Js5Packet.Prefetches -> {
                    client.state = Js5ClientState.ACTIVE
                    ctx.channel().write(Js5Packet.Magic(5, client.version))
                    ctx.channel().write(Js5Packet.LoggedOut(client.version))
                    ctx.channel().flush()
                    client.notifyConnected()
                }
                else -> throw RuntimeException("idk how to handle: $msg")
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            client.state = Js5ClientState.CRASHED
            logger.warn { "Client handler crashed! Stack trace is below." }
            cause.printStackTrace()
            ctx.channel().close().sync()
        }
    }
}