package ai.patterns.services;

import static ai.patterns.utils.Ansi.cyan;
import static ai.patterns.utils.Ansi.blue;
import static ai.patterns.utils.RAGUtils.augmentWithVectorDataList;
import static ai.patterns.utils.RAGUtils.formatVectorData;

import ai.patterns.base.AbstractBase;
import ai.patterns.dao.CapitalDataAccessDAO;
import ai.patterns.web.endpoints.ChatEndpoint.ChatOptions;
import ai.patterns.web.endpoints.ChatEndpoint.ChunkingType;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService extends AbstractBase {

  // with multiple models, AI framework starters are not yet configured for supporting multiple models
  private Environment env;
  private final ChatMemoryProvider chatMemoryProvider;
  private final CapitalDataAccessDAO dataAccess;

  public ChatService(Environment env,
                     ChatMemoryProvider chatMemoryProvider,
                     CapitalDataAccessDAO dataAccess) {
    this.env = env;
    this.chatMemoryProvider = chatMemoryProvider;
    this.dataAccess = dataAccess;
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

    System.out.println(blue("\n>>> FINAL RESPONSE REPORT:\n"));
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

    // augment with vector data if RAG is enabled
    List<Map<String, Object>> vectorDataList = augmentWithVectorDataList(userMessage, options, dataAccess);

    // format RAG data to send to LLM
    String additionalVectorData = vectorDataList.stream()
        .map(map -> Optional.ofNullable(map.get("chunk")))
        .filter(Optional::isPresent) //filter out optionals that are empty
        .map(Optional::get) //get the value from the optional.
        .map(Object::toString) //convert each object to string.
        .collect(Collectors.joining("\n"));

    // format sources to send back to UI
    String sources = formatVectorData(vectorDataList);

    // build message
    String finalUserMessage = PromptTemplate.from("""
        Here's the question from the user:
        <question>
        {{userMessage}}
        </question>
        
        Answer the question using the following information:
        <excerpts>
        {{contents}}
        </excerpts>
        
        Please add at the end of your answer, the following String as-is, for reference purposes:
        ---------------------
        SOURCES:
        
        {{sources}}
        
        ---------------------
        """).apply(Map.of(
        "userMessage", userMessage,
        "contents", additionalVectorData,
        "sources", sources
    )).toUserMessage().singleText();

    return assistant.stream(chatId, systemMessage, finalUserMessage)
        .doOnNext(System.out::print)
        .doOnComplete(() -> {
          System.out.println(blue("\n\n>>> STREAM COMPLETE")); // Indicate stream completion
        });
  }

  interface ChatAssistant {
    @SystemMessage(fromResource = "templates/chat-service-system.txt")
    String chat(@MemoryId String chatId, @V("systemMessage") String systemMessage, @UserMessage String userMessage);

    @SystemMessage(fromResource = "templates/chat-service-system.txt")
    Flux<String> stream(@MemoryId String chatId, @V("systemMessage") String systemMessage, @UserMessage String userMessage);
  }
}
