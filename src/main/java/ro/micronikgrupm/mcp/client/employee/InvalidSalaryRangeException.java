package ro.micronikgrupm.mcp.client.employee;

import java.io.Serial;

/** InvalidSalaryRangeException component. */
public class InvalidSalaryRangeException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  public InvalidSalaryRangeException(String message) {
    super(message);
  }
}
