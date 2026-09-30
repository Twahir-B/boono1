package com.aether.agent.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Invisible (or lightly visible) touch zone around the camera cutout.
 * Detects single/double/long tap and horizontal swipes.
 */
@SuppressLint("ClickableViewAccessibility")
class NotchTouchView(context: Context) : View(context) {

    var onSingleTap: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000
        style = Paint.Style.FILL
    }

    private val pill = RectF()
    private val handler = Handler(Looper.getMainLooper())

    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onSingleTap?.invoke()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleTap?.invoke()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            onLongPress?.invoke()
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            val dx = e2.x - e1.x
            val dy = e2.y - e1.y
            if (abs(dx) > abs(dy) && abs(dx) > 80) {
                if (dx > 0) onSwipeRight?.invoke() else onSwipeLeft?.invoke()
                return true
            }
            return false
        }
    })

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Centered pill roughly matching typical Dynamic Island size
        val pw = (w * 0.35f).coerceIn(120f, 280f)
        val ph = h * 0.55f
        val left = (w - pw) / 2f
        val top = (h - ph) / 2f
        pill.set(left, top, left + pw, top + ph)
    }

    override fun onDraw(canvas: Canvas) {
        // Subtle visual so users know the zone exists (can be made fully invisible)
        canvas.drawRoundRect(pill, pill.height() / 2f, pill.height() / 2f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Only react if touch is near the pill zone
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (!pill.contains(event.x, event.y) &&
                !isNearPill(event.x, event.y, 40f)
            ) {
                return false
            }
        }
        return detector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    private fun isNearPill(x: Float, y: Float, pad: Float): Boolean {
        return x >= pill.left - pad && x <= pill.right + pad &&
                y >= pill.top - pad && y <= pill.bottom + pad
    }
}
