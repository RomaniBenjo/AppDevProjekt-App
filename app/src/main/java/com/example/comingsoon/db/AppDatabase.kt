package com.example.comingsoon.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [JourneyEntity::class, ClaimedCountryEntity::class, FriendEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journeyDao(): JourneyDao
    abstract fun claimedCountryDao(): ClaimedCountryDao
    abstract fun friendDao(): FriendDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "journey_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS friends (
                        identityKey TEXT NOT NULL PRIMARY KEY,
                        serverUserId INTEGER,
                        deviceId TEXT,
                        displayName TEXT NOT NULL,
                        email TEXT NOT NULL,
                        pictureUrl TEXT,
                        addedNearby INTEGER NOT NULL,
                        isServerFriend INTEGER NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE friends ADD COLUMN deletedLocally INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("ALTER TABLE friends ADD COLUMN pairingId TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journeys ADD COLUMN serverId INTEGER")
                db.execSQL("ALTER TABLE journeys ADD COLUMN deletedLocally INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE claimed_countries ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
