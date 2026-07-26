package com.example.data.local

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "student_profile")
data class StudentProfile(
    @PrimaryKey val id: Int = 1,
    val studentName: String,
    val schoolName: String,
    val phoneNumber: String,
    val gradeLevel: String = "الثالث الثانوي (العلمي)",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "curriculum_documents")
data class CurriculumDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String,
    val docType: String, // "BOOK", "SUMMARY", "EXAM"
    val filePath: String,
    val fileSize: String,
    val pageCount: Int,
    val addedDate: Long = System.currentTimeMillis(),
    val isPreloaded: Boolean = false,
    val stage: String = "المرحلة الثانوية",
    val gradeLevel: String = "الثالث الثانوي",
    val semester: String = "الفصل الأول",
    val tableOfContents: String = "", // JSON / Formatted Index of Chapters & Real Pages
    val totalEmbeddedImages: Int = 0,
    val totalTablesExtracted: Int = 0
)

@Entity(tableName = "document_page_chunks")
data class DocumentPageChunk(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val documentTitle: String,
    val subject: String,
    val pageNumber: Int, // System Page index (1, 2, 3...)
    val realPageNumber: String = "", // Printed Real Page Number (e.g. "45", "ص 12")
    val pageText: String,
    val keywords: String,
    val headings: String = "", // Extracted Headings & Subheadings
    val tables: String = "", // Extracted Table contents / Markdown tables
    val embeddedImages: String = "", // Extracted Figures / Diagrams captions & paths
    val isScannedOcr: Boolean = false, // Indicates if OCR was run for scanned page
    val isTableOfContents: Boolean = false // Indicates if page contains index/TOC
)

@Fts4(contentEntity = DocumentPageChunk::class)
@Entity(tableName = "document_page_chunks_fts")
data class DocumentPageChunkFts(
    val pageText: String,
    val keywords: String
)

@Entity(tableName = "history_records")
data class HistoryRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "EXAM_SOLVER" or "QUIZ" or "SUMMARY"
    val title: String,
    val subject: String,
    val resultSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_logs")
data class SearchLogRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val encryptedQuery: String = "",
    val searchType: String = "RAG_FTS", // "RAG", "FTS", "SEMANTIC"
    val resultsCount: Int = 0,
    val isEncrypted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "book_chapter_indices")
data class BookChapterIndex(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val unitName: String,
    val chapterTitle: String,
    val startPage: Int,
    val endPage: Int,
    val subSectionsJson: String = "",
    val isEncrypted: Boolean = false
)

