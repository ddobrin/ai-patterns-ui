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
import ai.patterns.utils.ChatUtils;
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

  private ChatAssistant assistant = null;
  private final Map<String, MessageWindowChatMemory> chatMemories = new ConcurrentHashMap<>();

  private ExecutorService executorService = Executors.newFixedThreadPool(5);

  public ChatService(CapitalDataAccessDAO dataAccess,
                     CurrencyManagerTool currencyManagerTool,
                     WeatherForecastMCPTool weatherForecastMCPTool) {
    this.dataAccess = dataAccess;
    this.currencyManagerTool = currencyManagerTool;
    this.weatherForecastMCPTool = weatherForecastMCPTool;
  }

  public String chat(String chatId,
                    String systemMessage,
                    String userMessage,
                    String messageAttachments,
                    ChatOptions options) {
    if(assistant == null) {
      assistant = AiServices.builder(ChatService.ChatAssistant.class)
          .chatLanguageModel(getChatLanguageModel(options))
          .tools(currencyManagerTool, weatherForecastMCPTool)
          .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
          .build();
    }

    String report = assistant.chat(chatId, systemMessage, userMessage);

    System.out.println(blue("\n>>> FINAL RESPONSE REPORT:\n") + cyan(report));

    return report;
  }

  public Flux<String> stream(String chatId,
                             String systemMessage,
                             String userMessage,
                             String messageAttachments,
                             ChatOptions options) {

    CompletableFuture<ModerateTextResponse> inputModerationFuture = null;
    if (options.useGuardrails()) {
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
    assistant = AiServices.builder(ChatService.ChatAssistant.class)
        .streamingChatLanguageModel(getChatLanguageModelStreaming(options))
        .tools(currencyManagerTool, weatherForecastMCPTool)
        .chatMemoryProvider(memoryId -> chatMemories.getOrDefault(
                  memoryId,
                  MessageWindowChatMemory.withMaxMessages(10)))
        .build();

    // collect execution steps
    List<String> steps = new ArrayList<>();
    steps.add("* **Execution steps:**");

    // compress the query if required
    if(options.queryCompression()){
      steps.add("   1. Executing Query Compression for the Original Query: " +
          "_**" + userMessage.replaceAll("\n", " ").trim() + "**_");
      userMessage = compressQuery(chatId, userMessage, chatMemory, getChatLanguageModel(options))
          .replaceAll("\n", " ");

      System.out.println(blue("\n>>> COMPRESSED QUERY:\n") + cyan(userMessage));
      steps.add("   1. Generated the Compressed Query: _**" + userMessage.trim() + "**_");
    }

    // Hypothetical Document Embedding:
    // search for a hypothetical answer generated by the LLM
    // instead of searching for the original question
    if (options.hyde()) {
      steps.add("   1. Collecting Hypothetical Answer from UserMessage: " +
          "_**" + userMessage.replaceAll("\n", " ").trim() + "**_");
      userMessage = hypotheticalAnswer(chatId, userMessage, chatMemory, getChatLanguageModel(options));

      System.out.println(blue("\n* HYPOTHETICAL ANSWER:\n") + cyan(userMessage));
      steps.add("   1. Generated Hypothetical Answer: _**" + userMessage.trim() + "**_");
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
      capitalChunks = augmentWithVectorDataList(userMessage,
                                                 options,
                                                 dataAccess);
      steps.add("   1. RAG retrieved items from datastore: **" + capitalChunks.size() + "**");

      // use reranking if enabled
      if (options.reranking()) {
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
    if (steps.isEmpty()) {
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

        return Flux.just(
            "❌ Your message was flagged for the following reasons: **" +
            moderationReasons.trim() + "**");
      } else {
        return assistant.stream(chatId, systemMessage, finalUserMessage)
          .doOnNext(System.out::print)
          .doOnComplete(() -> {
            System.out.println(blue("\n\n>>> STREAM COMPLETE")); // Indicate stream completion
          });
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
