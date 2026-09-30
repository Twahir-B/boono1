package com.aether.agent

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.aether.agent.data.SettingsRepository
import com.aether.agent.data.SecureKeyStore

class AetherApp : Application() {

    lateinit var settings: SettingsRepository
        private set
    lateinit var keyStore: SecureKeyStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsRepository(this)
        keyStore = SecureKeyStore(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val island = NotificationChannel(
                CHANNEL_ISLAND,
                getString(R.string.island_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.island_channel_desc)
                setShowBadge(false)
            }
            val agent = NotificationChannel(
                CHANNEL_AGENT,
                "Aether Agent",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Agent status and approvals"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(island)
            nm.createNotificationChannel(agent)
        }
    }

    companion object {
        const val CHANNEL_ISLAND = "aether_island"
        const val CHANNEL_AGENT = "aether_agent"

        @Volatile
        private var instance: AetherApp? = null

        fun get(): AetherApp = instance
            ?: throw IllegalStateException("AetherApp not initialized")
    }
}
