package com.aether.agent.service

import android.app.Notification
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Feeds notifications and the active media session into the Dynamic Island.
 * Needs "Notification access" enabled for Aether in system settings.
 */
class AetherNotificationListener : NotificationListenerService() {

    private val main = Handler(Looper.getMainLooper())
    private var msm: MediaSessionManager? = null
    private var tracked: MediaController? = null

    private val component by lazy { ComponentName(this, AetherNotificationListener::class.java) }

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { list -> pick(list) }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) { publishMedia() }
        override fun onPlaybackStateChanged(state: PlaybackState?) { publishMedia() }
        override fun onSessionDestroyed() {
            try { pick(msm?.getActiveSessions(component)) } catch (_: Exception) { pick(null) }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        IslandState.listenerConnected = true
        try {
            val m = getSystemService(MediaSessionManager::class.java)
            msm = m
            m.addOnActiveSessionsChangedListener(sessionsListener, component, main)
            pick(m.getActiveSessions(component))
        } catch (_: Exception) {
            // Notification access not fully granted yet
        }
    }

    override fun onListenerDisconnected() {
        IslandState.listenerConnected = false
        try { msm?.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Exception) {}
        tracked?.unregisterCallback(controllerCallback)
        tracked = null
        IslandState.controller = null
        IslandState.setMedia(null)
        super.onListenerDisconnected()
    }

    private fun pick(list: List<MediaController>?) {
        val best = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list?.firstOrNull()
        if (best?.sessionToken != tracked?.sessionToken) {
            tracked?.unregisterCallback(controllerCallback)
            tracked = best
            best?.registerCallback(controllerCallback, main)
        }
        publishMedia()
    }

    private fun publishMedia() {
        val c = tracked
        if (c == null) {
            IslandState.controller = null
            IslandState.setMedia(null)
            return
        }
        val md = c.metadata
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        if (title.isBlank()) {
            IslandState.controller = null
            IslandState.setMedia(null)
            return
        }
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val small = art?.let {
            try { Bitmap.createScaledBitmap(it, 160, 160, true) } catch (_: Exception) { null }
        }
        IslandState.controller = c
        IslandState.setMedia(
            MediaInfo(
                pkg = c.packageName,
                title = title,
                artist = artist,
                art = small,
                playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
            )
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val n = sbn.notification ?: return
        if (sbn.packageName == packageName) return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (n.category == Notification.CATEGORY_TRANSPORT ||
            n.category == Notification.CATEGORY_PROGRESS ||
            n.category == Notification.CATEGORY_SERVICE
        ) return

        val ex = n.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (ex.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: ex.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        val icon = try {
            toBitmap(packageManager.getApplicationIcon(sbn.packageName))
        } catch (_: Exception) { null }

        IslandState.setNotif(
            NotifInfo(
                key = sbn.key,
                pkg = sbn.packageName,
                title = title,
                text = text,
                icon = icon,
                intent = n.contentIntent,
                postedAt = System.currentTimeMillis()
            )
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val cur = IslandState.ui.value.notif
        if (sbn != null && cur?.key == sbn.key) IslandState.setNotif(null)
    }

    private fun toBitmap(d: Drawable): Bitmap {
        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.setBounds(0, 0, size, size)
        d.draw(c)
        return bmp
    }
}
