package com.opennxt

import com.opennxt.net.js5.Js5Session
import mu.KotlinLogging
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

// TODO This should probably be re-done entirely.
object Js5Thread: Thread("js5-thread") {

    private val running = AtomicBoolean(true)
    private val logger = KotlinLogging.logger {  }
    private val sessions = CopyOnWriteArrayList<Js5Session>()

    override fun run() {
        while (running.get()) {
            if (sessions.isEmpty()) {
                sleep(100)
            }

            sessions.forEach {
                it.process(10_000_000) // Process in blocks to avoid turning into "first-come first-serve"
            }
        }
    }

    fun addSession(session: Js5Session) {
        sessions.add(session)
    }

    fun removeSession(session: Js5Session) {
        sessions.remove(session)
    }

}