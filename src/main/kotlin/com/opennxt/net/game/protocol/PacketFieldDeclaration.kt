package com.opennxt.net.game.protocol

import com.opennxt.net.buf.*
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

data class PacketFieldDeclaration(val key: String, val dataCodec: DataCodec<Any>) {
    interface DataCodec<T : Any> {
        fun read(buffer: GamePacketReader): T
        fun write(buffer: GamePacketBuilder, value: T)
    }

    companion object {
        fun fromString(value: String): PacketFieldDeclaration {
            val parts = value.split(" ", limit = 3)
            if (parts.size < 2)
                throw IllegalArgumentException("Parts size = ${parts.size} for $value. Proper format: 'name type <comment>'")

            val codec = Codecs.codecs[parts[1]] ?: throw NullPointerException("Codec not found for type ${parts[1]}")

            return PacketFieldDeclaration(parts[0], codec as DataCodec<Any>)
        }
    }

    object Codecs {
        val codecs = Object2ObjectOpenHashMap<String, DataCodec<*>>()

        init {
            codecs["string"] = StringCodec

            codecs["ubyte"] = UByteCodec
            codecs["ubyte128"] = UByte128Codec
            codecs["u128byte"] = U128ByteCodec
            codecs["ubytec"] = UByteCCodec

            codecs["sbyte"] = SByteCodec
            codecs["sbyte128"] = SByte128Codec
            codecs["s128byte"] = S128ByteCodec
            codecs["sbytec"] = SByteCCodec

            codecs["ushort"] = UShortCodec
            codecs["ushort128"] = UShort128Codec
            codecs["ushortle"] = UShortLECodec
            codecs["ushortle128"] = UShortLE128Codec

            codecs["umedium"] = UMediumCodec
            codecs["umediumle"] = UMediumLECodec

            codecs["int"] = IntCodec
            codecs["intle"] = IntLECodec
            codecs["intv1"] = IntV1Codec
            codecs["intv2"] = IntV2Codec
        }

        object StringCodec : DataCodec<String> {
            override fun read(buffer: GamePacketReader): String = buffer.getString()
            override fun write(buffer: GamePacketBuilder, value: String) = buffer.putString(value)
        }

        object UByteCodec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int = buffer.getUnsigned(DataType.BYTE).toInt()
            override fun write(buffer: GamePacketBuilder, value: Int) = buffer.put(DataType.BYTE, value)
        }

        object UByte128Codec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int =
                buffer.getUnsigned(DataType.BYTE, DataTransformation.ADD).toInt()

            override fun write(buffer: GamePacketBuilder, value: Int) =
                buffer.put(DataType.BYTE, DataTransformation.ADD, value)
        }

        object U128ByteCodec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int =
                buffer.getUnsigned(DataType.BYTE, DataTransformation.SUBTRACT).toInt()

            override fun write(buffer: GamePacketBuilder, value: Int) =
                buffer.put(DataType.BYTE, DataTransformation.SUBTRACT, value)
        }

        object UByteCCodec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int =
                buffer.getUnsigned(DataType.BYTE, DataTransformation.NEGATE).toInt()

            override fun write(buffer: GamePacketBuilder, value: Int) =
                buffer.put(DataType.BYTE, DataTransformation.NEGATE, value)
        }

        object SByteCodec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int = buffer.getSigned(DataType.BYTE).toInt()
            override fun write(buffer: GamePacketBuilder, value: Int) = buffer.put(DataType.BYTE, value)
        }

        object SByte128Codec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int =
                buffer.getSigned(DataType.BYTE, DataTransformation.ADD).toInt()

            override fun write(buffer: GamePacketBuilder, value: Int) =
                buffer.put(DataType.BYTE, DataTransformation.ADD, value)
        }

        object S128ByteCodec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int =
                buffer.getSigned(DataType.BYTE, DataTransformation.SUBTRACT).toInt()

            override fun write(buffer: GamePacketBuilder, value: Int) =
                buffer.put(DataType.BYTE, DataTransformation.SUBTRACT, value)
        }

        object SByteCCodec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int =
                buffer.getSigned(DataType.BYTE, DataTransformation.NEGATE).toInt()

            override fun write(buffer: GamePacketBuilder, value: Int) =
                buffer.put(DataType.BYTE, DataTransformation.NEGATE, value)
        }

        object UShortCodec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int = buffer.getUnsigned(DataType.SHORT).toInt()
            override fun write(buffer: GamePacketBuilder, value: Int) = buffer.put(DataType.SHORT, value)
        }

        object UShort128Codec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int =
                buffer.getUnsigned(DataType.SHORT, DataTransformation.ADD).toInt()

            override fun write(buffer: GamePacketBuilder, value: Int) =
                buffer.put(DataType.SHORT, DataTransformation.ADD, value)
        }

        object UShortLECodec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int =
                buffer.getUnsigned(DataType.SHORT, DataOrder.LITTLE).toInt()

            override fun write(buffer: GamePacketBuilder, value: Int) =
                buffer.put(DataType.SHORT, DataOrder.LITTLE, value)
        }

        object UShortLE128Codec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int =
                buffer.getUnsigned(DataType.SHORT, DataOrder.LITTLE, DataTransformation.ADD).toInt()

            override fun write(buffer: GamePacketBuilder, value: Int) =
                buffer.put(DataType.SHORT, DataOrder.LITTLE, DataTransformation.ADD, value)
        }

        object UMediumCodec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int = buffer.getUnsigned(DataType.MEDIUM).toInt()
            override fun write(buffer: GamePacketBuilder, value: Int) = buffer.put(DataType.MEDIUM, value)
        }

        object UMediumLECodec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int =
                buffer.getUnsigned(DataType.MEDIUM, DataOrder.LITTLE).toInt()

            override fun write(buffer: GamePacketBuilder, value: Int) =
                buffer.put(DataType.MEDIUM, DataOrder.LITTLE, value)
        }

        object IntCodec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int = buffer.getUnsigned(DataType.INT).toInt()
            override fun write(buffer: GamePacketBuilder, value: Int) = buffer.put(DataType.INT, value)
        }

        object IntLECodec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int =
                buffer.getUnsigned(DataType.INT, DataOrder.LITTLE).toInt()

            override fun write(buffer: GamePacketBuilder, value: Int) =
                buffer.put(DataType.INT, DataOrder.LITTLE, value)
        }

        object IntV1Codec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int =
                buffer.getUnsigned(DataType.INT, DataOrder.MIDDLE).toInt()

            override fun write(buffer: GamePacketBuilder, value: Int) =
                buffer.put(DataType.INT, DataOrder.MIDDLE, value)
        }

        object IntV2Codec : DataCodec<Int> {
            override fun read(buffer: GamePacketReader): Int =
                buffer.getUnsigned(DataType.INT, DataOrder.INVERSED_MIDDLE).toInt()

            override fun write(buffer: GamePacketBuilder, value: Int) =
                buffer.put(DataType.INT, DataOrder.INVERSED_MIDDLE, value)
        }
    }
}