package com.timecost.demo.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDs by preferencesDataStore("settings")

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val c: Context) {
    private val key = intPreferencesKey("notify_minute_of_day")
    /** 기본 오전 9시 (F3-5) */
    val notifyTime: Flow<LocalTime> = c.settingsDs.data.map { LocalTime.of((it[key] ?: 540) / 60, (it[key] ?: 540) % 60) }
    suspend fun setNotifyTime(t: LocalTime) { c.settingsDs.edit { it[key] = t.hour * 60 + t.minute } }
}
