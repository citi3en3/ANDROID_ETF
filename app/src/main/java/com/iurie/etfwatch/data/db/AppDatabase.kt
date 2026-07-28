package com.iurie.etfwatch.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [EtfEntity::class, QuoteEntity::class, PriceAlertEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun etfDao(): EtfDao
    abstract fun quoteDao(): QuoteDao
    abstract fun alertDao(): AlertDao

    companion object {
        /**
         * Adds the alert hysteresis latch. Written as a real migration rather than leaning on
         * `fallbackToDestructiveMigration()` so upgrading does not wipe the user's watchlist.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alerts ADD COLUMN armed INTEGER NOT NULL DEFAULT 1")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_4_5)
    }
}
