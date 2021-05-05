package com.gorilla.gorillagroove.database

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gorilla.gorillagroove.GGApplication
import com.gorilla.gorillagroove.database.dao.*
import com.gorilla.gorillagroove.database.entity.*
import com.gorilla.gorillagroove.database.migrations.MIGRATION_1_2
import com.gorilla.gorillagroove.di.Network
import com.gorilla.gorillagroove.service.GGLog.logDebug
import com.gorilla.gorillagroove.service.GGLog.logError
import com.gorilla.gorillagroove.service.GGLog.logInfo
import com.gorilla.gorillagroove.service.GGLog.logWarn
import com.gorilla.gorillagroove.util.Constants
import com.gorilla.gorillagroove.util.getNullableLong
import com.gorilla.gorillagroove.util.sharedPreferences
import kotlinx.coroutines.runBlocking
import java.io.File
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
@TypeConverters(InstantTypeConverter::class, OfflineAvailabilityTypeConverter::class)
abstract class GorillaDatabase: RoomDatabase() {

    abstract fun syncStatusDao(): SyncStatusDao
    abstract fun trackDao(): TrackDao
    abstract fun userDao(): UserDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistTrackDao(): PlaylistTrackDao
    abstract fun reviewSourceDao(): ReviewSourceDao

    companion object {
        private const val DATABASE_NAME: String = "gorilla_db"

        private var activeDatabase: GorillaDatabase? = null

        @Synchronized
        fun getDatabase(): GorillaDatabase {
            activeDatabase?.let { return it }

            runBlocking {
                migrateDbIfNeeded()
            }

            val databaseName = getDatabaseName()

            logDebug("Using database with name '$databaseName'")

            val newDatabase = Room.databaseBuilder(
                GGApplication.application,
                GorillaDatabase::class.java,
                databaseName
            ).addMigrations(
                MIGRATION_1_2
            ).build()

            activeDatabase = newDatabase
            return newDatabase
        }

        val syncStatusDao get() = getDatabase().syncStatusDao()
        val trackDao get() = getDatabase().trackDao()
        val userDao get() = getDatabase().userDao()
        val playlistDao get() = getDatabase().playlistDao()
        val playlistTrackDao get() = getDatabase().playlistTrackDao()
        val reviewSourceDao get() = getDatabase().reviewSourceDao()

        // TODO can be removed after 2.0 released
        private suspend fun migrateDbIfNeeded() {
            val userId = sharedPreferences.getNullableLong(Constants.KEY_USER_ID)
            if (userId == null) {
                logWarn("User has no ID assigned. Fetching new ID from API. This should only happen once.")
                try {
                    val foundId = Network.api.getSelf().id
                    logInfo("Discovered that the user's ID is $userId. Saving this to shared prefs")
                    sharedPreferences.edit().putLong(Constants.KEY_USER_ID, foundId).apply()
                } catch (e: Throwable) {
                    logError("Failed to get user ID when migrating DB!", e)
                    return
                }
            }

            val legacyDb = GGApplication.application.getDatabasePath(DATABASE_NAME)
            if (legacyDb.exists()) {
                legacyDb.delete()
            }
        }

        fun getDatabaseFile(): File = GGApplication.application.getDatabasePath(getDatabaseName())

        private fun getDatabaseName(): String {
            // TODO Once 2.0 is released this default value can be removed.
            // The only way this should happen is if someone migrates while they have no internet.
            val id = sharedPreferences.getLong(Constants.KEY_USER_ID, 8675309)
            return "$DATABASE_NAME-$id"
        }

        @Synchronized
        fun close() {
            activeDatabase?.close()
            activeDatabase = null
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
