package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CurriculumDocument
import com.example.engine.BookSummaryResult
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSummaryScreen(
    documents: List<CurriculumDocument>,
    summaryResult: BookSummaryResult?,
    isSummarizing: Boolean,
    onGenerateSummary: (CurriculumDocument) -> Unit,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedDoc by remember { mutableStateOf<CurriculumDocument?>(documents.firstOrNull()) }

    Scaffold(
        containerColor = SlateBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateBackground.copy(alpha = 0.85f),
                    titleContentColor = Color.White
                ),
                title = { Text("وظيفة تلخيص الكتاب والمادة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GlassBorderColor),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = FrostedTeal,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "التلخيص الذكي للدروس والوحدات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اختر أي كتاب مرفوع لطلب تلخيصه إلى نقاط مركزة وقوانين هامة تسهل عملية الحفظ والمراجعة للامتحانات.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Document Selector Buttons
            item {
                Text("اختر الكتاب المراد تلخيصه:", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))

                documents.forEach { doc ->
                    val isSelected = selectedDoc?.id == doc.id
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (isSelected) FrostedIndigo else GlassBorderColor),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) FrostedIndigo.copy(alpha = 0.25f) else SlateSurface.copy(alpha = 0.7f)
                        ),
                        onClick = { selectedDoc = doc },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedDoc = doc },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = FrostedIndigo,
                                    unselectedColor = TextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "المادة: ${doc.subject} • ${doc.pageCount} صفحة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Summarize Action Button
            item {
                Button(
                    onClick = {
                        selectedDoc?.let { onGenerateSummary(it) }
                    },
                    enabled = !isSummarizing && selectedDoc != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FrostedIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isSummarizing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري استخلاص ملخص الوحدات...", color = Color.White)
                    } else {
                        Icon(Icons.Default.Compress, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("توليد ملخص الكتاب الآن 📝", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Summary Result Card List
            if (summaryResult != null) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = GlassBorderColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ملخص: ${summaryResult.docTitle}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = summaryResult.overview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Export Summary Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val fullSummaryText = buildString {
                            appendLine("ملخص: ${summaryResult.docTitle}")
                            appendLine(summaryResult.overview)
                            appendLine()
                            summaryResult.sections.forEach { sec ->
                                appendLine("=== ${sec.title} (${sec.pageRange}) ===")
                                sec.keyPoints.forEach { pt -> appendLine(pt) }
                                sec.mainFormulaOrDefinition?.let { appendLine("قانون/تعريف: $it") }
                                appendLine()
                            }
                        }

                        Button(
                            onClick = { com.example.util.ExportAndSpeechUtils.exportToPdf(context, "ملخص_${summaryResult.docTitle}", fullSummaryText) },
                            colors = ButtonDefaults.buttonColors(containerColor = FrostedRose),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير PDF", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { com.example.util.ExportAndSpeechUtils.exportToWord(context, "ملخص_${summaryResult.docTitle}", fullSummaryText) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير Word", fontSize = 11.sp)
                        }

                        IconButton(
                            onClick = { com.example.util.ExportAndSpeechUtils.copyToClipboard(context, "ملخص الكتاب", fullSummaryText) }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = FrostedTeal)
                        }

                        IconButton(
                            onClick = { com.example.util.ExportAndSpeechUtils.shareText(context, fullSummaryText, "مشاركة ملخص المنهج") }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = FrostedAmber)
                        }
                    }
                }

                items(summaryResult.sections) { sec ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, GlassBorderColor),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.85f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = FrostedIndigo
                                ) {
                                    Text(
                                        text = sec.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = sec.pageRange,
                                    fontSize = 11.sp,
                                    color = FrostedTeal
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            sec.keyPoints.forEach { pt ->
                                Text(
                                    text = pt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            if (sec.mainFormulaOrDefinition != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SlateSurfaceVariant,
                                    border = BorderStroke(1.dp, FrostedTeal.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = FrostedAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = sec.mainFormulaOrDefinition,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

