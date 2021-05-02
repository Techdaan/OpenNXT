package com.opennxt.filesystem

import java.nio.ByteBuffer
import java.nio.file.Path

abstract class Filesystem(val path: Path) {

    private val checkedReferenceTables = BooleanArray(255)

    private val cachedReferenceTables = arrayOfNulls<ReferenceTable>(255)

    abstract fun exists(index: Int, archive: Int): Boolean

    abstract fun read(index: Int, archive: Int): ByteBuffer?

    abstract fun read(index: Int, name: String): ByteBuffer?

    abstract fun readReferenceTable(index: Int): ByteBuffer?

    abstract fun createIndex(id: Int)

    fun getReferenceTable(index: Int, ignoreChecked: Boolean = false): ReferenceTable? {
        val cached = cachedReferenceTables[index]
        if (cached != null) return cached

        if (!ignoreChecked) {
            if (checkedReferenceTables[index]) return null
            checkedReferenceTables[index] = true
        }

        val table = ReferenceTable(this, index)
        val container = readReferenceTable(index) ?: return null
        val data = ByteBuffer.wrap(Container.decode(container).data)
        table.decode(data)
        cachedReferenceTables[index] = table

        return table
    }

    abstract fun write(index: Int, archive: Int, data: Container)

    abstract fun write(index: Int, archive: Int, compressed: ByteArray, version: Int, crc: Int)

    abstract fun writeReferenceTable(index: Int, data: Container)

    abstract fun writeReferenceTable(index: Int, compressed: ByteArray, version: Int, crc: Int)

    abstract fun numIndices(): Int

    fun update() {
        cachedReferenceTables.forEach { it?.update() }
    }

}