package ro.micronikgrupm.mcp.client.mcp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import ro.micronikgrupm.mcp.client.employee.EmployeeNotFoundException;
import ro.micronikgrupm.mcp.client.employee.EmployeeRequest;
import ro.micronikgrupm.mcp.client.employee.EmployeeResponse;
import ro.micronikgrupm.mcp.client.employee.EmployeeService;
import ro.micronikgrupm.mcp.client.employee.InvalidSalaryRangeException;

/** Auto-generated documentation. */
@Component
public class EmployeeMcpTools {

  private static final String ERROR_PREFIX = "Eroare: ";
  private final EmployeeService service;

  public EmployeeMcpTools(EmployeeService service) {
    this.service = service;
  }

  /** Handles McpTool operation. */
  @McpTool(
      name = "listEmployees",
      description = "Listează angajați, opțional filtrați după departament")
  public String listEmployees(
      @McpToolParam(
              description = "Departamentul după care se filtrează (opțional)",
              required = false)
          String department) {
    List<EmployeeResponse> employees = service.list(department);
    if (employees.isEmpty()) {
      return "Niciun angajat găsit.";
    }
    return employees.stream().map(this::describe).reduce((a, b) -> a + "\n" + b).orElse("");
  }

  /** Handles McpTool operation. */
  @McpTool(name = "getEmployee", description = "Obține detaliile unui angajat după id")
  public String getEmployee(
      @McpToolParam(description = "Id-ul angajatului", required = true) Long id) {
    try {
      return describe(service.get(id));
    } catch (EmployeeNotFoundException e) {
      return ERROR_PREFIX + e.getMessage();
    }
  }

  /** Handles McpTool operation. */
  @McpTool(
      name = "searchBySalaryRange",
      description = "Caută angajați cu salariul într-un interval")
  public String searchBySalaryRange(
      @McpToolParam(description = "Salariul minim", required = true) BigDecimal min,
      @McpToolParam(description = "Salariul maxim", required = true) BigDecimal max) {
    try {
      List<EmployeeResponse> employees = service.searchBySalaryRange(min, max);
      if (employees.isEmpty()) {
        return "Niciun angajat găsit în intervalul de salariu dat.";
      }
      return employees.stream().map(this::describe).reduce((a, b) -> a + "\n" + b).orElse("");
    } catch (InvalidSalaryRangeException e) {
      return ERROR_PREFIX + e.getMessage();
    }
  }

  /** Handles McpTool operation. */
  @McpTool(name = "createEmployee", description = "Creează un angajat nou")
  public String createEmployee(
      @McpToolParam(description = "Prenume", required = true) String firstName,
      @McpToolParam(description = "Nume", required = true) String lastName,
      @McpToolParam(description = "Email", required = true) String email,
      @McpToolParam(description = "Departament", required = true) String department,
      @McpToolParam(description = "Salariu", required = true) BigDecimal salary,
      @McpToolParam(
              description = "Data angajării (AAAA-LL-ZZ) — opțional, implicit data curentă",
              required = false)
          LocalDate hireDate) {
    try {
      LocalDate effectiveHireDate = hireDate != null ? hireDate : LocalDate.now();
      EmployeeResponse created =
          service.create(
              new EmployeeRequest(
                  firstName, lastName, email, department, salary, effectiveHireDate));
      return "Angajat creat cu id " + created.id() + ": " + describe(created);
    } catch (IllegalArgumentException e) {
      return ERROR_PREFIX + e.getMessage();
    }
  }

  /** Handles McpTool operation. */
  @McpTool(name = "updateEmployee", description = "Actualizează un angajat existent")
  public String updateEmployee(
      @McpToolParam(description = "Id-ul angajatului", required = true) Long id,
      @McpToolParam(description = "Prenume", required = true) String firstName,
      @McpToolParam(description = "Nume", required = true) String lastName,
      @McpToolParam(description = "Email", required = true) String email,
      @McpToolParam(description = "Departament", required = true) String department,
      @McpToolParam(description = "Salariu", required = true) BigDecimal salary,
      @McpToolParam(description = "Data angajării (AAAA-LL-ZZ)", required = true)
          LocalDate hireDate) {
    try {
      EmployeeResponse updated =
          service.update(
              id, new EmployeeRequest(firstName, lastName, email, department, salary, hireDate));
      return "Angajat actualizat: " + describe(updated);
    } catch (EmployeeNotFoundException | IllegalArgumentException e) {
      return ERROR_PREFIX + e.getMessage();
    }
  }

  /** Handles McpTool operation. */
  @McpTool(name = "deleteEmployee", description = "Șterge un angajat după id")
  public String deleteEmployee(
      @McpToolParam(description = "Id-ul angajatului", required = true) Long id) {
    try {
      service.delete(id);
      return "Angajatul cu id " + id + " a fost șters.";
    } catch (EmployeeNotFoundException e) {
      return ERROR_PREFIX + e.getMessage();
    }
  }

  private String describe(EmployeeResponse e) {
    return "id=%d, %s %s, %s, departament=%s, salariu=%s, angajat la %s"
        .formatted(
            e.id(),
            e.firstName(),
            e.lastName(),
            e.email(),
            e.department(),
            e.salary(),
            e.hireDate());
  }
}
