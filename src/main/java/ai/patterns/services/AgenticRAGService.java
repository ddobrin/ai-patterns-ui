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

import static ai.patterns.utils.Ansi.cyan;
import static ai.patterns.utils.Ansi.blue;

import ai.patterns.base.AbstractBase;
import ai.patterns.tools.CurrencyManagerTool;
import ai.patterns.tools.HistoryGeographyTool;
import ai.patterns.tools.TouristBureauMCPTool;
import ai.patterns.tools.WeatherForecastMCPTool;
import ai.patterns.utils.ChatUtils.ChatOptions;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AgenticRAGService extends AbstractBase {
  // with multiple models, AI framework starters are not yet configured for supporting multiple models
  private Environment env;
  private HistoryGeographyTool historyGeographyTool;
  private TouristBureauMCPTool touristBureauMCPTool;
  private CurrencyManagerTool currencyManagerTool;
  private WeatherForecastMCPTool weatherForecastMCPTool;
  private final ChatMemoryProvider chatMemoryProvider;
  private final Tracer tracer;

  public AgenticRAGService(Environment env,
                           HistoryGeographyTool historyGeographyTool,
                           TouristBureauMCPTool touristBureauMCPTool,
                           CurrencyManagerTool currencyManagerTool,
                           WeatherForecastMCPTool weatherForecastMCPTool,
                           ChatMemoryProvider chatMemoryProvider,
                           Tracer tracer){
    this.env = env;
    this.historyGeographyTool = historyGeographyTool;
    this.touristBureauMCPTool = touristBureauMCPTool;
    this.currencyManagerTool = currencyManagerTool;
    this.weatherForecastMCPTool = weatherForecastMCPTool;
    this.chatMemoryProvider = chatMemoryProvider;
    this.tracer = tracer;
  }

  public String callAgent(String chatId,
                          String systemMessage,
                          String userMessage,
                          ChatOptions options) {
    Span span = tracer.spanBuilder("AgenticRAGService.callAgent")
        .setAttribute("chat.id", chatId)
        .setAttribute("chat.model", options.model())
        .startSpan();

    try (Scope scope = span.makeCurrent()) {
      span.addEvent("Building AI Assistant");
      AgenticAssistant assistant = AiServices.builder(AgenticAssistant.class)
          .chatLanguageModel(getChatLanguageModel(options))
          .tools(historyGeographyTool, touristBureauMCPTool, currencyManagerTool, weatherForecastMCPTool)
          .chatMemoryProvider(chatMemoryProvider)
          .build();

      span.addEvent("Calling AI Assistant");
      String report = assistant.chat(chatId, systemMessage, userMessage);

      span.setAttribute("response.length", report.length());
      System.out.println(blue("\n>>> FINAL RESPONSE REPORT:\n"));
      System.out.println(cyan(report));

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
    Span span = tracer.spanBuilder("AgenticRAGService.stream")
        .setAttribute("chat.id", chatId)
        .setAttribute("chat.model", options.model())
        .setAttribute("streaming", true)
        .startSpan();

    try (Scope scope = span.makeCurrent()) {
      span.addEvent("Building Streaming AI Assistant");
      // create AIAssistant with a streaming model and tools enabled
      AgenticAssistant assistant = AiServices.builder(AgenticAssistant.class)
          .streamingChatLanguageModel(getChatLanguageModelStreaming(options))
          .tools(historyGeographyTool, touristBureauMCPTool, currencyManagerTool, weatherForecastMCPTool)
          .chatMemoryProvider(chatMemoryProvider)
          .build();

      span.addEvent("Starting stream");
      return assistant.stream(chatId, systemMessage, userMessage)
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
    } catch (Exception e) {
      span.recordException(e);
      span.end();
      throw e;
    }
  }

  interface AgenticAssistant {
        @SystemMessage(fromResource = "templates/agentic-rag-service-system.txt")
        String chat(@MemoryId String chatId, @V("systemMessage") String systemMessage, @UserMessage String userMessage);

        @SystemMessage(fromResource = "templates/agentic-rag-service-system.txt")
        Flux<String> stream(@MemoryId String chatId, @V("systemMessage") String systemMessage, @UserMessage String userMessage);

  }
}
