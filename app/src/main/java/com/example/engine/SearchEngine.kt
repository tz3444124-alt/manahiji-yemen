package com.example.engine

import com.example.data.local.AppDao
import com.example.data.local.DocumentPageChunk
import com.example.data.local.HistoryRecord
import com.example.data.local.SearchLogRecord
import com.example.util.DatabaseBackupEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * High-Performance Local Directed Search Engine for RAG
 * Integrates Room SQLite (FTS4 & Keyword queries) with Vector Similarity (TF-IDF Cosine Vector Space Model)
 * to retrieve context & generate precise offline answers strictly from stored curriculum books without internet.
 */
class SearchEngine(
    private val appDao: AppDao
) {

    data class VectorSearchResult(
        val chunk: DocumentPageChunk,
        val similarityScore: Float,
        val ftsScore: Float,
        val combinedScore: Float,
        val highlightedText: String
    )

    data class LocalRagAnswer(
        val query: String,
        val isAnswerFound: Boolean,
        val synthesizedAnswer: String,
        val primarySourceBook: String?,
        val primarySourcePage: String?,
        val subject: String?,
        val confidenceScore: Float,
        val topMatchingChunks: List<DocumentPageChunk>
    )

    /**
     * Executes directed hybrid search combining Room FTS4/Keyword search with Vector Cosine Similarity.
     */
    suspend fun executeHybridSearch(
        query: String,
        filterSubject: String? = null,
        limit: Int = 10
    ): List<VectorSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val cleanQuery = query.trim()
        val normalizedQuery = ArabicTextNormalizer.normalize(cleanQuery)
        val queryKeywords = ArabicTextNormalizer.extractKeywords(cleanQuery)

        if (queryKeywords.isEmpty()) return@withContext emptyList()

        // 1. Retrieve Candidate Chunks from Room Database
        val candidateChunks = getCandidateChunksFromRoom(normalizedQuery, filterSubject)
        if (candidateChunks.isEmpty()) return@withContext emptyList()

        // 2. Compute Term Frequency & IDF across candidate chunks
        val idfMap = computeIdfMap(candidateChunks)
        val queryVector = computeTfIdfVector(normalizedQuery, idfMap)

        // 3. Rank Chunks using Vector Cosine Similarity + Keyword Matching
        val scoredResults = candidateChunks.map { chunk ->
            val normPageText = ArabicTextNormalizer.normalize(chunk.pageText)
            val chunkVector = computeTfIdfVector(normPageText, idfMap)
            
            val vectorSim = calculateCosineSimilarity(queryVector, chunkVector)
            val keywordScore = calculateKeywordFtsScore(queryKeywords, normPageText, chunk.keywords)
            
            // Weighted Hybrid Fusion: 60% Vector Similarity + 40% FTS Keyword Density
            val combinedScore = (vectorSim * 0.60f) + (keywordScore * 0.40f)

            val highlighted = highlightQueryTerms(chunk.pageText, queryKeywords)

            VectorSearchResult(
                chunk = chunk,
                similarityScore = vectorSim,
                ftsScore = keywordScore,
                combinedScore = combinedScore,
                highlightedText = highlighted
            )
        }
        .filter { it.combinedScore > 0.05f }
        .sortedByDescending { it.combinedScore }
        .take(limit)

        // 4. Log search query in Room DB search_logs table
        logSearchRecord(query, filterSubject, scoredResults.size)

        scoredResults
    }

    /**
     * Generates a complete Local RAG answer strictly extracted from offline Room database books.
     */
    suspend fun generateOfflineRagAnswer(
        query: String,
        filterSubject: String? = null
    ): LocalRagAnswer = withContext(Dispatchers.IO) {
        val searchResults = executeHybridSearch(query, filterSubject, limit = 5)

        if (searchResults.isEmpty()) {
            return@withContext LocalRagAnswer(
                query = query,
                isAnswerFound = false,
                synthesizedAnswer = RagSearchEngine.STRICT_NOT_FOUND_MESSAGE,
                primarySourceBook = null,
                primarySourcePage = null,
                subject = filterSubject,
                confidenceScore = 0f,
                topMatchingChunks = emptyList()
            )
        }

        val topResult = searchResults.first()
        val topChunk = topResult.chunk
        val realPage = if (topChunk.realPageNumber.isNotBlank()) topChunk.realPageNumber else "${topChunk.pageNumber}"

        // Build structured offline answer strictly based on retrieved content
        val answerBuilder = StringBuilder()
        answerBuilder.append("📚 **الإجابة المستخرجة محلياً من الكتاب:**\n\n")

        val bestParagraph = extractMostRelevantParagraph(topChunk.pageText, query)
        answerBuilder.append(bestParagraph.ifBlank { topChunk.pageText.take(400) })
        answerBuilder.append("\n\n---\n")
        answerBuilder.append("📖 **المصدر:** ${topChunk.documentTitle}\n")
        answerBuilder.append("📍 **الصفحة:** ص $realPage (المادة: ${topChunk.subject})\n")

        if (topChunk.headings.isNotBlank()) {
            answerBuilder.append("🏷️ **العنوان الرئيسي:** ${topChunk.headings}\n")
        }

        if (searchResults.size > 1) {
            answerBuilder.append("\n📌 **صفحات ذات صلة بالبحث:**\n")
            searchResults.drop(1).take(3).forEach { res ->
                val pNum = if (res.chunk.realPageNumber.isNotBlank()) res.chunk.realPageNumber else "${res.chunk.pageNumber}"
                answerBuilder.append("• ص $pNum في كتاب \"${res.chunk.documentTitle}\"\n")
            }
        }

        LocalRagAnswer(
            query = query,
            isAnswerFound = true,
            synthesizedAnswer = answerBuilder.toString(),
            primarySourceBook = topChunk.documentTitle,
            primarySourcePage = realPage,
            subject = topChunk.subject,
            confidenceScore = topResult.combinedScore,
            topMatchingChunks = searchResults.map { it.chunk }
        )
    }

    // ==========================================
    // Helper Methods for Room Query & Vector Math
    // ==========================================

    private suspend fun getCandidateChunksFromRoom(
        normalizedQuery: String,
        subject: String?
    ): List<DocumentPageChunk> {
        return try {
            if (!subject.isNullOrBlank() && subject != "الكل") {
                appDao.getChunksBySubject(subject)
            } else {
                // Try Full-Text Search (FTS4) first
                val ftsQuery = normalizedQuery.split(" ")
                    .filter { it.length > 1 }
                    .joinToString(" OR ") { "$it*" }

                val ftsMatches = if (ftsQuery.isNotBlank()) {
                    appDao.searchChunksFts(ftsQuery)
                } else emptyList()

                if (ftsMatches.isNotEmpty()) {
                    ftsMatches
                } else {
                    val kwMatches = appDao.searchChunksByKeyword(normalizedQuery)
                    if (kwMatches.isNotEmpty()) kwMatches else appDao.getAllChunks()
                }
            }
        } catch (e: Exception) {
            appDao.getAllChunks()
        }
    }

    private fun computeIdfMap(chunks: List<DocumentPageChunk>): Map<String, Float> {
        val docCount = chunks.size.toFloat()
        val docFreqMap = mutableMapOf<String, Int>()

        for (chunk in chunks) {
            val words = ArabicTextNormalizer.normalize(chunk.pageText)
                .split(" ")
                .filter { it.length > 1 }
                .toSet()

            for (w in words) {
                docFreqMap[w] = (docFreqMap[w] ?: 0) + 1
            }
        }

        val idfMap = mutableMapOf<String, Float>()
        for ((word, count) in docFreqMap) {
            idfMap[word] = ln((docCount + 1f) / (count + 1f)).toFloat() + 1f
        }
        return idfMap
    }

    private fun computeTfIdfVector(
        normalizedText: String,
        idfMap: Map<String, Float>
    ): Map<String, Float> {
        val words = normalizedText.split(" ").filter { it.length > 1 }
        if (words.isEmpty()) return emptyMap()

        val tfMap = mutableMapOf<String, Int>()
        for (w in words) {
            tfMap[w] = (tfMap[w] ?: 0) + 1
        }

        val totalWords = words.size.toFloat()
        val vector = mutableMapOf<String, Float>()

        for ((word, count) in tfMap) {
            val tf = count / totalWords
            val idf = idfMap[word] ?: 1.0f
            vector[word] = tf * idf
        }

        return vector
    }

    private fun calculateCosineSimilarity(
        vecA: Map<String, Float>,
        vecB: Map<String, Float>
    ): Float {
        if (vecA.isEmpty() || vecB.isEmpty()) return 0f

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for ((k, v) in vecA) {
            normA += v * v
            val vB = vecB[k]
            if (vB != null) {
                dotProduct += v * vB
            }
        }

        for (v in vecB.values) {
            normB += v * v
        }

        if (normA == 0f || normB == 0f) return 0f

        return (dotProduct / (sqrt(normA) * sqrt(normB))).toFloat()
    }

    private fun calculateKeywordFtsScore(
        queryKeywords: List<String>,
        pageTextNorm: String,
        chunkKeywordsNorm: String
    ): Float {
        if (queryKeywords.isEmpty()) return 0f

        var matchCount = 0
        for (kw in queryKeywords) {
            if (kw.length <= 1) continue
            val kwNorm = ArabicTextNormalizer.normalize(kw)
            if (pageTextNorm.contains(kwNorm) || chunkKeywordsNorm.contains(kwNorm)) {
                matchCount++
            }
        }

        return (matchCount.toFloat() / queryKeywords.size.toFloat()).coerceIn(0f, 1f)
    }

    private fun extractMostRelevantParagraph(text: String, query: String): String {
        val paragraphs = text.split(Regex("\n+")).filter { it.isNotBlank() }
        if (paragraphs.size <= 1) return text

        val queryKeywords = ArabicTextNormalizer.extractKeywords(query)
        var bestParagraph = paragraphs.first()
        var maxMatches = -1

        for (p in paragraphs) {
            val normP = ArabicTextNormalizer.normalize(p)
            var matches = 0
            for (kw in queryKeywords) {
                if (normP.contains(ArabicTextNormalizer.normalize(kw))) {
                    matches++
                }
            }
            if (matches > maxMatches) {
                maxMatches = matches
                bestParagraph = p
            }
        }

        return bestParagraph
    }

    private fun highlightQueryTerms(text: String, queryKeywords: List<String>): String {
        var highlighted = text
        for (kw in queryKeywords.take(5)) {
            if (kw.length > 2) {
                highlighted = highlighted.replace(kw, "**$kw**", ignoreCase = true)
            }
        }
        return highlighted
    }

    private suspend fun logSearchRecord(query: String, subject: String?, resultsCount: Int) {
        try {
            val encryptedQuery = DatabaseBackupEngine.encryptString(query)
            appDao.insertSearchLog(
                SearchLogRecord(
                    query = query,
                    encryptedQuery = encryptedQuery,
                    searchType = "RAG_HYBRID_VECTOR_ROOM",
                    resultsCount = resultsCount,
                    isEncrypted = true
                )
            )
            appDao.insertHistoryRecord(
                HistoryRecord(
                    type = "SEARCH_ENGINE_RAG",
                    title = "بحث محلي موجه: $query",
                    subject = subject ?: "جميع المواد",
                    resultSummary = "تم العثور على $resultsCount نتيجة في قاعدة البيانات المحلية"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
