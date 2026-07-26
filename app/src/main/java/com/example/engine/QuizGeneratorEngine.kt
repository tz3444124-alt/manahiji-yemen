package com.example.engine

import com.example.data.local.DocumentPageChunk

enum class QuizScope(val titleAr: String) {
    LESSON("اختبار الدرس 📝"),
    UNIT("اختبار الوحدة 📚"),
    BOOK("اختبار الكتاب الشامل 📘")
}

enum class QuizDifficulty(val titleAr: String) {
    EASY("سهل 🟢"),
    MEDIUM("متوسط 🟡"),
    HARD("صعب 🔴")
}

data class QuizQuestion(
    val id: Int,
    val questionText: String,
    val questionType: QuestionType = QuestionType.MCQ,
    val difficulty: QuizDifficulty = QuizDifficulty.MEDIUM,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0,
    val correctAnswerText: String = "",
    val explanation: String,
    val pageNumber: Int,
    val docTitle: String
)

data class QuizSession(
    val subject: String,
    val scope: QuizScope = QuizScope.BOOK,
    val questions: List<QuizQuestion>,
    var userAnswers: MutableMap<Int, Int> = mutableMapOf(), // questionId -> selectedIndex
    var userTextAnswers: MutableMap<Int, String> = mutableMapOf(), // for Essay / Fill-in
    var isSubmitted: Boolean = false,
    var score: Int = 0
)

class QuizGeneratorEngine {

    fun generateQuizForSubject(
        subject: String,
        allChunks: List<DocumentPageChunk>,
        questionCount: Int = 6,
        scope: QuizScope = QuizScope.BOOK
    ): QuizSession {
        val subjectChunks = if (subject == "الكل" || subject.isBlank()) {
            allChunks
        } else {
            allChunks.filter { it.subject.equals(subject, ignoreCase = true) }
        }

        val scopedChunks = when (scope) {
            QuizScope.LESSON -> subjectChunks.take(2.coerceAtMost(subjectChunks.size.coerceAtLeast(1)))
            QuizScope.UNIT -> subjectChunks.take(6.coerceAtMost(subjectChunks.size.coerceAtLeast(1)))
            QuizScope.BOOK -> subjectChunks
        }

        val chunksToUse = if (scopedChunks.isNotEmpty()) scopedChunks else allChunks
        val rawQuestions = mutableListOf<QuizQuestion>()

        var qId = 1
        for (chunk in chunksToUse.shuffled()) {
            if (rawQuestions.size >= questionCount) break

            val text = chunk.pageText
            val page = chunk.pageNumber
            val doc = chunk.documentTitle

            // 1. MCQ Question (Easy/Medium)
            rawQuestions.add(
                QuizQuestion(
                    id = qId++,
                    questionText = "ما المفهوم الأساسي الوارد في الصفحة $page من كتاب ($doc)؟",
                    questionType = QuestionType.MCQ,
                    difficulty = QuizDifficulty.EASY,
                    options = listOf(
                        text.lines().firstOrNull { it.length in 10..60 } ?: "القواعد المنهجية الأساسية",
                        "مبدأ حفظ الطاقة في الفراغ",
                        "التفاعل الانشطاري الثانوي",
                        "تحليل المتجهات العمودية"
                    ),
                    correctOptionIndex = 0,
                    explanation = "ورد هذا المفهموم بالنص ص $page لكتاب $doc.",
                    pageNumber = page,
                    docTitle = doc
                )
            )

            // 2. True/False Question (Easy)
            if (rawQuestions.size < questionCount) {
                rawQuestions.add(
                    QuizQuestion(
                        id = qId++,
                        questionText = "صح أم خطأ: تنص القواعد المذكورة في ص $page على تطابق النظريات العلمية مع المنهج اليمني المعتمد؟",
                        questionType = QuestionType.TRUE_FALSE,
                        difficulty = QuizDifficulty.EASY,
                        options = listOf("عبارة صحيحة ✔️", "عبارة خاطئة ❌"),
                        correctOptionIndex = 0,
                        explanation = "صحيح، المنهج يؤكد على المطابقة الكاملة للقواعد المعيارية.",
                        pageNumber = page,
                        docTitle = doc
                    )
                )
            }

            // 3. Fill-in-the-blanks Question (Medium)
            if (rawQuestions.size < questionCount) {
                rawQuestions.add(
                    QuizQuestion(
                        id = qId++,
                        questionText = "أكمل الفراغ: يتناسب التغير في المادة طردياً مع ...... حسب الوارد ص $page.",
                        questionType = QuestionType.FILL_IN_BLANKS,
                        difficulty = QuizDifficulty.MEDIUM,
                        options = listOf("معدل الشحنة والجهد", "درجة الحرارة العظمى", "ثابت الجاذبية", "الكثافة الحجمية"),
                        correctOptionIndex = 0,
                        explanation = "تكتمل القوانين في الصفحة $page بالنسبة للثوابت المعتمدة.",
                        pageNumber = page,
                        docTitle = doc
                    )
                )
            }

            // 4. Essay Question (Hard)
            if (rawQuestions.size < questionCount) {
                rawQuestions.add(
                    QuizQuestion(
                        id = qId++,
                        questionText = "سؤال مقالي / علل: اشرح باختصار أهم النتائج والتطبيقات العلمية للدرس في ص $page؟",
                        questionType = QuestionType.ESSAY,
                        difficulty = QuizDifficulty.HARD,
                        options = listOf("إجابة مقالية نموذجية مستخرجة من النص ص $page"),
                        correctOptionIndex = 0,
                        correctAnswerText = text.take(150),
                        explanation = "الإجابة المقالية تعتمد على استخراج الشرح المنهجي الوارد بالصفحة.",
                        pageNumber = page,
                        docTitle = doc
                    )
                )
            }
        }

        // Sort questions by difficulty: Easy -> Medium -> Hard (ترتيب الأسئلة حسب الصعوبة)
        val sortedQuestions = rawQuestions.sortedBy { q ->
            when (q.difficulty) {
                QuizDifficulty.EASY -> 1
                QuizDifficulty.MEDIUM -> 2
                QuizDifficulty.HARD -> 3
            }
        }

        return QuizSession(
            subject = subject,
            scope = scope,
            questions = sortedQuestions
        )
    }

    fun evaluateQuiz(quizSession: QuizSession): QuizSession {
        var score = 0
        for (q in quizSession.questions) {
            val userSelected = quizSession.userAnswers[q.id]
            if (userSelected != null && userSelected == q.correctOptionIndex) {
                score += 1
            }
        }
        quizSession.score = score
        quizSession.isSubmitted = true
        return quizSession
    }
}

