package com.opennxt.tools.impl.cachedownloader

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.opennxt.Constants
import com.opennxt.filesystem.Filesystem
import com.opennxt.filesystem.sqlite.SqliteFilesystem
import com.opennxt.tools.Tool
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import java.lang.Thread.sleep
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CacheCompletionChecker : Tool(
    "cache-completion-checker",
    "Displays information about cache completion based on the reference tables in the cache"
) {
    private val checkThreads by option(help = "The number of I/O threads for checking which files require updating").int()
        .default(4)

    private lateinit var cache: Filesystem
    private lateinit var checkerExecutor: ExecutorService

    override fun runTool() {
        logger.info { "Opening filesystem from ${Constants.CACHE_PATH}" }
        cache = SqliteFilesystem(Constants.CACHE_PATH)

        logger.info { "Starting table checks" }
        checkerExecutor = Executors.newFixedThreadPool(checkThreads, ThreadFactoryBuilder()
            .setNameFormat("table-checker-%d")
            .setUncaughtExceptionHandler { t, e ->
                logger.error { "Uncaught exception in thread ${t.name}: $e" }
                e.printStackTrace()
            }
            .build())

        val set = HashSet<Callable<Unit>>()
        val workers = Int2ObjectOpenHashMap<IndexCompletionChecker>()
        for (i in 0 until cache.numIndices()) {
            val table = cache.getReferenceTable(i)
            if (table != null) {
                workers[i] = IndexCompletionChecker(cache, i, null)
//                checkerExecutor.submit(workers[i])
                set += Callable<Unit> { workers[i].run() }
            }
        }

        Thread {
            while(true) {
                logger.info { " CHECKERS WORKING:" }
                workers.forEach { (index, worker) ->
                    if (!worker.completed && worker.started)
                        logger.info { "  Index $index: ${worker.progress / 1024L / 1024L}/${worker.estimatedTotal / 1024L / 1024L}MB" }
                }
                sleep(2500)
            }
        }.start()

        val start = System.currentTimeMillis()
        checkerExecutor.invokeAll(set, 1, TimeUnit.HOURS)
        logger.info { "Total time: ${System.currentTimeMillis() - start}" }
    }
}