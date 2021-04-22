package com.gorilla.gorillagroove.database

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.database.dao.*
import com.gorilla.gorillagroove.database.entity.*
import com.gorilla.gorillagroove.database.migrations.MIGRATION_1_2
import java.time.Instant

@Database(
    exportSchema = false,
    entities = [
        DbUser::class,
        DbTrack::class,
        DbPlaylist::class,
        DbPlaylistTrack::class,
        DbSyncStatus::class,
        DbReviewSource::class,
    ], version = 2
)
@TypeConverters(InstantTypeConverter::class)
abstract class GorillaDatabase: RoomDatabase() {

    abstract fun syncStatusDao(): SyncStatusDao
    abstract fun trackDao(): TrackDao
    abstract fun userDao(): UserDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistTrackDao(): PlaylistTrackDao
    abstract fun reviewSourceDao(): ReviewSourceDao

    companion object {
        const val DATABASE_NAME: String = "gorilla_db"

        private var activeDatabase: GorillaDatabase? = null

        fun getDatabase(): GorillaDatabase {
            activeDatabase?.let { return it }

            val newDatabase = Room.databaseBuilder(
                GGApplication.application,
                GorillaDatabase::class.java,
                DATABASE_NAME
            ).addMigrations(
                MIGRATION_1_2
            ).build()

            activeDatabase = newDatabase
            return newDatabase
        }
    }
}

fun SupportSQLiteDatabase.execMultipleSQL(sql: String) {
    sql.split(";").forEach { statement ->
        if (statement.isNotBlank()) {
            this.execSQL("$statement;")
        }
    }
}

class InstantTypeConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
}
