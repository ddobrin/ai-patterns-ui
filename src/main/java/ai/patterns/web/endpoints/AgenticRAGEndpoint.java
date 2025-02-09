package ai.patterns.web.endpoints;

import ai.patterns.services.AgenticRAGService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.springframework.web.bind.annotation.PostMapping;

@BrowserCallable
@AnonymousAllowed
public class AgenticRAGEndpoint {
  private final AgenticRAGService agenticRAGService;

  public AgenticRAGEndpoint(AgenticRAGService agenticRAGService){
    this.agenticRAGService = agenticRAGService;
  }

  public String  callAgent(String chatId,
      String systemMessage,
      String userMessage,
      boolean useVertex,
      String chatModel){
    return agenticRAGService.callAgent(chatId, systemMessage, userMessage, useVertex, chatModel);
  }
}
