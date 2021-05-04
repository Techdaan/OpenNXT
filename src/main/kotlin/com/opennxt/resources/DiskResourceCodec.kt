package com.opennxt.resources

import java.nio.file.Path

interface DiskResourceCodec<T: Any> {
    fun load(path: Path): T?
    fun store(path: Path, data: T)
    fun list(path: Path): Map<String, Path>
    fun getFileExtension(resource: T): String?
}