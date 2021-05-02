package com.opennxt.tools.impl

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.google.common.collect.Sets
import com.opennxt.filesystem.ChecksumTable
import com.opennxt.filesystem.Container
import com.opennxt.model.files.BinaryType
import com.opennxt.model.files.ClientConfig
import com.opennxt.net.Packet
import com.opennxt.net.buf.BitBuf
import com.opennxt.net.handshake.HandshakeType
import com.opennxt.net.js5.packet.Js5Packet
import com.opennxt.net.js5.packet.Js5PacketCodec
import com.opennxt.tools.Tool
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import mu.KotlinLogging
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class CacheDownloader : Tool("cache-downloader", "Updates / downloads the cache from Jagex' JS5 servers") {
    val ip by option(help = "Live js5 server ip").default("content.runescape.com")
    val port by option(help = "Live js5 server port").int().default(43594)
    val connections by option(help = "The amount of concurrent connections").int().default(1)

    val bootstrap = Bootstrap()
    val workerGroup = NioEventLoopGroup(8)

    private object Js5ClientWorkerThread : Thread("js5-client-worker") {
        private val logger = KotlinLogging.logger {  }

        val contexts = Sets.newConcurrentHashSet<Js5ConnectionContext>()
        val running = AtomicBoolean(true)

        fun Js5ConnectionContext.process() {
            if (state != Js5ClientState.ACTIVE) return

            synchronized(lock) {
                while (downloading.size < 20 && pending.isNotEmpty()) {
                    val request = pending.poll()
                    downloading[request.hash()] = request

                    channel.writeAndFlush(
                        Js5Packet.RequestFile(
                            request.priority,
                            request.index,
                            request.archive,
                            version,
                            true
                        )
                    )
                }

                while (downloaded.size > 0) {
                    val it = downloaded.iterator()
                    while (it.hasNext()) {
                        val (hash, file) = it.next()

                        if (file.index == 255 && file.archive == 255) {
                            val raw = ByteArray(file.buffer!!.readableBytes())
                            file.buffer!!.readBytes(raw)
                            file.buffer!!.release()

                            val table = ChecksumTable.decode(ByteBuffer.wrap(Container.decode(ByteBuffer.wrap(raw)).data))

                            logger.info { "Checksum Table" }
                            table.entries.forEachIndexed { i, entry ->
                                logger.info { "$i = $entry" }
                            }
                        }

                        it.remove()
                    }
                }
            }
        }

        override fun run() {
            while (running.get()) {
                contexts.forEach { it.process() }
                sleep(200)
            }
        }
    }

    override fun runTool() {
        logger.info { "Starting download from $ip:$port with $connections concurrent connections" }

        logger.info { "Setting up Netty bootstrap" }
        bootstrap.group(workerGroup)
        bootstrap.channel(NioSocketChannel::class.java)

        Js5ClientWorkerThread.start()

        for (i in 0 until connections) {
            logger.info { "Opening connection $i/$connections" }
            val config = ClientConfig.download("https://world5.runescape.com/jav_config.ws", BinaryType.WIN64)
            val version = config["server_version"] ?: throw NullPointerException("server_version not found")
            val token = config.getJs5Token()

            logger.info { "Terminating early but we had $token on $version" }
            val context = Js5ConnectionContext(version.toInt(), token)
            bootstrap.handler(Js5ClientChannelInitializer(context))
            context.channel = bootstrap.connect(ip, port).sync().channel()
            context.channel.writeAndFlush(Js5Packet.Handshake(version.toInt(), 1, token, 0))

            Js5ClientWorkerThread.contexts += context
            logger.info { "sync'd" }
        }
    }

    private class Js5ConnectionContext(
        val version: Int,
        val token: String,
        var state: Js5ClientState = Js5ClientState.HANDSHAKE
    ) {
        lateinit var channel: Channel
        val lock = Any()
        var disconnected = false
        val pending = LinkedList<FileRequest>() // TODO Obtain lock
        val downloading = Long2ObjectOpenHashMap<FileRequest>() // TODO Obtain lock
        var downloaded = Long2ObjectOpenHashMap<FileRequest>() // TODO Obtain lock
        var currentRequest: FileRequest? = null
        var lastRead = System.currentTimeMillis()

        fun request(priority: Boolean, index: Int, archive: Int) {
            synchronized(lock) {
                pending += FileRequest(priority, index, archive)
            }
        }
    }

    private data class FileRequest(val priority: Boolean, val index: Int, val archive: Int) {
        var buffer: ByteBuf? = null // TODO port this to ByteBuffer
        var offset = 0
        var size = 0

        fun allocate(size: Int) {
            this.size = size
            this.buffer = Unpooled.buffer(size)
        }

        fun hash(): Long {
            return ((index.toLong() shl 32) or archive.toLong())
        }
    }

    private enum class Js5ClientState {
        HANDSHAKE,
        PREFETCHES,
        ACTIVE
    }

    private class Js5ClientChannelInitializer(val context: Js5ConnectionContext) :
        ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel) {
            ch.pipeline().addLast("encoder", Js5ClientEncoder(context))
            ch.pipeline().addLast("decoder", Js5ClientDecoder(context))
            ch.pipeline().addLast("handler", Js5ClientHandler(context))
        }
    }

    private class Js5ClientDecoder(val context: Js5ConnectionContext) : ByteToMessageDecoder() {
        private val logger = KotlinLogging.logger { }

        override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
            context.lastRead = System.currentTimeMillis()
            logger.info { "${context.state}, ${buf.readableBytes()}" }

            if (!ctx.channel().isOpen || context.disconnected) return

            if (context.state == Js5ClientState.HANDSHAKE) {
                if (buf.readableBytes() < 1) return
                out.add(Js5PacketCodec.HandshakeResponse.decode(BitBuf(buf)))
                return
            }

            if (context.state == Js5ClientState.PREFETCHES) {
                if (buf.readableBytes() < 30 * 4) return
                out.add(Js5PacketCodec.Prefetches.decode(BitBuf(buf)))
                return
            }

            if (context.state == Js5ClientState.ACTIVE) {
                val request = context.currentRequest
                if (request == null) {
                    if (buf.readableBytes() < 5) return

                    val index = buf.readUnsignedByte().toInt().toLong()
                    val archive = buf.readInt().toLong()

                    val newRequest = synchronized(context.lock) {
                        context.downloading[(index shl 32) or archive]
                    } ?: throw IllegalStateException("server sent file we aren't asking for? $index, $archive")

                    context.currentRequest = newRequest
                } else if (request.buffer == null) {
                    if (buf.readableBytes() < 5) return

                    val compression = buf.readUnsignedByte().toInt()
                    val fileSize = buf.readInt()

                    val size = fileSize + (if (compression == 0) 5 else 9) + (if (request.index == 255) 0 else 2)
                    request.allocate(size)
                    val reqBuffer = request.buffer!!
                    reqBuffer.writeByte(compression)
                    reqBuffer.writeInt(fileSize)

                    request.offset = 10
                } else {
                    val buffer = request.buffer!!
                    val totalSize = buffer.capacity() - (if (request.index == 255) 0 else 2)
                    var blockSize = 102400 - request.offset
                    val remaining = totalSize - buffer.writerIndex()
                    if (remaining < blockSize)
                        blockSize = remaining
                    if (buf.readableBytes() < blockSize)
                        blockSize = buf.readableBytes()

                    buf.readBytes(request.buffer!!, blockSize)
                    request.offset += blockSize
                    if (buffer.writerIndex() == totalSize) {
                        synchronized(context.lock) {
                            context.downloaded[request.hash()] = request
                            if (context.downloading.remove(request.hash()) == null) {
                                throw NullPointerException("we just finished a file that we were not downloading?")
                            }
                            context.currentRequest = null
                        }
                    } else if (request.offset == 102400) {
                        request.offset = 0
                        context.currentRequest = null
                    }
                }
            }
        }
    }

    private class Js5ClientHandler(val context: Js5ConnectionContext) : SimpleChannelInboundHandler<Packet>() {
        private val logger = KotlinLogging.logger { }

        override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
            when (msg) {
                is Js5Packet.HandshakeResponse -> {
                    logger.info { "Received handshake response $msg" }
                    if (msg.code != 0) {
                        logger.error { "Received code ${msg.code}. Closing" }
                        context.disconnected = true
                        ctx.channel().close().sync()
                        return
                    }
                    context.state = Js5ClientState.PREFETCHES
                }
                is Js5Packet.Prefetches -> {
                    logger.info { "Prefetches: $msg" }
                    logger.info { "Switched to ACTIVE state" }
                    context.channel.writeAndFlush(Js5Packet.Magic(5, context.version))
                    context.channel.writeAndFlush(Js5Packet.LoggedIn(context.version))
                    context.request(true, 255, 255)
                    context.state = Js5ClientState.ACTIVE
                }
                else -> throw RuntimeException("idk what to handle $msg")
            }
        }
        /*


            val buf = Unpooled.buffer()
            buf.writeByte(HandshakeType.JS_5.id)
            buf.writeByte(42) // size
            buf.writeInt(version)
            buf.writeInt(1)
            buf.writeBytes(token.toByteArray(Charsets.US_ASCII))
            buf.writeByte(0)
            buf.writeByte(0)
            ch.writeAndFlush(buf)
         */
    }

    private class Js5ClientEncoder(val context: Js5ConnectionContext) : MessageToByteEncoder<Packet>() {
        override fun encode(ctx: ChannelHandlerContext, msg: Packet, out: ByteBuf) {
            println("encode $msg")
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

}