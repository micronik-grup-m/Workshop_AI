package ro.micronikgrupm.mcp.client.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EmployeeMcpToolsTest {

    @Autowired
    private EmployeeMcpTools tools;

    @Test
    void createsAndListsEmployee() {
        String created = tools.createEmployee("Maria", "Ionescu", "maria.ionescu@example.com",
                "Marketing", new BigDecimal("6000"), LocalDate.of(2024, 1, 15));

        assertThat(created).contains("Maria").contains("Ionescu");
        assertThat(tools.listEmployees(null)).contains("Maria");
    }

    @Test
    void createsEmployeeWithDefaultHireDateWhenOmitted() {
        String created = tools.createEmployee("Maria", "Ionescu", "maria.ionescu@example.com",
                "Marketing", new BigDecimal("6000"), null);

        assertThat(created).doesNotContain("null");
        assertThat(created).contains(LocalDate.now().toString());
        assertThat(tools.listEmployees(null)).contains("Maria").contains(LocalDate.now().toString());
    }

    @Test
    void getEmployeeReportsDescriptiveErrorWhenMissing() {
        String result = tools.getEmployee(999999L);

        assertThat(result).contains("Eroare").contains("999999");
    }

    @Test
    void searchBySalaryRangeReportsDescriptiveErrorWhenInvalid() {
        String result = tools.searchBySalaryRange(new BigDecimal("9000"), new BigDecimal("1000"));

        assertThat(result).contains("Eroare");
    }

    @Test
    void deleteEmployeeReportsDescriptiveErrorWhenMissing() {
        String result = tools.deleteEmployee(999999L);

        assertThat(result).contains("Eroare").contains("999999");
    }

    @Test
    void createEmployeeReportsDescriptiveErrorOnInvalidSalary() {
        String result = tools.createEmployee("Maria", "Ionescu", "maria.ionescu@example.com",
                "Marketing", new BigDecimal("-100"), LocalDate.of(2024, 1, 15));

        assertThat(result).contains("Eroare").contains("negativ");
    }
}
