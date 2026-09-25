package ro.micronikgrupm.mcp.client.rag;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression test for C1: a mid-ingestion {@code vectorStore.add()} failure must not leave an
 * orphaned {@code rag_document} row behind, now that {@link DocumentIngestionService#ingest} is
 * transactional.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("docker")
@Import({FakeEmbeddingModelTestConfig.class, DocumentIngestionServiceFailureTest.ThrowingVectorStoreTestConfig.class})
class DocumentIngestionServiceFailureTest {

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
    void failedVectorStoreAddRollsBackTheDocumentRow() throws Exception {
        long before = repository.count();
        byte[] pdfBytes = samplePdf("Delta content that will fail to index.");
        MockMultipartFile file =
                new MockMultipartFile("file", "boom.pdf", "application/pdf", pdfBytes);

        assertThrows(DocumentIngestionException.class, () -> ingestionService.ingest(file));

        assertThat(repository.count()).isEqualTo(before);
        assertThat(repository.findAll())
                .noneMatch(doc -> "boom.pdf".equals(doc.getFilename()));
    }

    @TestConfiguration
    static class ThrowingVectorStoreTestConfig {

        @Bean
        @Primary
        VectorStore throwingVectorStore() {
            return new VectorStore() {
                @Override
                public void add(List<Document> documents) {
                    throw new RuntimeException("simulated embedding failure");
                }

                @Override
                public void delete(List<String> idList) {
                    // not exercised by this test
                }

                @Override
                public void delete(Filter.Expression filterExpression) {
                    // not exercised by this test
                }

                @Override
                public List<Document> similaritySearch(SearchRequest request) {
                    return List.of();
                }
            };
        }
    }
}
