package ai.patterns.web.endpoints;

import ai.patterns.services.ChatService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;
import org.vaadin.components.experimental.chat.AiChatService;
import reactor.core.publisher.Flux;

import java.util.List;

@BrowserCallable
@AnonymousAllowed
public class ChatEndpoint implements AiChatService<ChatEndpoint.ChatOptions> {

  public record ChatOptions(
      String systemMessage,
      boolean useVertex,
      String model
  ) {
  }

  private final ChatService chatService;

  public ChatEndpoint(ChatService chatService) {
    this.chatService = chatService;
  }

  @Override
  public Flux<String> stream(String chatId, String userMessage, @Nullable ChatOptions options) {
    if(options == null) {
      options = new ChatOptions("", false, "gemini-2.0-flash-001");
    }
    return chatService.stream(chatId, options.systemMessage(), userMessage, options.useVertex(), options.model());
  }

  @Override
  public String uploadAttachment(String chatId, MultipartFile multipartFile) {
    return "";
  }

  @Override
  public void removeAttachment(String chatId, String attachmentId) {
    // no-op
  }

  @Override
  public List<Message> getHistory(String chatId) {
    return List.of();
  }

  @Override
  public void closeChat(String s) {

  }
}
