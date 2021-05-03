package com.opennxt.tools.impl.cachedownloader

import com.opennxt.ext.getCrc32
import com.opennxt.filesystem.Filesystem
import com.opennxt.util.Whirlpool
import java.io.Closeable
import java.lang.Thread.sleep
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class AsyncFilesystemAccessor(val filesystem: Filesystem) : Runnable, Closeable {

    private val running = AtomicBoolean(true)
    private val operations = LinkedList<IOOperation<*>>()

    fun pendingOperations(): Int = operations.size

    fun write(request: Js5RequestHandler.ArchiveRequest): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        operations.addLast(IOOperation.WriteRequestOperation(request, future))
        return future
    }

    fun write(index: Int, archive: Int, data: ByteArray, version: Int, crc: Int): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        operations.addLast(IOOperation.WriteOperation(index, archive, data, version, crc, future))
        return future
    }

    fun read(index: Int, archive: Int): CompletableFuture<ByteBuffer?> {
        val future = CompletableFuture<ByteBuffer?>()
        operations.addLast(IOOperation.ReadOperation(index, archive, future))
        return future
    }

    override fun close() {
        running.set(false)
    }

    override fun run() {
        try {
            while (running.get()) {
                val operation = operations.pollFirst()
                if (operation == null) {
                    sleep(10)
                    continue
                }

                try {
                    when (operation) {
                        is IOOperation.ReadOperation -> {
                            val index = operation.index
                            val archive = operation.archive

                            operation.future.complete(
                                if (index == 255) filesystem.readReferenceTable(archive) else filesystem.read(
                                    index,
                                    archive
                                )
                            )
                        }
                        is IOOperation.WriteOperation -> {
                            val index = operation.index
                            val archive = operation.archive
                            val version = operation.version
                            val crc = operation.crc

                            if (index == 255)
                                filesystem.writeReferenceTable(archive, operation.data, version, crc)
                            else
                                filesystem.write(index, archive, operation.data, version, crc)

                            operation.future.complete(Unit)
                        }
                        is IOOperation.WriteRequestOperation -> {
                            if (!operation.request.isCompleted())
                                throw IllegalArgumentException("Attempted to write uncompleted request: ${operation.request}")

                            val table = filesystem.getReferenceTable(operation.index)
                                ?: throw NullPointerException("Reference table for write request not found: ${operation.index}")
                            val entry = table.archives[operation.archive]
                                ?: throw NullPointerException("Reference table entry for write request not found: [${operation.index}, ${operation.archive}]")

                            val buffer = operation.request.buffer
                                ?: throw NullPointerException("Request buffer is missing")

                            val crc = buffer.getCrc32()
                            if (crc != entry.crc)
                                throw IllegalArgumentException("CRC mismatch in [${operation.index}, ${operation.archive}]. Got $crc, expected ${entry.crc}")

                            if (entry.whirlpool != null) {
                                val whirlpool = Whirlpool.getHash(buffer.array(), 0, buffer.limit())
                                if (!Arrays.equals(whirlpool, entry.whirlpool))
                                    throw IllegalArgumentException("Whirlpool mismatch in [${operation.index}, ${operation.archive}]")
                            }

                            val version = entry.version
                            buffer.position(buffer.limit()).limit(buffer.capacity())
                            buffer.put((version shr 8).toByte())
                            buffer.put(version.toByte())

                            filesystem.write(operation.index, operation.archive, buffer.array(), version, crc)

                            operation.future.complete(Unit)
                        }
                    }
                } catch (e: Exception) {
                    operation.future.completeExceptionally(e)
                }
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    sealed class IOOperation<T : Any?>(val index: Int, val archive: Int, val future: CompletableFuture<T>) {
        class WriteRequestOperation(
            val request: Js5RequestHandler.ArchiveRequest,
            future: CompletableFuture<Unit>
        ) : IOOperation<Unit>(request.index, request.archive, future)

        class WriteOperation(
            index: Int,
            archive: Int,
            val data: ByteArray,
            val version: Int,
            val crc: Int,
            future: CompletableFuture<Unit>
        ) : IOOperation<Unit>(index, archive, future)

        class ReadOperation(index: Int, archive: Int, future: CompletableFuture<ByteBuffer?>) :
            IOOperation<ByteBuffer?>(index, archive, future)
    }
}