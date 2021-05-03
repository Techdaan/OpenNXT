package com.opennxt.tools.impl.cachedownloader

import java.net.URL

class Js5HttpRequest(val url: URL, val request: Js5RequestHandler.ArchiveRequest): Runnable {
    override fun run() {
        val data = url.readBytes()

        request.allocateBuffer(data.size + 2)
        request.buffer!!.put(data, 0, data.size)
        request.buffer!!.flip()

        request.notifyCompleted()
    }
}