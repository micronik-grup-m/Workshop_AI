package ro.micronikgrupm.mcp.client.rag;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("docker")
@Import(FakeEmbeddingModelTestConfig.class)
class DocumentQueryServiceTest {

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
    private DocumentIngestionService ingestionService;

    @Autowired
    private DocumentQueryService queryService;

    private static byte[] samplePdf(String... pageTexts) throws Exception {
        try (PDDocument document = new PDDocument()) {
            for (String text : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(50, 700);
                    stream.showText(text);
                    stream.endText();
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void searchFindsRelevantChunkWithSourceAndPage() throws Exception {
        byte[] pdfBytes = samplePdf("Alpha secret code is 12345.", "Beta deadline is March 2027.");
        MockMultipartFile file = new MockMultipartFile("file", "query-test.pdf", "application/pdf", pdfBytes);
        ingestionService.ingest(file);

        String result = queryService.search("alpha", 5);
        String normalized = result.replaceAll("\\s+", " ");

        assertThat(normalized)
                .contains("query-test.pdf")
                .contains("Alpha secret code is 12345");
    }

    @Test
    void searchWithNoMatchesReturnsFriendlyMessage() {
        String result = queryService.search("nonexistent-keyword-xyz", 5);

        assertThat(result).isNotBlank();
    }

    @Test
    void listAllShowsUploadedDocument() throws Exception {
        byte[] pdfBytes = samplePdf("Delta content for listing test.");
        MockMultipartFile file = new MockMultipartFile("file", "list-test.pdf", "application/pdf", pdfBytes);
        RagDocument saved = ingestionService.ingest(file);

        String result = queryService.listAll();

        assertThat(result)
                .contains("list-test.pdf")
                .contains("id=" + saved.getId());
    }
}
