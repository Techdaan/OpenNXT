package com.opennxt.net.http

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.util.CharsetUtil

fun ChannelHandlerContext.sendHttpError(status: HttpResponseStatus, error: String = "Failed: $status\r\n") {
    val response = DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1, status,
        Unpooled.copiedBuffer(error, CharsetUtil.UTF_8)
    )
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")
    writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
}

fun ChannelHandlerContext.sendHttpFile(file: ByteArray, name: String) {
    val response = DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
        Unpooled.wrappedBuffer(file))
    response.headers().set(HttpHeaderNames.SERVER, "JaGeX/3.1")
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream")
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.size)
    response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, name)
    response.headers().set(HttpHeaderNames.CONTENT_ENCODING, "lzma")

    channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
}

fun ChannelHandlerContext.sendHttpText(text: ByteArray) {
    val response = DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
        Unpooled.wrappedBuffer(text))
    response.headers().set(HttpHeaderNames.SERVER, "JaGeX/3.1")
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=ISO-8859-1")
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, text.size)

    channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
}