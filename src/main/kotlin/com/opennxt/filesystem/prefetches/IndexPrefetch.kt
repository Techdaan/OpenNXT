package com.opennxt.filesystem.prefetches

import com.opennxt.filesystem.Filesystem

class IndexPrefetch(private val index: Int) : Prefetch {
    override fun calculateValue(store: Filesystem): Int {
        var value = 0
        val buf = store.readReferenceTable(index)!!
        val table = store.getReferenceTable(index)!!

        if (table.mask and 0x4 != 0) {
            value += table.totalCompressedSize().toInt()
        } else {
            for (entry in table.archives.keys) {
                value += store.read(index, entry)!!.capacity() - 2
            }
        }


        return value + buf.capacity()
    }
}