package com.zayd.novakeys

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo

class NovaIME : InputMethodService(), KeyboardView.Listener {
    private lateinit var kv: KeyboardView
    private var bn = false
    private var sym = false
    private var shift = 0
    private val buf = StringBuilder()

    override fun onCreateInputView(): View { kv = KeyboardView(this).also { it.listener = this }; refresh(); return kv }
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        buf.clear(); shift = 0; sym = false; if (::kv.isInitialized) refresh()
    }

    private fun refresh() {
        val sp = getSharedPreferences("nova", MODE_PRIVATE)
        kv.setup(layout(sym, sp.getBoolean("numrow", true), bn), Theme.all[sp.getInt("theme", 0).coerceIn(0, 2)],
            sp.getInt("height", 100) / 100f, sp.getInt("haptic", 15))
        kv.shift = shift; kv.candidates = BanglaPhonetic.candidates(buf.toString())
    }

    private fun chars(s: String, alt: String? = null) = s.mapIndexed { i, c -> Key(c.toString(), alt = alt?.get(i)?.toString()) }
    private fun layout(sym: Boolean, numRow: Boolean, bn: Boolean): List<List<Key>> {
        val bottom = listOf(
            Key(if (sym) "ABC" else "?123", if (sym) "ABC" else "SYM", 1.4f, 1),
            Key(if (bn) "বাং" else "EN", "LANG", 1.2f, 1), Key(" ", "SPACE", 5f, 3),
            Key(if (bn) "।" else ".", "."), Key("⏎", "ENTER", 1.4f, 2))
        val del = Key("⌫", "DEL", 1.5f, 1)
        if (sym) return listOf(chars("1234567890"), chars("@#৳_&-+()/"), chars("*\"':;!?=") + del, bottom)
        return listOfNotNull(
            if (numRow) chars("1234567890") else null,
            chars("qwertyuiop", if (numRow) null else "1234567890"), chars("asdfghjkl"),
            listOf(Key("⇧", "SHIFT", 1.5f, 1)) + chars("zxcvbnm") + del, bottom)
    }

    override fun onKey(k: Key) {
        when (k.code) {
            "SHIFT" -> shift = (shift + 1) % 3
            "DEL" -> del()
            "SPACE" -> { commitWord(); currentInputConnection?.commitText(" ", 1) }
            "ENTER" -> { commitWord(); enter() }
            "SYM" -> sym = true
            "ABC" -> sym = false
            "LANG" -> { commitWord(); bn = !bn }
            else -> type(k.code)
        }
        refresh()
    }

    private fun type(c: String) {
        val ic = currentInputConnection ?: return
        val letter = c[0].isLetter()
        if (bn && !sym && letter) {
            buf.append(if (shift != 0) c.uppercase() else c)
            ic.setComposingText(BanglaPhonetic.convert(buf.toString()), 1)
        } else {
            commitWord()
            ic.commitText(if (bn && c == ".") "।" else if (shift != 0 && letter) c.uppercase() else c, 1)
        }
        if (shift == 1) shift = 0
    }

    private fun commitWord() { if (buf.isNotEmpty()) { currentInputConnection?.finishComposingText(); buf.clear() } }

    private fun del() {
        val ic = currentInputConnection ?: return
        if (buf.isNotEmpty()) {
            buf.deleteCharAt(buf.length - 1)
            ic.setComposingText(BanglaPhonetic.convert(buf.toString()), 1)
            if (buf.isEmpty()) ic.commitText("", 1)
        } else sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    private fun enter() {
        val o = currentInputEditorInfo?.imeOptions ?: 0
        val a = o and EditorInfo.IME_MASK_ACTION
        if (a != EditorInfo.IME_ACTION_NONE && a != EditorInfo.IME_ACTION_UNSPECIFIED && o and EditorInfo.IME_FLAG_NO_ENTER_ACTION == 0)
            currentInputConnection?.performEditorAction(a)
        else sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
    }

    override fun onCandidate(s: String) {
        currentInputConnection?.commitText(s, 1); buf.clear(); refresh()
    }
}
