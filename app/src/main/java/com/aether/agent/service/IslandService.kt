package com.aether.agent.service

import android.animation.ValueAnimator
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.aether.agent.AetherApp
import com.aether.agent.MainActivity
import com.aether.agent.gestures.ActionExecutor
import com.aether.agent.gestures.GestureSettings
import com.aether.agent.overlay.NotchTouchView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class IslandService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var notchView: NotchTouchView? = null
    private var params: WindowManager.LayoutParams? = null
    private var anim: ValueAnimator? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val uiHandler = Handler(Looper.getMainLooper())
    private lateinit var gestures: GestureSettings
    private var snap = GestureSettings.Snapshot()

    private var expanded = false
    private var content: IslandContent? = null

    private val collapseRunnable = Runnable { setExpanded(false) }
    private val refreshRunnable = Runnable { recompute() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        gestures = GestureSettings(this)
        try {
            val n = buildNotification()
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIF_ID, n)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        scope.launch {
            snap = loadSnap()
            showOverlay()
            IslandState.ui.collect { recompute() }
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
        uiHandler.removeCallbacksAndMessages(null)
        anim?.cancel()
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

    // ------------------------------------------------------------ island state

    /** Decide what the island shows right now, and resize the window to fit. */
    private fun recompute() {
        val view = notchView ?: return
        val ui = IslandState.ui.value
        val now = System.currentTimeMillis()

        uiHandler.removeCallbacks(refreshRunnable)
        val n = ui.notif
        val notifLeft = if (n != null) NOTIF_SHOW_MS - (now - n.postedAt) else 0L

        val next: IslandContent? = when {
            n != null && notifLeft > 0 -> {
                uiHandler.postDelayed(refreshRunnable, notifLeft + 50)
                IslandContent(
                    isMedia = false,
                    title = n.title.ifBlank { n.text },
                    subtitle = if (n.title.isBlank()) "" else n.text,
                    image = n.icon,
                    playing = false
                )
            }
            ui.media != null && (ui.media.playing || expanded) -> IslandContent(
                isMedia = true,
                title = ui.media.title,
                subtitle = ui.media.artist,
                image = ui.media.art,
                playing = ui.media.playing
            )
            else -> null
        }

        if (next == null) expanded = false
        content = next
        view.content = next
        view.expanded = expanded
        val (w, h) = targetSize()
        animateTo(w, h)
    }

    private fun setExpanded(v: Boolean) {
        uiHandler.removeCallbacks(collapseRunnable)
        expanded = v && content != null
        if (expanded) uiHandler.postDelayed(collapseRunnable, EXPAND_MS)
        recompute()
    }

    private fun targetSize(): Pair<Int, Int> {
        val sw = resources.displayMetrics.widthPixels
        val density = resources.displayMetrics.density
        val idleW = (sw * snap.widthFrac).toInt().coerceIn(dp(90), sw)
        val idleH = (snap.heightDp * density).toInt().coerceIn(dp(28), dp(90))
        val maxW = (sw * 0.92f).toInt()
        val ct = content
        return when {
            ct != null && expanded -> Pair(maxW, dp(if (ct.isMedia) 150 else 118))
            ct != null -> Pair(maxOf(idleW, (sw * 0.62f).toInt()).coerceAtMost(maxW), idleH)
            else -> Pair(idleW, idleH)
        }
    }

    private fun animateTo(w: Int, h: Int) {
        val p = params ?: return
        val container = overlayView ?: return
        if (p.width == w && p.height == h) return
        anim?.cancel()
        val fw = p.width
        val fh = p.height
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener { a ->
                val t = a.animatedValue as Float
                p.width = (fw + (w - fw) * t).toInt()
                p.height = (fh + (h - fh) * t).toInt()
                try { windowManager?.updateViewLayout(container, p) } catch (_: Exception) {}
            }
            start()
        }
    }

    private fun openContent() {
        val ui = IslandState.ui.value
        val ct = content
        try {
            if (ct != null && ct.isMedia) {
                ui.media?.let { m ->
                    packageManager.getLaunchIntentForPackage(m.pkg)?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(it)
                    }
                }
            } else {
                ui.notif?.let { n ->
                    val pi = n.intent
                    if (pi != null) pi.send()
                    else packageManager.getLaunchIntentForPackage(n.pkg)?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(it)
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Can't open: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        setExpanded(false)
    }

    // ----------------------------------------------------------------- overlay

    private fun showOverlay() {
        if (overlayView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Grant Display over other apps first", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density
        val yPx = (snap.offsetYDp * density).toInt()
        val xPx = (snap.offsetXDp * density).toInt()

        expanded = false
        content = null
        val (w0, h0) = targetSize()

        val nv = NotchTouchView(this).apply {
            pillVisible = snap.visible
            onSingleTap = { ActionExecutor.run(this@IslandService, snap.single) }
            onDoubleTap = { ActionExecutor.run(this@IslandService, snap.double) }
            onLongPress = { ActionExecutor.run(this@IslandService, snap.long) }
            onSwipeLeft = { ActionExecutor.run(this@IslandService, snap.left) }
            onSwipeRight = { ActionExecutor.run(this@IslandService, snap.right) }
            onSwipeDown = { ActionExecutor.run(this@IslandService, snap.down) }
            onSwipeUp = { ActionExecutor.run(this@IslandService, snap.up) }

            onExpandRequest = { setExpanded(true) }
            onCollapseRequest = { setExpanded(false) }
            onOpenContent = { openContent() }
            onMediaPrev = { IslandState.controller?.transportControls?.skipToPrevious() }
            onMediaNext = { IslandState.controller?.transportControls?.skipToNext() }
            onMediaToggle = {
                val playing = IslandState.ui.value.media?.playing == true
                val tc = IslandState.controller?.transportControls
                if (playing) tc?.pause() else tc?.play()
            }
            onInteraction = {
                if (this@IslandService.expanded) {
                    uiHandler.removeCallbacks(collapseRunnable)
                    uiHandler.postDelayed(collapseRunnable, EXPAND_MS)
                }
            }
        }
        notchView = nv

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val lp = WindowManager.LayoutParams(
            w0, h0, type,
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
        params = lp

        val container = FrameLayout(this).apply {
            addView(nv, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }

        try {
            windowManager?.addView(container, lp)
            overlayView = container
            val hint = if (IslandState.listenerConnected) "Island ON"
            else "Island ON — enable Notification access to show notifications & media"
            Toast.makeText(this, hint, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Island failed: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun removeOverlay() {
        anim?.cancel()
        try { overlayView?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        overlayView = null
        notchView = null
        params = null
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
        private const val NOTIF_SHOW_MS = 6000L
        private const val EXPAND_MS = 8000L

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
