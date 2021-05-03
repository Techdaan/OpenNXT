package com.opennxt.filesystem.prefetches

import com.opennxt.filesystem.Filesystem

class FilePrefetch(private val index: Int, private val name: String) : Prefetch {
    override fun calculateValue(store: Filesystem): Int {
        val file = store.read(index, name.toLowerCase()) ?: return 0

        return file.capacity() - 2
    }
}
