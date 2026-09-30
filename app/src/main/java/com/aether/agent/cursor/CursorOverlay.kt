package com.aether.agent.cursor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.aether.agent.service.AetherAccessibilityService
import kotlin.math.max
import kotlin.math.min

/**
 * Quick Cursor: swipe from bottom edge → tracker + floating cursor
 * so one thumb can reach the whole screen (Android 10 friendly).
 */
class CursorController(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var trackerView: TrackerView? = null
    private var cursorView: CursorView? = null
    private var active = false

    fun show(startX: Float, startY: Float) {
        if (active) return
        active = true
        val metrics = context.resources.displayMetrics
        val screenH = metrics.heightPixels.toFloat()
        val screenW = metrics.widthPixels.toFloat()

        val tracker = TrackerView(context).also { trackerView = it }
        val cursor = CursorView(context).also { cursorView = it }

        val trackerParams = baseParams().apply {
            width = dp(72)
            height = dp(72)
            gravity = Gravity.TOP or Gravity.START
            x = startX.toInt().coerceIn(0, metrics.widthPixels - dp(72))
            y = startY.toInt().coerceIn(0, metrics.heightPixels - dp(72))
        }
        val cursorParams = baseParams().apply {
            width = dp(28)
            height = dp(28)
            gravity = Gravity.TOP or Gravity.START
            x = startX.toInt()
            y = (startY - dp(160)).toInt().coerceAtLeast(0)
        }

        tracker.onMove = { dx, dy ->
            trackerParams.x = (trackerParams.x + dx).coerceIn(0, metrics.widthPixels - trackerParams.width)
            trackerParams.y = (trackerParams.y + dy).coerceIn(0, metrics.heightPixels - trackerParams.height)
            wm.updateViewLayout(tracker, trackerParams)

            // Amplify movement for cursor (reach top from bottom half)
            val amplify = 1.8f
            cursorParams.x = (cursorParams.x + dx * amplify).toInt()
                .coerceIn(0, metrics.widthPixels - cursorParams.width)
            cursorParams.y = (cursorParams.y + dy * amplify).toInt()
                .coerceIn(0, metrics.heightPixels - cursorParams.height)
            wm.updateViewLayout(cursor, cursorParams)
        }
        tracker.onTap = {
            val cx = cursorParams.x + cursorParams.width / 2f
            val cy = cursorParams.y + cursorParams.height / 2f
            AetherAccessibilityService.instance?.tap(cx, cy)
            hide()
        }
        tracker.onCancel = { hide() }

        try {
            wm.addView(cursor, cursorParams)
            wm.addView(tracker, trackerParams)
        } catch (e: Exception) {
            e.printStackTrace()
            hide()
        }
    }

    fun hide() {
        active = false
        listOf(trackerView, cursorView).forEach { v ->
            try {
                if (v != null) wm.removeView(v)
            } catch (_: Exception) {}
        }
        trackerView = null
        cursorView = null
    }

    private fun baseParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    )

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()
}

@SuppressLint("ClickableViewAccessibility")
private class TrackerView(context: Context) : View(context) {
    var onMove: ((Float, Float) -> Unit)? = null
    var onTap: (() -> Unit)? = null
    var onCancel: (() -> Unit)? = null
    private var lastX = 0f
    private var lastY = 0f
    private var moved = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAA7C9CFF.toInt()
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        val r = min(width, height) / 2f
        canvas.drawCircle(width / 2f, height / 2f, r * 0.85f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX
                lastY = event.rawY
                moved = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - lastX
                val dy = event.rawY - lastY
                if (dx * dx + dy * dy > 16) moved = true
                onMove?.invoke(dx, dy)
                lastX = event.rawX
                lastY = event.rawY
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!moved) onTap?.invoke() else onCancel?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

private class CursorView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7C9CFF.toInt()
        style = Paint.Style.FILL
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawCircle(cx, cy, width * 0.35f, paint)
        canvas.drawCircle(cx, cy, width * 0.45f, ring)
    }
}
