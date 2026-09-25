package ro.micronikgrupm.mcp.client.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentMcpToolsTest {

  @Test
  void searchDocumentsUsesDefaultTopKWhenNull() {
    DocumentQueryService queryService = mock(DocumentQueryService.class);
    when(queryService.search("test", 5)).thenReturn("rezultat");
    DocumentMcpTools tools = new DocumentMcpTools(queryService);

    String result = tools.searchDocuments("test", null);

    assertThat(result).isEqualTo("rezultat");
    verify(queryService).search("test", 5);
  }

  @Test
  void searchDocumentsUsesProvidedTopK() {
    DocumentQueryService queryService = mock(DocumentQueryService.class);
    when(queryService.search("test", 3)).thenReturn("rezultat");
    DocumentMcpTools tools = new DocumentMcpTools(queryService);

    tools.searchDocuments("test", 3);

    verify(queryService).search("test", 3);
  }

  @Test
  void listDocumentsDelegatesToQueryService() {
    DocumentQueryService queryService = mock(DocumentQueryService.class);
    when(queryService.listAll()).thenReturn("lista");
    DocumentMcpTools tools = new DocumentMcpTools(queryService);

    assertThat(tools.listDocuments()).isEqualTo("lista");
  }
}
