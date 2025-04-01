package ai.patterns;

import static ai.patterns.utils.Ansi.*;

import ai.patterns.utils.CapitalsFileLoader;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.segment.TextSegmentTransformer;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.IngestionResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChunkingWithWiderContext {

    public static void main(String[] args) throws IOException {
        List<CapitalData> capitalsData = CapitalsFileLoader.loadFiles("txt");

        capitalsData.stream().forEach(capitalData -> {
                System.out.println(yellow(capitalData.capitalCity().city()) + " — " +
                    green(capitalData.country().country()) + " " +
                    blue(capitalData.continents().toString())
                );
            }
        );

        CapitalData firstCapital = capitalsData.getFirst();
        String text = firstCapital.article().text();

        Document capitalDocument = Document.from(text);

        System.out.println("\n== CAPITAL DOCUMENT " + "=".repeat(30) + "\n");
        System.out.println(capitalDocument.text());

        VertexAiEmbeddingModel embeddingModel = VertexAiEmbeddingModel.builder()
            .project(System.getenv("GCP_PROJECT_ID"))
            .endpoint(System.getenv("GCP_VERTEXAI_ENDPOINT"))
            .location(System.getenv("GCP_LOCATION"))
            .publisher("google")
            .modelName("text-embedding-005")
            .build();

        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
            .documentSplitter(new DocumentBySentenceSplitter(200, 20))
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .textSegmentTransformer(SurroundingContextEnricher.textSegmentTransformer(2, 3))
            .build();

        IngestionResult ingestionResult = ingestor.ingest(capitalDocument);

        System.out.println("Total token count: " + green(ingestionResult.tokenUsage().totalTokenCount().toString()));

        VertexAiGeminiChatModel chatModel = VertexAiGeminiChatModel.builder()
            .project(System.getenv("GCP_PROJECT_ID"))
            .location(System.getenv("GCP_LOCATION"))
            .modelName("gemini-2.0-flash-001")
            .listeners(List.of(new ChatModelListener() {
                @Override
                public void onRequest(ChatModelRequestContext requestContext) {
                    System.out.println("\n== AUGMENTED PROMPT " + "=".repeat(30) + "\n");
                    System.out.println(yellow(requestContext.chatRequest().messages().getFirst().text()));
                }
            }))
            .build();

        interface CapitalsAssistant {
            Result<String> learnAboutCapitals(String query);
        }

        CapitalsAssistant assistant = AiServices.builder(CapitalsAssistant.class)
            .chatLanguageModel(chatModel)
            .retrievalAugmentor(DefaultRetrievalAugmentor.builder()
                .contentRetriever(EmbeddingStoreContentRetriever.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .maxResults(3)
                    .minScore(0.75)
                    .build())
                .contentInjector(SurroundingContextEnricher.contentInjector(PromptTemplate.from("""
                    You are a helpful history and geography assistant knowing everything about the capitals of the world.
                    
                    Here's the question from the user:
                    <question>
                    {{userMessage}}
                    </question>
                    
                    Answer the question using the following information:
                    <excerpts>
                    {{contents}}
                    </excerpts>
                    """)))
                .build())
            .build();

        Result<String> response = assistant.learnAboutCapitals("How many inhabitants live in the capital of Somaliland?");

        System.out.println("== RESPONSE " + "=".repeat(30) + "\n");
        System.out.println(green(response.content()));

        System.out.println("== SOURCES " + "=".repeat(30) + "\n");
        response.sources().forEach(src -> {
            System.out.println(" - " + blue(src.textSegment().text()) + "\n");
            System.out.println(bold("    surrounding context: ") +
                italic(src.textSegment().metadata().getString(SurroundingContextEnricher.METADATA_CONTEXT_KEY)) + "\n");
        });
    }

    private static class SurroundingContextEnricher {
        private static final String METADATA_CONTEXT_KEY = "Surrounding context";

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
