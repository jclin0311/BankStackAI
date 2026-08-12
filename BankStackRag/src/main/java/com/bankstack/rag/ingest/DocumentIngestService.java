package com.bankstack.rag.ingest;

import com.bankstack.rag.clean.DocumentCleaner;
import com.bankstack.rag.chunking.RawChunk;
import com.bankstack.rag.chunking.StructuralChunker;
import com.bankstack.rag.index.KeywordIndexWriter;
import com.bankstack.rag.mapping.ChunkDocumentMapper;
import com.bankstack.rag.model.Chunk;
import com.bankstack.rag.model.DocumentRef;
import com.bankstack.rag.util.ChunkIdUtil;
import com.bankstack.rag.util.HashUtil;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentIngestService {

    private final DocumentCleaner documentCleaner;
    private final StructuralChunker structuralChunker;
    private final ChunkDocumentMapper chunkDocumentMapper;
    private final VectorStore vectorStore;
    private final KeywordIndexWriter keywordIndexWriter;

    public DocumentIngestService(
            DocumentCleaner documentCleaner,
            StructuralChunker structuralChunker,
            ChunkDocumentMapper chunkDocumentMapper,
            VectorStore vectorStore,
            KeywordIndexWriter keywordIndexWriter
    ) {
        this.documentCleaner = documentCleaner;
        this.structuralChunker = structuralChunker;
        this.chunkDocumentMapper = chunkDocumentMapper;
        this.vectorStore = vectorStore;
        this.keywordIndexWriter = keywordIndexWriter;
    }

    public List<Chunk> ingest(DocumentInput input) {
        DocumentRef ref = input.ref();

        String cleanedText = documentCleaner.clean(input.text());
        List<RawChunk> rawChunks = structuralChunker.chunk(cleanedText);
        List<Chunk> chunks = new ArrayList<>();
        List<Document> documentsToEmbed = new ArrayList<>();

        for (RawChunk raw : rawChunks) {
            String normalized = normalize(raw.text());

            String chunkHash = HashUtil.sha256Hex(
                    ref.docId() + "|" +
                    ref.version() + "|" +
                    raw.sectionPath() + "|" +
                    normalized
            );

            boolean alreadyIngested = keywordIndexWriter.existsByDocVersionAndHash(
                    ref.docId(),
                    ref.version(),
                    chunkHash
            );

            if (alreadyIngested) {
                continue;
            }

            String chunkId = ChunkIdUtil.deterministicChunkId(
                    ref.docId(),
                    ref.version(),
                    raw.sectionPath(),
                    raw.ordinal(),
                    chunkHash
            );

            Chunk chunk = new Chunk(
                    chunkId,
                    ref,
                    raw.sectionPath(),
                    raw.ordinal(),
                    normalized,
                    chunkHash,
                    input.policyTags(),
                    input.allowedScopes(),
                    input.sensitivity()
            );

            chunks.add(chunk);
            documentsToEmbed.add(chunkDocumentMapper.toDocument(chunk));

            keywordIndexWriter.upsert(
                    chunk,
                    chunkDocumentMapper.toMetadataJson(chunk)
            );
        }

        if (!documentsToEmbed.isEmpty()) {
            vectorStore.add(documentsToEmbed);
        }

        return chunks;
    }

    private static String normalize(String text) {
        return text == null
                ? ""
                : text.strip()
                      .replaceAll("\\s+\n", "\n")
                      .replaceAll("[ \\t]+", " ")
                      .trim();
    }
}