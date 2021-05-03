package com.opennxt.net.http

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.QueryStringDecoder
import mu.KotlinLogging

@ChannelHandler.Sharable
class HttpRequestHandler: SimpleChannelInboundHandler<FullHttpRequest>() {
    private val logger = KotlinLogging.logger {  }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        if (!msg.decoderResult().isSuccess) {
            ctx.sendHttpError(HttpResponseStatus.BAD_REQUEST)
            return
        }

        if (msg.method() != HttpMethod.GET) {
            ctx.sendHttpError(HttpResponseStatus.METHOD_NOT_ALLOWED)
            return
        }
        val uri = msg.uri()
        val query = QueryStringDecoder(uri)

        logger.debug("Received request path: ${query.path()}")
        when {
            else -> ctx.sendHttpError(HttpResponseStatus.NOT_FOUND)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        if (ctx.channel().isActive) {
            ctx.sendHttpError(HttpResponseStatus.INTERNAL_SERVER_ERROR)
        }
    }
}