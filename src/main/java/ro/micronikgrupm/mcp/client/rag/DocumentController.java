package ro.micronikgrupm.mcp.client.rag;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** DocumentController component. */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

  private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

  private final DocumentIngestionService ingestionService;
  private final RagDocumentRepository repository;

  public DocumentController(
      DocumentIngestionService ingestionService, RagDocumentRepository repository) {
    this.ingestionService = ingestionService;
    this.repository = repository;
  }

  /** Handles upload operation. */
  @PostMapping(consumes = "multipart/form-data")
  @ResponseStatus(HttpStatus.CREATED)
  public DocumentResponse upload(@RequestParam("file") MultipartFile file) {
    if (log.isInfoEnabled()) {
      log.info("Primit upload PDF: {}", file.getOriginalFilename());
    }
    return DocumentResponse.from(ingestionService.ingest(file));
  }

  /** Handles list operation. */
  @GetMapping
  public List<DocumentResponse> list() {
    return repository.findAll().stream().map(DocumentResponse::from).toList();
  }

  /** Handles delete operation. */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    ingestionService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
