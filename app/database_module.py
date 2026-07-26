"""
SQLite Database Module for User Profiles and Local File Metadata.
Provides full CRUD capabilities for student profiles and PDF/Summary metadata storage.
"""

import sqlite3
import os
from typing import Dict, List, Optional

DB_NAME = "app_data.db"

def get_connection(db_path: str = DB_NAME) -> sqlite3.Connection:
    """Connect to the SQLite database and enable dictionary-like row access."""
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    return conn

def init_db(db_path: str = DB_NAME) -> None:
    """Initialize SQLite tables for User Profiles and Local File Metadata."""
    with get_connection(db_path) as conn:
        cursor = conn.cursor()
        
        # User Profiles table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS user_profiles (
                id INTEGER PRIMARY KEY DEFAULT 1,
                name TEXT NOT NULL,
                school TEXT NOT NULL,
                phone TEXT NOT NULL,
                grade_level TEXT DEFAULT 'الثالث الثانوي (العلمي)',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)

        # Local File Metadata table for PDFs & Summaries
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS file_metadata (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                subject TEXT NOT NULL,
                doc_type TEXT NOT NULL, -- 'BOOK', 'SUMMARY', 'EXAM'
                file_path TEXT NOT NULL,
                file_size TEXT DEFAULT '0 MB',
                page_count INTEGER DEFAULT 1,
                added_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)

        # FTS5 Virtual Table for Fast Full-Text Search in Curriculum Chunks & Summaries
        try:
            cursor.execute("""
                CREATE VIRTUAL TABLE IF NOT EXISTS file_content_fts5 USING fts5(
                    doc_title,
                    subject,
                    page_number,
                    content,
                    tokenize = 'unicode61'
                )
            """)
        except sqlite3.OperationalError:
            # Fallback if FTS5 is not compiled in sqlite3 environment
            cursor.execute("""
                CREATE TABLE IF NOT EXISTS file_content_fts5 (
                    doc_title TEXT,
                    subject TEXT,
                    page_number INTEGER,
                    content TEXT
                )
            """)
        conn.commit()

def index_chunk_fts5(doc_title: str, subject: str, page_number: int, content: str, db_path: str = DB_NAME) -> None:
    """Index a document chunk into the FTS5 full-text search table."""
    with get_connection(db_path) as conn:
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO file_content_fts5 (doc_title, subject, page_number, content)
            VALUES (?, ?, ?, ?)
        """, (doc_title, subject, page_number, content))
        conn.commit()

def search_fts5(query: str, subject_filter: Optional[str] = None, limit: int = 10, db_path: str = DB_NAME) -> List[Dict]:
    """
    Perform fast full-text search using SQLite FTS5 extension.
    Returns matched chunks ranked by relevance with page citations.
    """
    with get_connection(db_path) as conn:
        cursor = conn.cursor()
        clean_query = query.replace("'", "''").strip()
        
        try:
            if subject_filter and subject_filter != "الكل":
                sql = """
                    SELECT doc_title, subject, page_number, content, rank
                    FROM file_content_fts5
                    WHERE file_content_fts5 MATCH ? AND subject = ?
                    ORDER BY rank
                    LIMIT ?
                """
                cursor.execute(sql, (clean_query, subject_filter, limit))
            else:
                sql = """
                    SELECT doc_title, subject, page_number, content, rank
                    FROM file_content_fts5
                    WHERE file_content_fts5 MATCH ?
                    ORDER BY rank
                    LIMIT ?
                """
                cursor.execute(sql, (clean_query, limit))
        except sqlite3.OperationalError:
            # Fallback LIKE search if FTS MATCH fails
            sql = """
                SELECT doc_title, subject, page_number, content, 0 AS rank
                FROM file_content_fts5
                WHERE content LIKE ?
                LIMIT ?
            """
            cursor.execute(sql, (f"%{clean_query}%", limit))

        rows = cursor.fetchall()
        return [dict(row) for row in rows]

def save_user_profile(name: str, school: str, phone: str, grade: str = "الثالث الثانوي (العلمي)", db_path: str = DB_NAME) -> None:
    """Save or update student user profile."""
    with get_connection(db_path) as conn:
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO user_profiles (id, name, school, phone, grade_level)
            VALUES (1, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                name = excluded.name,
                school = excluded.school,
                phone = excluded.phone,
                grade_level = excluded.grade_level
        """, (name, school, phone, grade))
        conn.commit()

def get_user_profile(db_path: str = DB_NAME) -> Optional[Dict]:
    """Retrieve the current user profile."""
    with get_connection(db_path) as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM user_profiles WHERE id = 1")
        row = cursor.fetchone()
        return dict(row) if row else None

def add_file_metadata(title: str, subject: str, doc_type: str, file_path: str, file_size: str = "3.5 MB", page_count: int = 1, db_path: str = DB_NAME) -> int:
    """Add metadata for a PDF book or summary file."""
    with get_connection(db_path) as conn:
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO file_metadata (title, subject, doc_type, file_path, file_size, page_count)
            VALUES (?, ?, ?, ?, ?, ?)
        """, (title, subject, doc_type, file_path, file_size, page_count))
        conn.commit()
        return cursor.lastrowid

def get_all_file_metadata(doc_type: Optional[str] = None, db_path: str = DB_NAME) -> List[Dict]:
    """Fetch stored file metadata, optionally filtered by document type ('BOOK', 'SUMMARY', 'EXAM')."""
    with get_connection(db_path) as conn:
        cursor = conn.cursor()
        if doc_type:
            cursor.execute("SELECT * FROM file_metadata WHERE doc_type = ? ORDER BY added_date DESC", (doc_type,))
        else:
            cursor.execute("SELECT * FROM file_metadata ORDER BY added_date DESC")
        rows = cursor.fetchall()
        return [dict(row) for row in rows]

def delete_file_metadata(doc_id: int, db_path: str = DB_NAME) -> bool:
    """Delete a document's metadata by its ID."""
    with get_connection(db_path) as conn:
        cursor = conn.cursor()
        cursor.execute("DELETE FROM file_metadata WHERE id = ?", (doc_id,))
        conn.commit()
        return cursor.rowcount > 0

if __name__ == "__main__":
    init_db()
    print("Database initialized successfully.")
    
    # Example usage
    save_user_profile("علي أحمد المحمادي", "مدرسه الميثاق الثانوي", "771234567")
    print("Saved Profile:", get_user_profile())
    
    doc_id = add_file_metadata(
        title="كتاب الفيزياء - الصف الثالث الثانوي",
        subject="الفيزياء",
        doc_type="BOOK",
        file_path="/documents/physics_3rd.pdf",
        file_size="12.4 MB",
        page_count=180
    )
    print("Added File Metadata ID:", doc_id)
    print("Stored Files:", get_all_file_metadata())
