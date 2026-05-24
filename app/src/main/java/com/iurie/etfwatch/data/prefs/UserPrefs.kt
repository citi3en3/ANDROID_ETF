package com.iurie.etfwatch.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "etfwatch_prefs")

@Singleton
class UserPrefs @Inject constructor(@ApplicationContext private val ctx: Context) {

    private object Keys {
        val RefreshIntervalMin = intPreferencesKey("refresh_interval_min")
        val LastRefreshAt = longPreferencesKey("last_refresh_at")
    }

    val refreshIntervalMinutes: Flow<Int> =
        ctx.dataStore.data.map { it[Keys.RefreshIntervalMin] ?: DEFAULT_INTERVAL_MIN }

    val lastRefreshAt: Flow<Long> =
        ctx.dataStore.data.map { it[Keys.LastRefreshAt] ?: 0L }

    suspend fun setRefreshIntervalMinutes(minutes: Int) {
        ctx.dataStore.edit { it[Keys.RefreshIntervalMin] = minutes.coerceIn(MIN_INTERVAL_MIN, MAX_INTERVAL_MIN) }
    }

    suspend fun markRefreshed(ts: Long = System.currentTimeMillis()) {
        ctx.dataStore.edit { it[Keys.LastRefreshAt] = ts }
    }

    companion object {
        const val DEFAULT_INTERVAL_MIN = 30
        const val MIN_INTERVAL_MIN = 15
        const val MAX_INTERVAL_MIN = 240
        val INTERVAL_CHOICES = listOf(15, 30, 60, 120, 240)
    }
}
