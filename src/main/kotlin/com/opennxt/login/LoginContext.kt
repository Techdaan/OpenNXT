package com.opennxt.login

import com.opennxt.model.Build
import com.opennxt.net.login.LoginPacket
import io.netty.channel.Channel
import java.util.*

class LoginContext(
    val packet: LoginPacket,
    val callback: (LoginContext) -> Unit,
    val build: Build,
    val username: String,
    val password: String,
    var attempt: Int = 0,
    var uuid: UUID? = null,
    val channel: Channel,
    var result: LoginResult = LoginResult.FAILED_LOADING
)