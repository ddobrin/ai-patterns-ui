package ai.patterns.web;

import ai.patterns.actuator.StartupCheck;
import ai.patterns.services.ChatService;
import ai.patterns.web.endpoints.ChatEndpoint.ChatOptions;
import java.text.SimpleDateFormat;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
  private final ChatService chatService;

  public ChatController(ChatService chatService){
    this.chatService = chatService;
  }

  @PostMapping("/chat")
  public String chat(@RequestBody Map<String, Object> body) {
    String chatId = (String) body.get("chatId");
    String systemMessage = (String) body.get("systemMessage");
    String userMessage = (String) body.get("userMessage");
    boolean useVertex = Boolean.parseBoolean((String)body.get("useVertex"));
    String chatModel = (String) body.get("chatModel");

    ChatOptions options = new ChatOptions("", true, false, "gemini-2.0-flash-001", true, false, false);
    return chatService.chat(chatId, systemMessage, userMessage, options);
  }

  @PostConstruct
  public void init() {
    System.out.println("ChatController: Post Construct Initializer: " +
        new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
    System.out.println("ChatController: - StartupCheck can be enabled");

    StartupCheck.up();
  }
}
