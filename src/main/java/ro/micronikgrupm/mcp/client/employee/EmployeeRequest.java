package ro.micronikgrupm.mcp.client.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/** EmployeeRequest component. */
public record EmployeeRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank @Email String email,
    @NotBlank String department,
    @NotNull @PositiveOrZero BigDecimal salary,
    @NotNull LocalDate hireDate) {}
