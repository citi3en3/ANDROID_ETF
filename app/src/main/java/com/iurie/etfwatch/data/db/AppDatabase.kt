package com.iurie.etfwatch.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EtfEntity::class, QuoteEntity::class, PriceAlertEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun etfDao(): EtfDao
    abstract fun quoteDao(): QuoteDao
    abstract fun alertDao(): AlertDao
}
