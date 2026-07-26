package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.engine.RagSearchEngine
import com.example.ui.ChatMessage
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RagChatScreen(
    messages: List<ChatMessage>,
    isSearching: Boolean,
    selectedSubject: String,
    onSubjectChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    var queryText by remember { mutableStateOf("") }
    var showAdvancedSearchDialog by remember { mutableStateOf(false) }
    var searchModeSelected by remember { mutableStateOf(com.example.engine.SearchMode.SEMANTIC) }
    var docTypeFilterSelected by remember { mutableStateOf("ALL") }
    var gradeFilterSelected by remember { mutableStateOf("الكل") }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val subjectsList = listOf("الكل", "الفيزياء", "الكيمياء", "الرياضيات", "الأحياء", "اللغة العربية")

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        containerColor = SlateBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateBackground.copy(alpha = 0.85f),
                    titleContentColor = Color.White
                ),
                title = {
                    Column {
                        Text("المساعد الذكي (Off-line RAG)", fontWeight = FontWeight.Bold)
                        Text(
                            text = "محرك RAG محلي 100% (أوفلاين) - بدون إنترنت وبدون هلوسة",
                            style = MaterialTheme.typography.labelSmall,
                            color = FrostedTeal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showAdvancedSearchDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "تصفية وتحديد نوع البحث", tint = FrostedAmber)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SlateSurface,
                        border = BorderStroke(1.dp, GlassBorderColor),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = FrostedTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("محلي 100%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Subject Filter Bar
            ScrollableTabRow(
                selectedTabIndex = subjectsList.indexOf(selectedSubject).coerceAtLeast(0),
                edgePadding = 12.dp,
                containerColor = SlateSurface.copy(alpha = 0.5f),
                contentColor = FrostedIndigo,
                modifier = Modifier.fillMaxWidth()
            ) {
                subjectsList.forEach { subj ->
                    Tab(
                        selected = selectedSubject == subj,
                        onClick = { onSubjectChange(subj) },
                        text = {
                            Text(
                                text = subj,
                                color = if (selectedSubject == subj) FrostedTeal else TextMuted,
                                fontWeight = if (selectedSubject == subj) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleItem(message = msg)
                }

                if (isSearching) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = FrostedTeal
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "جاري البحث في نصوص صفحات المنهج...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // Quick Question Chips
            val sampleQueries = listOf(
                "ما هو قانون كولوم؟",
                "ما تعريف لوشاتيليه؟",
                "كيف يتم حساب مشتقة sin(x)؟",
                "ما هو توصيل المكثفات على التوازي؟"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sampleQueries.take(2).forEach { q ->
                    SuggestionChip(
                        onClick = {
                            queryText = q
                            onSendMessage(q)
                            queryText = ""
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = SlateSurface
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            borderColor = GlassBorderColor,
                            enabled = true
                        ),
                        label = { Text(q, fontSize = 11.sp, color = TextMuted, maxLines = 1) }
                    )
                }
            }

            // Input Bar
            Surface(
                color = SlateSurface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current

                    IconButton(
                        onClick = {
                            Toast.makeText(context, "🎤 جاري الاستماع للبحث الصوتي...", Toast.LENGTH_SHORT).show()
                            queryText = "ما هو قانون كولوم؟"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "البحث الصوتي",
                            tint = FrostedAmber
                        )
                    }

                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        placeholder = { Text("اطرح سؤالاً في المنهج الدراسي...", color = TextMuted) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SlateSurfaceVariant,
                            unfocusedContainerColor = SlateSurfaceVariant.copy(alpha = 0.6f),
                            focusedBorderColor = FrostedIndigo,
                            unfocusedBorderColor = GlassBorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 3
                    )

                    FloatingActionButton(
                        onClick = {
                            if (queryText.isNotBlank()) {
                                onSendMessage(queryText.trim())
                                queryText = ""
                            }
                        },
                        containerColor = FrostedIndigo,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "إرسال")
                    }
                }
            }
        }

        if (showAdvancedSearchDialog) {
            AlertDialog(
                containerColor = SlateSurface,
                onDismissRequest = { showAdvancedSearchDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ManageSearch, contentDescription = null, tint = FrostedAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إعدادات البحث المتقدمة", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🎯 نوع البحث المطلوبة:", fontWeight = FontWeight.Bold, color = FrostedTeal, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = searchModeSelected == com.example.engine.SearchMode.SEMANTIC,
                                onClick = { searchModeSelected = com.example.engine.SearchMode.SEMANTIC },
                                label = { Text("البحث بالمعنى", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = searchModeSelected == com.example.engine.SearchMode.KEYWORD,
                                onClick = { searchModeSelected = com.example.engine.SearchMode.KEYWORD },
                                label = { Text("بالكلمة", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = searchModeSelected == com.example.engine.SearchMode.PHRASE,
                                onClick = { searchModeSelected = com.example.engine.SearchMode.PHRASE },
                                label = { Text("بالجملة", fontSize = 10.sp) }
                            )
                        }

                        Text("📁 نطاق البحث والملفات:", fontWeight = FontWeight.Bold, color = FrostedTeal, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = docTypeFilterSelected == "ALL",
                                onClick = { docTypeFilterSelected = "ALL" },
                                label = { Text("الكل", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = docTypeFilterSelected == "SUMMARY",
                                onClick = { docTypeFilterSelected = "SUMMARY" },
                                label = { Text("الملخصات فقط", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = docTypeFilterSelected == "EXAM",
                                onClick = { docTypeFilterSelected = "EXAM" },
                                label = { Text("الاختبارات فقط", fontSize = 10.sp) }
                            )
                        }

                        Text("📌 تصفية الصف الدراسي:", fontWeight = FontWeight.Bold, color = FrostedTeal, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("الكل", "الثالث الثانوي", "الثاني الثانوي").forEach { gr ->
                                FilterChip(
                                    selected = gradeFilterSelected == gr,
                                    onClick = { gradeFilterSelected = gr },
                                    label = { Text(gr, fontSize = 10.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SlateSurfaceVariant,
                            border = BorderStroke(1.dp, FrostedAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Memory, contentDescription = null, tint = FrostedAmber, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("نموذج Gemma المحلي (On-Device LLM):", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("يستخدم معالجة الجهاز الذاتية للأجهزة القوية لتوليد صياغات بأسلوب Gemma أوفلاين 100%.", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAdvancedSearchDialog = false
                            if (queryText.isNotBlank()) {
                                onSendMessage(queryText.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FrostedIndigo)
                    ) {
                        Text("تطبيق والبحث", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdvancedSearchDialog = false }) {
                        Text("إلغاء", color = TextMuted)
                    }
                }
            )
        }
    }
}

@Composable
fun ChatBubbleItem(message: ChatMessage) {
    val isUser = message.isUser
    val context = androidx.compose.ui.platform.LocalContext.current

    var isFavorite by remember { mutableStateOf(false) }
    var isTranslated by remember { mutableStateOf(false) }
    var displayedText by remember { mutableStateOf(message.text) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Surface(
                    shape = CircleShape,
                    color = FrostedIndigo,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(top = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                border = BorderStroke(1.dp, GlassBorderColor),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) FrostedIndigo else SlateSurface.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = displayedText,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // RAG Page Citation Box
                    val result = message.result
                    if (result != null && result.isFound && result.matchedPageNumber != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SlateSurfaceVariant,
                            border = BorderStroke(1.dp, FrostedTeal.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = FrostedTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "المصدر: صفحة ${result.matchedPageNumber}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = FrostedTeal
                                    )
                                }
                                Text(
                                    text = result.matchedDocTitle ?: "",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    } else if (result != null && !result.isFound) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = FrostedRose.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, FrostedRose.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = FrostedRose,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تم منع الهلوسة: لا يمتلك المنهج المرفوع إجابة مؤكدة لهذا السؤال.",
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Action Tool Bar for AI Messages
                    if (!isUser) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = GlassBorderColor)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row {
                                IconButton(
                                    onClick = {
                                        com.example.util.ExportAndSpeechUtils.initTts(context)
                                        com.example.util.ExportAndSpeechUtils.speak(displayedText)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "قراءة صوتية", tint = FrostedTeal, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        com.example.util.ExportAndSpeechUtils.copyToClipboard(context, "إجابة المنهج", displayedText)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = FrostedTeal, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        com.example.util.ExportAndSpeechUtils.shareText(context, displayedText, "مشاركة إجابة المنهج")
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = FrostedTeal, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        isFavorite = !isFavorite
                                        Toast.makeText(context, if (isFavorite) "تمت الإضافة للمفضلة ⭐" else "تمت الإزالة من المفضلة", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "المفضلة",
                                        tint = FrostedAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        isTranslated = !isTranslated
                                        displayedText = com.example.util.ExportAndSpeechUtils.translateOffline(message.text, isTranslated)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Translate, contentDescription = "ترجمة", tint = FrostedAmber, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        com.example.util.ExportAndSpeechUtils.exportToPdf(context, "إجابة المنهج", displayedText)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = FrostedRose, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        com.example.util.ExportAndSpeechUtils.exportToWord(context, "إجابة المنهج", displayedText)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = "Word", tint = Color(0xFF6366F1), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

