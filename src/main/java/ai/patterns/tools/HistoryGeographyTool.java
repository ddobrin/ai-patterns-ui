package ai.patterns.tools;

import static com.datastax.astra.internal.utils.AnsiUtils.cyan;
import static com.datastax.astra.internal.utils.AnsiUtils.magenta;
import static com.datastax.astra.internal.utils.AnsiUtils.yellow;

import ai.patterns.base.AbstractBase;
import ai.patterns.data.TopicReport;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Component;

@Component
public class HistoryGeographyTool extends AbstractBase {

  private EmbeddingStore<TextSegment> embeddingStore;

  public HistoryGeographyTool(EmbeddingStore<TextSegment> embeddingStore){
    this.embeddingStore = embeddingStore;
  }

  interface TopicAssistant {
    @SystemMessage("""
                You are a knowledgeable history and geography assistant who knows how to succinctly summarize a topic.
                Summarize the information for the topic asked by the user.
                """)
    Result<String> report(String subTopic);
  }

  @Tool("Search information in the database")
  TopicReport searchInformationInDatabase(String query) {
    System.out.println(magenta(">>> Invoking `searchInformation` tool with query: ") + query);

    TopicAssistant topicAssistant = AiServices.builder(TopicAssistant.class)
        .chatLanguageModel(getChatLanguageModel(MODEL_GEMINI_FLASH))
        .contentRetriever(EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(getEmbeddingModel(MODEL_EMBEDDING_TEXT))
            .build())
        .build();

    Result<String> reportResult = topicAssistant.report(query);

    reportResult.sources().forEach(content -> {
      System.out.println(cyan("- Source: ") + content.textSegment().text());
    });
    System.out.println(yellow("\n-> Topic report: ") + reportResult.content().replaceAll("\\n", "\n"));

    return new TopicReport(query, reportResult.content());
  }
}