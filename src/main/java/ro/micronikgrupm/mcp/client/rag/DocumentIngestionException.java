package ro.micronikgrupm.mcp.client.rag;

import java.io.Serial;

/** Thrown when indexing an uploaded PDF fails after it was read. */
public class DocumentIngestionException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  public DocumentIngestionException(String filename, Throwable cause) {
    super("Indexarea documentului '" + filename + "' a eșuat", cause);
  }
}
