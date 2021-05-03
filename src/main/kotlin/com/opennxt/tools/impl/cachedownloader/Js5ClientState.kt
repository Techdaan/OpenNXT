package com.opennxt.tools.impl.cachedownloader

enum class Js5ClientState(val canRead: Boolean = true) {
    HANDSHAKE,
    PREFETCHES,
    ACTIVE,
    CRASHED(false),
    DISCONNECTED(false)
}