package ro.micronikgrupm.mcp.client.employee;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/** Auto-generated documentation. */
@Service
public class EmployeeService {

  private final EmployeeRepository repository;

  public EmployeeService(EmployeeRepository repository) {
    this.repository = repository;
  }

  /** Handles list operation. */
  public List<EmployeeResponse> list(String department) {
    List<Employee> employees =
        (department == null || department.isBlank())
            ? repository.findAll()
            : repository.findByDepartment(department);
    return employees.stream().map(EmployeeResponse::from).toList();
  }

  public EmployeeResponse get(Long id) {
    return EmployeeResponse.from(findOrThrow(id));
  }

  /** Handles searchBySalaryRange operation. */
  public List<EmployeeResponse> searchBySalaryRange(BigDecimal min, BigDecimal max) {
    if (min.compareTo(max) > 0) {
      throw new InvalidSalaryRangeException(
          "Intervalul de salariu este invalid: min ("
              + min
              + ") este mai mare decât max ("
              + max
              + ")");
    }
    return repository.findBySalaryBetween(min, max).stream().map(EmployeeResponse::from).toList();
  }

  /** Handles create operation. */
  public EmployeeResponse create(EmployeeRequest request) {
    validate(request);
    Employee employee = new Employee();
    applyRequest(employee, request);
    return EmployeeResponse.from(repository.save(employee));
  }

  /** Handles update operation. */
  public EmployeeResponse update(Long id, EmployeeRequest request) {
    validate(request);
    Employee employee = findOrThrow(id);
    applyRequest(employee, request);
    return EmployeeResponse.from(repository.save(employee));
  }

  /** Handles delete operation. */
  public void delete(Long id) {
    if (!repository.existsById(id)) {
      throw new EmployeeNotFoundException(id);
    }
    repository.deleteById(id);
  }

  private Employee findOrThrow(Long id) {
    return repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
  }

  private void applyRequest(Employee employee, EmployeeRequest request) {
    employee.setFirstName(request.firstName());
    employee.setLastName(request.lastName());
    employee.setEmail(request.email());
    employee.setDepartment(request.department());
    employee.setSalary(request.salary());
    employee.setHireDate(request.hireDate());
  }

  private void validate(EmployeeRequest request) {
    if (request.salary() == null || request.salary().signum() < 0) {
      throw new IllegalArgumentException("Salariul nu poate fi negativ");
    }
    if (request.email() == null || !request.email().contains("@")) {
      throw new IllegalArgumentException("Email invalid: " + request.email());
    }
  }
}
