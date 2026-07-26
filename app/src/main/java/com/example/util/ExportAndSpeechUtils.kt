package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ExportAndSpeechUtils {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    fun initTts(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("ar"))
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        isTtsInitialized = true
                    }
                }
            }
        }
    }

    fun speak(text: String) {
        if (isTtsInitialized && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "RAG_TTS_ID")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun copyToClipboard(context: Context, label: String = "الإجابة", text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ النص إلى الحافظة 📋", Toast.LENGTH_SHORT).show()
    }

    fun shareText(context: Context, text: String, title: String = "مشاركة المنهج") {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    fun exportToPdf(context: Context, title: String, content: String): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            val paint = Paint().apply {
                textSize = 14f
                color = android.graphics.Color.BLACK
            }

            canvas.drawText(title, 40f, 50f, paint.apply { textSize = 18f; isFakeBoldText = true })
            paint.apply { textSize = 12f; isFakeBoldText = false }

            var y = 90f
            content.lines().forEach { line ->
                if (y > 800f) return@forEach
                canvas.drawText(line.take(80), 40f, y, paint)
                y += 20f
            }

            pdfDocument.finishPage(page)

            val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "CurriculumExports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val pdfFile = File(exportDir, "${title.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            pdfDocument.close()

            Toast.makeText(context, "تم حفظ الملف بنجاح PDF: ${pdfFile.name} 📄", Toast.LENGTH_LONG).show()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر إنشاء ملف PDF", Toast.LENGTH_SHORT).show()
            null
        }
    }

    fun exportToWord(context: Context, title: String, content: String): File? {
        return try {
            val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "CurriculumExports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val docFile = File(exportDir, "${title.replace(" ", "_")}_${System.currentTimeMillis()}.doc")
            FileOutputStream(docFile).use { out ->
                val formattedDoc = "=== $title ===\n\n$content\n\n--- تم التصدير أوفلاين من تطبيق مناهجي المنهج اليمني ---"
                out.write(formattedDoc.toByteArray(Charsets.UTF_8))
            }

            Toast.makeText(context, "تم تصدير ملف Word (.doc) بنجاح: ${docFile.name} 📝", Toast.LENGTH_LONG).show()
            docFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر تصدير ملف Word", Toast.LENGTH_SHORT).show()
            null
        }
    }

    fun translateOffline(text: String, toEnglish: Boolean): String {
        // Quick Arabic <-> English term dictionary fallback for local offline translation
        if (toEnglish) {
            return text
                .replace("الكتاب", "Book")
                .replace("الصفحة", "Page")
                .replace("الفقرة", "Paragraph")
                .replace("المنهج", "Curriculum")
                .replace("الإجابة النموذجية", "Model Answer")
                .replace("السؤال", "Question")
                .replace("الوحدة", "Unit")
                .replace("تعريف", "Definition")
                .replace("قانون", "Law / Formula")
        } else {
            return text
                .replace("Book", "الكتاب")
                .replace("Page", "الصفحة")
                .replace("Paragraph", "الفقرة")
                .replace("Curriculum", "المنهج")
        }
    }
}
