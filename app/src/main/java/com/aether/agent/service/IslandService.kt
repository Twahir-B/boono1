package com.aether.agent.service

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.aether.agent.AetherApp
import com.aether.agent.MainActivity
import com.aether.agent.gestures.ActionExecutor
import com.aether.agent.gestures.GestureSettings
import com.aether.agent.gestures.NotchAction
import com.aether.agent.overlay.NotchTouchView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class IslandService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var notchView: NotchTouchView? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var gestures: GestureSettings
    private var snap = GestureSettings.Snapshot()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        gestures = GestureSettings(this)
        try { startForeground(NOTIF_ID, buildNotification()) } catch (e: Exception) { e.printStackTrace() }
        scope.launch {
            snap = loadSnap()
            showOverlay()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            ACTION_RELOAD -> {
                scope.launch {
                    snap = loadSnap()
                    removeOverlay()
                    showOverlay()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        removeOverlay()
        super.onDestroy()
    }

    private suspend fun loadSnap(): GestureSettings.Snapshot {
        return GestureSettings.Snapshot(
            single = gestures.singleTap.first(),
            double = gestures.doubleTap.first(),
            long = gestures.longPress.first(),
            left = gestures.swipeLeft.first(),
            right = gestures.swipeRight.first(),
            down = gestures.swipeDown.first(),
            up = gestures.swipeUp.first(),
            widthFrac = gestures.islandWidthFrac.first(),
            heightDp = gestures.islandHeightDp.first(),
            offsetYDp = gestures.islandOffsetYDp.first(),
            offsetXDp = gestures.islandOffsetXDp.first(),
            visible = gestures.islandVisible.first()
        )
    }

    private fun showOverlay() {
        if (overlayView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Grant Display over other apps first", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        val heightPx = (snap.heightDp * density).toInt().coerceIn(dp(28), dp(90))
        val yPx = (snap.offsetYDp * density).toInt()
        val xPx = (snap.offsetXDp * density).toInt()

        val nv = NotchTouchView(this).apply {
            pillWidthFrac = snap.widthFrac
            pillVisible = snap.visible
            onSingleTap = { ActionExecutor.run(this@IslandService, snap.single) }
            onDoubleTap = { ActionExecutor.run(this@IslandService, snap.double) }
            onLongPress = { ActionExecutor.run(this@IslandService, snap.long) }
            onSwipeLeft = { ActionExecutor.run(this@IslandService, snap.left) }
            onSwipeRight = { ActionExecutor.run(this@IslandService, snap.right) }
            onSwipeDown = { ActionExecutor.run(this@IslandService, snap.down) }
            onSwipeUp = { ActionExecutor.run(this@IslandService, snap.up) }
        }
        notchView = nv

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            heightPx,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = xPx
            y = yPx
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val container = FrameLayout(this).apply {
            addView(nv, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }

        try {
            windowManager?.addView(container, params)
            overlayView = container
            Toast.makeText(this, "Island ON — customize in Aether → Notch gestures", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Island failed: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun removeOverlay() {
        try { overlayView?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        overlayView = null
        notchView = null
    }

    private fun buildNotification(): Notification {
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, AetherApp.CHANNEL_ISLAND)
            .setContentTitle("Aether Island active")
            .setContentText("Customize gestures in the app")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pending)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    companion object {
        const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.aether.agent.STOP_ISLAND"
        const val ACTION_RELOAD = "com.aether.agent.RELOAD_ISLAND"

        fun start(context: android.content.Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "First grant Display over other apps", Toast.LENGTH_LONG).show()
                return
            }
            val i = Intent(context, IslandService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
                else context.startService(i)
            } catch (e: Exception) {
                Toast.makeText(context, "Start failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        fun stop(context: android.content.Context) {
            context.startService(Intent(context, IslandService::class.java).setAction(ACTION_STOP))
        }

        fun reload(context: android.content.Context) {
            context.startService(Intent(context, IslandService::class.java).setAction(ACTION_RELOAD))
        }
    }
}
