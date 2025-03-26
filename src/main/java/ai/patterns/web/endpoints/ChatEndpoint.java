package ai.patterns.web.endpoints;

import ai.patterns.utils.ChatUtils;
import ai.patterns.utils.ChatUtils.ChatOptions;
import ai.patterns.utils.Models;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;
import org.vaadin.components.experimental.chat.AiChatService;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.BrowserCallable;

import ai.patterns.services.AgenticRAGService;
import ai.patterns.services.ChatService;
import reactor.core.publisher.Flux;

@BrowserCallable
@AnonymousAllowed
public class ChatEndpoint implements AiChatService<ChatOptions> {

    private static final String ATTACHMENT_TEMPLATE = """
        Answer the user question using the information from the following attachment:
        <attachment filename="%s">
                %s
        </attachment>
        """;
        
    // Map to store attachments by chatId
    private final Map<String, Map<String, String>> attachments = new HashMap<>();

    private final ChatService chatService;
    private final AgenticRAGService agenticRAGService;

    public ChatEndpoint(ChatService chatService, AgenticRAGService agenticRAGService) {
        this.chatService = chatService;
        this.agenticRAGService = agenticRAGService;
    }

    @Override
    public Flux<String> stream(String chatId, String userMessage, @Nullable ChatOptions options) {
        // if chat options are not captured, set defaults
        if (options == null) {
            options = ChatUtils.getDefaultChatOptions(Models.MODEL_GEMINI_FLASH);
        }
        
        // Append attachments to the user message if any exist for this chat
        String messageAttachments = "";
        Map<String, String> chatAttachments = attachments.get(chatId);
        if (chatAttachments != null && !chatAttachments.isEmpty()) {
            StringBuilder messageBuilder = new StringBuilder();

            // Append each attachment using the template
            for (Map.Entry<String, String> entry : chatAttachments.entrySet()) {
                String filename = entry.getKey();
                String content = entry.getValue();
                messageBuilder.append("\n\n").append(String.format(ATTACHMENT_TEMPLATE, filename, content));
            }
            
            messageAttachments = messageBuilder.toString();

            // Clear attachments after appending them
            attachments.remove(chatId);
        }

        // follow separate streams for chat, respectively agents
        return options.useAgents()
            ? agenticRAGService.stream(chatId, options.systemMessage(), userMessage, messageAttachments, options)
            : chatService.stream(chatId, options.systemMessage(), userMessage, messageAttachments, options);
    }

    @Override
    public String uploadAttachment(String chatId, MultipartFile multipartFile) {
        String originalFilename = multipartFile.getOriginalFilename();
        
        // Validate file type (only txt and md files are supported)
        if (originalFilename == null || 
            !(originalFilename.endsWith(".txt") || originalFilename.endsWith(".md"))) {
            throw new IllegalArgumentException("Only .txt and .md files are supported");
        }
        
        try {
            // Read file content
            String content = new String(multipartFile.getBytes(), StandardCharsets.UTF_8);
            
            // Generate a unique ID for the attachment
            String attachmentId = UUID.randomUUID().toString();
            
            // Store the attachment
            attachments.computeIfAbsent(chatId, k -> new HashMap<>())
                      .put(originalFilename, content);
            
            return attachmentId;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read attachment", e);
        }
    }

    @Override
    public void removeAttachment(String chatId, String attachmentId) {
        Map<String, String> chatAttachments = attachments.get(chatId);
        if (chatAttachments != null) {
            chatAttachments.remove(attachmentId);
            
            // Remove the chat entry if there are no more attachments
            if (chatAttachments.isEmpty()) {
                attachments.remove(chatId);
            }
        }
    }

    @Override
    public List<Message> getHistory(String chatId) {
        return List.of();
    }

    @Override
    public void closeChat(String chatId) {
        // Clear all attachments for this chat when it's closed
        attachments.remove(chatId);
    }
}
