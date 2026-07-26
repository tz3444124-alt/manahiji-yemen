package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ==========================================
    // 1. Student Profile CRUD
    // ==========================================
    @Query("SELECT * FROM student_profile WHERE id = 1")
    fun getStudentProfile(): Flow<StudentProfile?>

    @Query("SELECT * FROM student_profile WHERE id = 1")
    suspend fun getStudentProfileDirect(): StudentProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStudentProfile(profile: StudentProfile)


    // ==========================================
    // 2. Books / Curriculum Documents CRUD
    // ==========================================
    @Query("SELECT * FROM curriculum_documents ORDER BY addedDate DESC")
    fun getAllDocuments(): Flow<List<CurriculumDocument>>

    @Query("SELECT * FROM curriculum_documents ORDER BY addedDate DESC")
    suspend fun getAllDocumentsList(): List<CurriculumDocument>

    @Query("SELECT * FROM curriculum_documents WHERE docType = :type ORDER BY addedDate DESC")
    fun getDocumentsByType(type: String): Flow<List<CurriculumDocument>>

    @Query("SELECT * FROM curriculum_documents WHERE subject = :subject ORDER BY addedDate DESC")
    fun getDocumentsBySubject(subject: String): Flow<List<CurriculumDocument>>

    @Query("SELECT * FROM curriculum_documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): CurriculumDocument?

    @Query("SELECT * FROM curriculum_documents ORDER BY addedDate DESC LIMIT :limit")
    suspend fun getRecentDocuments(limit: Int = 5): List<CurriculumDocument>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: CurriculumDocument): Long

    @Update
    suspend fun updateDocument(doc: CurriculumDocument)

    @Delete
    suspend fun deleteDocument(doc: CurriculumDocument)

    @Query("DELETE FROM curriculum_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("DELETE FROM curriculum_documents")
    suspend fun deleteAllDocuments()


    // ==========================================
    // 3. Search Index / Page Chunks (RAG Engine)
    // ==========================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageChunk(chunk: DocumentPageChunk): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageChunks(chunks: List<DocumentPageChunk>)

    @Update
    suspend fun updatePageChunk(chunk: DocumentPageChunk)

    @Query("DELETE FROM document_page_chunks WHERE id = :id")
    suspend fun deleteChunkById(id: Long)

    @Query("DELETE FROM document_page_chunks WHERE documentId = :documentId")
    suspend fun deleteChunksByDocumentId(documentId: Long)

    @Query("DELETE FROM document_page_chunks")
    suspend fun deleteAllChunks()

    @Query("SELECT * FROM document_page_chunks")
    suspend fun getAllChunks(): List<DocumentPageChunk>

    @Query("SELECT * FROM document_page_chunks WHERE documentId = :documentId ORDER BY pageNumber ASC")
    suspend fun getChunksForDocument(documentId: Long): List<DocumentPageChunk>

    @Query("SELECT * FROM document_page_chunks WHERE subject = :subject ORDER BY pageNumber ASC")
    suspend fun getChunksBySubject(subject: String): List<DocumentPageChunk>

    // FTS4 / Full-Text Search for high-speed RAG context retrieval
    @Query("SELECT * FROM document_page_chunks WHERE rowid IN (SELECT rowid FROM document_page_chunks_fts WHERE document_page_chunks_fts MATCH :searchQuery)")
    suspend fun searchChunksFts(searchQuery: String): List<DocumentPageChunk>

    // Keyword Fallback Query for RAG system
    @Query("SELECT * FROM document_page_chunks WHERE pageText LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%' OR subject LIKE '%' || :query || '%' LIMIT 50")
    suspend fun searchChunksByKeyword(query: String): List<DocumentPageChunk>


    // ==========================================
    // 4. Book Chapter & Unit Indices
    // ==========================================
    @Query("SELECT * FROM book_chapter_indices WHERE documentId = :documentId ORDER BY startPage ASC")
    suspend fun getIndicesForDocument(documentId: Long): List<BookChapterIndex>

    @Query("SELECT * FROM book_chapter_indices ORDER BY id ASC")
    suspend fun getAllChapterIndices(): List<BookChapterIndex>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapterIndices(indices: List<BookChapterIndex>)

    @Query("DELETE FROM book_chapter_indices WHERE documentId = :documentId")
    suspend fun deleteIndicesForDocument(documentId: Long)


    // ==========================================
    // 5. Search History & Activity Log
    // ==========================================
    @Query("SELECT * FROM history_records ORDER BY timestamp DESC")
    fun getAllHistoryRecords(): Flow<List<HistoryRecord>>

    @Query("SELECT * FROM history_records ORDER BY timestamp DESC")
    suspend fun getAllHistoryRecordsList(): List<HistoryRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryRecord(record: HistoryRecord)

    @Query("DELETE FROM history_records WHERE id = :id")
    suspend fun deleteHistoryRecordById(id: Long)

    @Query("DELETE FROM history_records")
    suspend fun clearAllHistoryRecords()


    // ==========================================
    // 6. Encrypted Search Logs
    // ==========================================
    @Query("SELECT * FROM search_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearchLogs(limit: Int = 20): Flow<List<SearchLogRecord>>

    @Query("SELECT * FROM search_logs ORDER BY timestamp DESC")
    suspend fun getAllSearchLogsList(): List<SearchLogRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchLog(log: SearchLogRecord)

    @Query("DELETE FROM search_logs WHERE id = :id")
    suspend fun deleteSearchLogById(id: Long)

    @Query("DELETE FROM search_logs")
    suspend fun clearAllSearchLogs()
}

