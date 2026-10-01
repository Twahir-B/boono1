package com.aether.agent.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.aether.agent.service.IslandContent
import kotlin.math.abs

/**
 * The Dynamic Island. Fills its window (IslandService sizes the window).
 *  - content == null  -> plain pill, configurable gestures
 *  - content != null  -> compact: art/icon + title (+ equalizer for media)
 *  - expanded         -> big card; media gets prev / play-pause / next buttons
 */
@SuppressLint("ClickableViewAccessibility")
class NotchTouchView(context: Context) : View(context) {

    // Configurable gestures (used when the island shows nothing, or for non-card gestures)
    var onSingleTap: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null
    var onSwipeDown: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null

    // Card interactions
    var onExpandRequest: (() -> Unit)? = null
    var onCollapseRequest: (() -> Unit)? = null
    var onOpenContent: (() -> Unit)? = null
    var onMediaPrev: (() -> Unit)? = null
    var onMediaToggle: (() -> Unit)? = null
    var onMediaNext: (() -> Unit)? = null
    var onInteraction: (() -> Unit)? = null

    var pillVisible: Boolean = true
        set(v) { field = v; invalidate() }
    var content: IslandContent? = null
        set(v) { field = v; invalidate() }
    var expanded: Boolean = false
        set(v) { field = v; invalidate() }

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF000000.toInt() }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7C9CFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val gray = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A2A2E.toInt() }
    private val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF7C9CFF.toInt() }
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; typeface = Typeface.DEFAULT_BOLD
    }
    private val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB0B0B8.toInt() }

    private val rect = RectF()
    private val tmp = RectF()
    private val path = Path()
    private val btnPrev = RectF()
    private val btnPlay = RectF()
    private val btnNext = RectF()

    private val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean { onInteraction?.invoke(); return true }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val ct = content
            if (ct == null) { onSingleTap?.invoke(); return true }
            if (!expanded) { onExpandRequest?.invoke(); return true }
            if (ct.isMedia) {
                when {
                    btnPrev.contains(e.x, e.y) -> onMediaPrev?.invoke()
                    btnPlay.contains(e.x, e.y) -> onMediaToggle?.invoke()
                    btnNext.contains(e.x, e.y) -> onMediaNext?.invoke()
                    else -> onOpenContent?.invoke()
                }
            } else {
                onOpenContent?.invoke()
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean { onDoubleTap?.invoke(); return true }
        override fun onLongPress(e: MotionEvent) { onLongPress?.invoke() }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            if (e1 == null) return false
            val dx = e2.x - e1.x
            val dy = e2.y - e1.y
            if (abs(dx) > abs(dy) && abs(dx) > 50) {
                if (dx > 0) onSwipeRight?.invoke() else onSwipeLeft?.invoke()
                return true
            }
            if (abs(dy) > abs(dx) && abs(dy) > 40) {
                if (dy < 0 && expanded) { onCollapseRequest?.invoke(); return true }
                if (dy > 0) onSwipeDown?.invoke() else onSwipeUp?.invoke()
                return true
            }
            return false
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean =
        detector.onTouchEvent(event) || super.onTouchEvent(event)

    // ---------------------------------------------------------------- drawing

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val ct = content
        if (ct == null && !pillVisible) return

        val r = if (expanded && ct != null) minOf(dp(30f), h / 2f) else h / 2f
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, r, r, fill)

        if (ct == null) {
            tmp.set(1.5f, 1.5f, w - 1.5f, h - 1.5f)
            canvas.drawRoundRect(tmp, r, r, stroke)
            return
        }
        if (expanded) drawExpanded(canvas, ct, w, h) else drawCompact(canvas, ct, w, h)
    }

    private fun centerBaseline(p: Paint, cy: Float): Float {
        val fm = p.fontMetrics
        return cy - (fm.ascent + fm.descent) / 2f
    }

    private fun drawImage(c: Canvas, bmp: Bitmap?, left: Float, top: Float, size: Float, radius: Float) {
        tmp.set(left, top, left + size, top + size)
        if (bmp == null) {
            c.drawRoundRect(tmp, radius, radius, gray)
            return
        }
        c.save()
        path.reset()
        path.addRoundRect(tmp, radius, radius, Path.Direction.CW)
        c.clipPath(path)
        c.drawBitmap(bmp, null, tmp, bmpPaint)
        c.restore()
    }

    private fun drawBars(c: Canvas, left: Float, cy: Float, playing: Boolean) {
        val hs = if (playing) floatArrayOf(dp(8f), dp(14f), dp(10f))
        else floatArrayOf(dp(3f), dp(3f), dp(3f))
        for (i in 0..2) {
            val x = left + i * dp(6f)
            tmp.set(x, cy - hs[i] / 2f, x + dp(3f), cy + hs[i] / 2f)
            c.drawRoundRect(tmp, dp(1.5f), dp(1.5f), accent)
        }
    }

    private fun drawCompact(c: Canvas, ct: IslandContent, w: Float, h: Float) {
        val pad = h * 0.18f
        val img = h - pad * 2f
        drawImage(c, ct.image, pad, pad, img, img / 2f)
        val left = pad * 2f + img
        val rightPad = if (ct.isMedia) dp(30f) else pad * 1.5f
        titlePaint.textSize = (h * 0.34f).coerceIn(dp(11f), dp(15f))
        val avail = w - left - rightPad
        if (avail > dp(20f)) {
            val t = TextUtils.ellipsize(ct.title, titlePaint, avail, TextUtils.TruncateAt.END).toString()
            c.drawText(t, left, centerBaseline(titlePaint, h / 2f), titlePaint)
        }
        if (ct.isMedia) drawBars(c, w - pad * 1.5f - dp(16f), h / 2f, ct.playing)
    }

    private fun drawExpanded(c: Canvas, ct: IslandContent, w: Float, h: Float) {
        val pad = dp(16f)
        val img = dp(56f)
        drawImage(c, ct.image, pad, pad, img, dp(12f))
        val tx = pad * 2f + img
        val avail = w - tx - pad

        titlePaint.textSize = dp(16f)
        subPaint.textSize = dp(13f)
        val t = TextUtils.ellipsize(ct.title, titlePaint, avail, TextUtils.TruncateAt.END).toString()
        c.drawText(t, tx, pad + dp(20f), titlePaint)

        if (ct.subtitle.isNotBlank()) {
            if (ct.isMedia) {
                val s = TextUtils.ellipsize(ct.subtitle, subPaint, avail, TextUtils.TruncateAt.END).toString()
                c.drawText(s, tx, pad + dp(40f), subPaint)
            } else {
                val layout = StaticLayout.Builder
                    .obtain(ct.subtitle, 0, ct.subtitle.length, subPaint, avail.toInt().coerceAtLeast(1))
                    .setMaxLines(3)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
                c.save()
                c.translate(tx, pad + dp(28f))
                layout.draw(c)
                c.restore()
            }
        }

        if (ct.isMedia) {
            val cy = h - dp(34f)
            val cx = w / 2f
            val gap = dp(68f)
            val s = dp(10f)
            val hit = dp(30f)
            btnPrev.set(cx - gap - hit, cy - hit, cx - gap + hit, cy + hit)
            btnPlay.set(cx - hit, cy - hit, cx + hit, cy + hit)
            btnNext.set(cx + gap - hit, cy - hit, cx + gap + hit, cy + hit)

            // prev
            val px = cx - gap
            c.drawRect(px - s, cy - s, px - s + dp(3f), cy + s, white)
            path.reset()
            path.moveTo(px + s, cy - s); path.lineTo(px + s, cy + s); path.lineTo(px - s + dp(5f), cy)
            path.close(); c.drawPath(path, white)
            // next
            val nx = cx + gap
            c.drawRect(nx + s - dp(3f), cy - s, nx + s, cy + s, white)
            path.reset()
            path.moveTo(nx - s, cy - s); path.lineTo(nx - s, cy + s); path.lineTo(nx + s - dp(5f), cy)
            path.close(); c.drawPath(path, white)
            // play / pause on a white disc
            c.drawCircle(cx, cy, dp(22f), white)
            if (ct.playing) {
                c.drawRect(cx - s * 0.7f, cy - s * 0.9f, cx - s * 0.2f, cy + s * 0.9f, dark)
                c.drawRect(cx + s * 0.2f, cy - s * 0.9f, cx + s * 0.7f, cy + s * 0.9f, dark)
            } else {
                path.reset()
                path.moveTo(cx - s * 0.5f, cy - s); path.lineTo(cx - s * 0.5f, cy + s); path.lineTo(cx + s * 0.9f, cy)
                path.close(); c.drawPath(path, dark)
            }
        }
    }
}
