package com.zayd.novakeys

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import org.json.JSONArray

class NovaIME : InputMethodService(), KeyboardView.Listener {
    private lateinit var kv: KeyboardView
    private var bn = false
    private var sym = false
    private var shift = 0
    private var clipboardMode = false
    private val buf = StringBuilder()

    override fun onCreateInputView(): View {
        kv = KeyboardView(this).also { it.listener = this }
        refresh()
        return kv
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        buf.clear()
        shift = 0
        sym = false
        clipboardMode = false
        if (::kv.isInitialized) refresh()
    }

    private fun refresh() {
        val sp = getSharedPreferences("nova", MODE_PRIVATE)
        kv.setup(
            layout(sym, sp.getBoolean("numrow", true), bn),
            Theme.all[sp.getInt("theme", 0).coerceIn(0, Theme.all.lastIndex)],
            sp.getInt("height", 100) / 100f,
            sp.getInt("haptic", 15)
        )
        kv.shift = shift
        kv.candidates = if (clipboardMode) pinnedClips() else BanglaPhonetic.candidates(buf.toString())
    }

    private fun chars(s: String, alt: String? = null) =
        s.mapIndexed { i, c -> Key(c.toString(), alt = alt?.get(i)?.toString()) }

    private fun layout(sym: Boolean, numRow: Boolean, bn: Boolean): List<List<Key>> {
        // Utility buttons stay separate from normal character keys, so C/X/V shortcuts
        // never interfere with typing the letters c, x or v.
        val tools = listOf(
            Key("C", "COPY", 1f, 1),
            Key("X", "CUT", 1f, 1),
            Key("V", "PASTE", 1f, 1),
            Key("PIN", "PIN", 1.2f, 1),
            Key("CLP", "CLIPS", 1.2f, 1),
            Key("◐", "THEME", 1f, 1)
        )
        val bottom = listOf(
            Key(if (sym) "ABC" else "?123", if (sym) "ABC" else "SYM", 1.4f, 1),
            Key(if (bn) "বাং" else "EN", "LANG", 1.2f, 1), Key(" ", "SPACE", 5f, 3),
            Key(if (bn) "।" else ".", "."), Key("⏎", "ENTER", 1.4f, 2)
        )
        val del = Key("⌫", "DEL", 1.5f, 1)
        if (sym) return listOf(tools, chars("1234567890"), chars("@#৳_&-+()/"), chars("*\"':;!?=") + del, bottom)
        return listOfNotNull(
            tools,
            if (numRow) chars("1234567890") else null,
            chars("qwertyuiop", if (numRow) null else "1234567890"),
            chars("asdfghjkl"),
            listOf(Key("⇧", "SHIFT", 1.5f, 1)) + chars("zxcvbnm") + del,
            bottom
        )
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
            "COPY" -> copySelection(cut = false)
            "CUT" -> copySelection(cut = true)
            "PASTE" -> pasteClipboard()
            "PIN" -> pinClipboard()
            "CLIPS" -> {
                clipboardMode = !clipboardMode
                if (clipboardMode && pinnedClips().isEmpty()) {
                    clipboardMode = false
                    toast("No pinned clips yet. Copy text, then tap PIN.")
                }
            }
            "THEME" -> {
                val sp = getSharedPreferences("nova", MODE_PRIVATE)
                val next = (sp.getInt("theme", 0) + 1) % Theme.all.size
                sp.edit().putInt("theme", next).apply()
            }
            else -> type(k.code)
        }
        refresh()
    }

    private fun copySelection(cut: Boolean) {
        commitWord()
        val action = if (cut) android.R.id.cut else android.R.id.copy
        val ok = currentInputConnection?.performContextMenuAction(action) ?: false
        toast(when {
            ok && cut -> "Selected text cut"
            ok -> "Selected text copied"
            cut -> "Select text first, then tap X"
            else -> "Select text first, then tap C"
        })
    }

    private fun pasteClipboard() {
        commitWord()
        val ic = currentInputConnection ?: return
        val handled = ic.performContextMenuAction(android.R.id.paste)
        if (!handled) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
            if (!text.isNullOrEmpty()) ic.commitText(text, 1) else toast("Clipboard is empty")
        }
    }

    private fun pinClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
        if (text.isNullOrEmpty()) {
            toast("Copy some text first, then tap PIN")
            return
        }
        val clips = pinnedClips().toMutableList()
        clips.remove(text)
        clips.add(0, text)
        val array = JSONArray()
        clips.take(30).forEach { array.put(it) }
        getSharedPreferences("nova", MODE_PRIVATE).edit().putString("pinned_clips", array.toString()).apply()
        toast("Text pinned")
    }

    private fun pinnedClips(): List<String> {
        val raw = getSharedPreferences("nova", MODE_PRIVATE).getString("pinned_clips", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                array.optString(i).takeIf { it.isNotBlank() }
            }
        }.getOrDefault(emptyList())
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

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

    private fun commitWord() {
        if (buf.isNotEmpty()) {
            currentInputConnection?.finishComposingText()
            buf.clear()
        }
    }

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
        currentInputConnection?.commitText(s, 1)
        buf.clear()
        if (clipboardMode) clipboardMode = false
        refresh()
    }
}
