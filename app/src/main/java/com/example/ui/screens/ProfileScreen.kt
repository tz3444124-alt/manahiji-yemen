package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.HistoryRecord
import com.example.data.local.StudentProfile
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: StudentProfile?,
    historyRecords: List<HistoryRecord>,
    isDarkMode: Boolean = true,
    backupStatus: String? = null,
    onToggleTheme: () -> Unit = {},
    onUpdateProfile: (name: String, school: String, phone: String, grade: String) -> Unit,
    onExportBackup: () -> Unit = {},
    onRestoreBackup: (String) -> Unit = {},
    onExportDbFile: () -> Unit = {},
    onExportBackupFile: () -> Unit = {},
    onImportBackupFileUri: (android.net.Uri) -> Unit = {},
    onDismissBackupStatus: () -> Unit = {},
    onBack: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputText by remember { mutableStateOf("") }

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onImportBackupFileUri(uri)
        }
    }

    var nameInput by remember { mutableStateOf(profile?.studentName ?: "") }
    var schoolInput by remember { mutableStateOf(profile?.schoolName ?: "") }
    var phoneInput by remember { mutableStateOf(profile?.phoneNumber ?: "") }
    var gradeInput by remember { mutableStateOf(profile?.gradeLevel ?: "الثالث الثانوي (العلمي)") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = { Text("الملف الشخصي للطالب", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "الوضع الفاتح" else "الوضع الداكن",
                            tint = FrostedTeal
                        )
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
            // Profile Card Header with Indigo/Teal Gradient & Glass Border
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, GlassBorderColor),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        FrostedIndigo.copy(alpha = 0.85f),
                                        FrostedTeal.copy(alpha = 0.7f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SlateBackground,
                                border = BorderStroke(2.dp, FrostedTeal),
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = FrostedTeal,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = profile?.studentName ?: "طالب المنهج اليمني",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White
                            )

                            Text(
                                text = "🏫 ${profile?.schoolName ?: "المدرسة"}",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            Text(
                                text = "📱 ${profile?.phoneNumber ?: "000000000"}  •  🎓 ${profile?.gradeLevel ?: "الثالث الثانوي"}",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Edit Profile Form
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GlassBorderColor),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.8f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "بيانات الطالب المسجلة (SQLite)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )

                            TextButton(onClick = { isEditing = !isEditing }) {
                                Icon(
                                    if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = FrostedTeal
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEditing) "إلغاء" else "تعديل", color = FrostedTeal)
                            }
                        }

                        if (isEditing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("اسم الطالب", color = TextMuted) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = FrostedIndigo, unfocusedBorderColor = GlassBorderColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = schoolInput,
                                onValueChange = { schoolInput = it },
                                label = { Text("اسم المدرسة", color = TextMuted) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = FrostedIndigo, unfocusedBorderColor = GlassBorderColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = { Text("رقم الهاتف", color = TextMuted) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = FrostedIndigo, unfocusedBorderColor = GlassBorderColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = gradeInput,
                                onValueChange = { gradeInput = it },
                                label = { Text("الصف الدراسي", color = TextMuted) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                    focusedBorderColor = FrostedIndigo, unfocusedBorderColor = GlassBorderColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    onUpdateProfile(nameInput, schoolInput, phoneInput, gradeInput)
                                    isEditing = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FrostedIndigo),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("حفظ التحديثات", color = Color.White)
                            }
                        }
                    }
                }
            }

            // Appearance / Theme Mode Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GlassBorderColor),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = FrostedTeal,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = if (isDarkMode) "الوضع الداكن (حماية العينين)" else "الوضع الفاتح (النهاري)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isDarkMode) "مناسب للمذاكرة في الليل والإضاءة المنخفضة" else "مناسب للمذاكرة في النهار والأماكن المضيئة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onToggleTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FrostedIndigo,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = FrostedTeal
                            )
                        )
                    }
                }
            }

            // Room Database Backup, Encryption & Performance Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GlassBorderColor),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.85f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = FrostedTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إدارة قاعدة البيانات والنسخ الاحتياطي والأداء",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "🔐 **تشفير البيانات:** تستخدم قاعدة بيانات Room نظام التشفير المحتفظ به أوفلاين لحماية الملاحظات والنتائج.\n⚡ **الأداء والخلفية:** تتم معالجة الكتب بالخلفية عبر Kotlin Coroutines و Dispatchers.IO وبدون أي تجميد للواجهة.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (backupStatus != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = FrostedTeal.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, FrostedTeal),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = FrostedTeal)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = backupStatus,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = onDismissBackupStatus) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onExportBackupFile,
                                colors = ButtonDefaults.buttonColors(containerColor = FrostedIndigo),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصدير إلى ملف محلي", fontSize = 10.sp)
                            }

                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = FrostedTeal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد من ملف محلي", fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onExportBackup,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GlassBorderColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = FrostedAmber, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تشفير AES-256", fontSize = 10.sp, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = onExportDbFile,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GlassBorderColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = FrostedTeal, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تصدير Room DB", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // History Log Section
            item {
                Text(
                    text = "سجل النشاطات والاختبارات",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (historyRecords.isEmpty()) {
                item {
                    Text(
                        text = "لا توجد نشاطات مسجلة بعد.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                items(historyRecords) { rec ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GlassBorderColor),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.8f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (rec.type) {
                                    "EXAM_SOLVER" -> Icons.Default.DocumentScanner
                                    "QUIZ" -> Icons.Default.Quiz
                                    else -> Icons.Default.AutoAwesome
                                },
                                contentDescription = null,
                                tint = FrostedTeal,
                                modifier = Modifier.size(28.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rec.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = rec.resultSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showRestoreDialog) {
            AlertDialog(
                containerColor = SlateSurface,
                onDismissRequest = { showRestoreDialog = false },
                title = {
                    Text("استعادة النسخة الاحتياطية المشفرة", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text("أدخل رمز أو بيانات النسخة الاحتياطية المشفرة (AES-256):", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = restoreInputText,
                            onValueChange = { restoreInputText = it },
                            placeholder = { Text("لصق بيانات النسخة الاحتياطية...", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = FrostedIndigo, unfocusedBorderColor = GlassBorderColor
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRestoreDialog = false
                            onRestoreBackup(restoreInputText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FrostedTeal)
                    ) {
                        Text("استعادة الآن", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text("إلغاء", color = TextMuted)
                    }
                }
            )
        }
    }
}

