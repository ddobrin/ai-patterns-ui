package ai.patterns.tools;

import static ai.patterns.config.Config.PARAGRAPH_METADATA_KEY;
import static ai.patterns.utils.Ansi.blue;
import static ai.patterns.utils.Ansi.cyan;
import static ai.patterns.utils.Ansi.yellow;
import static ai.patterns.utils.Models.MODEL_EMBEDDING_TEXT;
import static ai.patterns.utils.Models.MODEL_GEMINI_FLASH;

import ai.patterns.base.AbstractBase;
import ai.patterns.data.TopicReport;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class HistoryGeographyTool extends AbstractBase {

  private EmbeddingStore<TextSegment> embeddingStore;

  public HistoryGeographyTool(EmbeddingStore<TextSegment> embeddingStore){
    this.embeddingStore = embeddingStore;
  }

  interface TopicAssistant {
    @SystemMessage(fromResource = "templates/history-geography-tool-system.txt")
    Result<String> report(String subTopic);
  }

  @Tool("Search information in the database")
  TopicReport searchInformationInDatabase(String query) {
    System.out.println(blue(">>> Invoking `searchInformation` tool with query: ") + query);

    TopicAssistant topicAssistant = AiServices.builder(TopicAssistant.class)
        .chatLanguageModel(getChatLanguageModel(MODEL_GEMINI_FLASH))
        .retrievalAugmentor(DefaultRetrievalAugmentor.builder()
            .contentRetriever(EmbeddingStoreContentRetriever.builder()
                .embeddingModel(getEmbeddingModel(MODEL_EMBEDDING_TEXT))
                .embeddingStore(embeddingStore)
                .maxResults(3)
                .minScore(0.75)
                .build())
        .contentInjector((contents, userMessage) -> {
          String excerpts = contents.stream()
              .map(content ->
                  content
                      .textSegment()
                      .metadata()
                      .getString(PARAGRAPH_METADATA_KEY))
              .collect(Collectors.joining("\n\n"));

          return PromptTemplate.from("""
                        Here's the question from the user:
                        <question>
                        {{userMessage}}
                        </question>
                        
                        Answer the question using the following information:
                        <excerpts>
                        {{contents}}
                        </excerpts>
                        """).apply(Map.of(
              "userMessage", query,
              "contents", excerpts
          )).toUserMessage();
        })
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