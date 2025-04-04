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
package ai.patterns.tools;

import static ai.patterns.utils.Ansi.blue;
import static ai.patterns.utils.Ansi.yellow;
import static ai.patterns.utils.ChatUtils.ChunkingType.HYPOTHETICAL;
import static ai.patterns.utils.RAGUtils.augmentWithVectorDataList;
import static ai.patterns.utils.RAGUtils.formatVectorSearchResults;
import static ai.patterns.utils.RAGUtils.prepareUserMessage;

import ai.patterns.base.AbstractBase;
import ai.patterns.dao.CapitalDataAccessDAO;
import ai.patterns.data.TopicReport;
import ai.patterns.utils.ChatUtils;
import ai.patterns.utils.ChatUtils.ChatOptions;
import ai.patterns.utils.Models;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class HistoryGeographyToolOllama extends AbstractBase {

  private final CapitalDataAccessDAO dataAccess;

  public HistoryGeographyToolOllama(CapitalDataAccessDAO dataAccess){
    this.dataAccess = dataAccess;
  }

  interface TopicAssistant {
    @SystemMessage(fromResource = "templates/history-geography-tool-system.txt")
    Result<String> report(String subTopic);
  }

  @Tool("Search information in the database")
  TopicReport searchInformationInDatabase(String query) {
    System.out.println(blue(">>> Invoking `searchInformation` tool with query - uses Gemma3 model: ") + query);

    TopicAssistant topicAssistant = AiServices.builder(TopicAssistant.class)
        .chatLanguageModel(getChatLanguageModelOllama(ChatUtils.getDefaultChatOptions(Models.MODEL_GEMMA3_4B)))
        .build();

    // augment with vector data if RAG is enabled
    // no RAG? ok
    List<CapitalDataAccessDAO.CapitalChunkRow> vectorDataList = new ArrayList<>();
    String additionalVectorData = "";
    String sources = "";

    vectorDataList = augmentWithVectorDataList(
        query,
        new ChatOptions("",
            false,
            true,
            false,
            false,
            Models.MODEL_GEMMA3_4B,
            false,
            true,
            false,
            false,
             HYPOTHETICAL,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            "",
            ""
        ),
        dataAccess);

    // format RAG data to send to LLM
    additionalVectorData = vectorDataList.stream()
        .map(CapitalDataAccessDAO.CapitalChunkRow::getChunk)
        .collect(Collectors.joining("\n"));

    // format sources in returnable format
    sources = formatVectorSearchResults(vectorDataList);

    //  prepare final UserMessage including original UserMessage, attachments, vector data (if available)
    String finalUserMessage = prepareUserMessage(query,
                            "",
                                                additionalVectorData,
                                                sources,
                                false);

    long start = System.currentTimeMillis();
    Result<String> reportResult = topicAssistant.report(finalUserMessage);
    System.out.println("Call with Ollama and Gemma models(ms): " + (System.currentTimeMillis() - start));

    System.out.println(yellow("\n-> Topic report: ") + reportResult.content().replaceAll("\\n", "\n"));

    return new TopicReport(query, reportResult.content());
  }
}