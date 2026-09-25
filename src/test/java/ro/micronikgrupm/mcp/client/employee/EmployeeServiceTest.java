package ro.micronikgrupm.mcp.client.employee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    private EmployeeService service;

    private EmployeeRequest validRequest;

    @BeforeEach
    void setUp() {
        service = new EmployeeService(repository);
        validRequest = new EmployeeRequest(
                "Maria", "Ionescu", "maria.ionescu@example.com",
                "Marketing", new BigDecimal("6000"), LocalDate.of(2024, 1, 15));
    }

    private Employee savedEmployee() {
        Employee e = new Employee();
        e.setFirstName(validRequest.firstName());
        e.setLastName(validRequest.lastName());
        e.setEmail(validRequest.email());
        e.setDepartment(validRequest.department());
        e.setSalary(validRequest.salary());
        e.setHireDate(validRequest.hireDate());
        // id is generated; set via reflection-free trick: not needed for id-less asserts
        return e;
    }

    @Test
    void createsEmployeeAndReturnsResponse() {
        when(repository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee arg = invocation.getArgument(0);
            return arg;
        });

        EmployeeResponse response = service.create(validRequest);

        assertThat(response.firstName()).isEqualTo("Maria");
        assertThat(response.email()).isEqualTo("maria.ionescu@example.com");
        verify(repository).save(any(Employee.class));
    }

    @Test
    void createRejectsNegativeSalary() {
        EmployeeRequest negative = new EmployeeRequest(
                "Maria", "Ionescu", "maria.ionescu@example.com",
                "Marketing", new BigDecimal("-100"), LocalDate.of(2024, 1, 15));

        assertThatThrownBy(() -> service.create(negative))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativ");

        verifyNoInteractions(repository);
    }

    @Test
    void createRejectsInvalidEmail() {
        EmployeeRequest badEmail = new EmployeeRequest(
                "Maria", "Ionescu", "not-an-email",
                "Marketing", new BigDecimal("6000"), LocalDate.of(2024, 1, 15));

        assertThatThrownBy(() -> service.create(badEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");

        verifyNoInteractions(repository);
    }

    @Test
    void getThrowsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void deleteThrowsWhenNotFound() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(EmployeeNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    @Test
    void searchBySalaryRangeRejectsMinGreaterThanMax() {
        BigDecimal minSalary = new BigDecimal("9000");
        BigDecimal maxSalary = new BigDecimal("5000");

        assertThatThrownBy(() -> service.searchBySalaryRange(minSalary, maxSalary))
                .isInstanceOf(InvalidSalaryRangeException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void searchBySalaryRangeDelegatesToRepository() {
        when(repository.findBySalaryBetween(new BigDecimal("5000"), new BigDecimal("7000")))
                .thenReturn(List.of(savedEmployee()));

        List<EmployeeResponse> result = service.searchBySalaryRange(new BigDecimal("5000"), new BigDecimal("7000"));

        assertThat(result).hasSize(1);
    }
}
