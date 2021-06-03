package com.opennxt.model.proxy

import io.netty.buffer.ByteBuf
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class PacketDumper(file: Path) : AutoCloseable {
    var file: Path = file
        set(value) {
            if (open.get()) {
                throw IllegalStateException("Attempted to set path while file was already opened")
            }

            field = value
        }

    private val open = AtomicBoolean(false)
    private val lock = Any()

    private lateinit var stream: OutputStream

    private fun ensureOpen() {
        if (!open.get()) {
            if (!Files.exists(file.parent))
                Files.createDirectories(file.parent)

            Files.createFile(file)
            stream = Files.newOutputStream(file)

            open.set(true)
        }
    }

    fun dump(opcode: Int, data: ByteBuf) {
        val raw = ByteArray(data.readableBytes())
        data.markReaderIndex()
        data.readBytes(raw)
        data.resetReaderIndex()

        dump(opcode, raw)
    }

    fun dump(opcode: Int, data: ByteArray) {
        synchronized(lock) {
            ensureOpen()

            if (!open.get()) {
                throw IllegalStateException("Tried to write to closed file")
            }

            val toWrite = ByteArray(data.size + 14)

            val wrapper = ByteBuffer.wrap(toWrite)
            wrapper.putLong(Instant.now().toEpochMilli())
            wrapper.putShort(opcode.toShort())
            wrapper.putInt(data.size)
            wrapper.put(data)

            stream.write(toWrite)
        }
    }

    override fun close() {
        synchronized(lock) {
            open.set(false)

            stream.close()
        }
    }
}