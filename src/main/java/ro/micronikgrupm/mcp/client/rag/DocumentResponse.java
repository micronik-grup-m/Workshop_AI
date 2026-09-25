package ro.micronikgrupm.mcp.client.rag;

import java.time.Instant;

/** DocumentResponse component. */
public record DocumentResponse(
    Long id, String filename, Instant uploadedAt, int pageCount, int chunkCount) {

  static DocumentResponse from(RagDocument d) {
    return new DocumentResponse(
        d.getId(), d.getFilename(), d.getUploadedAt(), d.getPageCount(), d.getChunkCount());
  }
}
