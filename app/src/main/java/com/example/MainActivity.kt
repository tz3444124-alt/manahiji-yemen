package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.YemenCurriculumTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            YemenCurriculumTheme(darkTheme = isDarkMode) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val studentProfile by viewModel.studentProfile.collectAsStateWithLifecycle()
                        val allDocuments by viewModel.allDocuments.collectAsStateWithLifecycle()
                        val historyRecords by viewModel.historyRecords.collectAsStateWithLifecycle()

                        var currentRoute by remember { mutableStateOf("home") }

                        if (studentProfile == null) {
                            OnboardingScreen(
                                onSaveProfile = { name, school, phone, grade ->
                                    viewModel.saveStudentProfile(name, school, phone, grade)
                                }
                            )
                        } else {
                            val importProgressState by viewModel.importProgressState.collectAsStateWithLifecycle()

                            when (currentRoute) {
                                "home" -> HomeScreen(
                                    profile = studentProfile,
                                    documents = allDocuments,
                                    importProgressState = importProgressState,
                                    isDarkMode = isDarkMode,
                                    onToggleTheme = { viewModel.toggleThemeMode() },
                                    onNavigate = { route -> currentRoute = route }
                                )

                                "rag_chat" -> {
                                    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
                                    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
                                    val selectedSubject by viewModel.selectedSubjectFilter.collectAsStateWithLifecycle()

                                    RagChatScreen(
                                        messages = chatMessages,
                                        isSearching = isSearching,
                                        selectedSubject = selectedSubject,
                                        onSubjectChange = { viewModel.setSubjectFilter(it) },
                                        onSendMessage = { viewModel.sendRagQuery(it) },
                                        onBack = { currentRoute = "home" }
                                    )
                                }

                                "exam_solver" -> {
                                    val examReport by viewModel.examReport.collectAsStateWithLifecycle()
                                    val isSolving by viewModel.isSolvingExam.collectAsStateWithLifecycle()

                                    ExamSolverScreen(
                                        examReport = examReport,
                                        isSolving = isSolving,
                                        onSolveExam = { ocrText, subject -> viewModel.solveExamPaper(ocrText, subject) },
                                        onBack = { currentRoute = "home" }
                                    )
                                }

                                "doc_management" -> {
                                    val importProgressState by viewModel.importProgressState.collectAsStateWithLifecycle()

                                    DocumentManagementScreen(
                                        documents = allDocuments,
                                        importProgressState = importProgressState,
                                        onAddDocument = { title, subject, docType, content ->
                                            viewModel.addCustomDocument(title, subject, docType, content)
                                        },
                                        onAddDocumentDetailed = { title, subject, docType, stage, grade, semester, fileSize, pageCount, content, filePath ->
                                            viewModel.addCustomDocumentDetailed(title, subject, docType, stage, grade, semester, fileSize, pageCount, content, filePath)
                                        },
                                        onBatchAddDocuments = { batchItems ->
                                            viewModel.batchAddDocuments(batchItems)
                                        },
                                        onEditDocument = { docId, title, subject, docType, stage, grade, semester, fileSize, pageCount ->
                                            viewModel.updateDocumentMetadata(docId, title, subject, docType, stage, grade, semester, fileSize, pageCount)
                                        },
                                        onUpdateDocumentContent = { docId, title, subject, docType, stage, grade, semester, fileSize, pageCount, newText ->
                                            viewModel.updateDocumentWithReindexing(docId, title, subject, docType, stage, grade, semester, fileSize, pageCount, newText)
                                        },
                                        onDeleteDocument = { docId -> viewModel.deleteDocument(docId) },
                                        onImportPdfUri = { uri, title, subject ->
                                            viewModel.importRealPdfUri(applicationContext, uri, title, subject)
                                        },
                                        onImportFolderUris = { uris, subject ->
                                            viewModel.importPdfFolderUris(applicationContext, uris, subject)
                                        },
                                        onResetImportProgress = { viewModel.resetImportProgress() },
                                        onBack = { currentRoute = "home" }
                                    )
                                }

                                "quiz" -> {
                                    val quizSession by viewModel.currentQuizSession.collectAsStateWithLifecycle()

                                    QuizScreen(
                                        quizSession = quizSession,
                                        onStartQuiz = { subject -> viewModel.startQuiz(subject) },
                                        onOptionSelected = { qId, idx -> viewModel.selectQuizAnswer(qId, idx) },
                                        onSubmitQuiz = { viewModel.submitQuiz() },
                                        onBack = { currentRoute = "home" }
                                    )
                                }

                                "summary" -> {
                                    val summaryResult by viewModel.currentBookSummary.collectAsStateWithLifecycle()
                                    val isSummarizing by viewModel.isSummarizing.collectAsStateWithLifecycle()

                                    BookSummaryScreen(
                                        documents = allDocuments,
                                        summaryResult = summaryResult,
                                        isSummarizing = isSummarizing,
                                        onGenerateSummary = { doc -> viewModel.generateBookSummary(doc) },
                                        onBack = { currentRoute = "home" }
                                    )
                                }

                                "profile" -> {
                                    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

                                    ProfileScreen(
                                        profile = studentProfile,
                                        historyRecords = historyRecords,
                                        isDarkMode = isDarkMode,
                                        backupStatus = backupStatus,
                                        onToggleTheme = { viewModel.toggleThemeMode() },
                                        onUpdateProfile = { name, school, phone, grade ->
                                            viewModel.saveStudentProfile(name, school, phone, grade)
                                        },
                                        onExportBackup = { viewModel.exportDatabaseBackup() },
                                        onRestoreBackup = { backupData -> viewModel.restoreDatabaseBackup(backupData) },
                                        onExportDbFile = { viewModel.exportRawDatabaseFile {} },
                                        onExportBackupFile = { viewModel.exportBackupFile {} },
                                        onImportBackupFileUri = { uri -> viewModel.importBackupFileUri(applicationContext, uri) },
                                        onDismissBackupStatus = { viewModel.clearBackupStatus() },
                                        onBack = { currentRoute = "home" }
                                    )
                                }

                                else -> HomeScreen(
                                    profile = studentProfile,
                                    documents = allDocuments,
                                    isDarkMode = isDarkMode,
                                    onToggleTheme = { viewModel.toggleThemeMode() },
                                    onNavigate = { route -> currentRoute = route }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
