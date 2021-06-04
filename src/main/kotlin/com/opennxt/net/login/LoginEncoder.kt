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
    "b86aa1562d8e3f1f68a8cd1581f5ae7996683ddb887dfd4c4547fa54b22282ec42fd2e11cd81e03667f31d5e55c7a7dd6ae005b758944d8eb61d185900941220c4c6d84b694a985c1bbdedbec024c7e0cc1f2f46ba50d42da8efc4fb6be44f8678c9305809b1482c681dd17ae75f90baaf9ffbbc01387c629a8e802859ba9205",
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
                tmp.writeByte(msg.byte6)
                tmp.writeShort(msg.playerIndex)
//                tmp.writeByte(msg.byte8)
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