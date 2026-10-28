package com.zayd.novakeys

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val sp = getSharedPreferences("nova", MODE_PRIVATE)
        val d = resources.displayMetrics.density
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding((20 * d).toInt(), (24 * d).toInt(), (20 * d).toInt(), (20 * d).toInt()) }
        fun label(s: String, size: Float = 14f) = TextView(this).apply { text = s; textSize = size; setPadding(0, (14 * d).toInt(), 0, (4 * d).toInt()) }.also { col.addView(it) }
        label("Nova Keys", 28f); label("Offline keyboard by Zayd • English + phonetic Bengali")
        col.addView(Button(this).apply { text = "1. Enable keyboard"; setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) } })
        col.addView(Button(this).apply { text = "2. Switch to Nova Keys"; setOnClickListener { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() } })
        label("Theme")
        val rg = RadioGroup(this)
        Theme.names.forEachIndexed { i, n -> rg.addView(RadioButton(this).apply { text = n; id = i + 100; isChecked = sp.getInt("theme", 0) == i }) }
        rg.setOnCheckedChangeListener { _, id -> sp.edit().putInt("theme", id - 100).apply() }
        col.addView(rg)
        col.addView(Switch(this).apply { text = "Number row"; isChecked = sp.getBoolean("numrow", true); setOnCheckedChangeListener { _, v -> sp.edit().putBoolean("numrow", v).apply() } })
        fun seek(title: String, key: String, def: Int, max: Int, offset: Int) {
            label(title)
            col.addView(SeekBar(this).apply {
                this.max = max; progress = sp.getInt(key, def) - offset
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) { sp.edit().putInt(key, v + offset).apply() }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            })
        }
        seek("Keyboard height (80–130%)", "height", 100, 50, 80)
        seek("Haptic strength (0–40 ms)", "haptic", 15, 40, 0)
        label("Try it here:")
        col.addView(EditText(this).apply { hint = "Type here… (switch EN→বাং, try: ami banglay gan gai)" })
        setContentView(ScrollView(this).apply { addView(col) })
    }
}
