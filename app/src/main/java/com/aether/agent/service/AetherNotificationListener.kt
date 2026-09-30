package com.aether.agent.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Optional: feeds notifications / media into the Dynamic Island.
 * Enable in system Notification access settings.
 */
class AetherNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Forward to Island UI later (package, title, text, media session)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}
