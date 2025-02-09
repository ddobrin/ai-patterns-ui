package ai.patterns.fw.langchain4j;

import static com.datastax.astra.internal.utils.AnsiUtils.cyan;

import ai.patterns.base.AbstractTest;
import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.Database;
import com.datastax.astra.client.model.SimilarityMetric;
import com.datastax.astra.langchain4j.store.embedding.AstraDbEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config extends AbstractTest {
  private static final String COLLECTION_NAME = "berlin_hypothetical_questions";
  private static final Database DATABASE = new DataAPIClient(ASTRA_TOKEN).getDatabase(ASTRA_API_ENDPOINT);

  private static boolean collectionExists() {
    return DATABASE.collectionExists(COLLECTION_NAME);
  }

  @Bean
  public EmbeddingStore<TextSegment> embeddingStore() {
    if (collectionExists()) {
      System.out.println(cyan("Collection exists in the database)" + COLLECTION_NAME));
      return new AstraDbEmbeddingStore(DATABASE.getCollection(COLLECTION_NAME));
    } else {
      System.out.println(cyan("Creating collection..."));
      return new AstraDbEmbeddingStore(DATABASE.createCollection(COLLECTION_NAME, 768, SimilarityMetric.COSINE));
    }
  }
}
