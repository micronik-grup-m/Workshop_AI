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
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@SpringBootTest
@ActiveProfiles("docker")
@Import(FakeEmbeddingModelTestConfig.class)
class DocumentIngestionServiceTest {

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
    private RagDocumentRepository repository;

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
    void ingestsAndPersistsMetadata() throws Exception {
        byte[] pdfBytes = samplePdf("Alpha secret code is 12345.", "Beta deadline is March 2027.");
        MockMultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", pdfBytes);

        RagDocument saved = ingestionService.ingest(file);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFilename()).isEqualTo("sample.pdf");
        assertThat(saved.getPageCount()).isEqualTo(2);
        assertThat(saved.getChunkCount()).isGreaterThan(0);
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void rejectsNonPdfFile() {
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "hello".getBytes());

        assertThrows(IllegalArgumentException.class, () -> ingestionService.ingest(file));
    }

    @Test
    void deleteRemovesDocumentRow() throws Exception {
        byte[] pdfBytes = samplePdf("Gamma content for deletion test.");
        MockMultipartFile file = new MockMultipartFile("file", "delete-me.pdf", "application/pdf", pdfBytes);
        RagDocument saved = ingestionService.ingest(file);

        ingestionService.delete(saved.getId());

        assertThat(repository.findById(saved.getId())).isEmpty();
        assertThat(queryService.search("gamma", 5)).doesNotContain("delete-me.pdf");
    }

    @Test
    void deleteUnknownIdThrows() {
        assertThrows(DocumentNotFoundException.class, () -> ingestionService.delete(999_999L));
    }
}
