package ai.patterns.web;

import ai.patterns.actuator.StartupCheck;
import ai.patterns.services.AgenticRAGService;
import java.text.SimpleDateFormat;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgenticRAGController {
  private final AgenticRAGService agenticRAGService;
  
  public AgenticRAGController(AgenticRAGService agenticRAGService){
    this.agenticRAGService = agenticRAGService;
  }

  @PostMapping("/call-agent")
  public String callAgent(@RequestBody Map<String, Object> body) {
    String chatId = (String) body.get("chatId");
    String systemMessage = (String) body.get("systemMessage");
    String userMessage = (String) body.get("userMessage");
    boolean useVertex = Boolean.parseBoolean((String)body.get("useVertex"));
    String chatModel = (String) body.get("chatModel");

    return agenticRAGService.callAgent(chatId, systemMessage, userMessage, useVertex, chatModel);
  }

  @PostConstruct
  public void init() {
    System.out.println("AgenticRAGController: Post Construct Initializer: " +
        new SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis())));
    System.out.println("AgenticRAGController: - StartupCheck can be enabled");

    StartupCheck.up();
  }
}
