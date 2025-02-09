package ai.patterns.services;

import static com.datastax.astra.internal.utils.AnsiUtils.cyan;
import static com.datastax.astra.internal.utils.AnsiUtils.magenta;

import ai.patterns.base.AbstractTest;
import ai.patterns.tools.HistoryGeographyTool;
import ai.patterns.tools.TouristBureauMCPTool;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class AgenticRAGService extends AbstractTest {
  // with multiple models, AI framework starters are not yet configured for supporting multiple models
  private Environment env;
  private HistoryGeographyTool historyGeographyTool;
  private TouristBureauMCPTool touristBureauMCPTool;


  public AgenticRAGService(Environment env,
                           HistoryGeographyTool historyGeographyTool,
                           TouristBureauMCPTool touristBureauMCPTool){
    this.env = env;
    this.historyGeographyTool = historyGeographyTool;
    this.touristBureauMCPTool = touristBureauMCPTool;
  }

  public String callAgent(String chatId,
                        String systemMessage,
                        String userMessage,
                        boolean useVertex,
                        String chatModel) {
    AgenticAssistant assistant = AiServices.builder(AgenticAssistant.class)
        .chatLanguageModel(getChatLanguageModel(MODEL_GEMINI_FLASH))
        .tools(historyGeographyTool, touristBureauMCPTool)
        .build();

    String report = assistant.chat(
        "Write a report about the population of Berlin, its geographic situation, its historical origins, and find an article about the city in the FileSystem"
        //  "Write a report about the population of Berlin, and find an article about the city in the filesystem"
        // "Write a report about the population of Berlin, its geographic situation, its historical origins, and get a printable article about the city"
        // "Write a report about the cultural aspects of Berlin"
    );

    System.out.println(magenta("\n>>> FINAL RESPONSE REPORT:\n"));
    System.out.println(cyan(report));

    return report;
  }

  interface AgenticAssistant {
    @SystemMessage("""
            You are a knowledgeable history and geography assistant.
            Your role is to write reports about a particular location or event,
            focusing on the key topics asked by the user.
            
            Think step by step:
            1) Identify the key topics the user is interested
            2) For each topic, devise a list of questions corresponding to those topics
            3) Search those questions in the database
            4) Collect all those answers together, and create the final report.
            """)
    String chat(String userMessage);
  }
}
