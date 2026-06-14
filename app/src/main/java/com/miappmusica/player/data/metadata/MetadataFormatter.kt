package com.miappmusica.player.data.metadata

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure, deterministic cleaning of titles/artists extracted from externally-downloaded files.
 * No I/O and no Android dependencies, so it is fully unit-testable.
 */
@Singleton
class MetadataFormatter @Inject constructor() {

    private val noisePatterns = listOf(
        Regex("""(?i)\(\s*official.*?\)"""),
        Regex("""(?i)\[\s*official.*?]"""),
        Regex("""(?i)\(\s*lyrics?.*?\)"""),
        Regex("""(?i)\[\s*lyrics?.*?]"""),
        Regex("""(?i)\(\s*audio.*?\)"""),
        Regex("""(?i)\(\s*video.*?\)"""),
        Regex("""(?i)\(\s*hd\s*\)"""),
        Regex("""(?i)\(\s*4k\s*\)"""),
        Regex("""(?i)\(\s*visualizer.*?\)"""),
        Regex("""(?i)\bofficial\s+music\s+video\b"""),
        Regex("""(?i)\blyric\s+video\b"""),
        Regex("""(?i)\bfull\s+album\b"""),
        Regex("""(?i)\bHQ\b"""),
        Regex("""(?i)\b320\s*kbps\b"""),
        Regex("""(?i)free\s*download""")
    )

    private val separators = listOf(" - ", " – ", " — ", " _ ", "_-_", "__")

    /** Removes leading track numbers like "01.", "1 -", "07_". */
    private val leadingTrackNo = Regex("""^\s*\d{1,3}\s*[.\-_)]\s*""")

    fun cleanString(raw: String): String {
        var s = raw
        noisePatterns.forEach { s = it.replace(s, " ") }
        s = s.replace('_', ' ')
        s = s.replace(Regex("""\s+"""), " ").trim()
        s = s.trim(' ', '-', '–', '—', '.', '|')
        return titleCaseSmart(s)
    }

    /**
     * Splits a filename-style "Artist - Title" string into a best-effort (artist, title) pair.
     * Returns null for the artist component when no reliable separator is present.
     */
    fun splitArtistTitle(raw: String): Pair<String?, String> {
        val base = leadingTrackNo.replace(raw, "")
            .substringBeforeLast('.') // drop extension if a filename leaked in
        val sep = separators.firstOrNull { base.contains(it) }
        return if (sep != null) {
            val parts = base.split(sep, limit = 2)
            cleanString(parts[0]).takeIf { it.isNotBlank() } to cleanString(parts.getOrElse(1) { "" })
        } else {
            null to cleanString(base)
        }
    }

    private val lowerWords = setOf(
        "a", "an", "and", "the", "of", "or", "to", "in", "on", "for", "by",
        "de", "la", "el", "los", "las", "y", "del", "en"
    )

    private fun titleCaseSmart(input: String): String {
        if (input.isBlank()) return input
        // Preserve ALL-CAPS acronyms and intentional stylings; only fix shouty/lower text.
        val words = input.split(" ")
        return words.mapIndexed { index, word ->
            when {
                word.isEmpty() -> word
                word.any { it.isDigit() } -> word
                word == word.uppercase() && word.length <= 4 -> word // acronym
                index != 0 && lowerWords.contains(word.lowercase()) -> word.lowercase()
                else -> word.lowercase().replaceFirstChar { it.uppercase() }
            }
        }.joinToString(" ")
    }
}
