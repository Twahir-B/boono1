package com.aether.agent.service

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.aether.agent.AetherApp
import com.aether.agent.MainActivity
import com.aether.agent.cursor.CursorController

class CursorTriggerService : Service() {

    private var wm: WindowManager? = null
    private val triggers = mutableListOf<View>()
    private var controller: CursorController? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            startForeground(NOTIF_ID, notif())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Grant Display over other apps first", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        controller = CursorController(this)
        addTriggers()
        Toast.makeText(this, "Cursor ON — swipe from bottom left or right edge", Toast.LENGTH_LONG).show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        triggers.forEach { try { wm?.removeView(it) } catch (_: Exception) {} }
        triggers.clear()
        controller?.hide()
        super.onDestroy()
    }

    private fun addTriggers() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = resources.displayMetrics
        val edgeW = (56 * metrics.density).toInt()
        val edgeH = (metrics.heightPixels * 0.40f).toInt()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        listOf(Gravity.START, Gravity.END).forEach { gravity ->
            val v = object : View(this) {
                override fun onTouchEvent(event: MotionEvent): Boolean {
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        if (!AetherAccessibilityService.isEnabled()) {
                            Toast.makeText(
                                this@CursorTriggerService,
                                "Enable Accessibility so cursor can tap",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        val x = if (gravity == Gravity.START) edgeW.toFloat()
                        else (metrics.widthPixels - edgeW).toFloat()
                        controller?.show(x, event.rawY)
                        return true
                    }
                    return false
                }
            }
            // Slightly visible edge so user knows where to swipe
            v.setBackgroundColor(0x227C9CFF)
            val params = WindowManager.LayoutParams(
                edgeW, edgeH, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                this.gravity = Gravity.BOTTOM or gravity
                this.y = (16 * metrics.density).toInt()
            }
            try {
                wm?.addView(v, params)
                triggers.add(v)
            } catch (e: Exception) {
                Toast.makeText(this, "Cursor edge failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun notif(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, AetherApp.CHANNEL_ISLAND)
            .setContentTitle("Quick Cursor active")
            .setContentText("Swipe from bottom edge")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        const val NOTIF_ID = 1003
        const val ACTION_STOP = "com.aether.agent.STOP_CURSOR"

        fun start(ctx: android.content.Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(ctx)) {
                Toast.makeText(ctx, "First grant Display over other apps", Toast.LENGTH_LONG).show()
                return
            }
            val i = Intent(ctx, CursorTriggerService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
                else ctx.startService(i)
            } catch (e: Exception) {
                Toast.makeText(ctx, "Cursor start failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        fun stop(ctx: android.content.Context) {
            ctx.startService(Intent(ctx, CursorTriggerService::class.java).setAction(ACTION_STOP))
        }
    }
}
