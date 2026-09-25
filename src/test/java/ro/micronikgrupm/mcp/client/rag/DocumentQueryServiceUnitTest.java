package ro.micronikgrupm.mcp.client.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class DocumentQueryServiceUnitTest {

  private VectorStore vectorStore;
  private RagDocumentRepository repository;
  private DocumentQueryService service;

  @BeforeEach
  void setUp() {
    vectorStore = mock(VectorStore.class);
    repository = mock(RagDocumentRepository.class);
    service = new DocumentQueryService(vectorStore, repository);
  }

  @Test
  void searchReturnsFriendlyMessageWhenNoResults() {
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

    String result = service.search("nimic", 3);

    assertThat(result).isEqualTo("Niciun fragment relevant găsit.");
  }

  @Test
  void searchFormatsDocumentMetadataAndContent() {
    Document doc = mock(Document.class);
    when(doc.getMetadata())
        .thenReturn(
            Map.of("filename", "manual.pdf", PagePdfDocumentReader.METADATA_START_PAGE_NUMBER, 2));
    when(doc.getText()).thenReturn("Conținut relevant.");
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

    String result = service.search("manual", 1);

    assertThat(result).isEqualTo("[manual.pdf, pagina 2] Conținut relevant.");
  }

  @Test
  void listAllReturnsFriendlyMessageWhenEmpty() {
    when(repository.findAll()).thenReturn(List.of());

    String result = service.listAll();

    assertThat(result).isEqualTo("Niciun document încărcat.");
  }

  @Test
  void listAllFormatsStoredDocuments() {
    RagDocument document = new RagDocument();
    document.setFilename("catalog.pdf");
    document.setPageCount(4);
    document.setChunkCount(10);
    when(repository.findAll()).thenReturn(List.of(document));

    String result = service.listAll();

    assertThat(result).contains("catalog.pdf").contains("4 pagini").contains("10 fragmente");
  }
}
