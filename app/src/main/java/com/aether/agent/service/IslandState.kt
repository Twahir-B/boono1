package com.aether.agent.service

import android.app.PendingIntent
import android.graphics.Bitmap
import android.media.session.MediaController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class NotifInfo(
    val key: String,
    val pkg: String,
    val title: String,
    val text: String,
    val icon: Bitmap?,
    val intent: PendingIntent?,
    val postedAt: Long
)

data class MediaInfo(
    val pkg: String,
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val playing: Boolean
)

/** What the Island view actually draws. */
data class IslandContent(
    val isMedia: Boolean,
    val title: String,
    val subtitle: String,
    val image: Bitmap?,
    val playing: Boolean
)

data class IslandUi(val notif: NotifInfo? = null, val media: MediaInfo? = null)

/** Shared between AetherNotificationListener (producer) and IslandService (consumer). */
object IslandState {
    val ui = MutableStateFlow(IslandUi())

    @Volatile var controller: MediaController? = null
    @Volatile var listenerConnected = false

    fun setNotif(n: NotifInfo?) = ui.update { it.copy(notif = n) }
    fun setMedia(m: MediaInfo?) = ui.update { it.copy(media = m) }
}
