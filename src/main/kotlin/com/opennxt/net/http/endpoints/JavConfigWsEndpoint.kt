package com.opennxt.net.http.endpoints

import com.opennxt.OpenNXT
import com.opennxt.model.files.BinaryType
import com.opennxt.model.files.ClientConfig
import com.opennxt.model.files.FileChecker
import com.opennxt.net.http.sendHttpText
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder

object JavConfigWsEndpoint {
    fun handle(ctx: ChannelHandlerContext, msg: FullHttpRequest, query: QueryStringDecoder) {

        val type = BinaryType.values()[query.parameters().getOrElse("binaryType") { listOf("2") }.first().toInt()]
        val config = FileChecker.getConfig("compressed", type) ?: throw NullPointerException("Can't get config for type $type")
        if (!OpenNXT.enableProxySupport) {
            ctx.sendHttpText(config.toString().toByteArray(Charsets.ISO_8859_1))
            return
        }

        val liveConfig = ClientConfig.download("https://world5.runescape.com/jav_config.ws", type)

        var download = 0
        while (config.entries.containsKey("download_name_$download")) {
            liveConfig.entries["download_name_$download"] = config.entries.getValue("download_name_$download")
            liveConfig.entries["download_crc_$download"] = config.entries.getValue("download_crc_$download")
            liveConfig.entries["download_hash_$download"] = config.entries.getValue("download_hash_$download")
            download++
        }

        liveConfig["codebase"] = "http://${OpenNXT.config.hostname}/"

        for (i in 0..liveConfig.highestParam) {
            val value = liveConfig.getParam(i) ?: continue

            if (value.contains("runescape.com") || value.contains("jagex.com")) {
                liveConfig["param=$i"] = OpenNXT.config.hostname
            }
        }

        ctx.sendHttpText(liveConfig.toString().toByteArray(Charsets.ISO_8859_1))
    }
}