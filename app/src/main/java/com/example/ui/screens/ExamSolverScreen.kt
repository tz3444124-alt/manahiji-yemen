package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.example.engine.ExamQuestionSolution
import com.example.engine.ExamSolverReport
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSolverScreen(
    examReport: ExamSolverReport?,
    isSolving: Boolean,
    onSolveExam: (ocrText: String, subject: String?) -> Unit,
    onBack: () -> Unit
) {
    var rawOcrInput by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("الفيزياء") }

    val sampleExams = listOf(
        "امتحان الفيزياء 2024" to """
            س1: عرف قانون كولوم مع كتابة صيغته الرياضية وبيان وحدات القياس؟
            س2: اذكر الشروط والفرق بين توصيل المكثفات على التوازي وتوصيلها على التوالي؟
            س3: احسب السعة المكافئة لمكثفين سعتيهما 4 ميكروفاراد و 6 ميكروفاراد متصلين على التوازي؟
            س4: ما هي الظاهرة الكهروضوئية وكيف فسرها العالم أينشتاين؟
        """.trimIndent(),
        "امتحان الكيمياء 2023" to """
            س1: وضح مبدأ لوشاتيليه العوامل المؤثرة على نظام في حالة اتزان؟
            س2: عرف الرقم الهيدروجيني pH وأوجد قيمته للماء النقي؟
            س3: قارن بين المصعد (الأنود) والمهبط (الكاثود) في الخلايا الجلفانية؟
        """.trimIndent(),
        "امتحان الرياضيات 2024" to """
            س1: احسب المشتقة الأولى للدالة المثلثية sin(x) والدالة cos(x)؟
            س2: أوجد نقاط الانقلاب واختبار المشتقة الثانية للدوال؟
            س3: احسب قيمة التكامل غير المحدد لدالة الجيب؟
        """.trimIndent()
    )

    Scaffold(
        containerColor = SlateBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateBackground.copy(alpha = 0.85f),
                    titleContentColor = Color.White
                ),
                title = { Text("حل ورقة الامتحان الذكي (OCR)", fontWeight = FontWeight.Bold) },
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
                    colors = CardDefaults.cardColors(
                        containerColor = SlateSurface.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = FrostedTeal,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "الماسح الضوئي واستخراج أسئلة ورقة الاختبار",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "يمكنك فتح الكاميرا لتصوير ورقة الأسئلة، أو اختيار صورة ملف الاختبار، وسيقوم النظام باستخراج النصوص (OCR) وحل كل سؤال مع ربطه برقم الصفحة في الكتاب.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Buttons Bar: Camera & Gallery & Preloaded Samples
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            rawOcrInput = sampleExams[0].second
                            selectedSubject = "الفيزياء"
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FrostedIndigo),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("التقاط كاميرا", fontSize = 12.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            rawOcrInput = sampleExams[1].second
                            selectedSubject = "الكيمياء"
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GlassBorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp), tint = FrostedTeal)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اختر صورة", fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            // Quick Samples Chips
            item {
                Text("أو اختر نموذج امتحان وزاري سريع:", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sampleExams.forEach { (title, text) ->
                        SuggestionChip(
                            onClick = {
                                rawOcrInput = text
                                selectedSubject = if (title.contains("فيزياء")) "الفيزياء" else if (title.contains("كيمياء")) "الكيمياء" else "الرياضيات"
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = SlateSurface),
                            border = SuggestionChipDefaults.suggestionChipBorder(borderColor = GlassBorderColor, enabled = true),
                            label = { Text(title, fontSize = 11.sp, color = TextMuted) }
                        )
                    }
                }
            }

            // Extracted OCR Text Input Field
            item {
                OutlinedTextField(
                    value = rawOcrInput,
                    onValueChange = { rawOcrInput = it },
                    label = { Text("النص المستخرج من ورقة الاختبار (OCR Input)", color = TextMuted) },
                    placeholder = { Text("ضع هنا نص أسئلة الامتحان الممسوحة ضوئياً...", color = TextMuted) },
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
                        .height(130.dp),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Solve Button
            item {
                Button(
                    onClick = {
                        if (rawOcrInput.isNotBlank()) {
                            onSolveExam(rawOcrInput, selectedSubject)
                        }
                    },
                    enabled = !isSolving && rawOcrInput.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FrostedIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isSolving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري استخراج وإجابة الأسئلة عبر RAG...", color = Color.White)
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("استخراج وتحليل إجابات الامتحان 📝", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Report Results Section
            if (examReport != null) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = GlassBorderColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تقرير الإجابات من المنهج المرفوع",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = FrostedTeal.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, FrostedTeal.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "تم حل ${examReport.solvedQuestionsCount} من أصل ${examReport.totalQuestionsCount} أسئلة",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = FrostedTeal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                items(examReport.questions) { q ->
                    ExamQuestionCardItem(question = q)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ExamQuestionCardItem(question: ExamQuestionSolution) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GlassBorderColor),
        colors = CardDefaults.cardColors(
            containerColor = SlateSurface.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FrostedIndigo
                ) {
                    Text(
                        text = "سؤال ${question.questionNumber}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SlateSurfaceVariant,
                    border = BorderStroke(1.dp, FrostedAmber.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = question.questionType.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FrostedAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (question.isFoundInCurriculum && question.pageCitation != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SlateSurfaceVariant,
                        border = BorderStroke(1.dp, FrostedTeal.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = FrostedTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ص ${question.pageNumber ?: "1"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FrostedTeal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = question.questionText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (question.isFoundInCurriculum) SlateSurfaceVariant.copy(alpha = 0.7f)
                else FrostedRose.copy(alpha = 0.15f),
                border = BorderStroke(
                    1.dp,
                    if (question.isFoundInCurriculum) GlassBorderColor else FrostedRose.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (question.isFoundInCurriculum) "🎯 الإجابة النموذجية المستخرجة من المنهج:" else "⚠️ نتيجة البحث في المنهج:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (question.isFoundInCurriculum) FrostedTeal else FrostedRose
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = question.solutionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        lineHeight = 20.sp
                    )

                    if (question.isFoundInCurriculum && question.bookTitle != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = GlassBorderColor)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, tint = FrostedAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "الكتاب: ${question.bookTitle}  •  الصفحة: ${question.pageNumber}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FrostedAmber
                            )
                        }

                        if (question.paragraphExcerpt != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📌 الفقرة: ${question.paragraphExcerpt}",
                                fontSize = 11.sp,
                                color = TextMuted,
                                maxLines = 3
                            )
                        }
                    }
                }
            }
        }
    }
}


