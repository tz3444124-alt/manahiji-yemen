package com.example.engine

import com.example.data.local.DocumentPageChunk

enum class QuestionType(val displayName: String) {
    MCQ("اختيار من متعدد 🔘"),
    TRUE_FALSE("صواب / خطأ ⚡"),
    FILL_IN_BLANKS("أكمل الفراغات ✍️"),
    ESSAY("سؤال مقالي / علل 📝"),
    MATCHING("توصيل / مقارنة 🔗")
}

data class ExamQuestionSolution(
    val questionNumber: Int,
    val questionText: String,
    val questionType: QuestionType,
    val solutionText: String,
    val isFoundInCurriculum: Boolean,
    val bookTitle: String? = null,
    val pageNumber: String? = null,
    val paragraphExcerpt: String? = null,
    val pageCitation: String? = null
)

data class ExamSolverReport(
    val examTitle: String,
    val rawExtractedText: String,
    val questions: List<ExamQuestionSolution>,
    val totalQuestionsCount: Int,
    val solvedQuestionsCount: Int
)

class OcrExamSolverEngine(private val ragEngine: RagSearchEngine) {

    companion object {
        const val STRICT_NOT_FOUND_EXAM_MSG = "الإجابة غير موجودة في الكتب المضافة."
    }

    fun solveExamPaper(
        ocrText: String,
        allChunks: List<DocumentPageChunk>,
        subjectFilter: String? = null
    ): ExamSolverReport {
        val normalizedOcrText = ArabicTextNormalizer.normalizeOcrText(ocrText)
        val extractedQuestions = parseExamQuestions(if (normalizedOcrText.isNotBlank()) normalizedOcrText else ocrText)
        val solvedQuestionsList = mutableListOf<ExamQuestionSolution>()

        var solvedCount = 0

        for ((index, qText) in extractedQuestions.withIndex()) {
            val qNum = index + 1
            val type = detectQuestionType(qText)
            val ragResult = ragEngine.search(qText, allChunks, subjectFilter)

            if (ragResult.isFound && !ragResult.answerText.contains("لم نجد نتائج") && !ragResult.answerText.contains("عذراً")) {
                solvedCount++
                val bookName = ragResult.matchedDocTitle ?: "الكتاب المنهجي"
                val realPage = ragResult.realPageNumber ?: "${ragResult.matchedPageNumber ?: 1}"
                val citation = "📖 كتاب: $bookName | 📍 صفحة: $realPage"
                val excerpt = ragResult.matchedExcerpt ?: "الفقرة المعتمدة في المنهج الدراسي"

                val formattedSolution = buildString {
                    append("🎯 **الإجابة النموذجية من المنهج:**\n")
                    append("${ragResult.matchedExcerpt?.trim()}\n\n")
                    append("📚 **اسم الكتاب:** $bookName\n")
                    append("📍 **الصفحة:** ص $realPage\n")
                    append("📄 **الفقرة النصية:** $excerpt")
                }

                solvedQuestionsList.add(
                    ExamQuestionSolution(
                        questionNumber = qNum,
                        questionText = qText,
                        questionType = type,
                        solutionText = formattedSolution,
                        isFoundInCurriculum = true,
                        bookTitle = bookName,
                        pageNumber = realPage,
                        paragraphExcerpt = excerpt,
                        pageCitation = citation
                    )
                )
            } else {
                solvedQuestionsList.add(
                    ExamQuestionSolution(
                        questionNumber = qNum,
                        questionText = qText,
                        questionType = type,
                        solutionText = STRICT_NOT_FOUND_EXAM_MSG,
                        isFoundInCurriculum = false,
                        bookTitle = null,
                        pageNumber = null,
                        paragraphExcerpt = null,
                        pageCitation = null
                    )
                )
            }
        }

        return ExamSolverReport(
            examTitle = "ورقة اختبار ممسوحة ضوئياً (PDF / OCR)",
            rawExtractedText = ocrText,
            questions = solvedQuestionsList,
            totalQuestionsCount = extractedQuestions.size,
            solvedQuestionsCount = solvedCount
        )
    }

    private fun detectQuestionType(text: String): QuestionType {
        val lower = text.trim().lowercase()
        return when {
            lower.contains("اختر") || lower.contains("أ)") || lower.contains("ب)") || lower.contains("ج)") || lower.contains("د)") || lower.contains("الخيارات") -> QuestionType.MCQ
            lower.contains("ضع كلمة صح") || lower.contains("صواب") || lower.contains("خطأ") || lower.contains("✔") || lower.contains("✘") -> QuestionType.TRUE_FALSE
            lower.contains("أكمل") || lower.contains("فراغ") || lower.contains(".....") || lower.contains("___") -> QuestionType.FILL_IN_BLANKS
            lower.contains("صل") || lower.contains("قارن") || lower.contains("العمود") -> QuestionType.MATCHING
            else -> QuestionType.ESSAY
        }
    }

    private fun parseExamQuestions(text: String): List<String> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val questions = mutableListOf<String>()

        var currentBuffer = StringBuilder()

        for (line in lines) {
            val isQuestionHeader = line.matches(Regex("^(س|سؤال|Q|q|\\d+)[\\.\\:\\-\\s\\)].*")) ||
                    line.endsWith("؟") || line.contains("علل") || line.contains("عرف") ||
                    line.contains("اذكر") || line.contains("احسب") || line.contains("اختر") || line.contains("ضع كلمة")

            if (isQuestionHeader && currentBuffer.isNotBlank()) {
                questions.add(currentBuffer.toString().trim())
                currentBuffer = StringBuilder()
            }

            if (currentBuffer.isNotEmpty()) {
                currentBuffer.append(" ")
            }
            currentBuffer.append(line)
        }

        if (currentBuffer.isNotBlank()) {
            questions.add(currentBuffer.toString().trim())
        }

        return if (questions.isEmpty() && text.isNotBlank()) {
            listOf(text.trim())
        } else {
            questions
        }
    }
}

