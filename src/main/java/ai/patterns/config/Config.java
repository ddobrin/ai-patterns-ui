package ai.patterns.config;

import static ai.patterns.utils.Ansi.cyan;

import ai.patterns.base.AbstractBase;
import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.Database;
import com.datastax.astra.client.model.SimilarityMetric;
import com.datastax.astra.langchain4j.store.embedding.AstraDbEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config extends AbstractBase {
  private static final String COLLECTION_NAME = "hypothetical_questions";
  private static final Database DATABASE = new DataAPIClient(ASTRA_TOKEN).getDatabase(ASTRA_API_ENDPOINT);
  public static final String PARAGRAPH_METADATA_KEY = "paragraph";

  private static boolean collectionExists() {
    return DATABASE.collectionExists(COLLECTION_NAME);
  }

  @Bean
  public EmbeddingStore<TextSegment> embeddingStore() {
    if (collectionExists()) {
      System.out.println(cyan("Collection exists in the database: " + COLLECTION_NAME));
      return new AstraDbEmbeddingStore(DATABASE.getCollection(COLLECTION_NAME));
    } else {
      System.out.println(cyan("Creating collection..."));
      return new AstraDbEmbeddingStore(DATABASE.createCollection(COLLECTION_NAME, 768, SimilarityMetric.COSINE));
    }
  }

  @Bean
  ChatMemoryProvider chatMemoryProvider() {
    // return chatId -> TokenWindowChatMemory.withMaxTokens(1000, tokenizer);
    return chatId -> MessageWindowChatMemory.withMaxMessages(10000);
  }
}
