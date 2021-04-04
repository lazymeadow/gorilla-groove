package com.gorilla.gorillagroove.database.dao

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

abstract class BaseRoomDao<T>(private val tableName: String) {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(obj: T): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(obj: List<T>): List<Long>

    @Delete
    abstract fun delete(obj: T): Int

    fun delete(id: Long) {
        val query = SimpleSQLiteQuery("DELETE FROM $tableName WHERE id = $id")
        executeSqlWithReturn(query)
    }

    fun delete(ids: List<Long>) {
        if (ids.isEmpty()) {
            return
        }

        val idString = ids.joinToString(",")

        val query = SimpleSQLiteQuery("DELETE FROM $tableName WHERE id IN ($idString)")
        executeSqlWithReturn(query)
    }

    fun findById(id: Long): T? {
        val query = SimpleSQLiteQuery("SELECT * FROM $tableName WHERE id = $id")
        return executeSqlWithReturn(query).firstOrNull()
    }

    fun findById(ids: List<Long>): List<T> {
        if (ids.isEmpty()) {
            return emptyList()
        }

        val idString = ids.joinToString(",")
        val query = SimpleSQLiteQuery("SELECT * FROM $tableName WHERE id IN ($idString)")

        return executeSqlWithReturn(query)
    }

    fun findAll(): List<T> {
        val query = SimpleSQLiteQuery("SELECT * FROM $tableName")
        return executeSqlWithReturn(query)
    }

    @RawQuery
    abstract fun executeSqlWithReturn(query: SupportSQLiteQuery): List<T>
}
