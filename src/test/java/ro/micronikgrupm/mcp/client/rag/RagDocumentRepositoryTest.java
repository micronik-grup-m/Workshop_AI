package ro.micronikgrupm.mcp.client.rag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RagDocumentRepositoryTest {

    @Autowired
    private RagDocumentRepository repository;

    @Test
    void savesAndFindsById() {
        RagDocument document = new RagDocument();
        document.setFilename("test.pdf");
        document.setUploadedAt(Instant.now());
        document.setPageCount(2);
        document.setChunkCount(3);

        RagDocument saved = repository.save(document);

        assertThat(repository.findById(saved.getId())).isPresent();
        assertThat(repository.findById(saved.getId()).get().getFilename()).isEqualTo("test.pdf");
    }
}
