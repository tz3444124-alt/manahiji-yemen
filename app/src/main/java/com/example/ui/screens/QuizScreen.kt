package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.example.engine.QuizQuestion
import com.example.engine.QuizSession
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    quizSession: QuizSession?,
    onStartQuiz: (subject: String) -> Unit,
    onOptionSelected: (questionId: Int, optionIndex: Int) -> Unit,
    onSubmitQuiz: () -> Unit,
    onBack: () -> Unit
) {
    var selectedSubject by remember { mutableStateOf("الفيزياء") }
    val subjectsList = listOf("الفيزياء", "الكيمياء", "الرياضيات", "الأحياء", "اللغة العربية", "الكل")

    Scaffold(
        containerColor = SlateBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateBackground.copy(alpha = 0.85f),
                    titleContentColor = Color.White
                ),
                title = { Text("اختبار الطالب وتقييم الأداء", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Subject Selection Header
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GlassBorderColor),
                colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "مولد الاختبارات من كتب المنهج اليمني",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اختر المادة لتوليد أسئلة اختيار من متعدد وتقييم إجاباتك فورياً مع توضيح الإجابات وتوثيق الصفحات.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScrollableTabRow(
                            selectedTabIndex = subjectsList.indexOf(selectedSubject).coerceAtLeast(0),
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent,
                            contentColor = FrostedIndigo,
                            modifier = Modifier.weight(1f)
                        ) {
                            subjectsList.forEach { subj ->
                                Tab(
                                    selected = selectedSubject == subj,
                                    onClick = { selectedSubject = subj },
                                    text = {
                                        Text(
                                            subj,
                                            fontSize = 12.sp,
                                            color = if (selectedSubject == subj) FrostedTeal else TextMuted
                                        )
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = { onStartQuiz(selectedSubject) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FrostedIndigo)
                        ) {
                            Text("بدء الاختبار 🎯", color = Color.White)
                        }
                    }
                }
            }

            if (quizSession == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            tint = FrostedTeal,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "اضغط على \"بدء الاختبار\" لتوليد أسئلة المادة المختارة",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )
                    }
                }
            } else {
                // Score Header if submitted
                if (quizSession.isSubmitted) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (quizSession.score >= quizSession.questions.size / 2) FrostedTeal else FrostedRose
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (quizSession.score >= quizSession.questions.size / 2)
                                FrostedTeal.copy(alpha = 0.15f)
                            else FrostedRose.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (quizSession.score >= quizSession.questions.size / 2) Icons.Default.EmojiEvents else Icons.Default.SentimentDissatisfied,
                                contentDescription = null,
                                tint = if (quizSession.score >= quizSession.questions.size / 2) FrostedAmber else FrostedRose,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "نتيجة الاختبار: ${quizSession.score} من ${quizSession.questions.size}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = if (quizSession.score >= quizSession.questions.size / 2) "ممتاز! واصل المذاكرة والتفوق 🌟" else "يرجى مراجعة صفحات المنهج المشار إليها أدناه.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                // Questions List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(quizSession.questions, key = { it.id }) { q ->
                        QuizQuestionCard(
                            question = q,
                            selectedOption = quizSession.userAnswers[q.id],
                            isSubmitted = quizSession.isSubmitted,
                            onOptionSelected = { idx -> onOptionSelected(q.id, idx) }
                        )
                    }

                    item {
                        if (!quizSession.isSubmitted) {
                            Button(
                                onClick = onSubmitQuiz,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FrostedIndigo),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .padding(vertical = 8.dp)
                            ) {
                                Text("إنهاء وتصحيح الاختبار 🏁", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        } else {
                            Button(
                                onClick = { onStartQuiz(selectedSubject) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FrostedIndigo),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .padding(vertical = 8.dp)
                            ) {
                                Text("إعادة الاختبار بأسئلة جديدة 🔄", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizQuestionCard(
    question: QuizQuestion,
    selectedOption: Int?,
    isSubmitted: Boolean,
    onOptionSelected: (Int) -> Unit
) {
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
                        text = "سؤال ${question.id}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "صفحة ${question.pageNumber}",
                    fontSize = 11.sp,
                    color = FrostedTeal
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = question.questionText,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            question.options.forEachIndexed { index, optionText ->
                val isSelected = selectedOption == index
                val isCorrect = isSubmitted && index == question.correctOptionIndex
                val isWrongSelection = isSubmitted && isSelected && !isCorrect

                val cardBg = when {
                    isCorrect -> FrostedTeal.copy(alpha = 0.25f)
                    isWrongSelection -> FrostedRose.copy(alpha = 0.25f)
                    isSelected -> FrostedIndigo.copy(alpha = 0.35f)
                    else -> SlateSurfaceVariant.copy(alpha = 0.5f)
                }

                val borderCol = when {
                    isCorrect -> FrostedTeal
                    isWrongSelection -> FrostedRose
                    isSelected -> FrostedIndigo
                    else -> GlassBorderColor
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(enabled = !isSubmitted) { onOptionSelected(index) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = if (!isSubmitted) { { onOptionSelected(index) } } else null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = FrostedIndigo,
                                unselectedColor = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = optionText,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected || isCorrect) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSubmitted) {
                            if (isCorrect) {
                                Icon(Icons.Default.Check, contentDescription = "صحيح", tint = FrostedTeal)
                            } else if (isWrongSelection) {
                                Icon(Icons.Default.Close, contentDescription = "خطأ", tint = FrostedRose)
                            }
                        }
                    }
                }
            }

            if (isSubmitted) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SlateSurfaceVariant,
                    border = BorderStroke(1.dp, GlassBorderColor)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "💡 التوضيح والمصدر:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = FrostedTeal
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${question.explanation}\n(المصدر: ${question.docTitle} - صفحة ${question.pageNumber})",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

