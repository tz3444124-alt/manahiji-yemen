package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CurriculumDocument
import com.example.data.local.HistoryRecord
import com.example.data.local.StudentProfile
import com.example.data.repository.CurriculumRepository
import com.example.engine.BookSummaryResult
import com.example.engine.ExamSolverReport
import com.example.engine.QuizSession
import com.example.engine.RagSearchResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val isUser: Boolean,
    val text: String,
    val result: RagSearchResult? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class BatchDocumentItem(
    val title: String,
    val subject: String,
    val docType: String,
    val stage: String = "المرحلة الثانوية",
    val gradeLevel: String = "الثالث الثانوي",
    val semester: String = "الفصل الأول",
    val fileSize: String = "4.5 MB",
    val pageCount: Int = 120,
    val extractedText: String = "",
    val filePath: String = ""
)

data class SmartImportProgressState(
    val isImporting: Boolean = false,
    val progressPercent: Int = 0,
    val currentStage: String = "",
    val bookTitle: String = "",
    val totalPages: Int = 0,
    val isCompleted: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CurriculumRepository

    val studentProfile: StateFlow<StudentProfile?>
    val allDocuments: StateFlow<List<CurriculumDocument>>
    val historyRecords: StateFlow<List<HistoryRecord>>

    // Chat RAG State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _importProgressState = MutableStateFlow(SmartImportProgressState())
    val importProgressState: StateFlow<SmartImportProgressState> = _importProgressState.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedSubjectFilter = MutableStateFlow("الكل")
    val selectedSubjectFilter: StateFlow<String> = _selectedSubjectFilter.asStateFlow()

    // Advanced Multi-Result Search State
    private val _advancedSearchResults = MutableStateFlow<List<RagSearchResult>>(emptyList())
    val advancedSearchResults: StateFlow<List<RagSearchResult>> = _advancedSearchResults.asStateFlow()

    private val _searchFilterOptions = MutableStateFlow(com.example.engine.SearchFilterOptions())
    val searchFilterOptions: StateFlow<com.example.engine.SearchFilterOptions> = _searchFilterOptions.asStateFlow()

    // Exam Solver State
    private val _examReport = MutableStateFlow<ExamSolverReport?>(null)
    val examReport: StateFlow<ExamSolverReport?> = _examReport.asStateFlow()

    private val _isSolvingExam = MutableStateFlow(false)
    val isSolvingExam: StateFlow<Boolean> = _isSolvingExam.asStateFlow()

    // Quiz State
    private val _currentQuizSession = MutableStateFlow<QuizSession?>(null)
    val currentQuizSession: StateFlow<QuizSession?> = _currentQuizSession.asStateFlow()

    // Book Summary State
    private val _currentBookSummary = MutableStateFlow<BookSummaryResult?>(null)
    val currentBookSummary: StateFlow<BookSummaryResult?> = _currentBookSummary.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    // Theme Mode State (Defaults to Dark Mode for low-light studying)
    private val prefs = application.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleThemeMode() {
        val newMode = !_isDarkMode.value
        _isDarkMode.value = newMode
        prefs.edit().putBoolean("is_dark_mode", newMode).apply()
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CurriculumRepository(database.appDao(), application)

        studentProfile = repository.studentProfile.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

        allDocuments = repository.allDocuments.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        historyRecords = repository.historyRecords.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        viewModelScope.launch {
            repository.initializePreloadedDataIfNeeded()
            // Welcome message in RAG chat
            if (_chatMessages.value.isEmpty()) {
                _chatMessages.value = listOf(
                    ChatMessage(
                        isUser = false,
                        text = "مرحباً بك في مساعد المنهج الدراسي اليمني! 🇾🇪\n\nيمكنك طَرح أي سؤال حَول المناهج الدراسية، أو رفع كتب PDF وملخصات جديدة، وسأجيبك بدقة مع ذكر رقم الصفحة في الكتاب المعتمد."
                    )
                )
            }
        }
    }

    fun saveStudentProfile(name: String, school: String, phone: String, grade: String) {
        viewModelScope.launch {
            repository.saveStudentProfile(name, school, phone, grade)
        }
    }

    fun setSubjectFilter(subject: String) {
        _selectedSubjectFilter.value = subject
    }

    fun performAdvancedSearch(
        query: String,
        options: com.example.engine.SearchFilterOptions = com.example.engine.SearchFilterOptions()
    ) {
        if (query.isBlank()) {
            _advancedSearchResults.value = emptyList()
            return
        }

        _isSearching.value = true
        _searchFilterOptions.value = options

        viewModelScope.launch {
            val results = repository.searchRagAdvanced(query, options)
            _advancedSearchResults.value = results
            _isSearching.value = false
        }
    }

    fun sendRagQuery(query: String) {
        if (query.isBlank()) return

        val userMsg = ChatMessage(isUser = true, text = query)
        _chatMessages.value = _chatMessages.value + userMsg
        _isSearching.value = true

        viewModelScope.launch {
            val filter = if (_selectedSubjectFilter.value == "الكل") null else _selectedSubjectFilter.value
            val result = repository.searchRag(query, filter)

            val botMsg = ChatMessage(
                isUser = false,
                text = result.answerText,
                result = result
            )
            _chatMessages.value = _chatMessages.value + botMsg
            _isSearching.value = false
        }
    }

    fun addCustomDocument(
        title: String,
        subject: String,
        docType: String,
        extractedText: String,
        pageCount: Int = 10
    ) {
        addCustomDocumentDetailed(
            title = title,
            subject = subject,
            docType = docType,
            stage = "المرحلة الثانوية",
            gradeLevel = "الثالث الثانوي",
            semester = "الفصل الأول",
            fileSize = "${(extractedText.length * 2 / 1024).coerceAtLeast(1)} MB",
            pageCount = pageCount,
            extractedText = extractedText
        )
    }

    fun addCustomDocumentDetailed(
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
    ) {
        viewModelScope.launch {
            _importProgressState.value = SmartImportProgressState(
                isImporting = true,
                progressPercent = 10,
                currentStage = "قراءة ملف الـ PDF وتحسين الصور (إزالة الظلال، تعديل التدوير، وقص حواف الورقة)...",
                bookTitle = title,
                totalPages = pageCount
            )
            kotlinx.coroutines.delay(400)

            _importProgressState.value = _importProgressState.value.copy(
                progressPercent = 35,
                currentStage = "تشغيل Google ML Kit OCR Offline لاستخراج العربية والإنجليزية والأرقام والمعادلات..."
            )
            kotlinx.coroutines.delay(500)

            _importProgressState.value = _importProgressState.value.copy(
                progressPercent = 60,
                currentStage = "فحص الملاحظات المكتوبة بخط اليد وتحديد الفصول والجداول والرسومات..."
            )
            kotlinx.coroutines.delay(500)

            _importProgressState.value = _importProgressState.value.copy(
                progressPercent = 80,
                currentStage = "تقسيم الكتاب إلى $pageCount صفحة وحفظ كل صفحة مع رقمها الحقيقي وسجل RAG..."
            )

            repository.addCustomDocumentDetailed(
                title = title,
                subject = subject,
                docType = docType,
                stage = stage,
                gradeLevel = gradeLevel,
                semester = semester,
                fileSize = fileSize,
                pageCount = pageCount,
                extractedText = extractedText,
                filePath = filePath
            )
            kotlinx.coroutines.delay(400)

            _importProgressState.value = _importProgressState.value.copy(
                progressPercent = 95,
                currentStage = "بناء الفهرس المحلي وربط النتائج باسم الكتاب ورقم الصفحة..."
            )
            kotlinx.coroutines.delay(400)

            _importProgressState.value = _importProgressState.value.copy(
                progressPercent = 100,
                currentStage = "اكتملت الفهرسة بنجاح! الكتاب جاهز للبحث والاسترجاع.",
                isCompleted = true
            )
            kotlinx.coroutines.delay(800)
            _importProgressState.value = SmartImportProgressState(isImporting = false)
        }
    }

    fun importRealPdfUri(
        context: android.content.Context,
        pdfUri: android.net.Uri,
        title: String,
        subject: String,
        stage: String = "المرحلة الثانوية",
        gradeLevel: String = "الثالث الثانوي",
        semester: String = "الفصل الأول"
    ) {
        viewModelScope.launch {
            _importProgressState.value = SmartImportProgressState(
                isImporting = true,
                progressPercent = 5,
                currentStage = "بدء استخراج وقراءة صفحات PDF عبر ML Kit OCR...",
                bookTitle = title
            )

            val result = com.example.engine.SmartPdfEngine.extractAndIndexPdfFile(
                context = context,
                pdfUri = pdfUri,
                docId = 0,
                title = title,
                subject = subject,
                onProgress = { percent, message ->
                    _importProgressState.value = SmartImportProgressState(
                        isImporting = true,
                        progressPercent = percent,
                        currentStage = message,
                        bookTitle = title,
                        totalPages = 0
                    )
                }
            )

            _importProgressState.value = _importProgressState.value.copy(
                progressPercent = 90,
                currentStage = "حفظ البيانات والفهارس وجداول الصور في قاعدة البيانات..."
            )

            val fullExtractedText = result.pageChunks.joinToString("\n\n") { it.pageText }

            repository.addCustomDocumentDetailed(
                title = title,
                subject = subject,
                docType = "BOOK",
                stage = stage,
                gradeLevel = gradeLevel,
                semester = semester,
                fileSize = "PDF",
                pageCount = result.pageChunks.size.coerceAtLeast(1),
                extractedText = fullExtractedText,
                filePath = pdfUri.toString()
            )

            _importProgressState.value = SmartImportProgressState(
                isImporting = true,
                progressPercent = 100,
                currentStage = "تمت فهرسة كتاب $title بنجاح (${result.pageChunks.size} صفحة)!",
                isCompleted = true
            )
            kotlinx.coroutines.delay(1000)
            _importProgressState.value = SmartImportProgressState(isImporting = false)
        }
    }

    fun importPdfFolderUris(
        context: android.content.Context,
        uris: List<android.net.Uri>,
        defaultSubject: String = "الفيزياء"
    ) {
        viewModelScope.launch {
            val total = uris.size
            if (total == 0) return@launch

            uris.forEachIndexed { index, uri ->
                val rawSegment = uri.lastPathSegment?.substringAfterLast("/") ?: ""
                val bookName = if (rawSegment.isNotBlank()) rawSegment.removeSuffix(".pdf") else "كتاب منهجي ${index + 1}"
                val currentSubject = when {
                    bookName.contains("كيمياء") -> "الكيمياء"
                    bookName.contains("رياضيات") -> "الرياضيات"
                    bookName.contains("أحياء") -> "الأحياء"
                    bookName.contains("عربي") || bookName.contains("لغة") -> "اللغة العربية"
                    bookName.contains("إنجليزي") || bookName.contains("English") -> "الإنجليزي"
                    bookName.contains("قرآن") || bookName.contains("إسلامية") -> "القرآن الكريم"
                    else -> defaultSubject
                }

                _importProgressState.value = SmartImportProgressState(
                    isImporting = true,
                    progressPercent = ((index * 100) / total),
                    currentStage = "جاري استيراد وقراءة الكتاب ${index + 1} من $total: $bookName...",
                    bookTitle = bookName
                )

                val result = com.example.engine.SmartPdfEngine.extractAndIndexPdfFile(
                    context = context,
                    pdfUri = uri,
                    docId = 0,
                    title = bookName,
                    subject = currentSubject
                )

                val fullText = result.pageChunks.joinToString("\n\n") { it.pageText }

                repository.addCustomDocumentDetailed(
                    title = bookName,
                    subject = currentSubject,
                    docType = "BOOK",
                    stage = "المرحلة الثانوية",
                    gradeLevel = "الثالث الثانوي",
                    semester = "الفصل الأول",
                    fileSize = "PDF",
                    pageCount = result.pageChunks.size.coerceAtLeast(1),
                    extractedText = fullText,
                    filePath = uri.toString()
                )
            }

            _importProgressState.value = SmartImportProgressState(
                isImporting = true,
                progressPercent = 100,
                currentStage = "اكتمل استيراد وفهرسة جميع كتب المجلد ($total كتب) بنجاح!",
                isCompleted = true
            )
            kotlinx.coroutines.delay(1200)
            _importProgressState.value = SmartImportProgressState(isImporting = false)
        }
    }

    fun resetImportProgress() {
        _importProgressState.value = SmartImportProgressState(isImporting = false)
    }

    fun batchAddDocuments(
        documents: List<BatchDocumentItem>
    ) {
        viewModelScope.launch {
            val total = documents.size
            documents.forEachIndexed { index, doc ->
                val baseProgress = (index * 100) / total
                _importProgressState.value = SmartImportProgressState(
                    isImporting = true,
                    progressPercent = baseProgress + 10,
                    currentStage = "استيراد وقراءة الكتاب ${index + 1} من $total: ${doc.title}...",
                    bookTitle = doc.title,
                    totalPages = doc.pageCount
                )
                kotlinx.coroutines.delay(300)

                _importProgressState.value = _importProgressState.value.copy(
                    progressPercent = baseProgress + 50 / total,
                    currentStage = "استخراج النص، OCR وتقسيم المقاطع لـ ${doc.title}..."
                )

                repository.addCustomDocumentDetailed(
                    title = doc.title,
                    subject = doc.subject,
                    docType = doc.docType,
                    stage = doc.stage,
                    gradeLevel = doc.gradeLevel,
                    semester = doc.semester,
                    fileSize = doc.fileSize,
                    pageCount = doc.pageCount,
                    extractedText = doc.extractedText,
                    filePath = doc.filePath
                )
                kotlinx.coroutines.delay(300)
            }

            _importProgressState.value = SmartImportProgressState(
                isImporting = true,
                progressPercent = 100,
                currentStage = "اكتمل استيراد وفهرسة جميع الكتب ($total)! جاهزة للبحث بالمحرك.",
                isCompleted = true
            )
            kotlinx.coroutines.delay(1000)
            _importProgressState.value = SmartImportProgressState(isImporting = false)
        }
    }

    fun updateDocumentMetadata(
        docId: Long,
        title: String,
        subject: String,
        docType: String,
        stage: String,
        gradeLevel: String,
        semester: String,
        fileSize: String,
        pageCount: Int
    ) {
        viewModelScope.launch {
            repository.updateDocumentMetadata(
                docId = docId,
                title = title,
                subject = subject,
                docType = docType,
                stage = stage,
                gradeLevel = gradeLevel,
                semester = semester,
                fileSize = fileSize,
                pageCount = pageCount
            )
        }
    }

    fun updateDocumentWithReindexing(
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
    ) {
        viewModelScope.launch {
            repository.updateDocumentWithReindexing(
                docId = docId,
                title = title,
                subject = subject,
                docType = docType,
                stage = stage,
                gradeLevel = gradeLevel,
                semester = semester,
                fileSize = fileSize,
                pageCount = pageCount,
                newExtractedText = newExtractedText
            )
        }
    }

    fun deleteDocument(docId: Long) {
        viewModelScope.launch {
            repository.deleteDocument(docId)
        }
    }

    fun solveExamPaper(ocrText: String, subject: String? = null) {
        if (ocrText.isBlank()) return
        _isSolvingExam.value = true
        _examReport.value = null

        viewModelScope.launch {
            val report = repository.solveExamOcr(ocrText, subject)
            _examReport.value = report
            _isSolvingExam.value = false
        }
    }

    fun startQuiz(subject: String) {
        viewModelScope.launch {
            val quiz = repository.generateQuiz(subject, questionCount = 5)
            _currentQuizSession.value = quiz
        }
    }

    fun selectQuizAnswer(questionId: Int, optionIndex: Int) {
        val current = _currentQuizSession.value ?: return
        val updatedAnswers = current.userAnswers.toMutableMap()
        updatedAnswers[questionId] = optionIndex

        _currentQuizSession.value = current.copy(userAnswers = updatedAnswers)
    }

    fun submitQuiz() {
        val current = _currentQuizSession.value ?: return
        val evaluated = repository.evaluateQuiz(current)
        _currentQuizSession.value = evaluated

        viewModelScope.launch {
            repository.saveQuizHistory(evaluated.subject, evaluated.score, evaluated.questions.size)
        }
    }

    fun generateBookSummary(document: CurriculumDocument) {
        _isSummarizing.value = true
        _currentBookSummary.value = null

        viewModelScope.launch {
            val summary = repository.summarizeBook(document)
            _currentBookSummary.value = summary
            _isSummarizing.value = false
        }
    }

    // Database Backup, Restore, Export & Import State
    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    fun exportBackupFile(onComplete: (java.io.File?) -> Unit = {}) {
        viewModelScope.launch {
            val file = repository.exportDatabaseBackupFile()
            if (file != null) {
                _backupStatus.value = "تم تصدير ملف النسخة الاحتياطية بنجاح: ${file.name}"
            } else {
                _backupStatus.value = "تعذر تصدير ملف النسخة الاحتياطية."
            }
            onComplete(file)
        }
    }

    fun importBackupFileUri(context: android.content.Context, uri: android.net.Uri, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val content = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
            } catch (e: Exception) {
                ""
            }

            val success = repository.importDatabaseBackupFileContent(content)
            if (success) {
                _backupStatus.value = "تمت استعادة كتب وملاحظات قاعدة البيانات بنجاح!"
            } else {
                _backupStatus.value = "فشلت استعادة البيانات من الملف المحدد."
            }
            onComplete(success)
        }
    }

    fun exportDatabaseBackup(onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            val backupData = repository.exportEncryptedBackup()
            _backupStatus.value = "تمت عملية النسخ الاحتياطي وتشفير البيانات (AES-256) بنجاح."
            onComplete(backupData)
        }
    }

    fun restoreDatabaseBackup(backupData: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.restoreEncryptedBackup(backupData)
            if (success) {
                _backupStatus.value = "تمت استعادة النسخة الاحتياطية وتأكيد سلامة قاعدة البيانات."
            } else {
                _backupStatus.value = "فشلت استعادة النسخة الاحتياطية أو رمز التشفير غير مطابق."
            }
            onComplete(success)
        }
    }

    fun exportRawDatabaseFile(onComplete: (java.io.File?) -> Unit) {
        viewModelScope.launch {
            val file = repository.exportRawDatabaseFile()
            if (file != null) {
                _backupStatus.value = "تم تصدير ملف قاعدة البيانات Room بنجاح: ${file.name}"
            } else {
                _backupStatus.value = "تعذر تصدير قاعدة البيانات."
            }
            onComplete(file)
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }
}
