package com.opennxt.filesystem.sqlite

import com.opennxt.filesystem.ReferenceTable
import java.io.Closeable
import java.nio.file.Path
import java.sql.DriverManager

/**
 * Read-write accessor to SQLite files
 */
class SqliteIndexFile(val path: Path) : Closeable, AutoCloseable {
    val table: ReferenceTable? = null

    val connection = DriverManager.getConnection("jdbc:sqlite:$path")

    init {
        connection.prepareStatement(
            """
            CREATE TABLE IF NOT EXISTS `cache`(
              `KEY` INTEGER PRIMARY KEY,
              `DATA` BLOB,
              `VERSION` INTEGER,
              `CRC` INTEGER
            );
        """.trimIndent()
        ).use { stmt -> stmt.executeUpdate() }

        connection.prepareStatement(
            """
            CREATE TABLE IF NOT EXISTS `cache_index`(
              `KEY` INTEGER PRIMARY KEY,
              `DATA` BLOB,
              `VERSION` INTEGER,
              `CRC` INTEGER
            );
        """.trimIndent()
        ).use { stmt -> stmt.executeUpdate() }
    }

    val archiveExistsStmt = connection.prepareStatement("SELECT 1 FROM `cache` WHERE `KEY` = ?;")
    val getMaxArchiveStmt = connection.prepareStatement("SELECT MAX(`KEY`) FROM `cache`;")
    val getArchiveDataStmt = connection.prepareStatement("SELECT `DATA` FROM `cache` WHERE `KEY` = ?;")
    val getReferenceDataStmt = connection.prepareStatement("SELECT `DATA` FROM `cache_index` WHERE `KEY` = 1;")
    val putArchiveDataStmt = connection.prepareStatement(
        """
            INSERT INTO `cache`(`KEY`, `DATA`, `VERSION`, `CRC`)
              VALUES(?, ?, ?, ?)
              ON CONFLICT(`KEY`) DO UPDATE SET
                `DATA` = ?, `VERSION` = ?, `CRC` = ?
              WHERE `KEY` = ?;
    """.trimIndent()
    )
    val putReferenceDataStmt = connection.prepareStatement(
        """
            INSERT INTO `cache_index`(`KEY`, `DATA`, `VERSION`, `CRC`)
              VALUES(1, ?, ?, ?)
              ON CONFLICT(`KEY`) DO UPDATE SET
                `DATA` = ?, `VERSION` = ?, `CRC` = ?
              WHERE `KEY` = 1;
    """.trimIndent()
    )

    fun putRawTable(data: ByteArray, version: Int, crc: Int): Int {
        val stmt = putReferenceDataStmt
        stmt.clearParameters()
        stmt.setBytes(1, data)
        stmt.setInt(2, version)
        stmt.setInt(3, crc)
        stmt.setBytes(4, data)
        stmt.setInt(5, version)
        stmt.setInt(6, crc)
        return stmt.executeUpdate()
    }

    fun putRaw(archive: Int, data: ByteArray, version: Int, crc: Int): Int {
//        val stmt = putArchiveDataStmt
        connection.prepareStatement(
            """
            INSERT INTO `cache`(`KEY`, `DATA`, `VERSION`, `CRC`)
              VALUES(?, ?, ?, ?)
              ON CONFLICT(`KEY`) DO UPDATE SET
                `DATA` = ?, `VERSION` = ?, `CRC` = ?
              WHERE `KEY` = ?;
            """
        ).use { stmt ->
            stmt.clearParameters()
            stmt.setInt(1, archive)
            stmt.setBytes(2, data)
            stmt.setInt(3, version)
            stmt.setInt(4, crc)
            stmt.setBytes(5, data)
            stmt.setInt(6, version)
            stmt.setInt(7, crc)
            stmt.setInt(8, archive)
            return stmt.executeUpdate()
        }
    }

    fun hasReferenceTable(): Boolean {
        getReferenceDataStmt.executeQuery().use { return it.next() }
    }

    override fun close() {
        getMaxArchiveStmt.close()
        getArchiveDataStmt.close()
        getReferenceDataStmt.close()
        putArchiveDataStmt.close()
        putReferenceDataStmt.close()
    }

    fun getMaxArchive(): Int {
        getMaxArchiveStmt.executeQuery().use {
            if (!it.next()) return 0
            return it.getInt(1)
        }
    }

    fun exists(id: Int): Boolean {
        val stmt = archiveExistsStmt
        stmt.clearParameters()
        stmt.setInt(1, id)
        stmt.executeQuery().use { return it.next() }
    }

    fun getRaw(id: Int): ByteArray? {
//        val stmt = getArchiveDataStmt
        connection.prepareStatement("SELECT `DATA` FROM `cache` WHERE `KEY` = ?;").use { stmt ->
            stmt.clearParameters()
            stmt.setInt(1, id)
            stmt.executeQuery().use {
                if (!it.next()) return null
                return it.getBytes("DATA")
            }
        }
    }

    fun getRawTable(): ByteArray? {
        getReferenceDataStmt.executeQuery().use {
            if (!it.next()) return null
            return it.getBytes("DATA")
        }
    }
}