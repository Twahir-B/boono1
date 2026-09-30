package com.aether.agent.service

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.aether.agent.AetherApp
import com.aether.agent.MainActivity
import com.aether.agent.cursor.CursorController

/**
 * Thin edge triggers at bottom-left / bottom-right to invoke Quick Cursor.
 */
class CursorTriggerService : Service() {

    private var wm: WindowManager? = null
    private val triggers = mutableListOf<View>()
    private var controller: CursorController? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, notif())
        controller = CursorController(this)
        addTriggers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        triggers.forEach {
            try { wm?.removeView(it) } catch (_: Exception) {}
        }
        triggers.clear()
        controller?.hide()
        super.onDestroy()
    }

    private fun addTriggers() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = resources.displayMetrics
        val edgeW = (48 * metrics.density).toInt()
        val edgeH = (metrics.heightPixels * 0.35f).toInt()
        val y = metrics.heightPixels - edgeH - (24 * metrics.density).toInt()

        listOf(Gravity.START, Gravity.END).forEach { gravity ->
            val v = object : View(this) {
                override fun onTouchEvent(event: MotionEvent): Boolean {
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        val x = if (gravity == Gravity.START) edgeW.toFloat()
                        else (metrics.widthPixels - edgeW).toFloat()
                        val yTouch = event.rawY
                        controller?.show(x, yTouch)
                        return true
                    }
                    return false
                }
            }
            val params = WindowManager.LayoutParams(
                edgeW, edgeH,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                this.gravity = Gravity.BOTTOM or gravity
                this.y = (24 * metrics.density).toInt()
            }
            try {
                wm?.addView(v, params)
                triggers.add(v)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun notif(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, AetherApp.CHANNEL_ISLAND)
            .setContentTitle("Quick Cursor active")
            .setContentText("Swipe from bottom edge to reach the top")
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
            val i = Intent(ctx, CursorTriggerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }

        fun stop(ctx: android.content.Context) {
            ctx.startService(Intent(ctx, CursorTriggerService::class.java).setAction(ACTION_STOP))
        }
    }
}
