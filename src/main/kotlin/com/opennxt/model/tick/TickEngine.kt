package com.opennxt.model.tick

import com.google.common.util.concurrent.ThreadFactoryBuilder
import mu.KotlinLogging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.min

class TickEngine {
    private val logger = KotlinLogging.logger { }

    private val executor = Executors.newScheduledThreadPool(
        min(1, Runtime.getRuntime().availableProcessors() - 2),
        ThreadFactoryBuilder()
            .setNameFormat("tick-engine")
            .setUncaughtExceptionHandler { t, e -> logger.error("Error with thread $t", e) }
            .build())

    /**
     * Tickables submitted here will indefinitely be invoked once every 600 milliseconds.
     *
     * The same tickable won't be called concurrently. If ticking takes, for example, 1000 milliseconds, the next
     * execution will start 400 millis late.
     */
    fun submitTickable(tickable: Tickable) {
        executor.scheduleAtFixedRate(tickable::tick, 0, 600, TimeUnit.MILLISECONDS)
    }

    /**
     * Executes a task asynchronously
     */
    fun executeAsync(delay: Long = 0L, runnable: () -> Unit) {
        if (delay <= 0L) executor.execute(runnable)
        else executor.schedule(runnable, delay, TimeUnit.MILLISECONDS)
    }

}