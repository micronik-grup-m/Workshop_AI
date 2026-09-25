package ro.micronikgrupm.mcp.client.employee;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** EmployeeRepository component. */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

  List<Employee> findByDepartment(String department);

  List<Employee> findBySalaryBetween(BigDecimal min, BigDecimal max);
}
