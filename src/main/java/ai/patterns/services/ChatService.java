package ai.patterns.services;

import static com.datastax.astra.internal.utils.AnsiUtils.cyan;
import static com.datastax.astra.internal.utils.AnsiUtils.magenta;

import ai.patterns.base.AbstractBase;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class ChatService extends AbstractBase {
  // with multiple models, AI framework starters are not yet configured for supporting multiple models
  private Environment env;
  private final ChatMemoryProvider chatMemoryProvider;

  public ChatService(Environment env, ChatMemoryProvider chatMemoryProvider){
    this.env = env;
    this.chatMemoryProvider = chatMemoryProvider;
  }

  public String chat(String chatId,
                    String systemMessage,
                    String userMessage,
                    boolean useVertex,
                    String chatModel) {
    ChatService.ChatAssistant assistant = AiServices.builder(ChatService.ChatAssistant.class)
        .chatLanguageModel(getChatLanguageModel(chatModel))
        .chatMemoryProvider(chatMemoryProvider)
        .build();

    String report = assistant.chat(chatId, systemMessage, userMessage);

    System.out.println(magenta("\n>>> FINAL RESPONSE REPORT:\n"));
    System.out.println(cyan(report));

    return report;
  }

  interface ChatAssistant {
    @SystemMessage("""
            You are a knowledgeable history, geography and tourist assistant.
            Your role is to write reports about a particular location or event,
            focusing on the key topics asked by the user.
            
            {{systemMessage}}
            """)
    String chat(@MemoryId String chatId, @V("systemMessage") String systemMessage, @UserMessage String userMessage);
  }
}
