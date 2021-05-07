package com.opennxt.net.proxy

enum class ProxyLoginState {
    HANDSHAKE,
    UNIQUE_ID,
    LOGIN_RESPONSE,
    WAITING_LOGIN_RESPONSE,
    FINISHED,

    WAITING_SERVERPERM_VARCS,
    WAITING_WORLDLOGIN_RESPONSE,
    // TODO Game login states
}