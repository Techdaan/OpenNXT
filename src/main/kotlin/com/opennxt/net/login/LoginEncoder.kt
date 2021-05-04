package com.opennxt.net.login

import com.opennxt.ext.decipherXtea
import com.opennxt.ext.readBuild
import com.opennxt.ext.readString
import com.opennxt.net.login.LoginRSAHeader.Companion.writeLoginHeader
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import mu.KotlinLogging
import java.math.BigInteger

private val RS3_MODULUS = BigInteger(
    "da1bf980d920a113eb1ba9646e0d2c87f8fbb1afc117ef1f98eadcda42cc491006bedd3e8129c5d9269e0bacdf4322a0ee7de4614b08300998dd65c05d93e99d9aa66fbba3397886a9dc5e2f186e443f783babc5fb2e5ac1ae5c248889df68bc7be8a2345e24c449db2a2f9e55fbb6197344f6dc24c7be08573051bde7aaaa55",
    16
)
private val RS3_EXPONENT = BigInteger("10001", 16)

class LoginEncoder: MessageToByteEncoder<LoginPacket>() {
    private val logger = KotlinLogging.logger {  }

    fun check(buf: ByteBuf, original: LoginPacket.LobbyLoginRequest) {
        val originalHeader = original.header as LoginRSAHeader.Fresh

        val type = buf.readUnsignedByte()
        logger.info { "type = $type" }
        val len = buf.readUnsignedShort()
        logger.info { "body len = $len" }
        val body = buf.readBytes(len)
        logger.info { "build = ${body.readBuild()}" }
        val rsaLen =body.readUnsignedShort()
        body.skipBytes(rsaLen)
        logger.info { "skipped $rsaLen bytes (rsa)" }
        body.decipherXtea(originalHeader.seeds)

        body.skipBytes(1)
        logger.info { "username: ${body.readString()}" }
    }

    override fun encode(ctx: ChannelHandlerContext, msg: LoginPacket, out: ByteBuf) {
        when (msg) {
            is LoginPacket.LoginResponse -> out.writeByte(msg.code.id)
            is LoginPacket.SendUniqueId -> out.writeLong(msg.id)
            is LoginPacket.LobbyLoginRequest -> {
                val tmp = Unpooled.buffer()

                tmp.writeByte(LoginType.LOBBY.id)

                val wrapper = Unpooled.buffer()
                wrapper.writeInt(msg.build.major)
                wrapper.writeInt(msg.build.minor)
//                logger.info { "wi = ${wrapper.writerIndex()}" }

                val header = msg.header as LoginRSAHeader.Fresh

                wrapper.writeLoginHeader(LoginType.LOBBY, header, RS3_EXPONENT, RS3_MODULUS)
//                logger.info { "wi3 = ${wrapper.writerIndex()}" }
                msg.remaining.readerIndex(0)
                wrapper.writeBytes(msg.remaining)
//                logger.info { "wi2 = ${wrapper.writerIndex()}" }
//
//                logger.info { "wrapper wi = ${wrapper.writerIndex()}, remaining = ${msg.remaining.writerIndex()}" }

                tmp.writeShort(wrapper.writerIndex())
                tmp.writeBytes(wrapper)

//                check(tmp, msg)

//                exitProcess(0)
//                tmp.readerIndex(0)

                out.writeBytes(tmp)
            }
            else -> TODO("Encode $msg")
        }
    }
}