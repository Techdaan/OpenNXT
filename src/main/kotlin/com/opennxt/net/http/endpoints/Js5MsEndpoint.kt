package com.opennxt.net.http.endpoints

import com.opennxt.OpenNXT
import com.opennxt.model.files.BinaryType
import com.opennxt.model.files.FileChecker
import com.opennxt.net.http.sendHttpError
import com.opennxt.net.http.sendHttpFile
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*

object Js5MsEndpoint {
    fun handle(ctx: ChannelHandlerContext, msg: FullHttpRequest, query: QueryStringDecoder) {
        if (!query.parameters().containsKey("a") || !query.parameters().containsKey("g")) {
            ctx.sendHttpError(HttpResponseStatus.BAD_REQUEST)
            return
        }

        val index: Int = try {
            query.parameters().getValue("a").first().toInt()
        } catch (e: NumberFormatException) {
            ctx.sendHttpError(HttpResponseStatus.BAD_REQUEST)
            return
        }

        val archive: Int = try {
            query.parameters().getValue("g").first().toInt()
        } catch (e: NumberFormatException) {
            ctx.sendHttpError(HttpResponseStatus.BAD_REQUEST)
            return
        }

        if (index == 255 && archive == 255) {
            sendFile(msg, ctx, Unpooled.wrappedBuffer(OpenNXT.httpChecksumTable))
            return
        } else if (index == 40) {
            val data = OpenNXT.filesystem.read(40, archive)
            if (data == null) {
                ctx.sendHttpError(HttpResponseStatus.NOT_FOUND)
                return
            }

            sendFile(msg, ctx, Unpooled.wrappedBuffer(data))
        }

        ctx.sendHttpError(HttpResponseStatus.NOT_FOUND)
    }

    private fun sendFile(request: FullHttpRequest, ctx: ChannelHandlerContext, buf: ByteBuf) {
        val size = buf.readableBytes()
        val response = DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK, buf)
        response.headers().set(HttpHeaderNames.SERVER, "JaGeX/3.1")
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream")
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, size)

        ctx.channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }
}