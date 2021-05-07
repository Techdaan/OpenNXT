package com.opennxt.net.login

import com.opennxt.ext.*
import com.opennxt.net.login.LoginRSAHeader.Companion.writeLoginHeader
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import mu.KotlinLogging
import java.math.BigInteger
import kotlin.system.exitProcess

// TODO Grab this from patcher & store in file, can be done automatically.
private val RS3_MODULUS = BigInteger(
    "da1bf980d920a113eb1ba9646e0d2c87f8fbb1afc117ef1f98eadcda42cc491006bedd3e8129c5d9269e0bacdf4322a0ee7de4614b08300998dd65c05d93e99d9aa66fbba3397886a9dc5e2f186e443f783babc5fb2e5ac1ae5c248889df68bc7be8a2345e24c449db2a2f9e55fbb6197344f6dc24c7be08573051bde7aaaa55",
    16
)
private val RS3_EXPONENT = BigInteger("10001", 16)

class LoginEncoder: MessageToByteEncoder<LoginPacket>() {
    private val logger = KotlinLogging.logger {  }

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

                val header = msg.header as LoginRSAHeader.Fresh

                wrapper.writeLoginHeader(LoginType.LOBBY, header, RS3_EXPONENT, RS3_MODULUS)
                msg.remaining.readerIndex(0)
                wrapper.writeBytes(msg.remaining)
                tmp.writeShort(wrapper.writerIndex())
                tmp.writeBytes(wrapper)

                out.writeBytes(tmp)
            }
            is LoginPacket.GameLoginRequest -> {
                val tmp = Unpooled.buffer()

                tmp.writeByte(LoginType.GAME.id)

                val wrapper = Unpooled.buffer()
                wrapper.writeInt(msg.build.major)
                wrapper.writeInt(msg.build.minor)

                val header = msg.header as LoginRSAHeader.Fresh

                wrapper.writeLoginHeader(LoginType.GAME, header, RS3_EXPONENT, RS3_MODULUS)
                msg.remaining.readerIndex(0)
                wrapper.writeBytes(msg.remaining)
                tmp.writeShort(wrapper.writerIndex())
                tmp.writeBytes(wrapper)

                out.writeBytes(tmp)
            }
            is LoginPacket.LobbyLoginResponse -> {
                val tmp = Unpooled.buffer()
                tmp.writeByte(msg.byte0)
                tmp.writeByte(msg.rights)
                tmp.writeByte(msg.byte2)
                tmp.writeByte(msg.byte3)
                tmp.writeMedium(msg.medium4)
                tmp.writeByte(msg.byte5)
                tmp.writeByte(msg.byte6)
                tmp.writeByte(msg.byte7)
                tmp.writeLong(msg.long8)
                tmp.writeInt(msg.int9)
                tmp.writeByte(msg.byte10)
                tmp.writeByte(msg.byte11)
                tmp.writeInt(msg.int12)
                tmp.writeInt(msg.int13)
                tmp.writeShort(msg.short14)
                tmp.writeShort(msg.short15)
                tmp.writeShort(msg.short16)
                tmp.writeInt(msg.ip)
                tmp.writeByte(msg.byte17)
                tmp.writeShort(msg.short18)
                tmp.writeShort(msg.short19)
                tmp.writeByte(msg.byte20)
                tmp.writeNullCircumfixedString(msg.username)
                tmp.writeByte(msg.byte22)
                tmp.writeInt(msg.int23)
                tmp.writeShort(msg.short24)
                tmp.writeNullCircumfixedString(msg.defaultWorld)
                tmp.writeShort(msg.defaultWorldPort1)
                tmp.writeShort(msg.defaultWorldPort2)

                out.writeByte(tmp.writerIndex())
                out.writeBytes(tmp)
                tmp.release()
            }
            is LoginPacket.GameLoginResponse -> {
                val tmp = Unpooled.buffer()
                tmp.writeByte(msg.byte0)
                tmp.writeByte(msg.rights)
                tmp.writeByte(msg.byte2)
                tmp.writeByte(msg.byte3)
                tmp.writeByte(msg.byte4)
                tmp.writeByte(msg.byte5)
//                tmp.writeByte(msg.byte6)
                tmp.writeShort(msg.playerIndex)
                tmp.writeByte(msg.byte8)
                tmp.writeMedium(msg.medium9)
                tmp.writeByte(msg.isMember)
                tmp.writeNullCircumfixedString(msg.username)
                tmp.writeShort(msg.short12)
                tmp.writeInt(msg.int13)

                out.writeByte(tmp.writerIndex())
                out.writeBytes(tmp)
                tmp.release()
            }
            is LoginPacket.ServerpermVarcChunk -> {
                val tmp = Unpooled.buffer()
                tmp.writeByte(if(msg.finished) 1 else 0)
                msg.varcs.forEach { (k, v) ->
                    tmp.writeShort(k)
                    tmp.writeInt(v as Int)
                }

                out.writeShort(tmp.writerIndex())
                out.writeBytes(tmp)
                tmp.release()
            }
            else -> {
                // exceptions didn't get logged to console, need to figure out why netty can be weird.
                logger.error { "Attempted to encode unknown login message $msg" }
                exitProcess(0)
            }
        }
    }
}