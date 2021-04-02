package com.gorilla.gorillagroove.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gorilla.gorillagroove.database.entity.*
import java.time.Instant

@Database(
    entities = [
        TrackCacheEntity::class,
        UserCacheEntity::class,
        PlaylistKeyCacheEntity::class,
        PlaylistItemReferenceData::class,

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

    abstract fun databaseDao(): DatabaseDao

    companion object {
        const val DATABASE_NAME: String = "gorilla_db"
    }
}

fun SupportSQLiteDatabase.execMultipleSQL(sql: String) {
    sql.split(";").forEach { statement ->
        if (statement.isNotBlank()) {
            this.execSQL("$statement;")
        }
    }
}

object InstantTypeConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
}
