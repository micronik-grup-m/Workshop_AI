package ro.micronikgrupm.mcp.client.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ro.micronikgrupm.mcp.client.employee.EmployeeNotFoundException;
import ro.micronikgrupm.mcp.client.employee.InvalidSalaryRangeException;
import ro.micronikgrupm.mcp.client.rag.DocumentIngestionException;
import ro.micronikgrupm.mcp.client.rag.DocumentNotFoundException;

/** Auto-generated documentation. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private static final String BAD_REQUEST = "BAD_REQUEST";
  private static final String NOT_FOUND = "NOT_FOUND";
  private static final String INTERNAL_ERROR = "INTERNAL_ERROR";

  @ExceptionHandler(EmployeeNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(EmployeeNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError(NOT_FOUND, ex.getMessage()));
  }

  /** Handles ExceptionHandler operation. */
  @ExceptionHandler(DocumentNotFoundException.class)
  public ResponseEntity<ApiError> handleDocumentNotFound(DocumentNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError(NOT_FOUND, ex.getMessage()));
  }

  @ExceptionHandler(InvalidSalaryRangeException.class)
  public ResponseEntity<ApiError> handleInvalidRange(InvalidSalaryRangeException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError(BAD_REQUEST, ex.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError(BAD_REQUEST, ex.getMessage()));
  }

  /** Handles ExceptionHandler operation. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .orElse("Date invalide");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(BAD_REQUEST, message));
  }

  /** Handles ExceptionHandler operation. */
  @ExceptionHandler(DocumentIngestionException.class)
  public ResponseEntity<ApiError> handleIngestionFailure(DocumentIngestionException ex) {
    if (log.isErrorEnabled()) {
      log.error("Ingestion failed: {}", ex.getMessage());
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ApiError(
                INTERNAL_ERROR,
                "Indexarea documentului a eșuat — verifică dacă modelul de "
                    + "embedding rulează în LM Studio."));
  }

  @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
  public ResponseEntity<ApiError> handleAuthentication(
      org.springframework.security.core.AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ApiError("UNAUTHORIZED", "Utilizator sau parolă incorectă"));
  }
}
