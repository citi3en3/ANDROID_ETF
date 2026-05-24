package com.iurie.etfwatch.di

import android.content.Context
import androidx.room.Room
import com.iurie.etfwatch.data.db.AlertDao
import com.iurie.etfwatch.data.db.AppDatabase
import com.iurie.etfwatch.data.db.EtfDao
import com.iurie.etfwatch.data.db.QuoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "etfwatch.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun etfDao(db: AppDatabase): EtfDao = db.etfDao()
    @Provides fun quoteDao(db: AppDatabase): QuoteDao = db.quoteDao()
    @Provides fun alertDao(db: AppDatabase): AlertDao = db.alertDao()
}
