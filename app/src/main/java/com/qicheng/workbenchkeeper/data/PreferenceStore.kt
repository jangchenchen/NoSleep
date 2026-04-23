package com.qicheng.workbenchkeeper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.qicheng.workbenchkeeper.model.AppSettings
import com.qicheng.workbenchkeeper.model.WorkbenchPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "workbench_keeper")

class PreferenceStore(
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val settings = stringPreferencesKey("settings_json")
        val presets = stringPreferencesKey("presets_json")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        preferences[Keys.settings]
            ?.let { saved ->
                runCatching { json.decodeFromString(AppSettings.serializer(), saved) }.getOrNull()
            }
            ?: AppSettings()
    }

    val presetsFlow: Flow<List<WorkbenchPreset>> = context.dataStore.data.map { preferences ->
        preferences[Keys.presets]
            ?.let { saved ->
                runCatching {
                    json.decodeFromString(ListSerializer(WorkbenchPreset.serializer()), saved)
                }.getOrNull()
            }
            ?: emptyList()
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.settings] = json.encodeToString(AppSettings.serializer(), settings)
        }
    }

    suspend fun upsertPreset(preset: WorkbenchPreset) {
        context.dataStore.edit { preferences ->
            val current = decodePresets(preferences[Keys.presets]).toMutableList()
            val existingIndex = current.indexOfFirst { it.id == preset.id }
            if (existingIndex >= 0) {
                current[existingIndex] = preset
            } else {
                current.add(0, preset)
            }
            preferences[Keys.presets] = encodePresets(current)
        }
    }

    suspend fun deletePreset(presetId: String) {
        context.dataStore.edit { preferences ->
            val updated = decodePresets(preferences[Keys.presets]).filterNot { it.id == presetId }
            preferences[Keys.presets] = encodePresets(updated)
        }
    }

    private fun decodePresets(serialized: String?): List<WorkbenchPreset> {
        if (serialized.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(WorkbenchPreset.serializer()), serialized)
        }.getOrDefault(emptyList())
    }

    private fun encodePresets(presets: List<WorkbenchPreset>): String {
        return json.encodeToString(ListSerializer(WorkbenchPreset.serializer()), presets)
    }
}
