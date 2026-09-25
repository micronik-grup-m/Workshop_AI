package ro.micronikgrupm.mcp.client.employee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import ro.micronikgrupm.mcp.client.security.JwtService;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class EmployeeControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private RequestPostProcessor asAdmin() {
        return request -> {
            request.addHeader("Authorization", "Bearer " + jwtService.generateToken("admin", "ADMIN"));
            return request;
        };
    }

    private EmployeeRequest sampleRequest() {
        return new EmployeeRequest("Maria", "Ionescu", "maria.ionescu@example.com",
                "Marketing", new BigDecimal("6000"), LocalDate.of(2024, 1, 15));
    }

    @Test
    void createsAndFetchesEmployee() throws Exception {
        String createResponse = mockMvc.perform(post("/api/employees")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Maria"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/employees/" + id)
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maria.ionescu@example.com"));
    }

    @Test
    void getMissingEmployeeReturns404() throws Exception {
        mockMvc.perform(get("/api/employees/999999")
                        .with(asAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void createWithBlankFirstNameReturns400() throws Exception {
        String body = """
                {"firstName":"","lastName":"Ionescu","email":"maria.ionescu@example.com",
                 "department":"Marketing","salary":6000,"hireDate":"2024-01-15"}
                """;

        mockMvc.perform(post("/api/employees")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesEmployee() throws Exception {
        String createResponse = mockMvc.perform(post("/api/employees")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        EmployeeRequest updated = new EmployeeRequest("Maria", "Popescu", "maria.popescu@example.com",
                "IT", new BigDecimal("7000"), LocalDate.of(2024, 2, 1));

        mockMvc.perform(put("/api/employees/" + id)
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Popescu"))
                .andExpect(jsonPath("$.department").value("IT"));
    }

    @Test
    void deletesEmployee() throws Exception {
        String createResponse = mockMvc.perform(post("/api/employees")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/api/employees/" + id)
                        .with(asAdmin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/employees/" + id)
                        .with(asAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchBySalaryRange() throws Exception {
        mockMvc.perform(post("/api/employees")
                .with(asAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())));

        mockMvc.perform(get("/api/employees/search").param("min", "5000").param("max", "7000")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].department").value("Marketing"));
    }
}
