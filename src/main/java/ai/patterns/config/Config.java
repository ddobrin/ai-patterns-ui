package ai.patterns.config;

import ai.patterns.base.AbstractBase;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config extends AbstractBase {
  @Bean
  ChatMemoryProvider chatMemoryProvider() {
    return chatId -> MessageWindowChatMemory.withMaxMessages(10000);
  }
}
