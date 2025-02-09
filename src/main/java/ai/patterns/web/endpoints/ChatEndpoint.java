package ai.patterns.web.endpoints;

import ai.patterns.services.ChatService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;

@BrowserCallable
@AnonymousAllowed
public class ChatEndpoint {

  private final ChatService chatService;

  public ChatEndpoint(ChatService chatService) {
    this.chatService = chatService;
  }

  public String chat(String chatId,
      String systemMessage,
      String userMessage,
      boolean useVertex,
      String chatModel) {
    return chatService.chat(chatId, systemMessage, userMessage, useVertex, chatModel);
  }
}
