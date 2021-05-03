package com.opennxt.filesystem.prefetches

import com.opennxt.filesystem.Filesystem

interface Prefetch {
    fun calculateValue(store: Filesystem): Int
}