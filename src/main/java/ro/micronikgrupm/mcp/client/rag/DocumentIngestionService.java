package ro.micronikgrupm.mcp.client.rag;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** DocumentIngestionService component. */
@Service
public class DocumentIngestionService {

  private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

  private final RagDocumentRepository repository;
  private final VectorStore vectorStore;

  public DocumentIngestionService(RagDocumentRepository repository, VectorStore vectorStore) {
    this.repository = repository;
    this.vectorStore = vectorStore;
  }

  /**
   * Handles ingest operation. Transactional so a mid-ingestion failure (e.g. the embedding
   * call inside {@code vectorStore.add()}) rolls back the {@code rag_document} row too, instead
   * of leaving an orphaned row with no matching vectors.
   */
  @Transactional
  public RagDocument ingest(MultipartFile file) {
    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
      throw new IllegalArgumentException("Doar fișiere PDF sunt acceptate: " + filename);
    }

    byte[] content;
    try {
      content = file.getBytes();
    } catch (IOException e) {
      throw new IllegalArgumentException("Nu am putut citi fișierul încărcat: " + filename, e);
    }

    PagePdfDocumentReader reader = new PagePdfDocumentReader(new ByteArrayResource(content));
    TokenTextSplitter splitter = TokenTextSplitter.builder().build();
    List<Document> pages;
    List<Document> chunks;
    try {
      pages = reader.read();
      chunks = splitter.split(pages);
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException("Nu am putut extrage text din PDF: " + filename, ex);
    }
    if (chunks.isEmpty()) {
      throw new IllegalArgumentException(
          "PDF-ul nu conține text extractibil (posibil scanat ca imagine): " + filename);
    }

    RagDocument ragDocument = new RagDocument();
    ragDocument.setFilename(filename);
    ragDocument.setUploadedAt(Instant.now());
    ragDocument.setPageCount(pages.size());
    ragDocument.setChunkCount(chunks.size());
    ragDocument = repository.save(ragDocument);

    String documentId = String.valueOf(ragDocument.getId());
    List<Document> taggedChunks =
        chunks.stream()
            .map(
                chunk ->
                    chunk.mutate()
                        .metadata("documentId", documentId)
                        .metadata("filename", filename)
                        .build())
            .toList();

    try {
      vectorStore.add(taggedChunks);
    } catch (RuntimeException ex) {
      throw new DocumentIngestionException(filename, ex);
    }
    if (log.isInfoEnabled()) {
      log.info(
          "Indexat documentul '{}' (id={}): {} pagini, {} fragmente",
          filename,
          ragDocument.getId(),
          pages.size(),
          chunks.size());
    }

    return ragDocument;
  }

  /** Handles delete operation. Transactional for the same reason as {@link #ingest}. */
  @Transactional
  public void delete(Long documentId) {
    RagDocument ragDocument =
        repository
            .findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));
    vectorStore.delete("documentId == '" + documentId + "'");
    repository.delete(ragDocument);
    if (log.isInfoEnabled()) {
      log.info("Șters documentul '{}' (id={})", ragDocument.getFilename(), documentId);
    }
  }
}
