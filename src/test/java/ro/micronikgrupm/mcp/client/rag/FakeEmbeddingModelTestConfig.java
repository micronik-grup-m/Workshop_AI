package ro.micronikgrupm.mcp.client.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

@TestConfiguration
public class FakeEmbeddingModelTestConfig {

    private static final List<String> KEYWORDS = List.of("alpha", "beta", "gamma", "delta");
    public static final int DIMENSIONS = 768;

    @Bean
    @Primary
    public EmbeddingModel fakeEmbeddingModel() {
        return new EmbeddingModel() {

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = new ArrayList<>();
                List<String> inputs = request.getInstructions();
                for (int i = 0; i < inputs.size(); i++) {
                    embeddings.add(new Embedding(vectorFor(inputs.get(i)), i));
                }
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(Document document) {
                return vectorFor(document.getText());
            }

            private float[] vectorFor(String text) {
                float[] vector = new float[DIMENSIONS];
                String normalized = text == null ? "" : text.toLowerCase();
                for (int i = 0; i < KEYWORDS.size(); i++) {
                    vector[i] = normalized.contains(KEYWORDS.get(i)) ? 1f : 0f;
                }
                return vector;
            }
        };
    }
}
