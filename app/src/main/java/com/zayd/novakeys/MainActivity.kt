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
    private val black = Color.rgb(0, 0, 0)
    private val ink = Color.rgb(25, 25, 28)
    private val paper = Color.rgb(248, 247, 252)
    private val red = Color.rgb(226, 30, 42)
    private val gold = Color.rgb(224, 185, 70)
    private val muted = Color.rgb(130, 130, 140)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int = 18, stroke: Int = 0) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }
    private fun txt(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(Typeface.create("sans-serif", Typeface.BOLD))
        setLineSpacing(dp(2).toFloat(), 1f)
    }
    private fun sectionLabel(value: String): TextView = txt(value.uppercase(), 11f, gold, true).apply {
        letterSpacing = 0.14f
        setPadding(dp(4), dp(24), 0, dp(2))
    }
    private fun card(parent: LinearLayout): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(paper, 22, Color.rgb(220, 218, 228))
            setPadding(dp(5), dp(2), dp(5), dp(2))
        }
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.topMargin = dp(10)
        parent.addView(box, lp)
        return box
    }
    private fun actionRow(parent: LinearLayout, icon: String, title: String, subtitle: String? = null, action: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(15), dp(16), dp(15)); isClickable = true
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x18000000), rounded(Color.TRANSPARENT, 14), null)
            setOnClickListener { action() }
        }
        val badge = txt(icon, 19f, if (icon == "◎") red else Color.WHITE, true).apply {
            gravity = Gravity.CENTER; background = rounded(if (icon == "◎") Color.rgb(255, 225, 229) else ink, 13)
        }
        row.addView(badge, LinearLayout.LayoutParams(dp(48), dp(48)))
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, dp(8), 0)
            addView(txt(title, 16f, ink, true))
            if (subtitle != null) addView(txt(subtitle, 12f, muted).apply { setPadding(0, dp(4), 0, 0) })
        }
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(txt("›", 30f, muted))
        parent.addView(row)
    }
    private fun divider(parent: LinearLayout) {
        val line = android.view.View(this).apply { setBackgroundColor(Color.rgb(218, 216, 225)) }
        parent.addView(line, LinearLayout.LayoutParams(-1, dp(1)).apply { leftMargin = dp(8); rightMargin = dp(8) })
    }
    private fun toggleRow(parent: LinearLayout, title: String, subtitle: String, key: String, default: Boolean, sp: android.content.SharedPreferences) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(15), dp(14), dp(15)) }
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(txt(title, 16f, ink, true))
        labels.addView(txt(subtitle, 12f, muted).apply { setPadding(0, dp(4), 0, 0) })
        row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Switch(this).apply {
            isChecked = sp.getBoolean(key, default)
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            trackTintList = android.content.res.ColorStateList.valueOf(if (isChecked) red else Color.rgb(170, 170, 180))
            setOnCheckedChangeListener { _, checked ->
                sp.edit().putBoolean(key, checked).apply()
                trackTintList = android.content.res.ColorStateList.valueOf(if (checked) red else Color.rgb(170, 170, 180))
            }
        })
        parent.addView(row)
    }
    private fun primaryButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 14f; setTextColor(Color.WHITE)
        background = rounded(red, 16); setPadding(dp(12), dp(8), dp(12), dp(8))
        setOnClickListener { action() }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sp = getSharedPreferences("nova", MODE_PRIVATE)
        window.statusBarColor = black; window.navigationBarColor = black
        val scroll = ScrollView(this).apply { setBackgroundColor(black); clipToPadding = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(20)); setBackgroundColor(black)
        }
        val brandRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val titleCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleCol.addView(txt("ORBIT BOARD", 25f, Color.WHITE, true).apply { letterSpacing = 0.08f })
        titleCol.addView(txt("Crafted for Nothing OS", 13f, muted).apply { setPadding(0, dp(3), 0, 0) })
        brandRow.addView(titleCol, LinearLayout.LayoutParams(0, -2, 1f))
        brandRow.addView(txt("v1.1.0", 11f, red, true).apply {
            gravity = Gravity.CENTER; setPadding(dp(12), dp(9), dp(12), dp(9)); background = rounded(0x22E21E2A, 18, 0xFF6A2228.toInt())
        })
        content.addView(brandRow)

        content.addView(sectionLabel("Input & languages"))
        val input = card(content)
        actionRow(input, "◎", "Languages", "Keyboard languages and input methods") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        divider(input)
        actionRow(input, "▦", "Keyboard & Typing", "Layout, number row and key behaviour") {
            Toast.makeText(this, "Use the options below to customise typing.", Toast.LENGTH_SHORT).show()
        }
        divider(input)
        actionRow(input, "A̲", "Text Prediction", "Phonetic Bengali conversion is offline") {
            Toast.makeText(this, "Bengali phonetic conversion works offline.", Toast.LENGTH_SHORT).show()
        }
        divider(input)
        actionRow(input, "▤", "Personal dictionary", "Private words stay on this device") {
            Toast.makeText(this, "Personal dictionary is planned for a future update.", Toast.LENGTH_SHORT).show()
        }

        content.addView(sectionLabel("Bengali layouts"))
        val langs = card(content)
        toggleRow(langs, "Avro (অভ্র)", "Phonetic typing (ami → আমি)", "layout_avro", true, sp)
        divider(langs)
        toggleRow(langs, "Jatiyo (জাতীয়)", "National standard layout (ক খ গ)", "layout_jatiyo", false, sp)
        divider(langs)
        toggleRow(langs, "Probhat (প্রভাত)", "Probhat keyboard layout", "layout_probhat", true, sp)

        content.addView(sectionLabel("Customization"))
        val custom = card(content)
        actionRow(custom, "◉", "Theme", "Orbit, Nothing and dynamic colour") {
            val themes = arrayOf("Orbit Board", "Nothing Dark", "Dynamic Light", "AMOLED Black")
            val current = sp.getInt("theme", 0).coerceIn(0, 2)
            android.app.AlertDialog.Builder(this).setTitle("Theme Switcher")
                .setSingleChoiceItems(themes, current) { dialog, which ->
                    sp.edit().putInt("theme", which.coerceAtMost(2)).apply()
                    dialog.dismiss()
                    Toast.makeText(this, themes[which] + " selected", Toast.LENGTH_SHORT).show()
                }.setNegativeButton("Cancel", null).show()
        }
        divider(custom)
        toggleRow(custom, "Show number row", "Keep digits above the letters", "numrow", true, sp)
        divider(custom)
        toggleRow(custom, "Key vibration", "Enable tactile feedback", "vibration_enabled", true, sp)

        content.addView(sectionLabel("Keyboard setup"))
        val setup = card(content)
        setup.setPadding(dp(16), dp(16), dp(16), dp(16))
        setup.addView(txt("Two quick steps to start typing", 16f, ink, true))
        setup.addView(txt("Nova Keys works offline and does not request internet access.", 12f, muted).apply { setPadding(0, dp(6), 0, dp(14)) })
        setup.addView(primaryButton("1  ·  Enable Nova Keys") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }, LinearLayout.LayoutParams(-1, -2))
        setup.addView(Button(this).apply {
            text = "2  ·  Switch keyboard"; isAllCaps = false; textSize = 14f; setTextColor(ink)
            background = rounded(Color.WHITE, 16); setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        content.addView(sectionLabel("Try it out"))
        val previewCard = card(content)
        previewCard.setPadding(dp(16), dp(12), dp(16), dp(12))
        previewCard.addView(EditText(this).apply {
            hint = "Try typing here…"; textSize = 16f; setTextColor(ink); setHintTextColor(muted)
            minLines = 1; maxLines = 3; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.WHITE, 28, Color.rgb(225, 223, 232))
        }, LinearLayout.LayoutParams(-1, dp(58)))
        content.addView(txt("NOVA KEYS  ·  PRIVATE BY DESIGN", 10f, muted, true).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(24), 0, dp(6)); letterSpacing = 0.1f
        })
        scroll.addView(content); setContentView(scroll)
    }
}