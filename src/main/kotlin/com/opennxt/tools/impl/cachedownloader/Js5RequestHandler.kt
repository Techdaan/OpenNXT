package com.opennxt.tools.impl.cachedownloader

import com.opennxt.filesystem.Filesystem
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import mu.KotlinLogging
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class Js5RequestHandler(
    private val clientPool: Js5ClientPool,
    private val filesystem: Filesystem,
    private val numIoWorkers: Int = 0
) : Runnable {

    data class RequestDataSnapshot(
        val pendingHttpCount: Int,
        val pendingCount: Int,
        val processingCount: Int,
        val pendingHttpSize: Long,
        val pendingSize: Long,
        val processingSize: Long,
        val pendingIOOPerations: Int,
        val lastTick: Long
    )

    class ArchiveRequest(
        val index: Int,
        val archive: Int,
        var priority: Boolean = false,
        var size: Int = 0,
        var crc: Int = 0,
        var version: Int = 0
    ) {
        private val lock = CountDownLatch(1)

        var crashed = false
        var offset = 0 // Offset in this block, a block is 102400 bytes
        var buffer: ByteBuffer? = null

        fun allocateBuffer(size: Int): ByteBuffer {
            this.size = size
            val buffer = ByteBuffer.allocate(size)
            this.buffer = buffer
            return buffer
        }

        fun notifyCompleted() {
            lock.countDown()
        }

        fun isCompleted(): Boolean = lock.count == 0L

        fun awaitCompletion(timeout: Long, timeUnit: TimeUnit): Boolean {
            return lock.await(timeout, timeUnit)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ArchiveRequest

            if (index != other.index) return false
            if (archive != other.archive) return false
            if (priority != other.priority) return false

            return true
        }

        override fun hashCode(): Int {
            var result = index
            result = 31 * result + archive
            result = 31 * result + priority.hashCode()
            return result
        }

    }

    private val running = AtomicBoolean(true)
    private val logger = KotlinLogging.logger { }
    private val lock = Any()
    private val workers = Array(numIoWorkers) { AsyncFilesystemAccessor(filesystem) }
    private val workerThreads = Array(numIoWorkers) { Thread(workers[it], "async-filesystem-accessor-$it") }
    private var lastTick = System.currentTimeMillis()

    private val processingHttp = ObjectOpenHashSet<ArchiveRequest>()
    private val pending = Int2ObjectOpenHashMap<ObjectOpenHashSet<ArchiveRequest>>()
    private val processing = HashSet<ArchiveRequest>()

    init {
        logger.info { "Starting $numIoWorkers filesystem I/O threads" }
        workerThreads.forEach(Thread::start)
    }

    fun createSnapshot(): RequestDataSnapshot {
        synchronized(lock) {
            return RequestDataSnapshot(
                processingHttp.size,
                pending.values.sumOf { it.size },
                processing.size,
                processingHttp.sumOf { it.size.toLong() },
                pending.values.sumOf { index -> index.sumOf { it.size.toLong() } },
                processing.sumOf { it.size.toLong() },
                workers.sumOf { it.pendingOperations() },
                lastTick
            )
        }
    }

    fun request(requests: Collection<ArchiveRequest>) {
        synchronized(lock) {
            requests.forEach { request ->
                request.crashed = false

                if (request.index == 40) {
                    processingHttp += request
                    clientPool.addRequest(request)
                    return@forEach
                }

                if (request.index == 14 && request.archive == 0) {
                    logger.info { "Skipping $request to avoid crash loop" }
                    return@forEach
                }

                pending.getOrPut(request.index) { ObjectOpenHashSet() }.add(request)
            }
        }
    }

    override fun run() {
        logger.info { "run request handler" }
        try {
            while (running.get()) {
                lastTick = System.currentTimeMillis()
                clientPool.healthCheck()

                var i = 0
                val crashed = HashSet<ArchiveRequest>()
                synchronized(lock) {
                    pending.forEach { (_, requests) ->
                        if (requests.isNotEmpty())
                            clientPool.addRequestsFromIterator(requests.iterator(), processing)
                    }

                    val it = processing.iterator()
                    while (it.hasNext()) {
                        val request = it.next()

                        if (request.isCompleted()) {
                            workers[i++ % numIoWorkers].write(request)
                            it.remove()
                        }

                        if (request.crashed) {
                            it.remove()
                            crashed.add(request)
                        }
                    }

                    val it2 = processingHttp.iterator()
                    while (it2.hasNext()) {
                        val request = it2.next()

                        if (request.isCompleted()) {
                            workers[i++ % numIoWorkers].write(request)
                            it2.remove()
                        }

                        if (request.crashed) {
                            it.remove()
                            crashed.add(request)
                        }
                    }
                }

                if (crashed.isNotEmpty()) {
                    request(crashed)
                    logger.info { "Re-queued ${crashed.size} failed/crashed/lost requests" }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
        throw IllegalStateException("?????")
    }

    fun shutdown() {
        running.set(false)
    }

}