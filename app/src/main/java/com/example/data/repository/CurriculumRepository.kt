package com.example.data.repository

import android.content.Context
import com.example.data.PreloadedCurriculumData
import com.example.data.local.AppDao
import com.example.data.local.CurriculumDocument
import com.example.data.local.DocumentPageChunk
import com.example.data.local.HistoryRecord
import com.example.data.local.StudentProfile
import com.example.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CurriculumRepository(
    private val appDao: AppDao,
    private val context: Context
) {
    val studentProfile: Flow<StudentProfile?> = appDao.getStudentProfile()
    val allDocuments: Flow<List<CurriculumDocument>> = appDao.getAllDocuments()
    val historyRecords: Flow<List<HistoryRecord>> = appDao.getAllHistoryRecords()

    private val ragEngine = RagSearchEngine()
    val searchEngine = SearchEngine(appDao)
    private val ocrExamSolverEngine = OcrExamSolverEngine(ragEngine)
    private val quizGeneratorEngine = QuizGeneratorEngine()
    private val bookSummarizerEngine = BookSummarizerEngine()

    suspend fun initializePreloadedDataIfNeeded() = withContext(Dispatchers.IO) {
        val existingDocs = appDao.getAllChunks()
        if (existingDocs.isEmpty()) {
            val docs = PreloadedCurriculumData.getInitialDocuments()
            for (doc in docs) {
                appDao.insertDocument(doc)
            }
            val chunks = PreloadedCurriculumData.getInitialPageChunks()
            appDao.insertPageChunks(chunks)
        }
    }

    suspend fun saveStudentProfile(name: String, school: String, phone: String, grade: String) = withContext(Dispatchers.IO) {
        val profile = StudentProfile(
            studentName = name,
            schoolName = school,
            phoneNumber = phone,
            gradeLevel = grade
        )
        appDao.saveStudentProfile(profile)
    }

    suspend fun addCustomDocument(
        title: String,
        subject: String,
        docType: String,
        filePath: String,
        extractedText: String,
        pageCount: Int
    ): Long = addCustomDocumentDetailed(
        title = title,
        subject = subject,
        docType = docType,
        stage = "المرحلة الثانوية",
        gradeLevel = "الثالث الثانوي",
        semester = "الفصل الأول",
        fileSize = "${(extractedText.length * 2 / 1024).coerceAtLeast(1)} MB",
        pageCount = pageCount,
        extractedText = extractedText,
        filePath = filePath
    )

    suspend fun addCustomDocumentDetailed(
        title: String,
        subject: String,
        docType: String,
        stage: String,
        gradeLevel: String,
        semester: String,
        fileSize: String,
        pageCount: Int,
        extractedText: String,
        filePath: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val calculatedFileSize = if (fileSize.isNotBlank()) fileSize else "${(extractedText.length / 500).coerceAtLeast(1)} MB"
        val effectivePages = pageCount.coerceAtLeast(1)

        // 1. Run Smart PDF Parsing & Extraction Engine
        val smartResult = com.example.engine.SmartPdfEngine.parseAndIndexDocument(
            docId = 0,
            title = title,
            subject = subject,
            rawText = extractedText,
            pageCount = effectivePages
        )

        val doc = CurriculumDocument(
            title = title,
            subject = subject,
            docType = docType,
            filePath = if (filePath.isNotBlank()) filePath else "uploads/${System.currentTimeMillis()}.pdf",
            fileSize = calculatedFileSize,
            pageCount = effectivePages,
            isPreloaded = false,
            stage = stage,
            gradeLevel = gradeLevel,
            semester = semester,
            tableOfContents = smartResult.tableOfContents,
            totalEmbeddedImages = smartResult.totalImages,
            totalTablesExtracted = smartResult.totalTables
        )
        val docId = appDao.insertDocument(doc)

        // 2. Save EACH page as a distinct DocumentPageChunk in DB
        val chunks = smartResult.pageChunks.map { pageRes ->
            DocumentPageChunk(
                documentId = docId,
                documentTitle = title,
                subject = subject,
                pageNumber = pageRes.pageNumber,
                realPageNumber = pageRes.realPageNumber,
                pageText = pageRes.pageText,
                keywords = pageRes.keywords,
                headings = pageRes.headings,
                tables = pageRes.tables,
                embeddedImages = pageRes.embeddedImages,
                isScannedOcr = pageRes.isScannedOcr,
                isTableOfContents = pageRes.isTableOfContents
            )
        }
        appDao.insertPageChunks(chunks)

        docId
    }

    suspend fun updateDocumentMetadata(
        docId: Long,
        title: String,
        subject: String,
        docType: String,
        stage: String,
        gradeLevel: String,
        semester: String,
        fileSize: String,
        pageCount: Int
    ) = withContext(Dispatchers.IO) {
        val existing = appDao.getDocumentById(docId) ?: return@withContext
        val updatedDoc = existing.copy(
            title = title,
            subject = subject,
            docType = docType,
            stage = stage,
            gradeLevel = gradeLevel,
            semester = semester,
            fileSize = fileSize.ifBlank { existing.fileSize },
            pageCount = if (pageCount > 0) pageCount else existing.pageCount
        )
        appDao.updateDocument(updatedDoc)
    }

    suspend fun updateDocumentWithReindexing(
        docId: Long,
        title: String,
        subject: String,
        docType: String,
        stage: String,
        gradeLevel: String,
        semester: String,
        fileSize: String,
        pageCount: Int,
        newExtractedText: String
    ) = withContext(Dispatchers.IO) {
        val existing = appDao.getDocumentById(docId) ?: return@withContext
        val effectivePages = if (pageCount > 0) pageCount else existing.pageCount

        val smartResult = com.example.engine.SmartPdfEngine.parseAndIndexDocument(
            docId = docId,
            title = title,
            subject = subject,
            rawText = newExtractedText,
            pageCount = effectivePages
        )

        val updatedDoc = existing.copy(
            title = title,
            subject = subject,
            docType = docType,
            stage = stage,
            gradeLevel = gradeLevel,
            semester = semester,
            fileSize = fileSize.ifBlank { existing.fileSize },
            pageCount = effectivePages,
            tableOfContents = smartResult.tableOfContents,
            totalEmbeddedImages = smartResult.totalImages,
            totalTablesExtracted = smartResult.totalTables
        )
        appDao.updateDocument(updatedDoc)

        if (newExtractedText.isNotBlank()) {
            appDao.deleteChunksByDocumentId(docId)

            val chunks = smartResult.pageChunks.map { pageRes ->
                DocumentPageChunk(
                    documentId = docId,
                    documentTitle = title,
                    subject = subject,
                    pageNumber = pageRes.pageNumber,
                    realPageNumber = pageRes.realPageNumber,
                    pageText = pageRes.pageText,
                    keywords = pageRes.keywords,
                    headings = pageRes.headings,
                    tables = pageRes.tables,
                    embeddedImages = pageRes.embeddedImages,
                    isScannedOcr = pageRes.isScannedOcr,
                    isTableOfContents = pageRes.isTableOfContents
                )
            }
            appDao.insertPageChunks(chunks)
        }
    }

    private suspend fun generateAndInsertChunks(
        docId: Long,
        title: String,
        subject: String,
        extractedText: String,
        pageCount: Int
    ) {
        val lines = extractedText.lines().filter { it.isNotBlank() }
        val effectiveText = if (lines.isEmpty()) listOf("محتوى المنهج الخاص بكتاب $title في مادة $subject") else lines
        val totalLines = effectiveText.size
        val pages = pageCount.coerceAtLeast(1)
        val linesPerPage = (totalLines / pages).coerceAtLeast(1)

        val chunks = mutableListOf<DocumentPageChunk>()
        for (p in 1..pages) {
            val startIdx = (p - 1) * linesPerPage
            val endIdx = (p * linesPerPage).coerceAtMost(totalLines)
            val pageText = if (startIdx < totalLines) {
                effectiveText.subList(startIdx, endIdx).joinToString("\n")
            } else {
                "محتوى ومفاهيم الدرس صفحة $p من كتاب $title"
            }

            val keywords = ArabicTextNormalizer.extractKeywords(pageText).joinToString(" ")
            chunks.add(
                DocumentPageChunk(
                    documentId = docId,
                    documentTitle = title,
                    subject = subject,
                    pageNumber = p,
                    pageText = pageText,
                    keywords = keywords
                )
            )
        }
        appDao.insertPageChunks(chunks)
    }

    suspend fun deleteDocument(docId: Long) = withContext(Dispatchers.IO) {
        appDao.deleteDocumentById(docId)
        appDao.deleteChunksByDocumentId(docId)
    }

    suspend fun searchRag(query: String, filterSubject: String? = null): RagSearchResult = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext RagSearchResult(
                query = query,
                isFound = false,
                answerText = RagSearchEngine.STRICT_NOT_FOUND_MESSAGE,
                matchedPageNumber = null,
                matchedDocTitle = null,
                matchedSubject = null,
                matchedExcerpt = null,
                confidenceScore = 0f
            )
        }

        // 1. Fetch relevant chunks from Room database using FTS or Subject/Keyword filter
        val chunksToSearch = if (!filterSubject.isNullOrBlank() && filterSubject != "الكل") {
            appDao.getChunksBySubject(filterSubject)
        } else {
            val ftsNorm = ArabicTextNormalizer.normalize(query).split(" ").filter { it.length > 1 }.joinToString(" OR ") { "$it*" }
            val ftsChunks = try {
                if (ftsNorm.isNotBlank()) appDao.searchChunksFts(ftsNorm) else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            if (ftsChunks.isNotEmpty()) {
                ftsChunks
            } else {
                val kwChunks = appDao.searchChunksByKeyword(query)
                if (kwChunks.isNotEmpty()) kwChunks else appDao.getAllChunks()
            }
        }

        // 2. Query RAG engine using ONLY chunks stored in local Room database
        val result = ragEngine.search(query, chunksToSearch, filterSubject)

        // 3. Log search query in Room's encrypted search_logs table
        try {
            val encryptedQueryStr = com.example.util.DatabaseBackupEngine.encryptString(query)
            appDao.insertSearchLog(
                com.example.data.local.SearchLogRecord(
                    query = query,
                    encryptedQuery = encryptedQueryStr,
                    searchType = "RAG_ROOM_LOCAL",
                    resultsCount = if (result.isFound) 1 else 0,
                    isEncrypted = true
                )
            )
            appDao.insertHistoryRecord(
                HistoryRecord(
                    type = "RAG_SEARCH",
                    title = "استعلام RAG محلي: $query",
                    subject = filterSubject ?: "جميع المواد",
                    resultSummary = if (result.isFound) "تم العثور على النتيجة في ${result.matchedDocTitle} (ص ${result.realPageNumber})" else "لم توجد نتائج متطابقة في المنهج المخزن محلياً"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        result
    }

    suspend fun searchRagAdvanced(
        query: String,
        options: SearchFilterOptions
    ): List<RagSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        // Fetch from Room DB based on filters
        val chunksToSearch = if (!options.subject.isNullOrBlank() && options.subject != "الكل") {
            appDao.getChunksBySubject(options.subject)
        } else {
            val ftsNorm = ArabicTextNormalizer.normalize(query).split(" ").filter { it.length > 1 }.joinToString(" OR ") { "$it*" }
            val ftsChunks = try {
                if (ftsNorm.isNotBlank()) appDao.searchChunksFts(ftsNorm) else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            if (ftsChunks.isNotEmpty()) {
                ftsChunks
            } else {
                val kwChunks = appDao.searchChunksByKeyword(query)
                if (kwChunks.isNotEmpty()) kwChunks else appDao.getAllChunks()
            }
        }

        val results = ragEngine.searchAdvanced(query, chunksToSearch, options)

        // Log search in Room
        try {
            val encryptedQueryStr = com.example.util.DatabaseBackupEngine.encryptString(query)
            appDao.insertSearchLog(
                com.example.data.local.SearchLogRecord(
                    query = query,
                    encryptedQuery = encryptedQueryStr,
                    searchType = "RAG_ADVANCED_ROOM",
                    resultsCount = results.size,
                    isEncrypted = true
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        results
    }

    suspend fun solveExamOcr(ocrText: String, subjectFilter: String? = null): ExamSolverReport = withContext(Dispatchers.IO) {
        val allChunks = appDao.getAllChunks()
        val report = ocrExamSolverEngine.solveExamPaper(ocrText, allChunks, subjectFilter)

        // Log history
        appDao.insertHistoryRecord(
            HistoryRecord(
                type = "EXAM_SOLVER",
                title = "حل امتحان (${report.solvedQuestionsCount}/${report.totalQuestionsCount} سؤال في المنهج)",
                subject = subjectFilter ?: "جميع المواد",
                resultSummary = "تم حل ${report.solvedQuestionsCount} سؤال واستخراج الصفحات بدقة."
            )
        )

        report
    }

    suspend fun generateQuiz(subject: String, questionCount: Int = 5): QuizSession = withContext(Dispatchers.IO) {
        val allChunks = appDao.getAllChunks()
        quizGeneratorEngine.generateQuizForSubject(subject, allChunks, questionCount)
    }

    fun evaluateQuiz(session: QuizSession): QuizSession {
        return quizGeneratorEngine.evaluateQuiz(session)
    }

    suspend fun saveQuizHistory(subject: String, score: Int, total: Int) = withContext(Dispatchers.IO) {
        appDao.insertHistoryRecord(
            HistoryRecord(
                type = "QUIZ",
                title = "اختبار الطالب - $subject",
                subject = subject,
                resultSummary = "النتيجة: $score من $total"
            )
        )
    }

    suspend fun summarizeBook(
        document: CurriculumDocument,
        scope: com.example.engine.SummaryScope = com.example.engine.SummaryScope.BOOK
    ): BookSummaryResult = withContext(Dispatchers.IO) {
        val chunks = appDao.getChunksForDocument(document.id)
        val summary = bookSummarizerEngine.generateBookSummary(document, chunks, scope)

        appDao.insertHistoryRecord(
            HistoryRecord(
                type = "SUMMARY",
                title = "تلخيص (${scope.titleAr}): ${document.title}",
                subject = document.subject,
                resultSummary = "تم تلخيص المادة بواقع ${summary.sections.size} قسم مفهرس أوفلاين."
            )
        )

        summary
    }

    suspend fun getChunksForDocument(docId: Long): List<DocumentPageChunk> = withContext(Dispatchers.IO) {
        appDao.getChunksForDocument(docId)
    }

    suspend fun exportDatabaseBackupFile(): java.io.File? = withContext(Dispatchers.IO) {
        com.example.util.DatabaseBackupEngine.exportDatabaseBackupFile(context, appDao)
    }

    suspend fun importDatabaseBackupFileContent(content: String): Boolean = withContext(Dispatchers.IO) {
        com.example.util.DatabaseBackupEngine.importDatabaseBackupFileContent(appDao, content)
    }

    suspend fun exportEncryptedBackup(): String = withContext(Dispatchers.IO) {
        com.example.util.DatabaseBackupEngine.exportEncryptedBackup(appDao)
    }

    suspend fun restoreEncryptedBackup(backupData: String): Boolean = withContext(Dispatchers.IO) {
        com.example.util.DatabaseBackupEngine.restoreEncryptedBackup(appDao, backupData)
    }

    suspend fun exportRawDatabaseFile(): java.io.File? = withContext(Dispatchers.IO) {
        com.example.util.DatabaseBackupEngine.exportRawDatabaseFile(context)
    }

    suspend fun restoreRawDatabaseFile(backupFile: java.io.File): Boolean = withContext(Dispatchers.IO) {
        com.example.util.DatabaseBackupEngine.restoreRawDatabaseFile(context, backupFile)
    }

    suspend fun searchLocalVectorRag(query: String, filterSubject: String? = null): SearchEngine.LocalRagAnswer = withContext(Dispatchers.IO) {
        searchEngine.generateOfflineRagAnswer(query, filterSubject)
    }

    suspend fun executeDirectedHybridSearch(query: String, filterSubject: String? = null, limit: Int = 10): List<SearchEngine.VectorSearchResult> = withContext(Dispatchers.IO) {
        searchEngine.executeHybridSearch(query, filterSubject, limit)
    }
}
