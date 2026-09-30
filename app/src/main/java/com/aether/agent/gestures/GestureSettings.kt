package com.aether.agent.gestures

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gestureStore: DataStore<Preferences> by preferencesDataStore("aether_gestures")

class GestureSettings(private val context: Context) {

    private object K {
        val SINGLE = stringPreferencesKey("g_single")
        val DOUBLE = stringPreferencesKey("g_double")
        val LONG = stringPreferencesKey("g_long")
        val LEFT = stringPreferencesKey("g_left")
        val RIGHT = stringPreferencesKey("g_right")
        val DOWN = stringPreferencesKey("g_down")
        val UP = stringPreferencesKey("g_up")
        val WIDTH = floatPreferencesKey("island_w")
        val HEIGHT = floatPreferencesKey("island_h")
        val OFFSET_Y = floatPreferencesKey("island_y")
        val OFFSET_X = floatPreferencesKey("island_x")
        val VISIBLE = booleanPreferencesKey("island_visible")
    }

    // Defaults: sensible, NOT all power menu
    val singleTap: Flow<NotchAction> = context.gestureStore.data.map {
        NotchAction.fromId(it[K.SINGLE] ?: NotchAction.OPEN_AETHER.id)
    }
    val doubleTap: Flow<NotchAction> = context.gestureStore.data.map {
        NotchAction.fromId(it[K.DOUBLE] ?: NotchAction.NOTIFICATIONS.id)
    }
    val longPress: Flow<NotchAction> = context.gestureStore.data.map {
        NotchAction.fromId(it[K.LONG] ?: NotchAction.OPEN_CHAT.id)
    }
    val swipeLeft: Flow<NotchAction> = context.gestureStore.data.map {
        NotchAction.fromId(it[K.LEFT] ?: NotchAction.MEDIA_PREV.id)
    }
    val swipeRight: Flow<NotchAction> = context.gestureStore.data.map {
        NotchAction.fromId(it[K.RIGHT] ?: NotchAction.MEDIA_NEXT.id)
    }
    val swipeDown: Flow<NotchAction> = context.gestureStore.data.map {
        NotchAction.fromId(it[K.DOWN] ?: NotchAction.NOTIFICATIONS.id)
    }
    val swipeUp: Flow<NotchAction> = context.gestureStore.data.map {
        NotchAction.fromId(it[K.UP] ?: NotchAction.HOME.id)
    }

    /** Width of pill as fraction of screen width 0.2–0.9 */
    val islandWidthFrac: Flow<Float> = context.gestureStore.data.map { it[K.WIDTH] ?: 0.40f }
    /** Height in dp 28–80 */
    val islandHeightDp: Flow<Float> = context.gestureStore.data.map { it[K.HEIGHT] ?: 40f }
    /** Vertical offset from top in dp 0–80 */
    val islandOffsetYDp: Flow<Float> = context.gestureStore.data.map { it[K.OFFSET_Y] ?: 8f }
    /** Horizontal offset from center in dp -80..80 */
    val islandOffsetXDp: Flow<Float> = context.gestureStore.data.map { it[K.OFFSET_X] ?: 0f }
    val islandVisible: Flow<Boolean> = context.gestureStore.data.map { it[K.VISIBLE] ?: true }

    suspend fun setGesture(gesture: NotchGesture, action: NotchAction) {
        context.gestureStore.edit { prefs ->
            val key = when (gesture) {
                NotchGesture.SINGLE_TAP -> K.SINGLE
                NotchGesture.DOUBLE_TAP -> K.DOUBLE
                NotchGesture.LONG_PRESS -> K.LONG
                NotchGesture.SWIPE_LEFT -> K.LEFT
                NotchGesture.SWIPE_RIGHT -> K.RIGHT
                NotchGesture.SWIPE_DOWN -> K.DOWN
                NotchGesture.SWIPE_UP -> K.UP
            }
            prefs[key] = action.id
        }
    }

    suspend fun setIslandWidthFrac(v: Float) = context.gestureStore.edit { it[K.WIDTH] = v.coerceIn(0.2f, 0.9f) }
    suspend fun setIslandHeightDp(v: Float) = context.gestureStore.edit { it[K.HEIGHT] = v.coerceIn(28f, 80f) }
    suspend fun setIslandOffsetYDp(v: Float) = context.gestureStore.edit { it[K.OFFSET_Y] = v.coerceIn(0f, 100f) }
    suspend fun setIslandOffsetXDp(v: Float) = context.gestureStore.edit { it[K.OFFSET_X] = v.coerceIn(-100f, 100f) }
    suspend fun setIslandVisible(v: Boolean) = context.gestureStore.edit { it[K.VISIBLE] = v }

    /** Synchronous snapshot for the overlay service (reads once). */
    suspend fun snapshot(): Snapshot {
        var s = Snapshot()
        context.gestureStore.data.map { p ->
            Snapshot(
                single = NotchAction.fromId(p[K.SINGLE] ?: NotchAction.OPEN_AETHER.id),
                double = NotchAction.fromId(p[K.DOUBLE] ?: NotchAction.NOTIFICATIONS.id),
                long = NotchAction.fromId(p[K.LONG] ?: NotchAction.OPEN_CHAT.id),
                left = NotchAction.fromId(p[K.LEFT] ?: NotchAction.MEDIA_PREV.id),
                right = NotchAction.fromId(p[K.RIGHT] ?: NotchAction.MEDIA_NEXT.id),
                down = NotchAction.fromId(p[K.DOWN] ?: NotchAction.NOTIFICATIONS.id),
                up = NotchAction.fromId(p[K.UP] ?: NotchAction.HOME.id),
                widthFrac = p[K.WIDTH] ?: 0.40f,
                heightDp = p[K.HEIGHT] ?: 40f,
                offsetYDp = p[K.OFFSET_Y] ?: 8f,
                offsetXDp = p[K.OFFSET_X] ?: 0f,
                visible = p[K.VISIBLE] ?: true
            )
        }.collect { s = it; return@collect }
        return s
    }

    data class Snapshot(
        val single: NotchAction = NotchAction.OPEN_AETHER,
        val double: NotchAction = NotchAction.NOTIFICATIONS,
        val long: NotchAction = NotchAction.OPEN_CHAT,
        val left: NotchAction = NotchAction.MEDIA_PREV,
        val right: NotchAction = NotchAction.MEDIA_NEXT,
        val down: NotchAction = NotchAction.NOTIFICATIONS,
        val up: NotchAction = NotchAction.HOME,
        val widthFrac: Float = 0.40f,
        val heightDp: Float = 40f,
        val offsetYDp: Float = 8f,
        val offsetXDp: Float = 0f,
        val visible: Boolean = true
    )
}
