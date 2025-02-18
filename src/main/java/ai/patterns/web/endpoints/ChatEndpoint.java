package ai.patterns.web.endpoints;

import ai.patterns.services.AgenticRAGService;
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
        boolean useAgents,
        String model,
        boolean useGuardrails,
        boolean evaluateResponse,
        boolean useTools
    ) {
    }

    private final ChatService chatService;
    private final AgenticRAGService agenticRAGService;

    public ChatEndpoint(ChatService chatService, AgenticRAGService agenticRAGService) {
        this.chatService = chatService;
        this.agenticRAGService = agenticRAGService;
    }

    @Override
    public Flux<String> stream(String chatId, String userMessage, @Nullable ChatOptions options) {
        if (options == null) {
            options = new ChatOptions("", true, false, "gemini-2.0-flash-001", true, false, false);
        }

        if (options.useAgents()) {
            return agenticRAGService.stream(
                chatId,
                options.systemMessage(),
                userMessage,
                options);
        } else {
            return chatService.stream(
                chatId,
                options.systemMessage(),
                userMessage,
                options
            );
        }
    }

    // This demo does not use attachments or recalling message history

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
