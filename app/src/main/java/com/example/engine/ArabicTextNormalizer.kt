package com.example.engine

object ArabicTextNormalizer {

    private val TASHKEEL_REGEX = Regex("[\\u064B-\\u0652\\u0670]")
    private val TATWEEL_REGEX = Regex("[\\u0640]") // Kashida
    private val PUNCTUATION_REGEX = Regex("[^\\p{L}\\p{N}\\s]")

    private val EASTERN_ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /**
     * Normalizes raw Arabic text by removing Tashkeel, Tatweel, converting digits,
     * and unifying letter forms (Hamzas, Alef, Yeh, Teh Marbuta).
     */
    fun normalize(text: String): String {
        var result = text.trim()

        // Convert Eastern Arabic Digits (٠-٩) to Western (0-9)
        result = convertDigitsToWestern(result)

        // Remove Tashkeel (diacritics) and Tatweel (kashida)
        result = result.replace(TASHKEEL_REGEX, "")
        result = result.replace(TATWEEL_REGEX, "")

        // Normalize Alef variants (أ, إ, آ, ٱ) -> ا
        result = result.replace(Regex("[أإآٱ]"), "ا")

        // Normalize Yeh / Alef Maqsura (ى) -> ي
        result = result.replace("ى", "ي")

        // Normalize Teh Marbuta (ة) -> ه
        result = result.replace("ة", "ه")

        // Normalize Hamza variants (ؤ -> و, ئ -> ي)
        result = result.replace("ؤ", "و").replace("ئ", "ي")

        // Remove non-alphanumeric punctuation except spaces
        result = result.replace(PUNCTUATION_REGEX, " ")

        // Collapse multiple whitespace characters into single space
        result = result.replace(Regex("\\s+"), " ")

        return result.lowercase().trim()
    }

    /**
     * Cleans and normalizes raw text produced by OCR scanners.
     */
    fun normalizeOcrText(rawOcr: String): String {
        if (rawOcr.isBlank()) return ""

        var cleaned = rawOcr
            // Convert digits
            .let { convertDigitsToWestern(it) }
            // Remove diacritics
            .replace(TASHKEEL_REGEX, "")
            .replace(TATWEEL_REGEX, "")
            // Unify OCR question headers (e.g. س١ -> س1, س 2 -> س2)
            .replace(Regex("(س|سؤال|Q|q)\\s*([0-9]+|\\d+)"), "$1 $2")

        return cleaned.trim()
    }

    /**
     * Converts Eastern Arabic digits (٠١٢٣٤٥٦٧٨٩) to standard ASCII numbers (0123456789).
     */
    fun convertDigitsToWestern(text: String): String {
        var sb = StringBuilder(text.length)
        for (ch in text) {
            val digitIndex = EASTERN_ARABIC_DIGITS.indexOf(ch)
            if (digitIndex != -1) {
                sb.append(('0'.code + digitIndex).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Extracts distinct keywords from text, optionally stripping the 'ال' prefix for enhanced matching.
     */
    fun extractKeywords(text: String, stripPrefixAL: Boolean = true): List<String> {
        val stopWords = setOf(
            "في", "من", "على", "إلى", "عن", "مع", "هذا", "هذه", "التي", "الذي", "أن", "إن",
            "ما", "هو", "هي", "هم", "كان", "كانت", "يكون", "تكون", "ماذا", "كيف", "لماذا",
            "كم", "أين", "متى", "هل", "منذ", "حتى", "غير", "بين", "حول", "كل", "بعض", "سؤال",
            "إجابة", "جواب", "أو", "ثم", "بل", "قد", "لقد", "عند", "بعد", "قبل"
        )

        val normalized = normalize(text)
        val rawTokens = normalized.split(Regex("\\s+"))

        val keywords = mutableListOf<String>()

        for (token in rawTokens) {
            if (token.length <= 2 || stopWords.contains(token)) continue

            keywords.add(token)

            // Optionally add stem without 'ال' prefix (e.g. "الكيمياء" -> "كيمياء")
            if (stripPrefixAL && token.startsWith("ال") && token.length > 4) {
                keywords.add(token.substring(2))
            }
        }

        return keywords.distinct()
    }
}

