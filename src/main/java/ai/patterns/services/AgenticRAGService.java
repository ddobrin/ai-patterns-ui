package ai.patterns.services;

import static com.datastax.astra.internal.utils.AnsiUtils.cyan;
import static com.datastax.astra.internal.utils.AnsiUtils.magenta;

import ai.patterns.base.AbstractTest;
import ai.patterns.tools.HistoryGeographyTool;
import ai.patterns.tools.TouristBureauMCPTool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class AgenticRAGService extends AbstractTest {
  // with multiple models, AI framework starters are not yet configured for supporting multiple models
  private Environment env;
  private HistoryGeographyTool historyGeographyTool;
  private TouristBureauMCPTool touristBureauMCPTool;
  private final ChatMemoryProvider chatMemoryProvider;

  public AgenticRAGService(Environment env,
                           HistoryGeographyTool historyGeographyTool,
                           TouristBureauMCPTool touristBureauMCPTool,
                           ChatMemoryProvider chatMemoryProvider){
    this.env = env;
    this.historyGeographyTool = historyGeographyTool;
    this.touristBureauMCPTool = touristBureauMCPTool;
    this.chatMemoryProvider = chatMemoryProvider;
  }

  public String callAgent(String chatId,
                          String systemMessage,
                          String userMessage,
                          boolean useVertex,
                          String chatModel) {
    AgenticAssistant assistant = AiServices.builder(AgenticAssistant.class)
        .chatLanguageModel(getChatLanguageModel(chatModel))
        .tools(historyGeographyTool, touristBureauMCPTool)
        .chatMemoryProvider(chatMemoryProvider)
        .build();

    String report = assistant.chat(chatId, systemMessage, userMessage);

    System.out.println(magenta("\n>>> FINAL RESPONSE REPORT:\n"));
    System.out.println(cyan(report));

    return report;
  }

  interface AgenticAssistant {
    @SystemMessage("""
            You are a knowledgeable history, geography and tourist assistant.
            Your role is to write reports about a particular location or event,
            focusing on the key topics asked by the user.
            
            Think step by step:
            1) Identify the key topics the user is interested
            2) For each topic, devise a list of questions corresponding to those topics
            3) Search those questions in the database
            4) Collect all those answers together, and create the final report.
            {{systemMessage}}
            """)
    String chat(@MemoryId String chatId, @V("systemMessage") String systemMessage, @UserMessage String userMessage);
  }
}
