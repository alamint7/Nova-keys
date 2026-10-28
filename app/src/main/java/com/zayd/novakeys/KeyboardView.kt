package com.zayd.novakeys

import android.content.Context
import android.graphics.*
import android.os.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class Key(val label: String, val code: String = label, val w: Float = 1f, val kind: Int = 0, val alt: String? = null)

class Theme(val bg: Int, val key: Int, val text: Int, val fn: Int, val accent: Int, val accentText: Int, val orbit: Boolean = false) {
    companion object {
        val names = listOf("Orbit Board", "Nothing Dark", "AMOLED Black")
        val all = listOf(
            Theme(0xFF000000.toInt(), 0xFF1C1D21.toInt(), 0xFFF5F3F0.toInt(), 0xFF1C1D21.toInt(), 0xFFE5BD39.toInt(), 0xFF151515.toInt(), true),
            Theme(0xFF111114.toInt(), 0xFF25262B.toInt(), 0xFFF3F3F5.toInt(), 0xFF303137.toInt(), 0xFFE21E2A.toInt(), 0xFFFFFFFF.toInt()),
            Theme(0xFF000000.toInt(), 0xFF141414.toInt(), 0xFFEDEDED.toInt(), 0xFF222222.toInt(), 0xFFB9473A.toInt(), 0xFFFFFFFF.toInt())
        )
    }
}

class KeyboardView(ctx: Context) : View(ctx) {
    interface Listener { fun onKey(k: Key); fun onCandidate(s: String) }
    var listener: Listener? = null
    var shift = 0
    var candidates: List<String> = emptyList()
    private var rows: List<List<Key>> = emptyList()
    private var theme = Theme.all[0]
    private var hScale = 1f
    private var hapticMs = 15
    private val d = resources.displayMetrics.density
    private val candH = 48 * d
    private val rowH get() = 52 * d * hScale
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val rects = ArrayList<Pair<Key, RectF>>()
    private var pressed: Key? = null
    private var longDone = false
    private val h = Handler(Looper.getMainLooper())
    private val vib = ctx.getSystemService(Vibrator::class.java)
    private val orbitPalette = intArrayOf(0xFFE5BD39.toInt(), 0xFF31598C.toInt(), 0xFF1C1D21.toInt(), 0xFFF2F0EC.toInt(), 0xFFC83239.toInt(), 0xFF31598C.toInt(), 0xFFF2F0EC.toInt(), 0xFFC83239.toInt())

    private val repeat = object : Runnable {
        override fun run() { pressed?.let { if (it.code == "DEL") { listener?.onKey(it); h.postDelayed(this, 55) } } }
    }
    private val longPress = Runnable {
        pressed?.alt?.let { longDone = true; buzz(); listener?.onKey(Key(it)); invalidate() }
    }

    fun setup(r: List<List<Key>>, t: Theme, hs: Float, hm: Int) {
        rows = r; theme = t; hScale = hs; hapticMs = hm; requestLayout(); invalidate()
    }

    @Suppress("DEPRECATION")
    private fun buzz() {
        if (hapticMs <= 0) return
        if (Build.VERSION.SDK_INT >= 26) vib?.vibrate(VibrationEffect.createOneShot(hapticMs.toLong(), 120)) else vib?.vibrate(hapticMs.toLong())
    }

    override fun onMeasure(w: Int, hh: Int) =
        setMeasuredDimension(MeasureSpec.getSize(w), (candH + rows.size * rowH + 4 * d).toInt())

    private fun layoutKeys() {
        rects.clear()
        if (rows.isEmpty()) return
        val unit = width / max(10f, rows.maxOf { r -> r.sumOf { it.w.toDouble() }.toFloat() })
        rows.forEachIndexed { ri, row ->
            var x = (width - row.sumOf { it.w.toDouble() }.toFloat() * unit) / 2
            val top = candH + ri * rowH
            for (k in row) { rects.add(k to RectF(x + 3 * d, top + 3 * d, x + k.w * unit - 3 * d, top + rowH - 3 * d)); x += k.w * unit }
        }
    }

    private fun fill(k: Key): Int {
        if (theme.orbit && k.kind == 0 && k.code.length == 1) {
            val index = rects.indexOfFirst { it.first === k }.coerceAtLeast(0)
            return orbitPalette[(index + index / 10) % orbitPalette.size]
        }
        return when {
            k.code == "SHIFT" && shift != 0 -> theme.accent
            k.kind == 1 -> theme.fn
            k.kind == 2 -> theme.accent
            else -> theme.key
        }
    }
    private fun textCol(k: Key): Int {
        val color = fill(k)
        return if (color == theme.accent || color == 0xFFE5BD39.toInt() || color == 0xFFC83239.toInt()) Color.WHITE.let {
            if (color == 0xFFE5BD39.toInt() || color == 0xFFF2F0EC.toInt()) 0xFF151515.toInt() else it
        } else theme.text
    }
    private fun label(k: Key) = when {
        k.code == "SHIFT" -> if (shift == 2) "⇪" else "⇧"
        k.kind == 0 && shift != 0 && k.label.firstOrNull()?.isLetter() == true -> k.label.uppercase()
        else -> k.label
    }

    override fun onDraw(c: Canvas) {
        layoutKeys()
        c.drawColor(theme.bg)
        if (candidates.isNotEmpty()) {
            val cw = width / candidates.size.toFloat()
            candidates.forEachIndexed { i, s ->
                p.color = theme.text; p.textSize = 20 * d; p.isFakeBoldText = i == 0
                c.drawText(s, cw * i + cw / 2, candH * 0.65f, p)
            }
            p.isFakeBoldText = false
        }
        for ((k, r) in rects) {
            p.style = Paint.Style.FILL; p.color = fill(k)
            c.drawRoundRect(r, 9 * d, 9 * d, p)
            if (k === pressed) { p.color = 0x40808080; c.drawRoundRect(r, 9 * d, 9 * d, p) }
            p.color = textCol(k); p.textSize = (if (k.kind == 0) 21 else 16) * d
            c.drawText(label(k), r.centerX(), r.centerY() + p.textSize * 0.35f, p)
            k.alt?.let { p.textSize = 10 * d; p.alpha = 150; c.drawText(it, r.right - 9 * d, r.top + 13 * d, p); p.alpha = 255 }
        }
        pressed?.takeIf { it.kind == 0 && it.code.length == 1 }?.let { k ->
            val r = rects.firstOrNull { it.first === k }?.second
            if (r != null) {
                val bw = r.width() * 1.35f; val bh = 44 * d
                val b = RectF(r.centerX() - bw / 2, max(0f, r.top - 2 * d - bh), r.centerX() + bw / 2, max(0f, r.top - 2 * d - bh) + bh)
                b.offset(if (b.left < 0) -b.left else if (b.right > width) width - b.right else 0f, 0f)
                p.style = Paint.Style.FILL; p.color = fill(k); c.drawRoundRect(b, 12 * d, 12 * d, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 2 * d; p.color = theme.accent; c.drawRoundRect(b, 12 * d, 12 * d, p)
                p.style = Paint.Style.FILL; p.color = textCol(k); p.textSize = 26 * d
                c.drawText(label(k), b.centerX(), b.centerY() + 9 * d, p)
            }
        }
    }

    private fun keyAt(x: Float, y: Float) = rects.firstOrNull { it.second.contains(x, y) }?.first

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val k = if (e.y < candH) null else keyAt(e.x, e.y)
                if (k !== pressed) {
                    h.removeCallbacks(repeat); h.removeCallbacks(longPress)
                    pressed = k; longDone = false
                    if (k != null) {
                        buzz()
                        if (k.code == "DEL") { listener?.onKey(k); h.postDelayed(repeat, 400) }
                        else if (k.alt != null) h.postDelayed(longPress, 380)
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                h.removeCallbacks(repeat); h.removeCallbacks(longPress)
                val k = pressed
                if (e.y < candH && candidates.isNotEmpty()) {
                    val i = (e.x / (width / candidates.size.toFloat())).toInt().coerceIn(0, candidates.size - 1)
                    listener?.onCandidate(candidates[i])
                } else if (k != null && !longDone && k.code != "DEL") listener?.onKey(k)
                pressed = null; invalidate()
            }
            MotionEvent.ACTION_CANCEL -> { h.removeCallbacks(repeat); h.removeCallbacks(longPress); pressed = null; invalidate() }
        }
        return true
    }
}