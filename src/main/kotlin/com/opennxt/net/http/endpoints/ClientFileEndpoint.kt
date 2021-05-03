package com.opennxt.net.http.endpoints

import com.opennxt.model.files.BinaryType
import com.opennxt.model.files.FileChecker
import com.opennxt.net.http.sendHttpError
import com.opennxt.net.http.sendHttpFile
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.QueryStringDecoder

object ClientFileEndpoint {
    fun handle(ctx: ChannelHandlerContext, msg: FullHttpRequest, query: QueryStringDecoder) {
        if (!query.parameters().containsKey("binaryType") || !query.parameters().containsKey("fileName")) {
            ctx.sendHttpError(HttpResponseStatus.NOT_FOUND)
            return
        }

        val binaryType = BinaryType.values()[query.parameters().getValue("binaryType").first().toInt()]
        val filename = query.parameters().getValue("fileName").first()
        val crc = query.parameters().getValue("crc").first().toLong()

        val data = FileChecker.getFile("compressed", binaryType, file = filename, crc = crc)
        if (data == null) {
            ctx.sendHttpError(HttpResponseStatus.NOT_FOUND)
            return
        }

        ctx.sendHttpFile(data, filename)
    }
}