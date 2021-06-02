package com.opennxt.filesystem.prefetches

import com.opennxt.filesystem.Filesystem
import com.opennxt.filesystem.Index
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

class PrefetchTable(val entries: IntArray) {
    companion object {
        val RS3_DEFAULT = arrayOf(
            IndexPrefetch(Index.DEFAULTS),
            LibraryPrefetch("windows/x86/jaclib.dll"),
            LibraryPrefetch("windows/x86/jaggl.dll"),
            LibraryPrefetch("windows/x86/jagdx.dll"),
            LibraryPrefetch("windows/x86/sw3d.dll"),
            LibraryPrefetch("RuneScape-setup.exe"),
            LibraryPrefetch("windows/x86/hw3d.dll"),
            IndexPrefetch(Index.SHADERS),
            IndexPrefetch(Index.MATERIALS),
            IndexPrefetch(Index.CONFIG),
            IndexPrefetch(Index.CONFIG_OBJECT),
            IndexPrefetch(Index.CONFIG_ENUM),
            IndexPrefetch(Index.CONFIG_NPC),
            IndexPrefetch(Index.CONFIG_ITEM),
            IndexPrefetch(Index.CONFIG_SEQ),
            IndexPrefetch(Index.CONFIG_SPOT),
            IndexPrefetch(Index.CONFIG_STRUCT),
            IndexPrefetch(Index.DBTABLEINDEX),
            IndexPrefetch(Index.QUICKCHAT),
            IndexPrefetch(Index.QUICKCHAT_GLOBAL),
            IndexPrefetch(Index.PARTICLES),
            IndexPrefetch(Index.BILLBOARDS),
            FilePrefetch(Index.BINARY, "huffman"),
            IndexPrefetch(Index.INTERFACES),
            IndexPrefetch(Index.CLIENTSCRIPTS),
            IndexPrefetch(Index.FONTMETRICS),
            ArchivePrefetch(Index.WORLDMAP, 0),
            IndexPrefetch(57),
            IndexPrefetch(58),
            IndexPrefetch(59),
            IndexPrefetch(60),
        )

        fun of(fs: Filesystem, prefetches: Array<Prefetch> = RS3_DEFAULT): PrefetchTable =
            PrefetchTable(prefetches.map { it.calculateValue(fs) }.toIntArray())

        fun decode(buffer: ByteBuf, prefetches: Array<Prefetch> = RS3_DEFAULT): PrefetchTable {
            TODO("prefetch table decoding")
        }
    }

    fun encode(out: ByteBuf = Unpooled.buffer(entries.size * 4)): ByteBuf {
        for (entry in entries)
            out.writeInt(entry)
        return out
    }
}