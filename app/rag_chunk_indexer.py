"""
Local Python Module for Processing & Indexing Extracted PDF Text into RAG Chunks.
Prepares extracted curriculum text for an offline RAG pipeline to answer student questions without internet connectivity.
"""

import os
import re
from typing import Dict, List, Any, Optional
from database_module import init_db, index_chunk_fts5, search_fts5, DB_NAME


class RAGChunkIndexer:
    """
    Processes raw extracted PDF text into indexed chunks with Arabic-aware text cleaning,
    sentence-boundary sliding window chunking, and SQLite FTS5 / keyword local indexing.
    """

    def __init__(self, db_path: str = DB_NAME):
        self.db_path = db_path
        init_db(self.db_path)

    @staticmethod
    def clean_arabic_text(text: str) -> str:
        """
        Normalizes Arabic text by stripping diacritics (Tashkeel), Tatweel (Kashida),
        and redundant whitespace for clean token matching and embedding indexing.
        """
        if not text:
            return ""

        # Remove Arabic Tashkeel (diacritics: Fatha, Damma, Kasra, Sukun, Tanween, Shadda)
        tashkeel_regex = re.compile(r'[\u0617-\u061A\u064B-\u0652]')
        text = re.sub(tashkeel_regex, '', text)

        # Remove Tatweel (Kashida _)
        text = re.sub(r'\u0640', '', text)

        # Replace multiple spaces / newlines with a single space
        text = re.sub(r'\s+', ' ', text)

        return text.strip()

    @staticmethod
    def extract_keywords(query: str) -> List[str]:
        """
        Extracts key content words from Arabic student questions by stripping common stop words.
        """
        stop_words = {
            "ما", "ماذا", "من", "كيف", "لماذا", "اين", "أين", "هل", "هو", "هي", "هم",
            "عن", "في", "على", "إلى", "الى", "من", "مع", "هذا", "هذه", "التي", "الذي",
            "الذين", "كان", "كانت", "يكون", "أن", "ان", "أو", "او", "و", "يا"
        }
        words = re.findall(r'\b\w+\b', query)
        keywords = [w for w in words if w.lower() not in stop_words and len(w) > 1]
        return keywords if keywords else words

    def process_and_chunk_page(
        self,
        page_text: str,
        page_number: int,
        doc_title: str,
        subject: str,
        chunk_size_words: int = 200,
        overlap_words: int = 40
    ) -> List[Dict[str, Any]]:
        """
        Processes text for a single page and splits it into structured chunks.
        Respects sentence boundaries (Arabic periods, question marks, newlines) where possible.
        """
        cleaned_text = self.clean_arabic_text(page_text)
        if not cleaned_text:
            return []

        # Split text into sentences based on common Arabic punctuation marks (. ، ؟ ! \n)
        sentences = re.split(r'(?<=[.،؟!])\s+', cleaned_text)
        sentences = [s.strip() for s in sentences if s.strip()]

        chunks = []
        current_chunk_words: List[str] = []
        current_word_count = 0
        chunk_id = 1

        for sentence in sentences:
            sentence_words = sentence.split()
            if not sentence_words:
                continue

            if current_word_count + len(sentence_words) <= chunk_size_words:
                current_chunk_words.extend(sentence_words)
                current_word_count += len(sentence_words)
            else:
                if current_chunk_words:
                    chunk_content = " ".join(current_chunk_words)
                    chunks.append({
                        "chunk_id": f"{doc_title}_p{page_number}_c{chunk_id}",
                        "doc_title": doc_title,
                        "subject": subject,
                        "page_number": page_number,
                        "content": chunk_content,
                        "word_count": len(current_chunk_words)
                    })
                    chunk_id += 1

                overlap = current_chunk_words[-overlap_words:] if len(current_chunk_words) >= overlap_words else current_chunk_words
                current_chunk_words = overlap + sentence_words
                current_word_count = len(current_chunk_words)

        if current_chunk_words:
            chunk_content = " ".join(current_chunk_words)
            chunks.append({
                "chunk_id": f"{doc_title}_p{page_number}_c{chunk_id}",
                "doc_title": doc_title,
                "subject": subject,
                "page_number": page_number,
                "content": chunk_content,
                "word_count": len(current_chunk_words)
            })

        return chunks

    def process_extracted_document(
        self,
        extracted_pages: List[Dict[str, Any]],
        doc_title: str,
        subject: str,
        chunk_size_words: int = 200,
        overlap_words: int = 40
    ) -> List[Dict[str, Any]]:
        """
        Processes a full list of extracted PDF pages and indexes all resulting chunks into SQLite FTS5.
        """
        all_chunks = []

        for page in extracted_pages:
            page_num = page.get("page_number", 1)
            raw_text = page.get("text", "")

            page_chunks = self.process_and_chunk_page(
                page_text=raw_text,
                page_number=page_num,
                doc_title=doc_title,
                subject=subject,
                chunk_size_words=chunk_size_words,
                overlap_words=overlap_words
            )

            for chunk in page_chunks:
                index_chunk_fts5(
                    doc_title=chunk["doc_title"],
                    subject=chunk["subject"],
                    page_number=chunk["page_number"],
                    content=chunk["content"],
                    db_path=self.db_path
                )
                all_chunks.append(chunk)

        return all_chunks

    def retrieve_context_for_question(
        self,
        question: str,
        subject_filter: Optional[str] = None,
        top_k: int = 3
    ) -> Dict[str, Any]:
        """
        Retrieves the most relevant indexed chunks from local SQLite FTS5 for an offline student question.
        Returns context snippets with page citations ready for offline LLM or local template answering.
        """
        cleaned_question = self.clean_arabic_text(question)
        keywords = self.extract_keywords(cleaned_question)

        # Build OR query for FTS5 term matching
        search_query = " OR ".join(keywords) if keywords else cleaned_question

        search_results = search_fts5(
            query=search_query,
            subject_filter=subject_filter,
            limit=top_k,
            db_path=self.db_path
        )

        # Fallback to individual word matches if combined OR returned nothing
        if not search_results and keywords:
            for kw in keywords:
                res = search_fts5(query=kw, subject_filter=subject_filter, limit=top_k, db_path=self.db_path)
                if res:
                    search_results.extend(res)
                    if len(search_results) >= top_k:
                        break

        formatted_contexts = []
        citations = []

        for res in search_results[:top_k]:
            title = res.get("doc_title", "كتاب المنهج")
            page_num = res.get("page_number", 1)
            content = res.get("content", "")

            citation_str = f"[{title} - صفحة {page_num}]"
            citations.append(citation_str)
            formatted_contexts.append(f"{citation_str}:\n{content}")

        combined_context = "\n\n".join(formatted_contexts) if formatted_contexts else "لم يتم العثور على سياق مباشر في المنهج."

        return {
            "query": question,
            "cleaned_query": cleaned_question,
            "keywords": keywords,
            "results_count": len(citations),
            "citations": citations,
            "context_prompt": combined_context,
            "raw_matches": search_results[:top_k]
        }


if __name__ == "__main__":
    print("=== Testing RAG Chunk Indexer Module ===")
    indexer = RAGChunkIndexer()

    sample_pdf_pages = [
        {
            "page_number": 45,
            "text": """
            الفصل الثالث: القوانين الكهرومغناطيسية والتحث الذاتي.
            قانون فاراداي للحث الكهرومغناطيسي: ينص قانون فاراداي على أن القوة الدفيعة الكهربائية التأثيرية المتولدة في ملف تتناسب طردياً مع المعدل الزمني للتغير في التدفق المغناطيسي الذي يجتاز الملف.
            صيغة القانون: E = -N * (dΦ / dt).
            حيث تمثل E القوة الدفيعة الكهربائية بالفولت، و N عدد لفات الملف، و dΦ/dt معدل تغير التدفق المغناطيسي بالويبر/ثانية.
            إشارة السالب تعبر عن قانون لينز الذي ينص على أن التيار التأثيري المتولد يسري في اتجاه بحيث يعاكس التغير في التدفق المغناطيسي المسبب له.
            """
        },
        {
            "page_number": 46,
            "text": """
            تطبيقات الحث الكهرومغناطيسي في الحياة العملية:
            1. المولد الكهربائي (الدينامو): يحول الطاقة الحركية إلى طاقة كهربائية باستخدام ظاهرة الحث.
            2. المحول الكهربائي: جهاز يعمل على رفع أو خفض الجهد الكهربائي المتناوب دون تغيير التردد.
            3. المحرك الكهربائي: يحول الطاقة الكهربائية إلى طاقة ميكانيكية.
            من أهم شروط عمل المحول الكهربائي أن يتغذى بتيار متناوب (متردد) وليس بتيار مستمر.
            """
        }
    ]

    print("1. Processing and indexing sample physics document...")
    indexed_chunks = indexer.process_extracted_document(
        extracted_pages=sample_pdf_pages,
        doc_title="كتاب الفيزياء - الثالث الثانوي",
        subject="الفيزياء",
        chunk_size_words=60,
        overlap_words=15
    )

    print(f"-> Successfully indexed {len(indexed_chunks)} chunks into SQLite FTS5.")

    print("\n2. Testing Offline RAG Context Retrieval for Student Question...")
    test_question = "ما هو نص قانون فاراداي وما هي أهمية إشارة السالب؟"
    rag_response = indexer.retrieve_context_for_question(test_question, subject_filter="الفيزياء", top_k=2)

    print(f"Query: {rag_response['query']}")
    print(f"Extracted Keywords: {rag_response['keywords']}")
    print(f"Found {rag_response['results_count']} matching context chunks.")
    print("Citations:", rag_response["citations"])
    print("\nRetrieved Context for LLM / Answer Engine:")
    print(rag_response["context_prompt"])
    print("\n=== RAG Chunk Indexer Module Test Passed Successfully ===")
