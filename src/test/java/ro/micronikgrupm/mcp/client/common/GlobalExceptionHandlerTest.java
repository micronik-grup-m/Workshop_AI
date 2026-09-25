package ro.micronikgrupm.mcp.client.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestController;

import ro.micronikgrupm.mcp.client.employee.EmployeeNotFoundException;
import ro.micronikgrupm.mcp.client.employee.InvalidSalaryRangeException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @RestController
    static class ThrowingController {
        @GetMapping("/test/not-found")
        void notFound() {
            throw new EmployeeNotFoundException(42L);
        }

        @GetMapping("/test/invalid-range")
        void invalidRange() {
            throw new InvalidSalaryRangeException("min mai mare decât max");
        }

        @GetMapping("/test/bad-argument")
        void badArgument() {
            throw new IllegalArgumentException("Email invalid: nope");
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void notFoundMapsTo404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Nu există niciun angajat cu id 42"));
    }

    @Test
    void invalidRangeMapsTo400() throws Exception {
        mockMvc.perform(get("/test/invalid-range"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void illegalArgumentMapsTo400() throws Exception {
        mockMvc.perform(get("/test/bad-argument").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email invalid: nope"));
    }

    @Test
    void validationExceptionUsesFirstFieldErrorMessage() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "trebuie să fie valid"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        var response = new GlobalExceptionHandler().handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ApiError("BAD_REQUEST", "email: trebuie să fie valid"));
    }

    @Test
    void authenticationExceptionMapsToUnauthorizedError() {
        var response = new GlobalExceptionHandler()
                .handleAuthentication(new BadCredentialsException("bad credentials"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody())
                .isEqualTo(new ApiError("UNAUTHORIZED", "Utilizator sau parolă incorectă"));
    }
}
