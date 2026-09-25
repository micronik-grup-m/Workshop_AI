package ro.micronikgrupm.mcp.client.rag;

import java.io.Serial;

/** DocumentNotFoundException component. */
public class DocumentNotFoundException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  public DocumentNotFoundException(Long id) {
    super("Nu există niciun document cu id " + id);
  }
}
