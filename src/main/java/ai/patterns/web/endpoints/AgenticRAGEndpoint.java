package ai.patterns.web.endpoints;

import ai.patterns.services.AgenticRAGService;
import ai.patterns.web.endpoints.ChatEndpoint.ChatOptions;
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
      ChatOptions options){
    return agenticRAGService.callAgent(chatId, systemMessage, userMessage, options);
  }
}
