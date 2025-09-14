/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.patterns.services;

import static ai.patterns.utils.Ansi.*;
import static ai.patterns.utils.Models.RERANKING_SCORE_THRESHOLD;
import static ai.patterns.utils.RAGUtils.*;

import ai.patterns.base.AbstractBase;
import ai.patterns.dao.CapitalDataAccessDAO;
import ai.patterns.tools.CurrencyManagerTool;
import ai.patterns.tools.WeatherForecastMCPTool;
import ai.patterns.utils.ChatUtils.ChatOptions;
import com.google.cloud.language.v2.ClassificationCategory;
import com.google.cloud.language.v2.Document;
import com.google.cloud.language.v2.LanguageServiceClient;
import com.google.cloud.language.v2.ModerateTextResponse;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchRequest;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService extends AbstractBase {

  private final CapitalDataAccessDAO dataAccess;
  private final CurrencyManagerTool currencyManagerTool;
  private final WeatherForecastMCPTool weatherForecastMCPTool;
  private final Tracer tracer;

  private ChatAssistant assistant = null;
  private final Map<String, MessageWindowChatMemory> chatMemories = new ConcurrentHashMap<>();

  private ExecutorService executorService = Executors.newFixedThreadPool(5);

  public ChatService(CapitalDataAccessDAO dataAccess,
                     CurrencyManagerTool currencyManagerTool,
                     WeatherForecastMCPTool weatherForecastMCPTool,
                     Tracer tracer) {
    this.dataAccess = dataAccess;
    this.currencyManagerTool = currencyManagerTool;
    this.weatherForecastMCPTool = weatherForecastMCPTool;
    this.tracer = tracer;
  }

  public String chat(String chatId,
                    String systemMessage,
                    String userMessage,
                    String messageAttachments,
                    ChatOptions options) {
    Span span = tracer.spanBuilder("ChatService.chat")
        .setAttribute("chat.id", chatId)
        .setAttribute("chat.model", options.model())
        .setAttribute("tools.enabled", options.useTools())
        .startSpan();

    try (Scope scope = span.makeCurrent()) {
      span.addEvent("Building AI Assistant");
      AiServices<ChatService.ChatAssistant> builder = AiServices.builder(ChatService.ChatAssistant.class)
          .chatLanguageModel(getChatLanguageModel(options))
          // .tools(currencyManagerTool, weatherForecastMCPTool)
          .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10));

      if(options.useTools()) {
        span.addEvent("Adding tools");
        builder.tools(currencyManagerTool, weatherForecastMCPTool);
      }

      assistant = builder.build();

      span.addEvent("Calling AI Assistant");
      String report = assistant.chat(chatId, systemMessage, userMessage);

      span.setAttribute("response.length", report.length());
      System.out.println(blue("\n>>> FINAL RESPONSE REPORT:\n") + cyan(report));

      return report;
    } catch (Exception e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  public Flux<String> stream(String chatId,
                             String systemMessage,
                             String userMessage,
                             String messageAttachments,
                             ChatOptions options) {
    Span span = tracer.spanBuilder("ChatService.stream")
        .setAttribute("chat.id", chatId)
        .setAttribute("chat.model", options.model())
        .setAttribute("streaming", true)
        .setAttribute("rag.enabled", options.enableRAG())
        .setAttribute("tools.enabled", options.useTools())
        .setAttribute("guardrails.enabled", options.useGuardrails())
        .startSpan();

    try (Scope scope = span.makeCurrent()) {
      CompletableFuture<ModerateTextResponse> inputModerationFuture = null;
      if (options.useGuardrails()) {
        span.addEvent("Starting input moderation");
        inputModerationFuture = moderate(userMessage);
      }

    // Get or create chat memory for this specific chatId
    MessageWindowChatMemory chatMemory = chatMemories.computeIfAbsent(chatId,
        id -> MessageWindowChatMemory.withMaxMessages(10));

    // Add system message if provided and memory is empty
    if (systemMessage != null && !systemMessage.isEmpty() && chatMemory.messages().isEmpty()) {
      chatMemory.add(dev.langchain4j.data.message.SystemMessage.from(systemMessage));
    }

    // build an AIAssistant with a streaming model and memory
    AiServices<ChatService.ChatAssistant> builder = AiServices.builder(ChatService.ChatAssistant.class)
        .streamingChatLanguageModel(getChatLanguageModelStreaming(options))
        .chatMemoryProvider(memoryId -> chatMemories.getOrDefault(
                  memoryId,
                  MessageWindowChatMemory.withMaxMessages(10)));

        if(options.useTools())
          builder.tools(currencyManagerTool, weatherForecastMCPTool);

    assistant = builder.build();

    // collect execution steps
    List<String> steps = new ArrayList<>();

    // compress the query if required
    if(options.queryCompression()){
      span.addEvent("Starting query compression");
      steps.add("   1. Executing Query Compression for the Original Query: " +
          "_**" + userMessage.replaceAll("\n", " ").trim() + "**_");
      userMessage = compressQuery(chatId, userMessage, chatMemory, getChatLanguageModel(options))
          .replaceAll("\n", " ");

      System.out.println(blue("\n>>> COMPRESSED QUERY:\n") + cyan(userMessage));
      steps.add("   1. Generated the Compressed Query: _**" + userMessage.trim() + "**_");
      span.setAttribute("query.compressed", true);
    }

    // Hypothetical Document Embedding:
    // search for a hypothetical answer generated by the LLM
    // instead of searching for the original question
    if (options.hyde()) {
      span.addEvent("Starting HyDE (Hypothetical Document Embedding)");
      steps.add("   1. Collecting Hypothetical Answer from UserMessage: " +
          "_**" + userMessage.replaceAll("\n", " ").trim() + "**_");
      userMessage = hypotheticalAnswer(chatId, userMessage, chatMemory, getChatLanguageModel(options));

      System.out.println(blue("\n* HYPOTHETICAL ANSWER:\n") + cyan(userMessage));
      steps.add("   1. Generated Hypothetical Answer: _**" + userMessage.trim() + "**_");
      span.setAttribute("hyde.enabled", true);
    }

    // augment with vector data if RAG is enabled
    // no RAG? ok
    List<CapitalDataAccessDAO.CapitalChunkRow> capitalChunks = new ArrayList<>();
    String llmDataAugmentation = "";
    String sources = "";

    // Use query routing, to do an external web search
    // potentially in addition to RAG search
    if (options.queryRouting()) {
      TavilyWebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder()
          .apiKey(System.getenv("TAVILY_API_KEY"))
          .build();

      WebSearchResults searchResults = webSearchEngine.search(WebSearchRequest.builder()
          .searchTerms(userMessage)
          .build());

      steps.add("   1. Query Routing to web search; results: **" + searchResults.results().size() + "**");

      System.out.println(blue("\n>>> WEBSEARCH RESULTS:\n"));

      StringBuilder searchItem = new StringBuilder();
      searchResults.results().forEach(r -> {
        System.out.println(cyan(r.title()) + "\n" +
                green(URLDecoder.decode(r.url().toASCIIString())) + "\n" +
                r.snippet());

        searchItem.append(String.format("""
                * [%s](%s)
                    > %s
            """, r.title(), URLDecoder.decode(r.url().toASCIIString()), r.snippet()));
      });

      llmDataAugmentation += searchResults.results().stream()
          .map(WebSearchOrganicResult::snippet)
          .collect(Collectors.joining("\n"));

      sources += "* **Search results:**\n" + searchItem;
    }

    if (options.enableRAG()) {
      span.addEvent("Starting RAG retrieval");
      capitalChunks = augmentWithVectorDataList(userMessage,
                                                 options,
                                                 dataAccess);
      steps.add("   1. RAG retrieved items from datastore: **" + capitalChunks.size() + "**");
      span.setAttribute("rag.items.retrieved", capitalChunks.size());

      // use reranking if enabled
      if (options.reranking()) {
        span.addEvent("Starting reranking");
        System.out.println("\n" + blue(">>> RERANKING\n"));

        ScoringModel scoringModel = getScoringModel();

        List<TextSegment> contents = capitalChunks.stream()
            .map(capitalChunk ->
                new TextSegment(
                    capitalChunk.getContent(),
                    new Metadata(Map.of("id", capitalChunk.getChunkId()))
                )
            )
            .toList();

        // score all chunks
        Response<List<Double>> scoredCapitalChunks = scoringModel.scoreAll(contents, userMessage);

        for (int i = 0; i < scoredCapitalChunks.content().size(); i++) {
          capitalChunks.get(i).setRerankingScore(scoredCapitalChunks.content().get(i));
          System.out.println("- " + capitalChunks.get(i).getRerankingScore() + " — " + cyan(capitalChunks.get(i).getContent()));
        }

        // Keep only chunks with a reranking score above 0.6
        capitalChunks = capitalChunks.stream()
            .filter(capitalChunkRow -> capitalChunkRow.getRerankingScore() > RERANKING_SCORE_THRESHOLD)
            .toList();

        steps.add("   1. Reranking process retained data items retrieved via RAG: **" + capitalChunks.size() + "**");
      }

      // format RAG data to send to LLM
      llmDataAugmentation += capitalChunks.stream()
          .map(CapitalDataAccessDAO.CapitalChunkRow::getChunk)
          .collect(Collectors.joining("\n"));

      // format sources in returnable format
      sources += formatVectorSearchResults(capitalChunks);
    }

    // only add the execution steps if there's actually at least one step to be displayed
    if (!steps.isEmpty()) {
      steps.addFirst("* **Execution steps:**");
    }

    //  prepare final UserMessage including original UserMessage, attachments, vector data (if available)
    String finalUserMessage = prepareUserMessage(userMessage,
        messageAttachments,
        llmDataAugmentation,
        sources,
        String.join("\n", steps),
        options.showDataSources());

      String moderationReasons = "";
      if (inputModerationFuture != null) {
        try {
          moderationReasons = inputModerationFuture.get().getModerationCategoriesList().stream()
            .filter(classificationCategory -> classificationCategory.getConfidence() > 0.8)
            .map(ClassificationCategory::getName)
            .collect(Collectors.joining(", "));
        } catch (InterruptedException | ExecutionException e) {
          throw new RuntimeException(e);
        }
      }

      if (options.useGuardrails() && !moderationReasons.isEmpty()) {
        System.out.println(red(">>> FLAGGED: " + moderationReasons));
        span.addEvent("Message flagged by guardrails");
        span.setAttribute("guardrails.flagged", true);
        span.setAttribute("guardrails.reasons", moderationReasons);
        span.end();

        return Flux.just(
            "❌ Your message was flagged for the following reasons: **" +
            moderationReasons.trim() + "**");
      } else {
        span.addEvent("Starting AI stream");
        return assistant.stream(chatId, systemMessage, finalUserMessage)
          .doOnNext(System.out::print)
          .doOnComplete(() -> {
            span.addEvent("Stream completed");
            span.end();
            System.out.println(blue("\n\n>>> STREAM COMPLETE")); // Indicate stream completion
          })
          .doOnError(e -> {
            span.recordException(e);
            span.end();
          });
      }
    } catch (Exception e) {
      span.recordException(e);
      span.end();
      throw e;
    }
  }

  private static CompletableFuture<ModerateTextResponse> moderate(String msgToModerate) {
    System.out.println(blue("\n>>> REQUESTING INPUT MODERATION:\n"));

    return CompletableFuture.supplyAsync(() -> {
      try (LanguageServiceClient language = LanguageServiceClient.create()) {
        Document doc = Document.newBuilder()
            .setContent(msgToModerate)
            .setType(Document.Type.PLAIN_TEXT)
            .build();

        return language.moderateText(doc);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  interface ChatAssistant {
    @SystemMessage(fromResource = "templates/chat-service-system.txt")
    String chat(@MemoryId String chatId, @V("systemMessage") String systemMessage, @UserMessage String userMessage);

    @SystemMessage(fromResource = "templates/chat-service-system.txt")
    Flux<String> stream(@MemoryId String chatId, @V("systemMessage") String systemMessage, @UserMessage String userMessage);
  }
}
