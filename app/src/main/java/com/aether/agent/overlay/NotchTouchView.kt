package com.aether.agent.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
class NotchTouchView(context: Context) : View(context) {

    var onSingleTap: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null
    var onSwipeDown: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null

    /** 0.2–0.9 of parent width */
    var pillWidthFrac: Float = 0.40f
        set(v) { field = v; requestLayout(); invalidate() }
    var pillVisible: Boolean = true
        set(v) { field = v; invalidate() }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xE6000000.toInt()
        style = Paint.Style.FILL
    }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7C9CFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val pill = RectF()

    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onSingleTap?.invoke(); return true
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onDoubleTap?.invoke(); return true
        }
        override fun onLongPress(e: MotionEvent) {
            onLongPress?.invoke()
        }
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (e1 == null) return false
            val dx = e2.x - e1.x
            val dy = e2.y - e1.y
            if (abs(dx) > abs(dy) && abs(dx) > 50) {
                if (dx > 0) onSwipeRight?.invoke() else onSwipeLeft?.invoke()
                return true
            }
            if (abs(dy) > abs(dx) && abs(dy) > 40) {
                if (dy > 0) onSwipeDown?.invoke() else onSwipeUp?.invoke()
                return true
            }
            return false
        }
    })

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val pw = (w * pillWidthFrac).coerceIn(120f, w * 0.95f)
        val ph = (h * 0.75f).coerceAtLeast(28f)
        val left = (w - pw) / 2f
        val top = (h - ph) / 2f
        pill.set(left, top, left + pw, top + ph)
    }

    override fun onDraw(canvas: Canvas) {
        if (!pillVisible) return
        val r = pill.height() / 2f
        canvas.drawRoundRect(pill, r, r, fill)
        canvas.drawRoundRect(pill, r, r, stroke)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return detector.onTouchEvent(event) || super.onTouchEvent(event)
    }
}
