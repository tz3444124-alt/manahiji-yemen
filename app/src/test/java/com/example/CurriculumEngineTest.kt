package com.example

import com.example.data.local.CurriculumDocument
import com.example.data.local.DocumentPageChunk
import com.example.engine.*
import com.example.util.DatabaseBackupEngine
import org.junit.Assert.*
import org.junit.Test

class CurriculumEngineTest {

    @Test
    fun testArabicTextNormalizer_handlesEmptyAndSpecialStrings() {
        val normalizedEmpty = ArabicTextNormalizer.normalize("")
        assertEquals("", normalizedEmpty)

        val textWithDiacritics = "اَلْفِيزِيَاءُ"
        val normalized = ArabicTextNormalizer.normalize(textWithDiacritics)
        assertEquals("الفيزياء", normalized)

        val keywords = ArabicTextNormalizer.extractKeywords("")
        assertTrue(keywords.isEmpty())
    }

    @Test
    fun testSmartPdfEngine_parseAndIndexDocument_noCrashOnEdgeCases() {
        // Zero pages, empty raw text
        val resultZeroPages = SmartPdfEngine.parseAndIndexDocument(
            docId = 100L,
            title = "اختبار صفري",
            subject = "الفيزياء",
            rawText = "",
            pageCount = 0
        )
        assertNotNull(resultZeroPages)
        assertTrue(resultZeroPages.pageChunks.isNotEmpty())

        // Multiple pages, text with table and image keywords
        val sampleText = """
            الوحدة الأولى: الكهربائية والمغناطيسية
            الدرس الأول: الشحنة الكهربائية وقانون كولوم
            جدول (1-1): مقارنة بين الشحنات
            شكل (1-2): رسم تخطيطي للمجال
            ص 15
        """.trimIndent()

        val resultMultiPage = SmartPdfEngine.parseAndIndexDocument(
            docId = 101L,
            title = "فيزياء الثالث الثانوي",
            subject = "الفيزياء",
            rawText = sampleText,
            pageCount = 3
        )
        assertNotNull(resultMultiPage)
        assertEquals(3, resultMultiPage.pageChunks.size)
        assertTrue(resultMultiPage.totalTables >= 0)
        assertTrue(resultMultiPage.totalImages >= 0)
    }

    @Test
    fun testMlKitOfflineOcrEngine_processImageOfflineOcr_safety() {
        val ocrEmpty = MlKitOfflineOcrEngine.processImageOfflineOcr("", 1, "تست")
        assertNotNull(ocrEmpty)

        val ocrWithMath = MlKitOfflineOcrEngine.processImageOfflineOcr("F = m * a \n 12345", 2, "تست")
        assertNotNull(ocrWithMath)
        assertTrue(ocrWithMath.numbersAndDigits.contains("12345") || ocrWithMath.fullText.contains("12345"))
    }

    @Test
    fun testBookSummarizerEngine_generateBookSummary_returnsValidResult() {
        val summarizer = BookSummarizerEngine()
        val doc = CurriculumDocument(
            id = 1L,
            title = "فيزياء 3 ثانوي",
            subject = "الفيزياء",
            docType = "BOOK",
            pageCount = 10,
            filePath = "/sdcard/physics.pdf",
            fileSize = "2MB"
        )
        val chunks = listOf(
            DocumentPageChunk(
                id = 1L,
                documentId = 1L,
                documentTitle = doc.title,
                subject = doc.subject,
                pageNumber = 1,
                realPageNumber = "1",
                pageText = "تعريف قانون كولوم والقوة الكهربائية F = k*q1*q2/r^2",
                keywords = "قانون كولوم الشحنة الكهربائية"
            )
        )

        val result = summarizer.generateBookSummary(doc, chunks, SummaryScope.BOOK)
        assertNotNull(result)
        assertEquals("فيزياء 3 ثانوي", result.docTitle)
        assertTrue(result.sections.isNotEmpty())
    }

    @Test
    fun testQuizGeneratorEngine_generateQuizForSubject_returnsQuestions() {
        val quizEngine = QuizGeneratorEngine()
        val chunks = listOf(
            DocumentPageChunk(
                id = 1L,
                documentId = 1L,
                documentTitle = "كيمياء 3 ثانوي",
                subject = "الكيمياء",
                pageNumber = 1,
                realPageNumber = "1",
                pageText = "العناصر الانتقالية والتوزيع الإلكتروني لعنصر الحديد Fe",
                keywords = "العناصر الانتقالية الحديد"
            )
        )

        val quizSession = quizEngine.generateQuizForSubject("الكيمياء", chunks, questionCount = 3)
        assertNotNull(quizSession)
        assertEquals("الكيمياء", quizSession.subject)
        assertTrue(quizSession.questions.isNotEmpty())
        quizSession.questions.forEach { q ->
            assertNotNull(q.questionText)
            assertTrue(q.options.isNotEmpty())
        }
    }

    @Test
    fun testDatabaseBackupEngine_encryptionDecryption_roundTrip() {
        val secret = "YemenCurriculumBackup2026SecureData"
        val encrypted = DatabaseBackupEngine.encryptString(secret)
        assertNotNull(encrypted)

        val decrypted = DatabaseBackupEngine.decryptString(encrypted)
        assertEquals(secret, decrypted)
    }
}
