package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.CurriculumDocument
import com.example.data.local.StudentProfile
import com.example.ui.theme.*

data class HomeModuleItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val containerColor: Color,
    val routeKey: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profile: StudentProfile?,
    documents: List<CurriculumDocument>,
    importProgressState: com.example.ui.SmartImportProgressState = com.example.ui.SmartImportProgressState(),
    isDarkMode: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onNavigate: (String) -> Unit
) {
    val modules = listOf(
        HomeModuleItem(
            title = "المساعد الذكي (RAG)",
            subtitle = "بحث وتصفح مع ذكر رقم الصفحة دون هلوسة",
            icon = Icons.Default.Psychology,
            containerColor = FrostedIndigo,
            routeKey = "rag_chat"
        ),
        HomeModuleItem(
            title = "حل ورقة الامتحان (OCR)",
            subtitle = "تصوير ورقة الاختبار أو رفعها وحلها مع المصدر",
            icon = Icons.Default.DocumentScanner,
            containerColor = FrostedTeal,
            routeKey = "exam_solver"
        ),
        HomeModuleItem(
            title = "إدارة الكتب والملخصات",
            subtitle = "رفع وتخزين محلي للكتب وPDF وحذفها",
            icon = Icons.Default.FolderSpecial,
            containerColor = Color(0xFF6366F1),
            routeKey = "doc_management"
        ),
        HomeModuleItem(
            title = "اختبار الطالب الذكي",
            subtitle = "توليد أسئلة أوتوماتيكية وتقييم الإجابات",
            icon = Icons.Default.Quiz,
            containerColor = FrostedAmber,
            routeKey = "quiz"
        ),
        HomeModuleItem(
            title = "تلخيص الكتب والمواد",
            subtitle = "استخلاص القوانين والتعاريف والوحدات",
            icon = Icons.Default.AutoAwesome,
            containerColor = FrostedRose,
            routeKey = "summary"
        )
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🇾🇪", fontSize = 18.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "مناهجي - المنهج اليمني",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "تطبيق بدون إنترنت 100% (Frosted AI)",
                                fontSize = 12.sp,
                                color = FrostedTeal
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "تفعيل الوضع الفاتح" else "تفعيل الوضع الداكن",
                            tint = FrostedTeal
                        )
                    }
                    IconButton(onClick = { onNavigate("profile") }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "الملف الشخصي",
                            tint = FrostedTeal
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Book Import & Processing Progress Banner
            if (importProgressState.isImporting) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
                        border = BorderStroke(1.dp, FrostedIndigo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("doc_management") }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        progress = { importProgressState.progressPercent / 100f },
                                        modifier = Modifier.size(24.dp),
                                        color = FrostedTeal,
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "معالجة الكتاب: ${importProgressState.bookTitle.ifEmpty { "كتاب جديد" }}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = FrostedIndigo.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, FrostedIndigo)
                                ) {
                                    Text(
                                        text = "${importProgressState.progressPercent}%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = FrostedTeal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { importProgressState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = FrostedTeal,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "📍 ${importProgressState.currentStage}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Student Profile Header Card (Gradient Frosted Glass)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("profile") }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        FrostedIndigo.copy(alpha = 0.85f),
                                        FrostedTeal.copy(alpha = 0.75f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "أهلاً بك، ${profile?.studentName ?: "عزيزي الطالب"}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🏫 ${profile?.schoolName ?: "مدرستي"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FrostedAmber
                                )
                                Text(
                                    text = "🎓 ${profile?.gradeLevel ?: "الثالث الثانوي"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${documents.size}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "كتب مرفوعة",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GlassBorderColor),
                    colors = CardDefaults.cardColors(
                        containerColor = SlateSurface.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = FrostedTeal.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.OfflineBolt,
                                    contentDescription = null,
                                    tint = FrostedTeal,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "نظام الذكاء الاصطناعي المحلي (Off-line RAG)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "معالجة كاملة محلياً على الجهاز بدون الحاجة للإنترنت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // Section Title
            item {
                Text(
                    text = "الخدمات والميزات المتاحة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Modules Cards
            items(modules.size) { index ->
                val module = modules[index]
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GlassBorderColor),
                    colors = CardDefaults.cardColors(
                        containerColor = SlateSurface.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(module.routeKey) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = module.containerColor,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = module.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = module.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = module.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Books Preview Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الكتب والملخصات المحملة",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = { onNavigate("doc_management") }) {
                        Text("عرض الكل (${documents.size})", color = FrostedTeal)
                    }
                }
            }

            items(documents.take(3).size) { idx ->
                val doc = documents[idx]
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GlassBorderColor),
                    colors = CardDefaults.cardColors(
                        containerColor = SlateSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("doc_management") }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = FrostedTeal,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.title,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

