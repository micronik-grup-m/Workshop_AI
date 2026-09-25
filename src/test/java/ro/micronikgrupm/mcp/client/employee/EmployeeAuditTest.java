package ro.micronikgrupm.mcp.client.employee;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Unlike every other test in this project (which relies on @Transactional rollback), this test COMMITS
// its data to the shared H2 test database, because Envers only audits committed transactions; its
// @AfterEach cleanup is therefore load-bearing for other test classes' assertions (e.g. EmployeeRepositoryTest's
// department/salary-range queries), not just tidiness — if it ever stopped running, leftover rows here would
// silently corrupt unrelated tests.
@SpringBootTest
class EmployeeAuditTest {

    @Autowired
    private EmployeeRepository repository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Long createdId;

    @AfterEach
    void cleanUp() {
        // Guard with findById (rather than a bare deleteById) so cleanup is a no-op instead of throwing
        // EmptyResultDataAccessException if the row is already gone for any reason; combined with the
        // createdId null-check (skips cleanup entirely if the test failed before the employee was saved),
        // this ensures a cleanup problem never throws a second, unrelated exception on top of — and
        // potentially masking — an original test failure.
        if (createdId != null) {
            repository.findById(createdId).ifPresent(repository::delete);
        }
    }

    @Test
    void recordsARevisionOnEachChange() {
        Employee employee = new Employee();
        employee.setFirstName("Maria");
        employee.setLastName("Ionescu");
        employee.setEmail("maria.ionescu@example.com");
        employee.setDepartment("Marketing");
        employee.setSalary(new BigDecimal("6000"));
        employee.setHireDate(LocalDate.of(2024, 1, 15));
        employee = repository.saveAndFlush(employee);
        createdId = employee.getId();

        employee.setSalary(new BigDecimal("7000"));
        repository.saveAndFlush(employee);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            AuditReader auditReader = AuditReaderFactory.get(entityManager);
            List<Number> revisions = auditReader.getRevisions(Employee.class, employee.getId());

            assertThat(revisions).hasSize(2);

            Employee firstRevision = auditReader.find(Employee.class, employee.getId(), revisions.get(0));
            Employee secondRevision = auditReader.find(Employee.class, employee.getId(), revisions.get(1));

            assertThat(firstRevision.getSalary()).isEqualByComparingTo("6000");
            assertThat(secondRevision.getSalary()).isEqualByComparingTo("7000");
        } finally {
            entityManager.close();
        }
    }
}
