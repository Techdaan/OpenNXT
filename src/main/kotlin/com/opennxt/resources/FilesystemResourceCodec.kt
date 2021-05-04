package com.opennxt.resources

import com.opennxt.filesystem.Filesystem

interface FilesystemResourceCodec<T : Any> {
    fun load(fs: Filesystem, id: Int): T?
    fun store(fs: Filesystem, id: Int, data: T)
    fun list(fs: Filesystem): Map<Int, T>
    fun getMaxId(fs: Filesystem): Int
}