package ai.patterns.services;

import static com.datastax.astra.internal.utils.AnsiUtils.cyan;
import static com.datastax.astra.internal.utils.AnsiUtils.magenta;

import ai.patterns.base.AbstractBase;
import ai.patterns.web.endpoints.ChatEndpoint.ChatOptions;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService extends AbstractBase {
  // with multiple models, AI framework starters are not yet configured for supporting multiple models
  private Environment env;
  private final ChatMemoryProvider chatMemoryProvider;

  public ChatService(Environment env,
                     ChatMemoryProvider chatMemoryProvider) {
    this.env = env;
    this.chatMemoryProvider = chatMemoryProvider;
  }

  public String chat(String chatId,
                    String systemMessage,
                    String userMessage,
                    ChatOptions options) {
    ChatService.ChatAssistant assistant = AiServices.builder(ChatService.ChatAssistant.class)
        .chatLanguageModel(getChatLanguageModel(options.model()))
        .chatMemoryProvider(chatMemoryProvider)
        .build();

    String report = assistant.chat(chatId, systemMessage, userMessage);

    System.out.println(magenta("\n>>> FINAL RESPONSE REPORT:\n"));
    System.out.println(cyan(report));

    return report;
  }

  public Flux<String> stream(String chatId,
                             String systemMessage,
                             String userMessage,
                             ChatOptions options) {
    ChatService.ChatAssistant assistant = AiServices.builder(ChatService.ChatAssistant.class)
        .streamingChatLanguageModel(getChatLanguageModelStreaming(options.model()))
        .chatMemoryProvider(chatMemoryProvider)
        .build();
        return assistant.stream(chatId, systemMessage, userMessage)
            .doOnNext(System.out::print)
            .doOnComplete(() -> {
              System.out.println(magenta("\n\n>>> STREAM COMPLETE")); // Indicate stream completion
            });
    }

  private static final String SYSTEM_MESSAGE = """
            You are a knowledgeable history, geography and tourist assistant.
            Your role is to write reports about a particular location or event,
            focusing on the key topics asked by the user.
            
            {{systemMessage}}
            """;
  interface ChatAssistant {
    @SystemMessage(SYSTEM_MESSAGE)
    String chat(@MemoryId String chatId, @V("systemMessage") String systemMessage, @UserMessage String userMessage);

    @SystemMessage(SYSTEM_MESSAGE)
    Flux<String> stream(@MemoryId String chatId, @V("systemMessage") String systemMessage, @UserMessage String userMessage);
  }
}
