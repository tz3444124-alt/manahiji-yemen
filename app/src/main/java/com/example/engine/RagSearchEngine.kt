package com.example.engine

import com.example.data.local.DocumentPageChunk

enum class SearchMode {
    KEYWORD,   // البحث بالكلمة
    PHRASE,    // البحث بالجملة
    SEMANTIC   // البحث بالمعنى
}

data class RagSearchResult(
    val query: String,
    val isFound: Boolean,
    val answerText: String,
    val matchedPageNumber: Int?,
    val realPageNumber: String? = null,
    val matchedDocTitle: String?,
    val matchedSubject: String?,
    val matchedExcerpt: String?,
    val highlightedExcerpt: String? = null,
    val headings: String? = null,
    val tables: String? = null,
    val embeddedImages: String? = null,
    val confidenceScore: Float
)

data class SearchFilterOptions(
    val subject: String? = null,
    val gradeLevel: String? = null,
    val docTitle: String? = null,
    val docTypeFilter: String? = null, // "ALL", "SUMMARY", "EXAM", "BOOK"
    val searchMode: SearchMode = SearchMode.SEMANTIC
)

class RagSearchEngine {

    companion object {
        const val STRICT_NOT_FOUND_MESSAGE = "عذراً، لم نجد نتائج تطابق معايير البحث في المنهج المرفوع"
        private const val MIN_CONFIDENCE_THRESHOLD = 0.08f
    }

    /**
     * Advanced Multi-Result Search Engine supporting:
     * - Keyword search, Phrase search, Semantic/Meaning search
     * - Filters by Subject, Grade, Specific Book, Summaries only, Exams only
     * - Relevance accuracy sorting (ترتيب النتائج حسب الدقة)
     * - Highlight matching query terms inside pages (إبراز الكلمة داخل الصفحة)
     */
    fun searchAdvanced(
        query: String,
        allChunks: List<DocumentPageChunk>,
        options: SearchFilterOptions = SearchFilterOptions()
    ): List<RagSearchResult> {
        if (query.isBlank() || allChunks.isEmpty()) {
            return emptyList()
        }

        val cleanQuery = query.trim()
        val queryKeywords = ArabicTextNormalizer.extractKeywords(cleanQuery)

        // 1. Filter Chunks according to SearchFilterOptions
        val filteredChunks = allChunks.filter { chunk ->
            // Subject filter
            val matchSubject = options.subject.isNullOrBlank() || options.subject.equals("الكل", ignoreCase = true) || chunk.subject.contains(options.subject, ignoreCase = true)
            
            // Document Title / Book filter
            val matchDocTitle = options.docTitle.isNullOrBlank() || options.docTitle.equals("الكل", ignoreCase = true) || chunk.documentTitle.contains(options.docTitle, ignoreCase = true)

            // Document Type Filter (Summaries / Exams / Books)
            val matchDocType = when (options.docTypeFilter?.uppercase()) {
                "SUMMARY", "ملخص", "ملخصات" -> chunk.documentTitle.contains("ملخص") || chunk.documentTitle.contains("تلاخيص") || chunk.keywords.contains("ملخص")
                "EXAM", "امتحان", "اختبارات" -> chunk.documentTitle.contains("امتحان") || chunk.documentTitle.contains("اختبار") || chunk.documentTitle.contains("أسئلة") || chunk.keywords.contains("امتحان")
                "BOOK", "كتاب" -> !chunk.documentTitle.contains("ملخص") && !chunk.documentTitle.contains("امتحان")
                else -> true
            }

            matchSubject && matchDocTitle && matchDocType
        }

        if (filteredChunks.isEmpty()) return emptyList()

        // 2. Score and Rank Chunks
        val scoredResults = mutableListOf<RagSearchResult>()

        for (chunk in filteredChunks) {
            val score = calculateScoreForMode(cleanQuery, queryKeywords, chunk, options.searchMode)
            if (score >= MIN_CONFIDENCE_THRESHOLD) {
                val highlighted = highlightKeywordsInText(chunk.pageText, queryKeywords)
                val docTitle = chunk.documentTitle
                val pageNum = chunk.pageNumber
                val realPage = if (chunk.realPageNumber.isNotBlank()) chunk.realPageNumber else "$pageNum"
                val subject = chunk.subject

                val answerTextBuilder = StringBuilder()
                answerTextBuilder.append("📖 الكتاب: $docTitle\n")
                answerTextBuilder.append("📍 الصفحة: ص $realPage (النظامي: $pageNum)\n\n")

                if (chunk.headings.isNotBlank()) {
                    answerTextBuilder.append("🏷️ العنوان: ${chunk.headings}\n\n")
                }

                answerTextBuilder.append("📌 النص:\n$highlighted\n")

                if (chunk.tables.isNotBlank()) {
                    answerTextBuilder.append("\n${chunk.tables}\n")
                }

                if (chunk.embeddedImages.isNotBlank()) {
                    answerTextBuilder.append("\n${chunk.embeddedImages}\n")
                }

                scoredResults.add(
                    RagSearchResult(
                        query = cleanQuery,
                        isFound = true,
                        answerText = answerTextBuilder.toString(),
                        matchedPageNumber = pageNum,
                        realPageNumber = realPage,
                        matchedDocTitle = docTitle,
                        matchedSubject = subject,
                        matchedExcerpt = chunk.pageText,
                        highlightedExcerpt = highlighted,
                        headings = chunk.headings,
                        tables = chunk.tables,
                        embeddedImages = chunk.embeddedImages,
                        confidenceScore = score
                    )
                )
            }
        }

        // 3. Sort Results by Accuracy / Relevance (ترتيب النتائج حسب الدقة)
        return scoredResults.sortedByDescending { it.confidenceScore }
    }

    /**
     * Compatibility single-match search method for chat engine
     */
    fun search(
        query: String,
        allChunks: List<DocumentPageChunk>,
        filterSubject: String? = null
    ): RagSearchResult {
        val results = searchAdvanced(query, allChunks, SearchFilterOptions(subject = filterSubject))
        return if (results.isNotEmpty()) {
            results.first()
        } else {
            RagSearchResult(
                query = query,
                isFound = false,
                answerText = STRICT_NOT_FOUND_MESSAGE,
                matchedPageNumber = null,
                matchedDocTitle = null,
                matchedSubject = null,
                matchedExcerpt = null,
                confidenceScore = 0f
            )
        }
    }

    private fun calculateScoreForMode(
        query: String,
        queryKeywords: List<String>,
        chunk: DocumentPageChunk,
        mode: SearchMode
    ): Float {
        val normQuery = ArabicTextNormalizer.normalize(query)
        val chunkNormText = ArabicTextNormalizer.normalize("${chunk.pageText} ${chunk.headings} ${chunk.keywords}")

        return when (mode) {
            SearchMode.KEYWORD -> {
                // Exact or partial keyword occurrences
                var matches = 0
                for (kw in queryKeywords) {
                    if (chunkNormText.contains(kw)) matches++
                }
                if (queryKeywords.isEmpty()) 0f else (matches.toFloat() / queryKeywords.size)
            }

            SearchMode.PHRASE -> {
                // Exact exact substring or phrase match score
                if (chunkNormText.contains(normQuery)) {
                    1.0f
                } else {
                    val words = normQuery.split(" ").filter { it.length > 2 }
                    val found = words.count { chunkNormText.contains(it) }
                    if (words.isEmpty()) 0f else (found.toFloat() / words.size) * 0.7f
                }
            }

            SearchMode.SEMANTIC -> {
                // Hybrid semantic TF-IDF + Keyword density score
                val chunkKeywords = ArabicTextNormalizer.extractKeywords(chunkNormText)
                if (chunkKeywords.isEmpty()) return 0f

                var matches = 0
                for (qKey in queryKeywords) {
                    if (chunkNormText.contains(qKey)) matches++
                }

                val keywordMatchRatio = if (queryKeywords.isEmpty()) 0f else matches.toFloat() / queryKeywords.size
                val textDensityRatio = (matches.toFloat() / (chunkKeywords.size.toFloat().coerceAtLeast(8f))) * 2.5f

                (keywordMatchRatio * 0.75f) + (textDensityRatio * 0.25f)
            }
        }
    }

    /**
     * Highlights keywords inside text excerpts with high contrast markers (e.g., 🌟 كلمة 🌟)
     */
    private fun highlightKeywordsInText(text: String, keywords: List<String>): String {
        if (text.isBlank() || keywords.isEmpty()) return text

        var result = text
        for (kw in keywords) {
            if (kw.length >= 2) {
                val regex = Regex("(?i)($kw)")
                result = regex.replace(result) { matchResult ->
                    "✨${matchResult.value}✨"
                }
            }
        }
        return result
    }
}

