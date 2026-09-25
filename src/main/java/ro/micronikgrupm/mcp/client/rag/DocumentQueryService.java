package ro.micronikgrupm.mcp.client.rag;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/** DocumentQueryService component. */
@Service
public class DocumentQueryService {

  private static final Logger log = LoggerFactory.getLogger(DocumentQueryService.class);

  private final VectorStore vectorStore;
  private final RagDocumentRepository repository;

  public DocumentQueryService(VectorStore vectorStore, RagDocumentRepository repository) {
    this.vectorStore = vectorStore;
    this.repository = repository;
  }

  /** Handles search operation. */
  public String search(String query, int topK) {
    List<Document> results =
        vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(topK).build());
    if (log.isDebugEnabled()) {
      log.debug("Căutare '{}' -> {} rezultate", query, results.size());
    }
    if (results.isEmpty()) {
      return "Niciun fragment relevant găsit.";
    }
    return results.stream()
        .map(this::describe)
        .reduce((a, b) -> a + "\n" + b)
        .orElse("");
  }

  /** Handles listAll operation. */
  public String listAll() {
    List<RagDocument> documents = repository.findAll();
    if (documents.isEmpty()) {
      return "Niciun document încărcat.";
    }
    return documents.stream()
        .map(
            d ->
                "id=%d, %s, %d pagini, %d fragmente"
                    .formatted(d.getId(), d.getFilename(), d.getPageCount(), d.getChunkCount()))
        .reduce((a, b) -> a + "\n" + b)
        .orElse("");
  }

  private String describe(Document document) {
    Object filename = document.getMetadata().get("filename");
    Object page = document.getMetadata().get(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER);
    return "[%s, pagina %s] %s".formatted(filename, page, document.getText());
  }
}
