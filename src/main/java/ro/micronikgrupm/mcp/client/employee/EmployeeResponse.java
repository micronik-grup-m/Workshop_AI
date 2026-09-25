package ro.micronikgrupm.mcp.client.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

/** EmployeeResponse component. */
public record EmployeeResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    String department,
    BigDecimal salary,
    LocalDate hireDate) {
  static EmployeeResponse from(Employee e) {
    return new EmployeeResponse(
        e.getId(),
        e.getFirstName(),
        e.getLastName(),
        e.getEmail(),
        e.getDepartment(),
        e.getSalary(),
        e.getHireDate());
  }
}
