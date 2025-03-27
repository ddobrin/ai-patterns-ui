package ai.patterns.services;

import static ai.patterns.utils.Ansi.cyan;
import static ai.patterns.utils.Ansi.blue;

import ai.patterns.base.AbstractBase;
import ai.patterns.tools.HistoryGeographyTool;
import ai.patterns.tools.HistoryGeographyToolOllama;
import ai.patterns.tools.TouristBureauMCPTool;
import ai.patterns.utils.ChatUtils.ChatOptions;
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
public class AgenticRAGService extends AbstractBase {
  // with multiple models, AI framework starters are not yet configured for supporting multiple models
  private Environment env;
  private HistoryGeographyTool historyGeographyTool;
  private HistoryGeographyToolOllama historyGeographyToolOllama;
  private TouristBureauMCPTool touristBureauMCPTool;
  private final ChatMemoryProvider chatMemoryProvider;

  public AgenticRAGService(Environment env,
                           HistoryGeographyTool historyGeographyTool,
                           HistoryGeographyToolOllama historyGeographyToolOllama,
                           TouristBureauMCPTool touristBureauMCPTool,
                           ChatMemoryProvider chatMemoryProvider){
    this.env = env;
    this.historyGeographyTool = historyGeographyTool;
    this.historyGeographyToolOllama = historyGeographyToolOllama;
    this.touristBureauMCPTool = touristBureauMCPTool;
    this.chatMemoryProvider = chatMemoryProvider;
  }

  public String callAgent(String chatId,
                          String systemMessage,
                          String userMessage,
                          ChatOptions options) {
    AgenticAssistant assistant = AiServices.builder(AgenticAssistant.class)
        .chatLanguageModel(getChatLanguageModel(options))
        .tools(historyGeographyTool, touristBureauMCPTool)
        .chatMemoryProvider(chatMemoryProvider)
        .build();

    String report = assistant.chat(chatId, systemMessage, userMessage);

    System.out.println(blue("\n>>> FINAL RESPONSE REPORT:\n"));
    System.out.println(cyan(report));

    return report;
  }
  public Flux<String> stream(String chatId,
                        String systemMessage,
                        String userMessage,
                        String messageAttachments,
                        ChatOptions options) {
    // create AIAssistant with a streaming model and tools enabled
    AgenticAssistant assistant = AiServices.builder(AgenticAssistant.class)
        .streamingChatLanguageModel(getChatLanguageModelStreaming(options))
        .tools(historyGeographyTool, touristBureauMCPTool)
        .chatMemoryProvider(chatMemoryProvider)
        .build();

    return assistant.stream(chatId, systemMessage, userMessage)
        .doOnNext(System.out::print)
        .doOnComplete(() -> {
          System.out.println(blue("\n\n>>> STREAM COMPLETE")); // Indicate stream completion
        });

  }

  interface AgenticAssistant {
        @SystemMessage(fromResource = "templates/agentic-rag-service-system.txt")
        String chat(@MemoryId String chatId, @V("systemMessage") String systemMessage, @UserMessage String userMessage);

        @SystemMessage(fromResource = "templates/agentic-rag-service-system.txt")
        Flux<String> stream(@MemoryId String chatId, @V("systemMessage") String systemMessage, @UserMessage String userMessage);

  }
}
