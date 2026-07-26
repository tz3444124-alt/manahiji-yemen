package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onSaveProfile: (name: String, school: String, phone: String, grade: String) -> Unit
) {
    var studentName by remember { mutableStateOf("") }
    var schoolName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var gradeLevel by remember { mutableStateOf("الثالث الثانوي (العلمي)") }
    var errorMessage by remember { mutableStateOf("") }

    val gradeOptions = listOf(
        "الثالث الثانوي (العلمي)",
        "الثالث الثانوي (الأدبي)",
        "التاسع الأساسي",
        "الأول الثانوي",
        "الثاني الثانوي"
    )
    var expandedGradeDropdown by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SlateBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Banner Image Card with Frosted Overlay
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GlassBorderColor),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.yemen_study_banner_1784676688601),
                        contentDescription = "طلاب اليمن",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        SlateBackground.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "مرحباً بك في مناهجي 🇾🇪",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "رقمنة المنهج الدراسي للطلاب في اليمن",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FrostedTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "إعداد حساب الطالب",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "يرجى إدخال بياناتك للمرة الأولى فقط ليتم حفظها محلياً على جهازك والبدء في استكشاف المناهج بدون إنترنت.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            if (errorMessage.isNotEmpty()) {
                Surface(
                    color = FrostedRose.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, FrostedRose),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Student Name Input
            OutlinedTextField(
                value = studentName,
                onValueChange = { studentName = it; errorMessage = "" },
                label = { Text("اسم الطالب الرباعي", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = FrostedTeal) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SlateSurface.copy(alpha = 0.7f),
                    unfocusedContainerColor = SlateSurface.copy(alpha = 0.5f),
                    focusedBorderColor = FrostedIndigo,
                    unfocusedBorderColor = GlassBorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // School Name Input
            OutlinedTextField(
                value = schoolName,
                onValueChange = { schoolName = it; errorMessage = "" },
                label = { Text("اسم المدرسة / المجمع التعليمي", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = FrostedTeal) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SlateSurface.copy(alpha = 0.7f),
                    unfocusedContainerColor = SlateSurface.copy(alpha = 0.5f),
                    focusedBorderColor = FrostedIndigo,
                    unfocusedBorderColor = GlassBorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Phone Number Input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it; errorMessage = "" },
                label = { Text("رقم الهاتف (مثال: 771234567)", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = FrostedTeal) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SlateSurface.copy(alpha = 0.7f),
                    unfocusedContainerColor = SlateSurface.copy(alpha = 0.5f),
                    focusedBorderColor = FrostedIndigo,
                    unfocusedBorderColor = GlassBorderColor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Grade Selector Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedGradeDropdown,
                onExpandedChange = { expandedGradeDropdown = !expandedGradeDropdown },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = gradeLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("الصف الدراسي", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Book, contentDescription = null, tint = FrostedTeal) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGradeDropdown) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateSurface.copy(alpha = 0.7f),
                        unfocusedContainerColor = SlateSurface.copy(alpha = 0.5f),
                        focusedBorderColor = FrostedIndigo,
                        unfocusedBorderColor = GlassBorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedGradeDropdown,
                    onDismissRequest = { expandedGradeDropdown = false },
                    modifier = Modifier.background(SlateSurface)
                ) {
                    gradeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = Color.White) },
                            onClick = {
                                gradeLevel = option
                                expandedGradeDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save & Continue Button
            Button(
                onClick = {
                    if (studentName.trim().isBlank()) {
                        errorMessage = "يرجى إدخال اسم الطالب"
                    } else if (schoolName.trim().isBlank()) {
                        errorMessage = "يرجى إدخال اسم المدرسة"
                    } else if (phoneNumber.trim().isBlank()) {
                        errorMessage = "يرجى إدخال رقم الهاتف"
                    } else {
                        onSaveProfile(studentName.trim(), schoolName.trim(), phoneNumber.trim(), gradeLevel)
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FrostedIndigo),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "حفظ البيانات والبدء الآن 🚀",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

