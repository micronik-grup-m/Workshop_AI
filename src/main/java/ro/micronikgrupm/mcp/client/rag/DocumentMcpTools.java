package ro.micronikgrupm.mcp.client.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/** DocumentMcpTools component. */
@Component
public class DocumentMcpTools {

  private static final Logger log = LoggerFactory.getLogger(DocumentMcpTools.class);

  private final DocumentQueryService queryService;

  public DocumentMcpTools(DocumentQueryService queryService) {
    this.queryService = queryService;
  }

  /** Handles searchDocuments MCP tool operation. */
  @McpTool(
      name = "searchDocuments",
      description =
          "Caută fragmente relevante în documentele PDF încărcate, pe baza unei întrebări")
  public String searchDocuments(
      @McpToolParam(description = "Întrebarea sau textul căutat", required = true) String query,
      @McpToolParam(
              description = "Numărul maxim de fragmente întoarse (opțional, implicit 5)",
              required = false)
          Integer topK) {
    int effectiveTopK = topK != null ? topK : 5;
    if (log.isInfoEnabled()) {
      log.info("MCP searchDocuments: query='{}', topK={}", query, effectiveTopK);
    }
    return queryService.search(query, effectiveTopK);
  }

  /** Handles listDocuments MCP tool operation. */
  @McpTool(name = "listDocuments", description = "Listează documentele PDF încărcate")
  public String listDocuments() {
    return queryService.listAll();
  }
}
