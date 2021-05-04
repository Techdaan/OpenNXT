package com.opennxt.net.proxy

enum class ProxyLoginState {
    HANDSHAKE,
    UNIQUE_ID,
    LOGIN_RESPONSE,
    WAITING_SERVER_RESPOSNE,
    FINISHED
    // TODO Game login states
}