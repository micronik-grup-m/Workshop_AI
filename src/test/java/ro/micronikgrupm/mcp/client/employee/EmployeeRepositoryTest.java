package ro.micronikgrupm.mcp.client.employee;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository repository;

    private Employee employee(String department, String salary) {
        Employee e = new Employee();
        e.setFirstName("Maria");
        e.setLastName("Ionescu");
        e.setEmail("maria.ionescu@example.com");
        e.setDepartment(department);
        e.setSalary(new BigDecimal(salary));
        e.setHireDate(LocalDate.of(2024, 1, 15));
        return e;
    }

    @Test
    void savesAndFindsById() {
        Employee saved = repository.save(employee("Marketing", "6000"));

        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void findsByDepartment() {
        repository.save(employee("Marketing", "6000"));
        repository.save(employee("IT", "7000"));

        List<Employee> marketing = repository.findByDepartment("Marketing");

        assertThat(marketing).hasSize(1);
        assertThat(marketing.get(0).getDepartment()).isEqualTo("Marketing");
    }

    @Test
    void findsBySalaryBetween() {
        repository.save(employee("Marketing", "4000"));
        repository.save(employee("IT", "6000"));
        repository.save(employee("IT", "9000"));

        List<Employee> inRange = repository.findBySalaryBetween(new BigDecimal("5000"), new BigDecimal("7000"));

        assertThat(inRange).hasSize(1);
        assertThat(inRange.get(0).getSalary()).isEqualByComparingTo("6000");
    }
}
