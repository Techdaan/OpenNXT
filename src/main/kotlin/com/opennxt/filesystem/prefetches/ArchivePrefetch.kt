package com.opennxt.filesystem.prefetches

import com.opennxt.filesystem.Filesystem

class ArchivePrefetch(private val index: Int, private val archive: Int) : Prefetch {
    override fun calculateValue(store: Filesystem): Int {
        return store.read(index, archive)!!.capacity() - 2
    }
}