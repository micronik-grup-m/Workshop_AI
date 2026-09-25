package ro.micronikgrupm.mcp.client.rag;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ro.micronikgrupm.mcp.client.security.JwtService;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("docker")
@Import(FakeEmbeddingModelTestConfig.class)
class DocumentControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("workshopai")
                    .withUsername("workshopai")
                    .withPassword("workshopai");

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

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

    private RequestPostProcessor asUser() {
        return request -> {
            request.addHeader("Authorization", "Bearer " + jwtService.generateToken("user", "USER"));
            return request;
        };
    }

    private byte[] samplePdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("Delta test content for controller test.");
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void adminUploadsListsAndDeletesDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "controller-test.pdf", "application/pdf", samplePdf());

        String uploadResponse = mockMvc.perform(multipart("/api/documents").file(file).with(asAdmin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("controller-test.pdf"))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(uploadResponse).get("id").asLong();

        mockMvc.perform(get("/api/documents").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("controller-test.pdf"));

        mockMvc.perform(delete("/api/documents/" + id).with(asAdmin()))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonAdminCannotUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "forbidden.pdf", "application/pdf", samplePdf());

        mockMvc.perform(multipart("/api/documents").file(file).with(asUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteMissingDocumentReturns404() throws Exception {
        mockMvc.perform(delete("/api/documents/999999").with(asAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
