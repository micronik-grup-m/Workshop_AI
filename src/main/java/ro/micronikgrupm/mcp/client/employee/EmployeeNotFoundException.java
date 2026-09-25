package ro.micronikgrupm.mcp.client.employee;

import java.io.Serial;

/** EmployeeNotFoundException component. */
public class EmployeeNotFoundException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  public EmployeeNotFoundException(Long id) {
    super("Nu există niciun angajat cu id " + id);
  }
}
