package ai.patterns.dbingestion;

import static com.datastax.astra.internal.utils.AnsiUtils.yellow;

import ai.patterns.CapitalData;
import ai.patterns.CapitalDataRAG;
import ai.patterns.CapitalDataRAG.EmbedType;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.segment.TextSegmentTransformer;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.vertexai.HarmCategory;
import dev.langchain4j.model.vertexai.SafetyThreshold;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;

@Service
public class HierarchicalEmbeddingService {

  private static final String METADATA_CONTEXT_KEY = "Surrounding context";

  public List<CapitalDataRAG> generateHierarchicalEmbeddingCapitals(CapitalData capital) {
    List<CapitalDataRAG> capitalDataRAGList = new ArrayList<>();

    String text = capital.article().text();

    // text files
    Document capitalDoc = Document.from(text);

    System.out.println("\n== CAPITAL DOCUMENT " + "=".repeat(30) + "\n");
    System.out.println(capitalDoc.text());

    // split the text for a capital
    DocumentSplitter splitter = new DocumentBySentenceSplitter(200, 20);
    List<TextSegment> textSegments = splitter.split(capitalDoc);

    TextSegmentTransformer hierarchicalTransformer = SurroundingContextEnricher.textSegmentTransformer(2,3);
    List<TextSegment> hierarchicalSegments = hierarchicalTransformer.transformAll(textSegments);

    hierarchicalSegments
        // .parallelStream()
        .forEach(segment -> {
          System.out.println("\n" + "-".repeat(100));
          System.out.println(yellow("ORIGINAL:\n") + segment.text());
          System.out.println(yellow("\nParent CHUNK:\n") + segment.metadata().getString(METADATA_CONTEXT_KEY));
            capitalDataRAGList.add(new CapitalDataRAG(
                capital,
                EmbedType.HIERARCHICAL,
                List.of(segment.text()),
                segment.metadata().getString(METADATA_CONTEXT_KEY)));
          }
        );

    return capitalDataRAGList;
  }

  private static class SurroundingContextEnricher {

    public static TextSegmentTransformer textSegmentTransformer(int nSegmentsBefore, int nSegmentsAfter) {
      return new TextSegmentTransformer() {
        @Override
        public TextSegment transform(TextSegment segment) {
          return transformAll(Collections.singletonList(segment)).getFirst();
        }

        @Override
        public List<TextSegment> transformAll(List<TextSegment> segments) {
          if (segments == null || segments.isEmpty()) {
            return Collections.emptyList();
          }

          List<TextSegment> list = new ArrayList<>();

          for (int i = 0; i < segments.size(); i++) {
            TextSegment textSegment = segments.get(i);

            String context = IntStream.rangeClosed(i - nSegmentsBefore, i + nSegmentsAfter)
                .filter(j -> j >= 0 && j < segments.size())
                .mapToObj(j -> segments.get(j).text())
                .collect(Collectors.joining(" "));

            Metadata metadata = new Metadata(textSegment.metadata().toMap());
            metadata.put(METADATA_CONTEXT_KEY, context);

            list.add(TextSegment.from(textSegment.text(), metadata));
          }

          return list;
        }
      };
    }

    public static ContentInjector contentInjector(PromptTemplate promptTemplate) {
      return (contents, userMessage) -> {
        String excerpts = contents.stream()
            .map(content ->
                content
                    .textSegment()
                    .metadata()
                    .getString(METADATA_CONTEXT_KEY))
            .collect(Collectors.joining("\n\n"));

        return promptTemplate.apply(Map.of(
            "userMessage", userMessage.singleText(),
            "contents", excerpts
        )).toUserMessage();
      };
    }
  }
}