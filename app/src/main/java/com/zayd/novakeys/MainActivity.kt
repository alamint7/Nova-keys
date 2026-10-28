package com.zayd.novakeys

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.*

class MainActivity : Activity() {
    private val ink = Color.rgb(23, 32, 51)
    private val paper = Color.rgb(246, 243, 237)
    private val gold = Color.rgb(217, 169, 62)
    private val muted = Color.rgb(112, 119, 132)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int = 18, stroke: Int = 0) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }
    private fun txt(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(2).toFloat(), 1f)
    }
    private fun section(parent: LinearLayout, title: String, subtitle: String? = null): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(Color.WHITE, 20, Color.rgb(231, 228, 221))
        }
        box.addView(txt(title, 17f, ink, true))
        if (subtitle != null) {
            val s = txt(subtitle, 12f, muted)
            val lp = LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(5); box.addView(s, lp)
        }
        val lp = LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(12); parent.addView(box, lp)
        return box
    }
    private fun button(label: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 14f
        setTextColor(if (primary) ink else paper)
        background = rounded(if (primary) gold else Color.rgb(42, 52, 72), 14)
        setPadding(dp(12), dp(8), dp(12), dp(8)); setOnClickListener { action() }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sp = getSharedPreferences("nova", MODE_PRIVATE)
        window.statusBarColor = ink; window.navigationBarColor = ink
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28)); setBackgroundColor(paper)
        }
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(24), dp(22), dp(22))
            background = rounded(ink, 24)
        }
        val brandRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val mark = txt("N", 27f, ink, true).apply { gravity = Gravity.CENTER; background = rounded(gold, 16) }
        brandRow.addView(mark, LinearLayout.LayoutParams(dp(52), dp(52)))
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0)
            addView(txt("NOVA KEYS", 22f, Color.WHITE, true))
            addView(txt("A thoughtful keyboard", 12f, Color.rgb(199, 205, 216)))
        }
        brandRow.addView(brand, LinearLayout.LayoutParams(0, -2, 1f)); hero.addView(brandRow)
        val tagline = txt("Type English. Write Bangla.\nKeep your words private.", 15f, Color.rgb(225, 228, 234))
        val tagLp = LinearLayout.LayoutParams(-1, -2); tagLp.topMargin = dp(18); hero.addView(tagline, tagLp)
        val pill = txt("●  WORKS OFFLINE  ·  NO INTERNET PERMISSION", 10f, gold, true)
        val pillLp = LinearLayout.LayoutParams(-1, -2); pillLp.topMargin = dp(16); hero.addView(pill, pillLp)
        content.addView(hero)

        val setup = section(content, "Get started", "Two quick steps to start typing with Nova Keys.")
        setup.addView(button("1  ·  Enable Nova Keys", true) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }, LinearLayout.LayoutParams(-1, -2))
        val second = LinearLayout.LayoutParams(-1, -2); second.topMargin = dp(8)
        setup.addView(button("2  ·  Switch keyboard") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }, second)

        val themeBox = section(content, "Appearance", "Choose the look that feels right.")
        val rg = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        Theme.names.forEachIndexed { i, name ->
            rg.addView(RadioButton(this).apply {
                text = name; textSize = 14f; setTextColor(ink); id = 100 + i
                isChecked = sp.getInt("theme", 0) == i
                buttonTintList = android.content.res.ColorStateList.valueOf(gold)
                setPadding(0, dp(4), 0, dp(4))
            })
        }
        rg.setOnCheckedChangeListener { _, id -> sp.edit().putInt("theme", id - 100).apply() }
        themeBox.addView(rg)
        themeBox.addView(Switch(this).apply {
            text = "Show number row"; textSize = 14f; setTextColor(ink)
            isChecked = sp.getBoolean("numrow", true)
            setOnCheckedChangeListener { _, enabled -> sp.edit().putBoolean("numrow", enabled).apply() }
        })

        val tuning = section(content, "Keyboard feel", "Fine-tune height and vibration.")
        fun seek(title: String, key: String, def: Int, max: Int, offset: Int, suffix: String) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            heading.addView(txt(title, 14f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
            val value = txt(sp.getInt(key, def).toString() + suffix, 12f, muted, true)
            heading.addView(value); row.addView(heading)
            row.addView(SeekBar(this).apply {
                this.max = max; progress = (sp.getInt(key, def) - offset).coerceIn(0, max)
                progressTintList = android.content.res.ColorStateList.valueOf(gold)
                thumbTintList = android.content.res.ColorStateList.valueOf(ink)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, v: Int, fromUser: Boolean) {
                        val actual = v + offset
                        value.text = actual.toString() + suffix
                        if (fromUser) sp.edit().putInt(key, actual).apply()
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            })
            val lp = LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(12); tuning.addView(row, lp)
        }
        seek("Keyboard height", "height", 100, 50, 80, "%")
        seek("Vibration", "haptic", 15, 40, 0, " ms")

        val preview = section(content, "Try it out", "Test English and phonetic Bangla here.")
        preview.addView(EditText(this).apply {
            hint = "ami banglay gan gai…"; textSize = 16f; setTextColor(ink); setHintTextColor(muted)
            minLines = 2; gravity = Gravity.TOP; setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(paper, 12, Color.rgb(226, 222, 213))
        }, LinearLayout.LayoutParams(-1, -2))
        content.addView(txt("NOVA KEYS  ·  PRIVATE BY DESIGN", 10f, muted, true).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(22), 0, dp(4))
        })
        scroll.addView(content); setContentView(scroll)
    }
}
