package ai.patterns.web.endpoints;

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
public class ChatEndpoint implements AiChatService<ChatEndpoint.ChatOptions> {

    private static final String ATTACHMENT_TEMPLATE = """
        <attachment filename="%s">
                %s
        </attachment>
        """;
        
    // Map to store attachments by chatId
    private final Map<String, Map<String, String>> attachments = new HashMap<>();

    public enum ChunkingType {
        NONE,
        HIERARCHICAL,
        HYPOTHETICAL,
        CONTEXTUAL,
        LATE
    }

    public enum RetrievalType {
        NONE,
        FILTERING,
        QUERY_COMPRESSION,
        QUERY_ROUTING,
        HYDE,
        RERANKING,
    }

    public record ChatOptions(
        String systemMessage,
        boolean useVertex,
        boolean useAgents,
        boolean useWebsearch,
        String model,
        boolean enableSafety,
        boolean useGuardrails,
        boolean evaluateResponse,
        boolean enableRAG,
        ChunkingType chunkingType,
        RetrievalType retrievalType,
        boolean writeActions) {
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
            options = new ChatOptions("",
                true,
                false,
                false,
                 Models.MODEL_GEMINI_FLASH,
                true,
                true,
                false,
                false,
                ChunkingType.NONE,
                RetrievalType.NONE,
                false);
        }
        
        // Append attachments to the user message if any exist for this chat
        String messageWithAttachments = userMessage;
        Map<String, String> chatAttachments = attachments.get(chatId);
        if (chatAttachments != null && !chatAttachments.isEmpty()) {
            StringBuilder messageBuilder = new StringBuilder(userMessage);
            
            // Append each attachment using the template
            for (Map.Entry<String, String> entry : chatAttachments.entrySet()) {
                String filename = entry.getKey();
                String content = entry.getValue();
                messageBuilder.append("\n\n").append(String.format(ATTACHMENT_TEMPLATE, filename, content));
            }
            
            messageWithAttachments = messageBuilder.toString();
            
            // Clear attachments after appending them
            attachments.remove(chatId);
        }

        // Use the final message with attachments
        final String finalMessage = messageWithAttachments;

        // follow separate streams for chat, respectively agents
        if (options.useAgents()) {
            return agenticRAGService.stream(
                chatId,
                options.systemMessage(),
                finalMessage,
                options);
        } else {
            return chatService.stream(
                chatId,
                options.systemMessage(),
                finalMessage,
                options
            );
        }
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
