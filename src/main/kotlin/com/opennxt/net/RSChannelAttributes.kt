package com.opennxt.net

import com.opennxt.net.login.LoginType
import com.opennxt.util.ISAACCipher
import io.netty.channel.Channel
import io.netty.util.AttributeKey

object RSChannelAttributes {
    // An id generated by the server that the client sends in the RSA-encrypted login block. Prevents packet replaying
    //  attacks.
    val LOGIN_UNIQUE_ID = AttributeKey.newInstance<Long>("login-unique-id")

    val LOGIN_TYPE = AttributeKey.newInstance<LoginType>("login-type")

    val INCOMING_ISAAC = AttributeKey.newInstance<ISAACCipher>("incoming-isaac")
    val OUTGOING_ISAAC = AttributeKey.newInstance<ISAACCipher>("outgoing-isaac")

    val SIDE = AttributeKey.newInstance<Side>("side")
    val PASSTHROUGH_CHANNEL = AttributeKey.newInstance<Channel>("passthrough-channel")

    val CONNECTED_CLIENT = AttributeKey.newInstance<ConnectedClient>("connected-client")
}