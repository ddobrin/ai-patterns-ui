package ai.patterns.tools;

import static ai.patterns.config.Config.PARAGRAPH_METADATA_KEY;
import static ai.patterns.utils.Ansi.blue;
import static ai.patterns.utils.Ansi.cyan;
import static ai.patterns.utils.Ansi.yellow;
import static ai.patterns.utils.Models.MODEL_EMBEDDING_TEXT;
import static ai.patterns.utils.Models.MODEL_GEMINI_FLASH;
import static ai.patterns.utils.RAGUtils.augmentWithVectorDataList;
import static ai.patterns.utils.RAGUtils.formatVectorData;
import static ai.patterns.utils.RAGUtils.prepareUserMessage;

import ai.patterns.base.AbstractBase;
import ai.patterns.dao.CapitalDataAccessDAO;
import ai.patterns.data.TopicReport;
import ai.patterns.services.ChatService;
import ai.patterns.web.endpoints.ChatEndpoint.ChunkingType;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class HistoryGeographyTool extends AbstractBase {

  private EmbeddingStore<TextSegment> embeddingStore;
  private final CapitalDataAccessDAO dataAccess;

  public HistoryGeographyTool(EmbeddingStore<TextSegment> embeddingStore,
                              CapitalDataAccessDAO dataAccess){
    this.embeddingStore = embeddingStore;
    this.dataAccess = dataAccess;
  }

  interface TopicAssistant {
    @SystemMessage(fromResource = "templates/history-geography-tool-system.txt")
    Result<String> report(String subTopic);
  }

  @Tool("Search information in the database")
  TopicReport searchInformationInDatabase(String query) {
    System.out.println(blue(">>> Invoking `searchInformation` tool with query: ") + query);

    TopicAssistant topicAssistant = AiServices.builder(TopicAssistant.class)
        .streamingChatLanguageModel(getChatLanguageModelStreaming(MODEL_GEMINI_FLASH))
        .build();

    // augment with vector data if RAG is enabled
    // no RAG? ok
    List<Map<String, Object>> vectorDataList = new ArrayList<>();
    String additionalVectorData = "";
    String sources = "";

    vectorDataList = augmentWithVectorDataList(query, ChunkingType.HYPOTHETICAL.name().toLowerCase(), dataAccess);

    // format RAG data to send to LLM
    additionalVectorData = vectorDataList.stream()
        .map(map -> Optional.ofNullable(map.get("chunk")))
        .filter(Optional::isPresent) //filter out optionals that are empty
        .map(Optional::get) //get the value from the optional.
        .map(Object::toString) //convert each object to string.
        .collect(Collectors.joining("\n"));

    // format sources in returnable format
    sources = formatVectorData(vectorDataList);

    //  prepare final UserMessage including original UserMessage, attachments, vector data (if available)
    String finalUserMessage = prepareUserMessage(query,
                            "",
                                                additionalVectorData,
                                                sources,
                                false);

    Result<String> reportResult = topicAssistant.report(finalUserMessage);

    reportResult.sources().forEach(content -> {
      System.out.println(cyan("- Source: ") + content.textSegment().text());
    });
    System.out.println(yellow("\n-> Topic report: ") + reportResult.content().replaceAll("\\n", "\n"));

    return new TopicReport(query, reportResult.content());
  }
}