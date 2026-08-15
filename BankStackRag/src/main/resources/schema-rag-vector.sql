CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS vector_store (
    id TEXT,
    content TEXT,
    metadata JSONB,
    embedding public.vector NULL,
	CONSTRAINT vector_store_pkey PRIMARY KEY (id)
);
