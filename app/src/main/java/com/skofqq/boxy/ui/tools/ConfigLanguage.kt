package com.skofqq.boxy.ui.tools

import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.SimpleAnalyzeManager
import io.github.rosemoe.sora.lang.styling.MappedSpans
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Light-weight highlighting for the module's config files: YAML (Clash, Hysteria), JSON (sing-box, Xray, V2Ray)
 * and TOML / INI style "key = value" files (dnscrypt-proxy.toml, settings.ini, *.cfg).
 */
class ConfigLanguage(private val kind: Kind) : EmptyLanguage() {
    enum class Kind { YAML, JSON, TOML }

    private val manager = object : SimpleAnalyzeManager<Unit>() {
        override fun analyze(text: StringBuilder, delegate: Delegate<Unit>): Styles {
            val spans = MappedSpans.Builder()
            var line = 0
            var start = 0
            while (start <= text.length) {
                if (delegate.isCancelled) break
                val end = text.indexOf('\n', start).let { if (it < 0) text.length else it }
                val s = text.substring(start, end)
                when (kind) {
                    Kind.JSON -> jsonLine(spans, line, s)
                    Kind.YAML -> yamlLine(spans, line, s)
                    Kind.TOML -> tomlLine(spans, line, s)
                }
                line++
                start = end + 1
            }
            spans.determine(maxOf(0, line - 1))
            return Styles(spans.build())
        }
    }

    override fun getAnalyzeManager(): AnalyzeManager = manager

    companion object {
        /** Highlighting for a file name, or null for plain text. */
        fun forPath(path: String): ConfigLanguage? {
            val name = path.substringAfterLast('/').lowercase()
            return when (name.substringAfterLast('.', "")) {
                "json" -> ConfigLanguage(Kind.JSON)
                "yaml", "yml" -> ConfigLanguage(Kind.YAML)
                "toml", "ini", "cfg", "conf", "prop" -> ConfigLanguage(Kind.TOML)
                else -> null
            }
        }

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
        /** TOML / INI: [section] headers, key = value, strings, numbers, arrays and # comments. */
        private fun tomlLine(b: MappedSpans.Builder, line: Int, s: String) {
            b.addIfNeeded(line, 0, NORMAL)
            var i = 0
            while (i < s.length && (s[i] == ' ' || s[i] == '\t')) i++
            if (i >= s.length) return
            if (s[i] == '#' || s[i] == ';') {
                b.addIfNeeded(line, i, COMMENT)
                return
            }
            if (s[i] == '[' && s.indexOf('=') < 0) {
                var j = i
                while (j < s.length && s[j] == '[') j++
                b.addIfNeeded(line, i, PUNCT)
                val close = s.indexOf(']', j).let { if (it < 0) s.length else it }
                b.addIfNeeded(line, j, KEY)
                b.addIfNeeded(line, close, PUNCT)
                val hash = s.indexOf('#', close)
                if (hash >= 0) b.addIfNeeded(line, hash, COMMENT)
                return
            }
            val eq = tomlEquals(s, i)
            if (eq > i) {
                b.addIfNeeded(line, i, KEY)
                b.addIfNeeded(line, eq, PUNCT)
                b.addIfNeeded(line, eq + 1, NORMAL)
                i = eq + 1
            }
            tomlValue(b, line, s, i)
        }

        /** Index of the '=' after a bare or quoted key starting at [from], or -1. */
        private fun tomlEquals(s: String, from: Int): Int {
            var i = from
            var quote: Char? = null
            while (i < s.length) {
                val c = s[i]
                if (quote != null) {
                    if (c == quote) quote = null
                } else when (c) {
                    '"', '\'' -> quote = c
                    '=' -> return i
                    '#', '[', '{', '(', ',' -> return -1
                }
                i++
            }
            return -1
        }

        private fun tomlValue(b: MappedSpans.Builder, line: Int, s: String, from: Int) {
            var i = from
            while (i < s.length) {
                val c = s[i]
                when {
                    c == '#' -> {
                        b.addIfNeeded(line, i, COMMENT)
                        return
                    }
                    c == '"' || c == '\'' -> {
                        var j = i + 1
                        while (j < s.length && s[j] != c) {
                            if (c == '"' && s[j] == '\\') j++
                            j++
                        }
                        val end = minOf(j + 1, s.length)
                        b.addIfNeeded(line, i, STRING)
                        b.addIfNeeded(line, end, NORMAL)
                        i = end
                        continue
                    }
                    c.isDigit() || c == '-' || c == '+' || c.isLetter() -> {
                        var j = i
                        while (j < s.length && (s[j].isLetterOrDigit() || s[j] in ".-+_:")) j++
                        val word = s.substring(i, j)
                        if (word.replace("_", "").toDoubleOrNull() != null) {
                            b.addIfNeeded(line, i, NUMBER)
                            b.addIfNeeded(line, j, NORMAL)
                        } else if (word != "true" && word != "false" && from > 0) {
                            b.addIfNeeded(line, i, STRING)
                            b.addIfNeeded(line, j, NORMAL)
                        }
                        i = maxOf(j, i + 1)
                        continue
                    }
                    c in "[]{}(),=" -> {
                        b.addIfNeeded(line, i, PUNCT)
                        b.addIfNeeded(line, i + 1, NORMAL)
                    }
                }
                i++
            }
        }
    }
}
