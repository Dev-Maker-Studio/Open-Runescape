package com.darkan.bot.scripts

sealed interface ScriptHeader {
    data object Absent : ScriptHeader

    data class Unreadable(val reason: String) : ScriptHeader

    data class Parsed(
        val className: String,
        val name: String,
        val version: String,
        val author: String,
        val description: String,
        val visible: Boolean,
        val category: ScriptCategory,
        val configurable: Boolean,
    ) : ScriptHeader

    companion object {
        private val ANNOTATION = Regex("""@(?:com\.darkan\.bot\.scripts\.)?ScriptDescription\s*\(""")
        private val DECLARATION = Regex("""\b(?:class|object)\s+([A-Za-z_][A-Za-z0-9_]*)""")
        private val NAMED_ARGUMENT = Regex("""^([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$""", RegexOption.DOT_MATCHES_ALL)
        private val PARAMETERS = listOf("name", "version", "author", "description", "visible", "category")

        fun parse(source: String): ScriptHeader {
            val text = stripComments(source)
            val annotation = ANNOTATION.find(text) ?: return Absent
            val open = annotation.range.last
            val close = matchingParen(text, open) ?: return Unreadable("unterminated @ScriptDescription")

            val values = HashMap<String, String>()
            splitArguments(text.substring(open + 1, close)).forEachIndexed { index, argument ->
                val named = NAMED_ARGUMENT.matchEntire(argument)
                val key = named?.groupValues?.get(1) ?: PARAMETERS.getOrNull(index) ?: return Unreadable("too many arguments")
                values[key] = (named?.groupValues?.get(2) ?: argument).trim()
            }

            val declaration = DECLARATION.find(text, close) ?: return Unreadable("no class follows @ScriptDescription")
            val body = text.indexOf('{', declaration.range.last).let { if (it < 0) text.length else it }
            val supertypes = text.substring(declaration.range.last + 1, body)

            fun string(key: String): String? = values[key]?.let(::stringLiteral)
            val category = values["category"]?.let { value ->
                val constant = value.substringAfterLast('.').trim()
                ScriptCategory.entries.find { it.name == constant } ?: return Unreadable("unknown category $value")
            } ?: ScriptCategory.OTHER
            val visible = when (values["visible"]) {
                null, "true" -> true
                "false" -> false
                else -> return Unreadable("visible is not a literal")
            }

            return Parsed(
                className = declaration.groupValues[1],
                name = string("name") ?: return Unreadable("name is not a string literal"),
                version = string("version") ?: return Unreadable("version is not a string literal"),
                author = string("author") ?: return Unreadable("author is not a string literal"),
                description = string("description") ?: return Unreadable("description is not a string literal"),
                visible = visible,
                category = category,
                configurable = Regex("""\bConfigurableScript\b""").containsMatchIn(supertypes),
            )
        }

        private fun stripComments(source: String): String {
            val out = StringBuilder(source.length)
            var i = 0
            while (i < source.length) {
                val c = source[i]
                when {
                    source.startsWith("\"\"\"", i) -> {
                        val end = source.indexOf("\"\"\"", i + 3).let { if (it < 0) source.length else it + 3 }
                        out.append(source, i, end)
                        i = end
                    }
                    c == '"' -> {
                        val end = stringEnd(source, i)
                        out.append(source, i, end)
                        i = end
                    }
                    source.startsWith("//", i) -> {
                        while (i < source.length && source[i] != '\n') i++
                    }
                    source.startsWith("/*", i) -> {
                        val end = source.indexOf("*/", i + 2)
                        i = if (end < 0) source.length else end + 2
                        out.append(' ')
                    }
                    else -> {
                        out.append(c)
                        i++
                    }
                }
            }
            return out.toString()
        }

        private fun stringEnd(text: String, start: Int): Int {
            var i = start + 1
            while (i < text.length && text[i] != '"' && text[i] != '\n') {
                if (text[i] == '\\') i++
                i++
            }
            return minOf(i + 1, text.length)
        }

        private fun matchingParen(text: String, open: Int): Int? {
            var depth = 0
            var i = open
            while (i < text.length) {
                when {
                    text.startsWith("\"\"\"", i) -> i = text.indexOf("\"\"\"", i + 3).let { if (it < 0) return null else it + 2 }
                    text[i] == '"' -> i = stringEnd(text, i) - 1
                    text[i] == '(' -> depth++
                    text[i] == ')' -> if (--depth == 0) return i
                }
                i++
            }
            return null
        }

        private fun splitArguments(arguments: String): List<String> {
            val parts = mutableListOf<String>()
            var depth = 0
            var start = 0
            var i = 0
            while (i < arguments.length) {
                when {
                    arguments.startsWith("\"\"\"", i) -> i = arguments.indexOf("\"\"\"", i + 3).let { if (it < 0) arguments.length else it + 2 }
                    arguments[i] == '"' -> i = stringEnd(arguments, i) - 1
                    arguments[i] in "([{" -> depth++
                    arguments[i] in ")]}" -> depth--
                    arguments[i] == ',' && depth == 0 -> {
                        parts += arguments.substring(start, i)
                        start = i + 1
                    }
                }
                i++
            }
            parts += arguments.substring(start)
            return parts.map { it.trim() }.filter { it.isNotEmpty() }
        }

        private fun stringLiteral(value: String): String? {
            if (value.length >= 6 && value.startsWith("\"\"\"") && value.endsWith("\"\"\"")) {
                val raw = value.substring(3, value.length - 3)
                return raw.takeUnless { '$' in it }
            }
            if (value.length < 2 || !value.startsWith('"') || stringEnd(value, 0) != value.length) return null
            val out = StringBuilder()
            var i = 1
            while (i < value.length - 1) {
                val c = value[i]
                when {
                    c == '$' -> return null
                    c != '\\' -> out.append(c)
                    else -> {
                        i++
                        when (val escaped = value[i]) {
                            'n' -> out.append('\n')
                            't' -> out.append('\t')
                            'r' -> out.append('\r')
                            'b' -> out.append('\b')
                            'u' -> {
                                out.append(value.substring(i + 1, i + 5).toIntOrNull(16)?.toChar() ?: return null)
                                i += 4
                            }
                            else -> out.append(escaped)
                        }
                    }
                }
                i++
            }
            return out.toString()
        }
    }
}
