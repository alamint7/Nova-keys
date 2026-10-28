package com.zayd.novakeys

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
    private var probhat = false
    private var emojiMode = false
    private var clipboardMode = false
    private var toolbarVisible = true
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
        emojiMode = false
        clipboardMode = false
        if (::kv.isInitialized) refresh()
    }

    private fun refresh() {
        val sp = getSharedPreferences("nova", MODE_PRIVATE)
        toolbarVisible = sp.getBoolean("toolbar", true)
        kv.setup(
            layout(sym, sp.getBoolean("numrow", true), bn, toolbarVisible),
            Theme.all[sp.getInt("theme", 0).coerceIn(0, Theme.all.lastIndex)],
            sp.getInt("height", 100) / 100f,
            sp.getInt("haptic", 15)
        )
        kv.shift = shift
        kv.candidates = when {
            clipboardMode -> pinnedClips().take(3)
            emojiMode -> listOf("😀", "😂", "🥰")
            else -> if (probhat || !bn) emptyList() else BanglaPhonetic.candidates(buf.toString())
        }
    }

    private fun chars(s: String, alt: String? = null) =
        s.mapIndexed { i, c -> Key(c.toString(), alt = alt?.getOrNull(i)?.toString()) }

    private fun row(s: String) = s.trim().split(Regex("\\s+")).map { Key(it) }

    private fun layout(sym: Boolean, numRow: Boolean, bn: Boolean, toolbarVisible: Boolean): List<List<Key>> {
        val fullToolbar = listOf(
            Key("▤", "CLIPBOARD", 1.1f, 1, "PIN"),
            Key("▢", "COPY", 0.9f, 1),
            Key("✂", "CUT", 0.9f, 1),
            Key("▣", "PASTE", 0.9f, 1),
            Key("☺", "EMOJI", 1.0f, 1),
            Key(if (probhat) "প্রভাত" else "অভ্র", "LAYOUT", 1.4f, 1),
            Key("◐", "THEME", 0.9f, 1),
            Key("⚙", "SETTINGS", 0.9f, 1),
            Key("⌄", "TOOLBAR", 0.9f, 1)
        )
        val toolbar = if (toolbarVisible) fullToolbar else listOf(
            Key("☰", "TOOLBAR", 1.25f, 1),
            Key(if (bn) "বাং" else "EN", "LANG", 1.1f, 1),
            Key("☺", "EMOJI", 1.1f, 1),
            Key(if (probhat) "প্রভাত" else "অভ্র", "LAYOUT", 1.5f, 1),
            Key("⚙", "SETTINGS", 1.1f, 1)
        )
        val del = Key("⌫", "DEL", 1.5f, 1)
        val bottom = listOf(
            Key(if (sym || emojiMode) "ABC" else "?123", if (sym || emojiMode) "ABC" else "SYM", 1.35f, 1),
            Key(if (bn) "বাং" else "EN", "LANG", 1.1f, 1),
            Key(",", ",", 0.8f),
            Key("space", "SPACE", 4.3f, 3),
            Key(if (bn) "।" else ".", ".", 0.8f),
            Key("⏎", "ENTER", 1.3f, 2)
        )

        if (emojiMode) {
            return listOf(
                toolbar,
                row("😀 😃 😄 😁 😆 😅 😂 🙂 🙃 😉"),
                row("😊 😍 🥰 😘 😗 😙 😚 😋 😛 😜"),
                row("🤔 🤗 😎 😭 😡 👍 👎 🙏 ❤️"),
                listOf(Key("ABC", "ABC", 1.5f, 1), Key("space", "SPACE", 5.5f, 3), del),
                bottom
            )
        }
        if (sym) return listOf(
            toolbar, chars("1234567890"), chars("@#৳_&-+()/"), chars("*+-=!?;:") + del, bottom
        )

        if (probhat && bn) {
            // Probhat-style Bengali character layout; each key commits Bengali directly.
            return listOfNotNull(
                toolbar,
                if (numRow) chars("1234567890") else null,
                row("ৎ ং ঃ ঁ ় ৎ ঋ ৃ ৈ ৌ ূ ী"),
                row("অ আ ই ঈ উ ঊ ঋ এ ঐ ও ঔ"),
                row("ক খ গ ঘ ঙ চ ছ জ ঝ ঞ ট ঠ"),
                listOf(Key("⇧", "SHIFT", 1.35f, 1)) + row("ড ঢ ণ ত থ দ ধ ন প ফ ব ভ") + del,
                listOf(Key("?123", "SYM", 1.35f, 1), Key("য", "য"), Key("র", "র"), Key("ল", "ল"), Key("স", "স"), Key("ম", "ম"), Key("হ", "হ"), Key("য়", "য়"), Key("space", "SPACE", 3.2f, 3), Key("⏎", "ENTER", 1.3f, 2))
            )
        }

        return listOfNotNull(
            toolbar,
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
            "TOOLBAR" -> {
                toolbarVisible = !toolbarVisible
                getSharedPreferences("nova", MODE_PRIVATE).edit().putBoolean("toolbar", toolbarVisible).apply()
            }
            "DEL" -> del()
            "SPACE" -> { commitWord(); currentInputConnection?.commitText(" ", 1) }
            "ENTER" -> { commitWord(); enter() }
            "SYM" -> { sym = true; emojiMode = false }
            "ABC" -> { sym = false; emojiMode = false }
            "LANG" -> { commitWord(); bn = !bn; if (!bn) probhat = false }
            "LAYOUT" -> {
                commitWord()
                probhat = !probhat
                bn = true
                sym = false
                emojiMode = false
                toast(if (probhat) "Probhat layout" else "Phonetic layout")
            }
            "EMOJI" -> { commitWord(); emojiMode = !emojiMode; sym = false }
            "COPY" -> copySelection(cut = false)
            "CUT" -> copySelection(cut = true)
            "PASTE" -> pasteClipboard()
            "CLIPBOARD" -> showPinnedClips()
            "PIN" -> pinClipboard()
            "THEME" -> {
                val sp = getSharedPreferences("nova", MODE_PRIVATE)
                sp.edit().putInt("theme", (sp.getInt("theme", 0) + 1) % Theme.all.size).apply()
                toast(Theme.names[(sp.getInt("theme", 0)).coerceIn(0, Theme.names.lastIndex)])
            }
            "SETTINGS" -> startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            else -> type(k.code)
        }
        refresh()
    }

    private fun copySelection(cut: Boolean) {
        commitWord()
        val action = if (cut) android.R.id.cut else android.R.id.copy
        val ok = currentInputConnection?.performContextMenuAction(action) ?: false
        if (!ok) toast(if (cut) "Select text to cut first" else "Select text to copy first")
    }

    private fun pasteClipboard() {
        commitWord()
        val ic = currentInputConnection ?: return
        val handled = ic.performContextMenuAction(android.R.id.paste)
        if (!handled) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = cm.primaryClip?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.coerceToText(this)?.toString()
            if (!text.isNullOrEmpty()) ic.commitText(text, 1) else toast("Clipboard is empty")
        }
    }

    private fun pinClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
        if (text.isNullOrEmpty()) {
            toast("Copy text first, then long-press the clipboard icon")
            return
        }
        val clips = pinnedClips().toMutableList()
        clips.remove(text)
        clips.add(0, text)
        val array = JSONArray()
        clips.take(30).forEach { array.put(it) }
        getSharedPreferences("nova", MODE_PRIVATE).edit().putString("pinned_clips", array.toString()).apply()
        clipboardMode = true
        toast("Pinned to your clipboard")
    }

    private fun showPinnedClips() {
        clipboardMode = !clipboardMode
        val clips = pinnedClips()
        if (clipboardMode && clips.isEmpty()) {
            clipboardMode = false
            toast("No pinned text yet. Copy text and long-press the clipboard icon.")
        } else if (clipboardMode) toast("Tap a snippet in the suggestion strip to paste it")
    }

    private fun pinnedClips(): List<String> {
        val raw = getSharedPreferences("nova", MODE_PRIVATE).getString("pinned_clips", "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i -> array.optString(i).takeIf { it.isNotBlank() } }
        }.getOrDefault(emptyList())
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun type(c: String) {
        val ic = currentInputConnection ?: return
        if (emojiMode || (probhat && bn)) {
            commitWord()
            ic.commitText(c, 1)
            if (shift == 1) shift = 0
            return
        }
        val letter = c.firstOrNull()?.isLetter() == true
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
        if (clipboardMode) {
            currentInputConnection?.commitText(s, 1)
            clipboardMode = false
        } else if (emojiMode) {
            currentInputConnection?.commitText(s, 1)
        } else {
            currentInputConnection?.commitText(s, 1)
            buf.clear()
        }
        refresh()
    }
}
