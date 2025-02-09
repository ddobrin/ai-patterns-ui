package ai.patterns.web;

import ai.patterns.services.AgenticRAGService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgenticRAGController {
  private final AgenticRAGService agenticRAGService;
  
  public AgenticRAGController(AgenticRAGService agenticRAGService){
    this.agenticRAGService = agenticRAGService;
  }

  @PostMapping("/call-agent")
  public String  callAgent(String chatId,
                           String systemMessage,
                           String userMessage,
                           boolean useVertex,
                           String chatModel){
    return agenticRAGService.callAgent(chatId, systemMessage, userMessage, useVertex, chatModel);
  }
}
