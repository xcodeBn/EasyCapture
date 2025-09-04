package com.pisces.xcodebn.easycapture.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pisces.xcodebn.easycapture.domain.model.RecordingQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsLocalDataSource(private val context: Context) {

    private val qualityKey = stringPreferencesKey("recording_quality")

    suspend fun saveQualitySetting(quality: RecordingQuality) {
        context.dataStore.edit {
            it[qualityKey] = quality.javaClass.simpleName
        }
    }

    fun getSavedQualitySetting(): Flow<RecordingQuality> {
        return context.dataStore.data.map {
            when (it[qualityKey]) {
                RecordingQuality.LOW::class.java.simpleName -> RecordingQuality.LOW
                RecordingQuality.MEDIUM::class.java.simpleName -> RecordingQuality.MEDIUM
                RecordingQuality.HIGH::class.java.simpleName -> RecordingQuality.HIGH
                else -> RecordingQuality.MEDIUM // Default value
            }
        }
    }
}