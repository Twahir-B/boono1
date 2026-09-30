package com.aether.agent.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("aether_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val NOTCH_ENABLED = booleanPreferencesKey("notch_enabled")
        val CURSOR_ENABLED = booleanPreferencesKey("cursor_enabled")
        val ISLAND_ENABLED = booleanPreferencesKey("island_enabled")
        val DEFAULT_PROVIDER = stringPreferencesKey("default_provider")
        val DEFAULT_MODEL = stringPreferencesKey("default_model")
        val NOTCH_SIZE_DP = floatPreferencesKey("notch_size_dp")
        val CURSOR_TRIGGER_SIDE = stringPreferencesKey("cursor_trigger_side")
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val AGENT_AUTO_APPROVE_SAFE = booleanPreferencesKey("agent_auto_approve_safe")
    }

    val notchEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTCH_ENABLED] ?: true }
    val cursorEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.CURSOR_ENABLED] ?: false }
    val islandEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.ISLAND_ENABLED] ?: true }
    val defaultProvider: Flow<String> = context.dataStore.data.map { it[Keys.DEFAULT_PROVIDER] ?: "openai" }
    val defaultModel: Flow<String> = context.dataStore.data.map { it[Keys.DEFAULT_MODEL] ?: "gpt-4o" }
    val notchSizeDp: Flow<Float> = context.dataStore.data.map { it[Keys.NOTCH_SIZE_DP] ?: 48f }
    val cursorTriggerSide: Flow<String> = context.dataStore.data.map { it[Keys.CURSOR_TRIGGER_SIDE] ?: "both" }
    val voiceEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.VOICE_ENABLED] ?: true }
    val agentAutoApproveSafe: Flow<Boolean> = context.dataStore.data.map { it[Keys.AGENT_AUTO_APPROVE_SAFE] ?: false }

    suspend fun setNotchEnabled(v: Boolean) = context.dataStore.edit { it[Keys.NOTCH_ENABLED] = v }
    suspend fun setCursorEnabled(v: Boolean) = context.dataStore.edit { it[Keys.CURSOR_ENABLED] = v }
    suspend fun setIslandEnabled(v: Boolean) = context.dataStore.edit { it[Keys.ISLAND_ENABLED] = v }
    suspend fun setDefaultProvider(v: String) = context.dataStore.edit { it[Keys.DEFAULT_PROVIDER] = v }
    suspend fun setDefaultModel(v: String) = context.dataStore.edit { it[Keys.DEFAULT_MODEL] = v }
    suspend fun setNotchSizeDp(v: Float) = context.dataStore.edit { it[Keys.NOTCH_SIZE_DP] = v }
    suspend fun setCursorTriggerSide(v: String) = context.dataStore.edit { it[Keys.CURSOR_TRIGGER_SIDE] = v }
    suspend fun setVoiceEnabled(v: Boolean) = context.dataStore.edit { it[Keys.VOICE_ENABLED] = v }
    suspend fun setAgentAutoApproveSafe(v: Boolean) = context.dataStore.edit { it[Keys.AGENT_AUTO_APPROVE_SAFE] = v }
}
