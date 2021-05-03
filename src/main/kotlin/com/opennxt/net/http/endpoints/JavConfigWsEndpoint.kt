package com.opennxt.net.http.endpoints

import com.opennxt.model.files.BinaryType
import com.opennxt.model.files.FileChecker
import com.opennxt.net.http.sendHttpText
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder

object JavConfigWsEndpoint {
    fun handle(ctx: ChannelHandlerContext, msg: FullHttpRequest, query: QueryStringDecoder) {

        val config = FileChecker.getConfig(
            "compressed",
            BinaryType.values()[query.parameters().getOrElse("binaryType") { listOf("2") }.first().toInt()]
        )

        ctx.sendHttpText(config.toString().toByteArray(Charsets.ISO_8859_1))
    }
}