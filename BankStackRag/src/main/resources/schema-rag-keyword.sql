CREATE TABLE IF NOT EXISTS rag_keyword_index (
    chunk_id TEXT PRIMARY KEY,
    doc_id TEXT NOT NULL,
    version TEXT NOT NULL,
    chunk_hash TEXT NOT NULL,
    content TEXT NOT NULL,
    metadata JSONB NOT NULL,
    search_vector tsvector NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS rag_keyword_index_doc_version_hash_uidx
    ON rag_keyword_index (doc_id, version, chunk_hash);

CREATE INDEX IF NOT EXISTS rag_keyword_index_search_idx
    ON rag_keyword_index
    USING GIN (search_vector);