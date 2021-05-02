package com.opennxt.tools.impl

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.google.common.collect.Sets
import com.opennxt.Constants
import com.opennxt.ext.getCrc32
import com.opennxt.ext.toByteArray
import com.opennxt.filesystem.ChecksumTable
import com.opennxt.filesystem.Container
import com.opennxt.filesystem.Filesystem
import com.opennxt.filesystem.ReferenceTable
import com.opennxt.filesystem.sqlite.SqliteFilesystem
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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class CacheDownloader : Tool("cache-downloader", "Updates / downloads the cache from Jagex' JS5 servers") {
    val ip by option(help = "Live js5 server ip").default("content.runescape.com")
    val port by option(help = "Live js5 server port").int().default(43594)
    val connections by option(help = "The amount of concurrent connections").int().default(1)

    val bootstrap = Bootstrap()
    val workerGroup = NioEventLoopGroup(8)
    lateinit var cache: Filesystem

    private object Js5ClientWorkerThread : Thread("js5-client-worker") {
        private val logger = KotlinLogging.logger { }
        lateinit var tool: CacheDownloader

        var table: ChecksumTable? = null

        val contexts = Sets.newConcurrentHashSet<Js5ConnectionContext>()
        val running = AtomicBoolean(true)

        val toRequest = ConcurrentLinkedQueue<FileRequest>()

        val existingTables = arrayOfNulls<ReferenceTable>(255)
        val updatingTables = arrayOfNulls<ReferenceTable>(255)

        val pendingTables = HashSet<Int>()

        fun setupTables() {
            logger.info { "Setting up tables / files to request" }
            table!!.entries.forEachIndexed { index, entry ->
                // check if index is used or not
                if (entry.crc == 0 && entry.version == 0) return@forEachIndexed

                // check if we already have a reference table, if not, add request and check next table
                val existingRaw = tool.cache.readReferenceTable(index)
                if (existingRaw == null) {
                    logger.info { "Reference table for index $index missing - adding to downloads..." }
                    pendingTables += index
                    toRequest.add(FileRequest(true, 255, index))
                    return@forEachIndexed
                }

                // check crc of existing table
                val crc = existingRaw.getCrc32()
                if (entry.crc != crc) {
                    logger.info { "CRC mismatch in reference table for index $index - adding to downloads..." }
                    pendingTables += index
                    toRequest.add(FileRequest(true, 255, index))
                    return@forEachIndexed
                }

                // decode existing table
                val existing = ReferenceTable(tool.cache, index)
                existing.decode(ByteBuffer.wrap(Container.decode(existingRaw).data))
                existingTables[index] = existing

                // check version
                if (existing.version != entry.version) {
                    logger.info { "Version mismatch in reference table for index $index - adding to downloads..." }
                    pendingTables += index
                    toRequest.add(FileRequest(true, 255, index))
                    return@forEachIndexed
                }

                // we're good
                logger.info { "Existing table for index $index is up-to-date" }
            }

            if (pendingTables.isEmpty()) {
                logger.info { "All reference tables are up-to-date!" }
            } else {
                logger.info { "${pendingTables.size} reference tables require an update" }
            }
        }

        fun Js5ConnectionContext.process() {
            if (state != Js5ClientState.ACTIVE) return

            synchronized(lock) {
                // start downloads of pending files in this client
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

                // process all downloaded files
                while (downloaded.size > 0) {
                    val it = downloaded.iterator()
                    while (it.hasNext()) {
                        val (_, file) = it.next()

                        val buffer = file.buffer!!
                        if (file.index == 255 && file.archive == 255) {
                            val table = ChecksumTable.decode(ByteBuffer.wrap(Container.decode(buffer).data))
                            this.table = table

                            logger.info { "Received checksum table with ${table.entries.size} entries" }
                            if (table.entries.size > tool.cache.numIndices()) {
                                logger.info { "Need to expand cache! Got ${tool.cache.numIndices()} indices, need ${table.entries.size}" }

                                for (i in tool.cache.numIndices() until table.entries.size) {
                                    tool.cache.createIndex(i)
                                    logger.info { "Creating index $i in the cache" }
                                }
                            }

                            if (Js5ClientWorkerThread.table == null) {
                                Js5ClientWorkerThread.table = table
                                setupTables()
                            } else if (Js5ClientWorkerThread.table != table) {
                                logger.error { "Checksum tables between requests did not match" }
                                exitProcess(1)
                            }
                        } else if (file.index == 255) {
                            val referenceTable = ReferenceTable(tool.cache, file.archive)
                            val crc = buffer.getCrc32()
                            val container = Container.decode(buffer)
                            referenceTable.decode(ByteBuffer.wrap(container.data))
                            val entry = table!!.entries[file.archive]
                            if (crc != entry.crc) {
                                logger.error { "Downloaded reference table ${file.archive} CRC mismatch" }
                                exitProcess(1)
                            }
                            if (referenceTable.version != entry.version) {
                                logger.error { "Downloaded reference table ${file.archive} version mismatch" }
                                exitProcess(1)
                            }
                            updatingTables[file.archive] = referenceTable
                            pendingTables.remove(file.archive)
                            buffer.position(0)
                            tool.cache.writeReferenceTable(file.archive, buffer.toByteArray(), container.version, crc)
                            logger.info { "Finished downloading reference table for index ${file.archive}. Waiting for ${pendingTables.size} more tables..." }
                        } else {
                            logger.info { "Finished downloading file ${file}, but the result is unhandled!" }
                        }

                        it.remove()
                    }
                }

                // push new files to connections
                while (pending.size + downloading.size < 20 && toRequest.isNotEmpty()) {
                    val request = toRequest.poll() ?: break

                    pending += request
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
        Js5ClientWorkerThread.tool = this

        logger.info { "Starting download from $ip:$port with $connections concurrent connections" }

        logger.info { "Opening cache from ${Constants.CACHE_PATH}" }
        cache = SqliteFilesystem(Constants.CACHE_PATH)

        logger.info { "Setting up Netty bootstrap" }
        bootstrap.group(workerGroup)
        bootstrap.channel(NioSocketChannel::class.java)

        Js5ClientWorkerThread.start()

        for (i in 0 until connections) {
            logger.info { "Opening connection js5 $i/$connections" }
            val config = ClientConfig.download("https://world5.runescape.com/jav_config.ws", BinaryType.WIN64)
            val version = config["server_version"] ?: throw NullPointerException("server_version not found")
            val token = config.getJs5Token()

            val context = Js5ConnectionContext(version.toInt(), token)
            bootstrap.handler(Js5ClientChannelInitializer(context))
            context.channel = bootstrap.connect(ip, port).sync().channel()
            context.channel.writeAndFlush(Js5Packet.Handshake(version.toInt(), 1, token, 0))

            Js5ClientWorkerThread.contexts += context
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
        var table: ChecksumTable? = null

        fun request(priority: Boolean, index: Int, archive: Int) {
            synchronized(lock) {
                pending += FileRequest(priority, index, archive)
            }
        }
    }

    private data class FileRequest(val priority: Boolean, val index: Int, val archive: Int) {
        var buffer: ByteBuffer? = null
        var offset = 0
        var size = 0

        fun allocate(size: Int) {
            this.size = size
            this.buffer = ByteBuffer.allocate(size)
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
                    val archive = buf.readInt().toLong() and 0x7fffffff

                    val newRequest = synchronized(context.lock) {
                        context.downloading[(index shl 32) or archive]
                    } ?: throw IllegalStateException("server sent file we aren't asking for? $index, $archive")

                    newRequest.offset = 5
                    context.currentRequest = newRequest
                } else if (request.buffer == null) {
                    if (buf.readableBytes() < 5) return

                    val compression = buf.readUnsignedByte().toInt()
                    val fileSize = buf.readInt()

                    val size = fileSize + (if (compression == 0) 5 else 9) + (if (request.index == 255) 0 else 2)
                    request.allocate(size)
                    val reqBuffer = request.buffer!!
                    reqBuffer.put(compression.toByte())
                    reqBuffer.putInt(fileSize)

                    request.offset = 10
                } else {
                    val buffer = request.buffer!!
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
                    context.state = Js5ClientState.ACTIVE
                    context.channel.write(Js5Packet.Magic(5, context.version))
                    context.channel.write(Js5Packet.LoggedOut(context.version))
                    context.channel.flush()
                    context.request(true, 255, 255)
                }
                else -> throw RuntimeException("idk what to handle $msg")
            }
        }
    }

    private class Js5ClientEncoder(val context: Js5ConnectionContext) : MessageToByteEncoder<Packet>() {
        override fun encode(ctx: ChannelHandlerContext, msg: Packet, out: ByteBuf) {
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