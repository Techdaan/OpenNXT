package com.opennxt.filesystem.prefetches

import com.opennxt.filesystem.Filesystem

class LibraryPrefetch(private val name: String) : Prefetch {
    override fun calculateValue(store: Filesystem): Int {
        val file = store.read(30, name.toLowerCase()) ?: return 0

        return file.capacity() - 2
    }
}
