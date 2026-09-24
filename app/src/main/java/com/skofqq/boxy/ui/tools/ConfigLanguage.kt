package com.skofqq.boxy.ui.tools

import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.SimpleAnalyzeManager
import io.github.rosemoe.sora.lang.styling.MappedSpans
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/** Light-weight highlighting for the module's config files: YAML (Clash, Hysteria) and JSON (sing-box, Xray, V2Ray). */
class ConfigLanguage(private val json: Boolean) : EmptyLanguage() {
    private val manager = object : SimpleAnalyzeManager<Unit>() {
        override fun analyze(text: StringBuilder, delegate: Delegate<Unit>): Styles {
            val spans = MappedSpans.Builder()
            var line = 0
            var start = 0
            while (start <= text.length) {
                if (delegate.isCancelled) break
                val end = text.indexOf('\n', start).let { if (it < 0) text.length else it }
                val s = text.substring(start, end)
                if (json) jsonLine(spans, line, s) else yamlLine(spans, line, s)
                line++
                start = end + 1
            }
            spans.determine(maxOf(0, line - 1))
            return Styles(spans.build())
        }
    }

    override fun getAnalyzeManager(): AnalyzeManager = manager

    companion object {
        private val NORMAL = TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL)
        private val KEY = TextStyle.makeStyle(EditorColorScheme.KEYWORD)
        private val STRING = TextStyle.makeStyle(EditorColorScheme.LITERAL)
        private val NUMBER = TextStyle.makeStyle(EditorColorScheme.FUNCTION_NAME)
        private val PUNCT = TextStyle.makeStyle(EditorColorScheme.OPERATOR)
        private val COMMENT = TextStyle.makeStyle(EditorColorScheme.COMMENT, 0, false, true, false)

        private val SCALAR_WORDS = setOf("true", "false", "null", "~", "yes", "no", "on", "off")

        private fun yamlLine(b: MappedSpans.Builder, line: Int, s: String) {
            b.addIfNeeded(line, 0, NORMAL)
            var i = 0
            while (i < s.length && s[i] == ' ') i++
            if (i < s.length && s[i] == '#') {
                b.addIfNeeded(line, i, COMMENT)
                return
            }
            // List markers
            while (i + 1 <= s.length && i < s.length && s[i] == '-' && (i + 1 == s.length || s[i + 1] == ' ')) {
                b.addIfNeeded(line, i, NORMAL)
                i++
                while (i < s.length && s[i] == ' ') i++
            }
            // key: value
            val colon = keyColon(s, i)
            if (colon > i) {
                b.addIfNeeded(line, i, KEY)
                b.addIfNeeded(line, colon, PUNCT)
                i = colon + 1
                b.addIfNeeded(line, i, NORMAL)
            }
            value(b, line, s, i)
        }

        /** Index of the ':' ending a mapping key that starts at [from], or -1. */
        private fun keyColon(s: String, from: Int): Int {
            var i = from
            var quote: Char? = null
            if (i < s.length && (s[i] == '"' || s[i] == '\'')) quote = s[i].also { i++ }
            while (i < s.length) {
                val c = s[i]
                if (quote != null) {
                    if (c == quote) quote = null
                } else {
                    if (c == ':' && (i + 1 == s.length || s[i + 1] == ' ')) return i
                    if (c == '#' || c == '{' || c == '[') return -1
                }
                i++
            }
            return -1
        }

        /**
         * YAML scalar after "key:" or "- ": quoted and plain strings are teal, numbers green,
         * booleans / null stay plain, a trailing " #" starts a comment.
         */
        private fun value(b: MappedSpans.Builder, line: Int, s: String, from: Int) {
            var i = from
            while (i < s.length && s[i] == ' ') i++
            if (i >= s.length) return
            if (s[i] == '#') {
                b.addIfNeeded(line, i, COMMENT)
                return
            }
            // Find where a comment starts (outside quotes).
            var quote: Char? = null
            var commentAt = -1
            var j = i
            while (j < s.length) {
                val c = s[j]
                if (quote != null) {
                    if (c == quote) quote = null
                } else if (c == '"' || c == '\'') {
                    quote = c
                } else if (c == '#' && s[j - 1] == ' ') {
                    commentAt = j
                    break
                }
                j++
            }
            val valueEnd = if (commentAt >= 0) commentAt else s.length
            val raw = s.substring(i, valueEnd).trimEnd()
            val style = when {
                raw.isEmpty() -> NORMAL
                raw.lowercase() in SCALAR_WORDS -> NORMAL
                raw.toDoubleOrNull() != null -> NUMBER
                raw == "|" || raw == ">" || raw == "|-" || raw == ">-" -> PUNCT
                else -> STRING
            }
            b.addIfNeeded(line, i, style)
            if (commentAt >= 0) {
                b.addIfNeeded(line, i + raw.length, NORMAL)
                b.addIfNeeded(line, commentAt, COMMENT)
            }
        }

        private fun jsonLine(b: MappedSpans.Builder, line: Int, s: String) {
            b.addIfNeeded(line, 0, NORMAL)
            var i = 0
            while (i < s.length) {
                val c = s[i]
                when {
                    (c == '/' && i + 1 < s.length && s[i + 1] == '/') || (c == '#' && s.substring(0, i).isBlank()) -> {
                        b.addIfNeeded(line, i, COMMENT)
                        return
                    }
                    c == '"' -> {
                        var j = i + 1
                        while (j < s.length && s[j] != '"') {
                            if (s[j] == '\\') j++
                            j++
                        }
                        val end = minOf(j + 1, s.length)
                        var k = end
                        while (k < s.length && s[k] == ' ') k++
                        b.addIfNeeded(line, i, if (k < s.length && s[k] == ':') KEY else STRING)
                        b.addIfNeeded(line, end, NORMAL)
                        i = end
                        continue
                    }
                    c.isDigit() || c == '-' || c.isLetter() -> {
                        var j = i
                        while (j < s.length && (s[j].isLetterOrDigit() || s[j] in ".-+eE")) j++
                        if (s.substring(i, j).toDoubleOrNull() != null) {
                            b.addIfNeeded(line, i, NUMBER)
                            b.addIfNeeded(line, j, NORMAL)
                        }
                        i = maxOf(j, i + 1)
                        continue
                    }
                    c in "{}[],:" -> {
                        b.addIfNeeded(line, i, PUNCT)
                        b.addIfNeeded(line, i + 1, NORMAL)
                    }
                }
                i++
            }
        }
    }
}
