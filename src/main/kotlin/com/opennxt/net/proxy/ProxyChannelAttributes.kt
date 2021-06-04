package com.opennxt.net.proxy

import com.opennxt.login.LoginResult
import com.opennxt.net.login.LoginPacket
import io.netty.channel.Channel
import io.netty.util.AttributeKey

object ProxyChannelAttributes {
    val LOGIN_STATE = AttributeKey.newInstance<ProxyLoginState>("proxy-login-state")
    val LOGIN_HANDLER = AttributeKey.newInstance<(Channel?, LoginResult) -> Unit>("proxy-login-handler")

    val USERNAME = AttributeKey.newInstance<String>("proxy-username")
    val PASSWORD = AttributeKey.newInstance<String>("proxy-password")
    val PACKET = AttributeKey.newInstance<LoginPacket>("proxy-packet")
    val PROXY_CLIENT = AttributeKey.newInstance<ConnectedProxyClient>("proxy-client")
    val PROXY_PLAYER = AttributeKey.newInstance<ProxyPlayer>("proxy-player")
    val PLAYER_INDEX = AttributeKey.newInstance<Int>("player-index")
}