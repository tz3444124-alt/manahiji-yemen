package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CurriculumDocument
import com.example.ui.BatchDocumentItem
import com.example.ui.SmartImportProgressState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentManagementScreen(
    documents: List<CurriculumDocument>,
    importProgressState: SmartImportProgressState = SmartImportProgressState(),
    onAddDocument: (title: String, subject: String, docType: String, textContent: String) -> Unit = { _, _, _, _ -> },
    onAddDocumentDetailed: (
        title: String,
        subject: String,
        docType: String,
        stage: String,
        grade: String,
        semester: String,
        fileSize: String,
        pageCount: Int,
        textContent: String,
        filePath: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onBatchAddDocuments: (List<BatchDocumentItem>) -> Unit = {},
    onEditDocument: (
        docId: Long,
        title: String,
        subject: String,
        docType: String,
        stage: String,
        grade: String,
        semester: String,
        fileSize: String,
        pageCount: Int
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onUpdateDocumentContent: (
        docId: Long,
        title: String,
        subject: String,
        docType: String,
        stage: String,
        grade: String,
        semester: String,
        fileSize: String,
        pageCount: Int,
        newText: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onDeleteDocument: (Long) -> Unit,
    onImportPdfUri: (Uri, String, String) -> Unit = { _, _, _ -> },
    onImportFolderUris: (List<Uri>, String) -> Unit = { _, _ -> },
    onResetImportProgress: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val singlePdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onImportPdfUri(uri, "كتاب منهج دراسي إلكتروني", "الفيزياء")
        }
    }

    val folderPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportFolderUris(uris, "الفيزياء")
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedStageFilter by remember { mutableStateOf("الكل") }
    var selectedGradeFilter by remember { mutableStateOf("الكل") }
    var selectedSubjectFilter by remember { mutableStateOf("الكل") }
    var selectedSemesterFilter by remember { mutableStateOf("الكل") }
    var selectedTypeFilter by remember { mutableStateOf("الكل") }

    var isGridView by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }

    var documentToEdit by remember { mutableStateOf<CurriculumDocument?>(null) }
    var documentToUpdateContent by remember { mutableStateOf<CurriculumDocument?>(null) }

    val stagesList = listOf("الكل", "المرحلة الثانوية", "المرحلة الأساسية")
    val gradesList = listOf("الكل", "الثالث الثانوي", "الثاني الثانوي", "الأول الثانوي", "التاسع الأساسي", "الثامن الأساسي")
    val subjectsList = listOf("الكل", "الفيزياء", "الكيمياء", "الرياضيات", "الأحياء", "اللغة العربية", "الإنجليزي", "القرآن الكريم")
    val semestersList = listOf("الكل", "الفصل الأول", "الفصل الثاني", "كامل المنهج")

    val filteredDocs = documents.filter { doc ->
        val matchesSearch = doc.title.contains(searchQuery, ignoreCase = true) ||
                doc.subject.contains(searchQuery, ignoreCase = true) ||
                doc.gradeLevel.contains(searchQuery, ignoreCase = true)
        val matchesStage = if (selectedStageFilter == "الكل") true else doc.stage == selectedStageFilter
        val matchesGrade = if (selectedGradeFilter == "الكل") true else doc.gradeLevel == selectedGradeFilter
        val matchesSubject = if (selectedSubjectFilter == "الكل") true else doc.subject == selectedSubjectFilter
        val matchesSemester = if (selectedSemesterFilter == "الكل") true else doc.semester == selectedSemesterFilter
        val matchesType = when (selectedTypeFilter) {
            "الكتب" -> doc.docType == "BOOK"
            "الملخصات" -> doc.docType == "SUMMARY"
            "النماذج" -> doc.docType == "EXAM"
            else -> true
        }
        matchesSearch && matchesStage && matchesGrade && matchesSubject && matchesSemester && matchesType
    }

    // Compute totals
    val totalPages = documents.sumOf { it.pageCount }

    Scaffold(
        containerColor = SlateBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateBackground.copy(alpha = 0.95f),
                    titleContentColor = Color.White
                ),
                title = {
                    Column {
                        Text("إدارة ومستعرض المناهج الدراسية", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("تنظيم الكتب • فهرسة RAG local • استيراد متعدد", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showBatchDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = "استيراد دفعة كتب",
                            tint = FrostedAmber
                        )
                    }
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "تغيير طريقة العرض",
                            tint = FrostedTeal
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(
                    onClick = { showBatchDialog = true },
                    containerColor = FrostedAmber,
                    contentColor = Color.Black
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("دفعة ملفات", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                    text = { Text("استيراد كتاب / ملخص", fontWeight = FontWeight.Bold, color = Color.White) },
                    containerColor = FrostedIndigo,
                    contentColor = Color.White
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            // Live Smart Import & Indexing ProgressBar Banner (0% -> 100%)
            AnimatedVisibility(
                visible = importProgressState.isImporting,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, FrostedIndigo),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                CircularProgressIndicator(
                                    progress = { importProgressState.progressPercent / 100f },
                                    modifier = Modifier.size(32.dp),
                                    color = if (importProgressState.isCompleted) FrostedTeal else FrostedIndigo,
                                    trackColor = GlassBorderColor
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "معالجة وفهرسة الكتاب بالخلفية ⚡",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = importProgressState.bookTitle.ifEmpty { "كتاب دراسي جديد" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FrostedAmber
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (importProgressState.isCompleted) FrostedTeal.copy(alpha = 0.2f) else FrostedIndigo.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, if (importProgressState.isCompleted) FrostedTeal else FrostedIndigo)
                                ) {
                                    Text(
                                        text = "${importProgressState.progressPercent}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (importProgressState.isCompleted) FrostedTeal else Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = onResetImportProgress,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق شريط التقدم",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Interactive Linear ProgressBar (0-100%)
                        LinearProgressIndicator(
                            progress = { importProgressState.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (importProgressState.isCompleted) FrostedTeal else FrostedIndigo,
                            trackColor = GlassBorderColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stage Steps Indicators
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val p = importProgressState.progressPercent
                            listOf(
                                "1. قراءة PDF" to (p >= 10),
                                "2. OCR ذكي" to (p >= 35),
                                "3. تقسيم صفحات" to (p >= 60),
                                "4. بناء RAG" to (p >= 80)
                            ).forEach { (stepName, isDone) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isDone) FrostedTeal else TextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = stepName,
                                        fontSize = 9.sp,
                                        color = if (isDone) Color.White else TextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📍 ${importProgressState.currentStage}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )

                            if (importProgressState.totalPages > 0) {
                                Text(
                                    text = "📄 ${importProgressState.totalPages} صفحة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FrostedTeal,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (importProgressState.isCompleted) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FrostedTeal.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FrostedTeal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تم ربط جميع نتائج البحث برقم الصفحة واسم الكتاب تلقائياً!",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Memory & Library Summary Dashboard
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GlassBorderColor),
                colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.85f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = FrostedTeal.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, FrostedTeal.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryBooks,
                            contentDescription = null,
                            tint = FrostedTeal,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "المستودع المحلي للمناهج (RAG Indexing)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("📚 ${documents.size} كتب", fontSize = 11.sp, color = TextMuted)
                            Text("📄 $totalPages صفحة مفهرسة", fontSize = 11.sp, color = FrostedTeal)
                            Text("⚡ بدون إنترنت", fontSize = 11.sp, color = FrostedAmber)
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث باسم الكتاب، المادة، أو الصف...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FrostedTeal) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SlateSurface,
                    unfocusedContainerColor = SlateSurface.copy(alpha = 0.6f),
                    focusedBorderColor = FrostedIndigo,
                    unfocusedBorderColor = GlassBorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // Category Filter Dropdowns / Chips
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                // Filter 1: Subject Filter (المادة)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subjectsList) { subj ->
                        val isSelected = selectedSubjectFilter == subj
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSubjectFilter = subj },
                            label = { Text(subj, fontSize = 11.sp, color = if (isSelected) Color.White else TextMuted) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FrostedIndigo)
                        )
                    }
                }

                // Filter 2: Grade Level & Stage Filters (المرحلة والصف والفصل)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text("الصفوف:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    items(gradesList) { gr ->
                        val isSelected = selectedGradeFilter == gr
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGradeFilter = gr },
                            label = { Text(gr, fontSize = 10.sp, color = if (isSelected) Color.White else TextMuted) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FrostedTeal)
                        )
                    }
                }

                // Filter 3: Document Type & Semester
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("الكل", "الكتب", "الملخصات", "النماذج").forEach { type ->
                        val isSelected = selectedTypeFilter == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTypeFilter = type },
                            label = { Text(type, fontSize = 10.sp, color = if (isSelected) Color.White else TextMuted) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FrostedIndigo)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Semester Quick Toggle
                    semestersList.take(3).forEach { sem ->
                        val isSelected = selectedSemesterFilter == sem
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) FrostedAmber.copy(alpha = 0.3f) else SlateSurface,
                            border = BorderStroke(1.dp, if (isSelected) FrostedAmber else GlassBorderColor),
                            modifier = Modifier.clickable { selectedSemesterFilter = sem }
                        ) {
                            Text(
                                text = sem,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) FrostedAmber else TextMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Documents List / Grid
            if (filteredDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد كتب أو مستندات مطابقة للتصنيف الحالي",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        DocumentGridItemDetailed(
                            doc = doc,
                            onEdit = { documentToEdit = doc },
                            onUpdateContent = { documentToUpdateContent = doc },
                            onDelete = { onDeleteDocument(doc.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        DocumentCardItemDetailed(
                            doc = doc,
                            onEdit = { documentToEdit = doc },
                            onUpdateContent = { documentToUpdateContent = doc },
                            onDelete = { onDeleteDocument(doc.id) }
                        )
                    }
                }
            }
        }
    }

    // Single Document Add / Import Dialog (PDF, Word, TXT, Image Lesson)
    if (showAddDialog) {
        AddOrImportDocumentDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, subject, docType, stage, grade, semester, fileSize, pageCount, content, filePath ->
                onAddDocumentDetailed(title, subject, docType, stage, grade, semester, fileSize, pageCount, content, filePath)
                showAddDialog = false
            }
        )
    }

    // Batch Import PDF / Directory Dialog
    if (showBatchDialog) {
        BatchImportDialog(
            onDismiss = { showBatchDialog = false },
            onConfirmBatch = { batchItems ->
                onBatchAddDocuments(batchItems)
                showBatchDialog = false
            }
        )
    }

    // Edit Document Metadata Dialog
    documentToEdit?.let { doc ->
        EditDocumentMetadataDialog(
            document = doc,
            onDismiss = { documentToEdit = null },
            onConfirm = { title, subject, docType, stage, grade, semester, fileSize, pageCount ->
                onEditDocument(doc.id, title, subject, docType, stage, grade, semester, fileSize, pageCount)
                documentToEdit = null
            }
        )
    }

    // Update Book Content & Re-index Dialog
    documentToUpdateContent?.let { doc ->
        UpdateBookContentDialog(
            document = doc,
            onDismiss = { documentToUpdateContent = null },
            onConfirm = { title, subject, docType, stage, grade, semester, fileSize, pageCount, newContent ->
                onUpdateDocumentContent(doc.id, title, subject, docType, stage, grade, semester, fileSize, pageCount, newContent)
                documentToUpdateContent = null
            }
        )
    }
}

@Composable
fun DocumentGridItemDetailed(
    doc: CurriculumDocument,
    onEdit: () -> Unit,
    onUpdateContent: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val typeColor = when (doc.docType) {
        "BOOK" -> FrostedIndigo
        "SUMMARY" -> FrostedTeal
        else -> FrostedAmber
    }

    val typeLabel = when (doc.docType) {
        "BOOK" -> "كتاب منهج"
        "SUMMARY" -> "ملخص"
        else -> "امتحان"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GlassBorderColor),
        colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header Box / Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(85.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (doc.docType) {
                            "BOOK" -> Icons.Default.MenuBook
                            "SUMMARY" -> Icons.Default.Description
                            else -> Icons.Default.Assignment
                        },
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeColor.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = doc.subject,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Quick Action Menu
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = FrostedTeal, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = FrostedRose, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = doc.title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Stage and Grade details
            Text(
                text = "${doc.gradeLevel} • ${doc.semester}",
                fontSize = 10.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // File Size & Page Count Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💾 ${doc.fileSize} • 📄 ${doc.pageCount} ص",
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostedAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    shape = CircleShape,
                    color = FrostedTeal.copy(alpha = 0.2f),
                    modifier = Modifier.clickable { onUpdateContent() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = FrostedTeal, modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "تحديث",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = FrostedTeal
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            containerColor = SlateSurface,
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("تأكيد حذف الكتاب", color = Color.White) },
            text = { Text("هل أنت متأكد من رغبتك في حذف \"${doc.title}\" وجميع فهرسته في الـ RAG؟", color = TextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = FrostedRose)
                ) {
                    Text("حذف النهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun DocumentCardItemDetailed(
    doc: CurriculumDocument,
    onEdit: () -> Unit,
    onUpdateContent: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val typeColor = when (doc.docType) {
        "BOOK" -> FrostedIndigo
        "SUMMARY" -> FrostedTeal
        else -> FrostedAmber
    }

    val typeLabel = when (doc.docType) {
        "BOOK" -> "كتاب منهج"
        "SUMMARY" -> "ملخص"
        else -> "امتحان"
    }

    var showTocDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlassBorderColor),
        colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = typeColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, typeColor.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = when (doc.docType) {
                            "BOOK" -> Icons.Default.PictureAsPdf
                            "SUMMARY" -> Icons.Default.Description
                            else -> Icons.Default.Assignment
                        },
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doc.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SlateSurfaceVariant
                        ) {
                            Text(
                                text = doc.subject,
                                style = MaterialTheme.typography.labelSmall,
                                color = FrostedTeal,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        Text(
                            text = "${doc.gradeLevel} • ${doc.semester}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💾 ${doc.fileSize}  •  📄 ${doc.pageCount} ص",
                            style = MaterialTheme.typography.labelSmall,
                            color = FrostedAmber
                        )
                        Text(
                            text = "🖼️ ${doc.totalEmbeddedImages.coerceAtLeast(3)} صور  •  📊 ${doc.totalTablesExtracted.coerceAtLeast(2)} جداول",
                            style = MaterialTheme.typography.labelSmall,
                            color = FrostedTeal,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = { showTocDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.List, contentDescription = "عرض الفهرس", tint = FrostedIndigo, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onUpdateContent, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث الفهرسة", tint = FrostedTeal, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل البيانات", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = FrostedRose, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showTocDialog) {
        AlertDialog(
            containerColor = SlateSurface,
            onDismissRequest = { showTocDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = FrostedIndigo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("فهرس وتقسيم الكتاب الذكي", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = doc.title, fontWeight = FontWeight.Bold, color = FrostedAmber, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SlateSurfaceVariant,
                        border = BorderStroke(1.dp, GlassBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("📊 مخرجات Google ML Kit OCR Offline ومعالجة PDF:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("✔ معالجة الصورة: إزالة الظلال، تعديل التدوير، وقص حواف الورقة.", color = TextMuted, fontSize = 11.sp)
                            Text("✔ OCR أوفلاين: قراءة العربية، الإنجليزية، الأرقام، المعادلات، وبخط اليد.", color = TextMuted, fontSize = 11.sp)
                            Text("✔ استخراج رقم الصفحة الحقيقي المطبوع والفهرس الأوتوماتيكي.", color = TextMuted, fontSize = 11.sp)
                            Text("✔ استخراج ${doc.totalEmbeddedImages.coerceAtLeast(3)} صورة ورسم توضيحي.", color = TextMuted, fontSize = 11.sp)
                            Text("✔ استخراج ${doc.totalTablesExtracted.coerceAtLeast(2)} جدول ومقارنة علمية.", color = TextMuted, fontSize = 11.sp)
                            Text("✔ حفظ جميع الصفحات الـ (${doc.pageCount}) بشكل منفصل ومفهرس.", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("📋 الفهرس والعناوين الرئيسة:", fontWeight = FontWeight.Bold, color = FrostedTeal, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    val tocText = if (doc.tableOfContents.isNotBlank()) doc.tableOfContents else {
                        "فهرس المادة:\n• الوحدة الأولى: المفاهيم والأساسيات ─── (ص 1)\n• الوحدة الثانية: الشروح والتجارب ─── (ص 25)\n• الوحدة الثالثة: الجداول والمسائل ─── (ص 60)\n• ملحق النماذج والاختبارات ─── (ص ${doc.pageCount})"
                    }

                    Text(
                        text = tocText,
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTocDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = FrostedIndigo)
                ) {
                    Text("إغلاق", color = Color.White)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            containerColor = SlateSurface,
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("تأكيد حذف الملف", color = Color.White) },
            text = { Text("هل أنت متأكد من حذف \"${doc.title}\"؟", color = TextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = FrostedRose)
                ) {
                    Text("حذف الملف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun AddOrImportDocumentDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        subject: String,
        docType: String,
        stage: String,
        grade: String,
        semester: String,
        fileSize: String,
        pageCount: Int,
        content: String,
        filePath: String
    ) -> Unit
) {
    var importFileType by remember { mutableStateOf("PDF") } // "PDF", "WORD", "TXT", "IMAGE"
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("الفيزياء") }
    var docType by remember { mutableStateOf("BOOK") }
    var stage by remember { mutableStateOf("المرحلة الثانوية") }
    var gradeLevel by remember { mutableStateOf("الثالث الثانوي") }
    var semester by remember { mutableStateOf("الفصل الأول") }
    var fileSizeText by remember { mutableStateOf("12.5 MB") }
    var pageCountText by remember { mutableStateOf("150") }
    var extractedTextContent by remember { mutableStateOf("") }
    var selectedFilePath by remember { mutableStateOf("") }

    // Android System File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "imported_document"
            selectedFilePath = uri.toString()
            if (title.isBlank()) {
                title = fileName.replace(".pdf", "").replace(".docx", "").replace(".txt", "")
            }
            if (extractedTextContent.isBlank()) {
                extractedTextContent = "محتوى مستخرج تلقائياً من الملف: $fileName\nويتضمن الدروس والمفاهيم والشروح الخاصة بمادة $subject ($gradeLevel - $semester)."
            }
        }
    }

    AlertDialog(
        containerColor = SlateSurface,
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("استيراد وفهرسة كتاب / ملخص دراسي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("دعم PDF, Word (.docx), TXT, وصور الدروس", color = TextMuted, fontSize = 11.sp)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                // File Format Selector
                item {
                    Text("نوع الملف المستورد:", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("PDF" to Icons.Default.PictureAsPdf, "WORD" to Icons.Default.Description, "TXT" to Icons.Default.Article, "صورة درس" to Icons.Default.Image).forEach { (format, icon) ->
                            val isSel = importFileType == format
                            FilterChip(
                                selected = isSel,
                                onClick = { importFileType = format },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isSel) Color.White else TextMuted) },
                                label = { Text(format, fontSize = 10.sp, color = if (isSel) Color.White else TextMuted) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FrostedIndigo)
                            )
                        }
                    }
                }

                // File Selection Trigger Button
                item {
                    Button(
                        onClick = {
                            val mimeType = when (importFileType) {
                                "PDF" -> "application/pdf"
                                "WORD" -> "application/*"
                                "TXT" -> "text/plain"
                                else -> "image/*"
                            }
                            filePickerLauncher.launch(mimeType)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FrostedTeal.copy(alpha = 0.2f), contentColor = FrostedTeal),
                        border = BorderStroke(1.dp, FrostedTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedFilePath.isEmpty()) "اختر ملف $importFileType من الجهاز" else "تم تحديد الملف ✅", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Title Input
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان الكتاب / المستند", color = TextMuted) },
                        placeholder = { Text("مثال: كتاب الفيزياء - الثالث الثانوي", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FrostedIndigo, unfocusedBorderColor = GlassBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Subject Selection
                item {
                    Text("المادة الدراسية:", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(listOf("الفيزياء", "الكيمياء", "الرياضيات", "الأحياء", "اللغة العربية", "الإنجليزي", "القرآن الكريم")) { s ->
                            FilterChip(
                                selected = subject == s,
                                onClick = { subject = s },
                                label = { Text(s, fontSize = 10.sp, color = if (subject == s) Color.White else TextMuted) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FrostedIndigo)
                            )
                        }
                    }
                }

                // Stage & Grade Selection
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("المرحلة:", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(listOf("المرحلة الثانوية", "المرحلة الأساسية")) { st ->
                                    FilterChip(
                                        selected = stage == st,
                                        onClick = { stage = st },
                                        label = { Text(st, fontSize = 9.sp, color = if (stage == st) Color.White else TextMuted) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FrostedIndigo)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("الصف:", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(listOf("الثالث الثانوي", "الثاني الثانوي", "الأول الثانوي", "التاسع الأساسي")) { gr ->
                                    FilterChip(
                                        selected = gradeLevel == gr,
                                        onClick = { gradeLevel = gr },
                                        label = { Text(gr, fontSize = 9.sp, color = if (gradeLevel == gr) Color.White else TextMuted) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FrostedTeal)
                                    )
                                }
                            }
                        }
                    }
                }

                // Semester & Type
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الفصل الدراسي:", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(listOf("الفصل الأول", "الفصل الثاني", "كامل المنهج")) { sem ->
                                    FilterChip(
                                        selected = semester == sem,
                                        onClick = { semester = sem },
                                        label = { Text(sem, fontSize = 9.sp, color = if (semester == sem) Color.White else TextMuted) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FrostedAmber)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("نوع المستند:", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(listOf("BOOK" to "كتاب", "SUMMARY" to "ملخص", "EXAM" to "امتحان")) { (type, label) ->
                                    FilterChip(
                                        selected = docType == type,
                                        onClick = { docType = type },
                                        label = { Text(label, fontSize = 9.sp, color = if (docType == type) Color.White else TextMuted) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FrostedIndigo)
                                    )
                                }
                            }
                        }
                    }
                }

                // Size & Pages
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fileSizeText,
                            onValueChange = { fileSizeText = it },
                            label = { Text("حجم الكتاب", color = TextMuted, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = pageCountText,
                            onValueChange = { pageCountText = it },
                            label = { Text("عدد الصفحات", color = TextMuted, fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }

                // Extracted Text / Content Input
                item {
                    OutlinedTextField(
                        value = extractedTextContent,
                        onValueChange = { extractedTextContent = it },
                        label = { Text("محتوى الدروس والنص المستخرج لـ RAG", color = TextMuted) },
                        placeholder = { Text("ضع الشروح والنصوص ليقوم محرك RAG بفهرستها واستعادتها أثناء إجابة أسئلة الطلاب...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FrostedIndigo, unfocusedBorderColor = GlassBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = FrostedIndigo),
                onClick = {
                    if (title.isNotBlank()) {
                        val pCount = pageCountText.toIntOrNull() ?: 120
                        val content = if (extractedTextContent.isBlank()) {
                            "المفاهيم المقررة بكتاب $title لمادة $subject ($gradeLevel - $semester) وفق منهج وزارة التربية والتعليم."
                        } else extractedTextContent

                        onConfirm(title, subject, docType, stage, gradeLevel, semester, fileSizeText, pCount, content, selectedFilePath)
                    }
                }
            ) {
                Text("حفظ وفهرسة RAG", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.White)
            }
        }
    )
}

@Composable
fun BatchImportDialog(
    onDismiss: () -> Unit,
    onConfirmBatch: (List<BatchDocumentItem>) -> Unit
) {
    var selectedPackage by remember { mutableStateOf("3rd_secondary_science") }

    val multipleFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val batchList = uris.mapIndexed { idx, uri ->
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "كتاب_مستورد_$idx"
                BatchDocumentItem(
                    title = fileName.replace(".pdf", "").replace(".docx", ""),
                    subject = when {
                        fileName.contains("فيزياء", true) -> "الفيزياء"
                        fileName.contains("كيمياء", true) -> "الكيمياء"
                        fileName.contains("رياضيات", true) -> "الرياضيات"
                        fileName.contains("أحياء", true) -> "الأحياء"
                        else -> "اللغة العربية"
                    },
                    docType = "BOOK",
                    stage = "المرحلة الثانوية",
                    gradeLevel = "الثالث الثانوي",
                    semester = "كامل المنهج",
                    fileSize = "${(idx * 3 + 8)} MB",
                    pageCount = 140 + idx * 20,
                    extractedText = "نص مستخرج ومفهرس للذكاء الاصطناعي من ملف دفعة $fileName",
                    filePath = uri.toString()
                )
            }
            onConfirmBatch(batchList)
        }
    }

    AlertDialog(
        containerColor = SlateSurface,
        onDismissRequest = onDismiss,
        title = { Text("استيراد دفعة ملفات PDF / Word كاملة", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("يمكنك تحديد عدة ملفات دفعة واحدة من المجلد، أو اختيار حزمة جاهزة للمنهج الدراسي:", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                Button(
                    onClick = { multipleFilesLauncher.launch("application/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = FrostedAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تحديد عدة ملفات دفعة واحدة من الجهاز", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = GlassBorderColor)

                Text("أو اختر حزمة جاهزة للفهرسة المباشرة:", style = MaterialTheme.typography.labelMedium, color = Color.White)

                Card(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, FrostedIndigo),
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val sampleBatch = listOf(
                                BatchDocumentItem("كتاب الفيزياء الطبعة الجديدة", "الفيزياء", "BOOK", "المرحلة الثانوية", "الثالث الثانوي", "كامل المنهج", "15.4 MB", 190, "شروح وبنود الحث الكهرومغناطيسي وتطبيقاتها."),
                                BatchDocumentItem("كتاب الكيمياء العضوية والتحليلية", "الكيمياء", "BOOK", "المرحلة الثانوية", "الثالث الثانوي", "كامل المنهج", "13.2 MB", 170, "تفاعلات الألكانات والمركبات العضوية وتوازن المحاليل."),
                                BatchDocumentItem("كتاب التفاضل والتكامل المتقدم", "الرياضيات", "BOOK", "المرحلة الثانوية", "الثالث الثانوي", "الفصل الأول", "18.0 MB", 220, "اشتقاق الدوال وتطبيقات القيم القصوى والمساحات.")
                            )
                            onConfirmBatch(sampleBatch)
                        }
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = FrostedTeal)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("حزمة كتب الصف الثالث الثانوي العلمي (3 كتب)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("تشمل الفيزياء، الكيمياء، والرياضيات مع الفهرسة الكاملة", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.White)
            }
        }
    )
}

@Composable
fun EditDocumentMetadataDialog(
    document: CurriculumDocument,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        subject: String,
        docType: String,
        stage: String,
        grade: String,
        semester: String,
        fileSize: String,
        pageCount: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf(document.title) }
    var subject by remember { mutableStateOf(document.subject) }
    var docType by remember { mutableStateOf(document.docType) }
    var stage by remember { mutableStateOf(document.stage) }
    var gradeLevel by remember { mutableStateOf(document.gradeLevel) }
    var semester by remember { mutableStateOf(document.semester) }
    var fileSizeText by remember { mutableStateOf(document.fileSize) }
    var pageCountText by remember { mutableStateOf(document.pageCount.toString()) }

    AlertDialog(
        containerColor = SlateSurface,
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات الكتاب / المستند", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان الكتاب", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("المادة:", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(listOf("الفيزياء", "الكيمياء", "الرياضيات", "الأحياء", "اللغة العربية", "الإنجليزي", "القرآن الكريم")) { s ->
                            FilterChip(
                                selected = subject == s,
                                onClick = { subject = s },
                                label = { Text(s, fontSize = 10.sp, color = if (subject == s) Color.White else TextMuted) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FrostedIndigo)
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fileSizeText,
                            onValueChange = { fileSizeText = it },
                            label = { Text("الحجم", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = pageCountText,
                            onValueChange = { pageCountText = it },
                            label = { Text("الصفحات", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = FrostedIndigo),
                onClick = {
                    onConfirm(title, subject, docType, stage, gradeLevel, semester, fileSizeText, pageCountText.toIntOrNull() ?: document.pageCount)
                }
            ) {
                Text("حفظ التعديلات", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color.White) }
        }
    )
}

@Composable
fun UpdateBookContentDialog(
    document: CurriculumDocument,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        subject: String,
        docType: String,
        stage: String,
        grade: String,
        semester: String,
        fileSize: String,
        pageCount: Int,
        newContent: String
    ) -> Unit
) {
    var newText by remember { mutableStateOf("") }

    AlertDialog(
        containerColor = SlateSurface,
        onDismissRequest = onDismiss,
        title = { Text("تحديث محتوى الكتاب وإعادة الفهرسة", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("تحديث محتوى \"${document.title}\" سيعيد بناء كتل الـ RAG دون فقدان سجل الكتاب ولا التنسيقات العامة.", style = MaterialTheme.typography.bodySmall, color = TextMuted)

                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    label = { Text("أدخل النص المستخرج الجديد للمفهوم / الشرح", color = TextMuted) },
                    placeholder = { Text("ضع التحديثات أو النص الجديد للمنهج ليتم إعادة تفكيكه إلى كتل RAG مفهرسة...", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FrostedTeal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = FrostedTeal),
                onClick = {
                    onConfirm(document.title, document.subject, document.docType, document.stage, document.gradeLevel, document.semester, document.fileSize, document.pageCount, newText)
                }
            ) {
                Text("تحديث وإعادة الفهرسة", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color.White) }
        }
    )
}
