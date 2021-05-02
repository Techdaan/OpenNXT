package com.opennxt.filesystem.compression

enum class ContainerCompression(val id: Int) {
    NONE(0),
    BZIP2(1),
    GZIP(2),
    LZMA(3);

    companion object {
        val values = values()

        fun of(id: Int): ContainerCompression {
            for (value in values) {
                if(value.id == id) return value
            }
            throw NullPointerException("No compression found for id: $id")
        }
    }
}