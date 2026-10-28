package com.zayd.novakeys

/** Original Avro-style phonetic Latin -> Bengali converter (written for Nova Keys). */
object BanglaPhonetic {
    private val cons = mapOf(
        "kkh" to "ক্ষ", "gg" to "জ্ঞ", "kh" to "খ", "k" to "ক", "gh" to "ঘ", "g" to "গ", "Ng" to "ঙ",
        "chh" to "ছ", "ch" to "চ", "jh" to "ঝ", "j" to "জ", "Th" to "ঠ", "T" to "ট", "Dh" to "ঢ", "D" to "ড",
        "N" to "ণ", "th" to "থ", "t" to "ত", "dh" to "ধ", "d" to "দ", "n" to "ন", "ph" to "ফ", "f" to "ফ",
        "p" to "প", "bh" to "ভ", "v" to "ভ", "b" to "ব", "m" to "ম", "z" to "য", "r" to "র", "l" to "ল",
        "sh" to "শ", "S" to "শ", "s" to "স", "h" to "হ", "Rh" to "ঢ়", "R" to "ড়", "y" to "য়"
    )
    private val vow = mapOf(
        "rri" to ("ঋ" to "ৃ"), "oi" to ("ঐ" to "ৈ"), "ou" to ("ঔ" to "ৌ"), "ii" to ("ঈ" to "ী"),
        "uu" to ("ঊ" to "ূ"), "a" to ("আ" to "া"), "i" to ("ই" to "ি"), "I" to ("ঈ" to "ী"),
        "u" to ("উ" to "ু"), "U" to ("ঊ" to "ূ"), "e" to ("এ" to "ে"), "o" to ("অ" to ""), "O" to ("ও" to "ো")
    )
    private val misc = mapOf("ng" to "ং", "^" to "ঁ")
    private val keys = (cons.keys + vow.keys + misc.keys).sortedByDescending { it.length }

    fun convert(s: String): String {
        val out = StringBuilder(); var i = 0; var prevCons = false; var last = ""
        while (i < s.length) {
            val t = keys.firstOrNull { s.startsWith(it, i) }
            if (t == null) { if (s[i] != '`') out.append(s[i]); prevCons = false; last = ""; i++; continue }
            when {
                t in cons -> { if (prevCons && last == t) out.append('্'); out.append(cons[t]); prevCons = true; last = t }
                t in vow -> { val (ind, sign) = vow[t]!!; out.append(if (prevCons) sign else ind); prevCons = false; last = "" }
                else -> { out.append(misc[t]); prevCons = false; last = "" }
            }
            i += t.length
        }
        return out.toString()
    }

    fun candidates(s: String): List<String> {
        if (s.isEmpty()) return emptyList()
        val idx = s.indexOfFirst { it in "tdnsTDNS" }
        val alt = if (idx >= 0) s.replaceRange(idx, idx + 1, (if (s[idx].isUpperCase()) s[idx].lowercase() else s[idx].uppercase())) else s
        return listOf(convert(s), convert(alt), s).distinct().take(3)
    }
}
