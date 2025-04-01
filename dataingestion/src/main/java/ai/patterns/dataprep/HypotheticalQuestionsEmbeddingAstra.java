package ai.patterns.dataprep;

import static ai.patterns.utils.Ansi.blue;
import static ai.patterns.utils.Ansi.bold;
import static ai.patterns.utils.Ansi.cyan;
import static ai.patterns.utils.Ansi.green;
import static ai.patterns.utils.Ansi.italic;
import static ai.patterns.utils.Ansi.yellow;

import ai.patterns.AbstractTest;
import ai.patterns.CapitalData;
import ai.patterns.utils.CapitalsFileLoader;
import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.Database;
import com.datastax.astra.client.model.SimilarityMetric;
import com.datastax.astra.langchain4j.store.embedding.AstraDbEmbeddingStore;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Type;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.segment.TextSegmentTransformer;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.IngestionResult;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HypotheticalQuestionsEmbeddingAstra extends AbstractTest {
    private static final String PARAGRAPH_METADATA_KEY = "paragraph";
    private static final String COLLECTION_NAME = "hypothetical_questions";
    private static final Database DATABASE = new DataAPIClient(ASTRA_TOKEN).getDatabase(ASTRA_API_ENDPOINT);

    private static boolean collectionExists() {
      return DATABASE.collectionExists(COLLECTION_NAME);
    }

    public static EmbeddingStore<TextSegment> getEmbeddingStore() {
      if (collectionExists()) {
        System.out.println(cyan("Collection already exists."));
        return new AstraDbEmbeddingStore(DATABASE.getCollection(COLLECTION_NAME));
      } else {
        System.out.println(cyan("Creating collection..."));
        return new AstraDbEmbeddingStore(DATABASE.createCollection(COLLECTION_NAME, 768, SimilarityMetric.COSINE));
      }
    }


  public static void main(String[] args) throws IOException {

        VertexAiEmbeddingModel embeddingModel = VertexAiEmbeddingModel.builder()
            .project(System.getenv("GCP_PROJECT_ID"))
            .endpoint(System.getenv("GCP_VERTEXAI_ENDPOINT"))
            .location(System.getenv("GCP_LOCATION"))
            .publisher("google")
            .modelName("text-embedding-005")
            .build();

        boolean loadData = ((args.length > 1) && args[1].equals("load")) ? true : false;

          // EmbeddingSearchResult<TextSegment> searchResults = getEmbeddingStore().search(
          //     EmbeddingSearchRequest.builder()
          //         .maxResults(1000)
          //         .queryEmbedding(embeddingModel.embed(args[0]).content())
          //         .build());
          //
          // // Check if any results actually contain the search term
          // boolean containsExactMatch = searchResults.matches().stream()
          //     .anyMatch(match -> {
          //       String text = match.embedded().text();
          //       String metadata = match.embedded().metadata().getString(PARAGRAPH_METADATA_KEY);
          //       boolean containsInText = text != null && text.contains(args[0]);
          //       boolean containsInMetadata = metadata != null && metadata.contains(args[0]);
          //
          //       if (containsInText || containsInMetadata) {
          //         System.out.println("Found match containing '" + args[0] + "': " +
          //             (containsInText ? "In text" : "In metadata: " + metadata));
          //         return true;
          //       }
          //       return false;
          //     });

        List<CapitalData> capitalsData = CapitalsFileLoader.loadFiles("txt");

        String capital = args[0];
        CapitalData worldCapital = capitalsData.stream()
            .filter(capitalData -> capitalData.capitalCity().city().equals(capital))
            .findFirst().get();

        String text = worldCapital.article().text();

        Document capitalDoc = Document.from(text);

        // InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore();

        VertexAiGeminiChatModel chatModel = VertexAiGeminiChatModel.builder()
            .project(System.getenv("GCP_PROJECT_ID"))
            .location(System.getenv("GCP_LOCATION"))
            .modelName("gemini-2.0-flash-001")
            .build();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
            .documentSplitter(DocumentSplitters.recursive(2000, 200))
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .textSegmentTransformer(new TextSegmentTransformer() {
                @Override
                public List<TextSegment> transformAll(List<TextSegment> segments) {
                    return segments.stream()
                        .parallel()
                        .flatMap(textSegment -> {
                            VertexAiGeminiChatModel questionModel = VertexAiGeminiChatModel.builder()
                                .project(System.getenv("GCP_PROJECT_ID"))
                                .location(System.getenv("GCP_LOCATION"))
                                .modelName("gemini-2.0-flash-001")
                                .responseSchema(Schema.newBuilder()
                                    .setType(Type.ARRAY)
                                    .setItems(Schema.newBuilder().setType(Type.STRING).build())
                                    .build())
                                .listeners(List.of(new ChatModelListener() {
                                    @Override
                                    public void onResponse(ChatModelResponseContext responseContext) {
                                        System.out.println(
                                            yellow(responseContext.chatRequest().messages().get(1).toString()) + "\n" +
                                            cyan(responseContext.chatResponse().aiMessage().text())
                                        );
                                    }
                                }))
                                .build();

                            interface QuestionGenerator {
                                @SystemMessage("""
                                    Suggest 20 clear questions whose answer could be given by the user provided text.
                                    Don't use pronouns, be explicit about the subjects and objects of the question.
                                    """)
                                List<String> generateQuestions(String text);
                            }

                            QuestionGenerator hypotheticalQuestions = AiServices.create(QuestionGenerator.class, questionModel);

                            return hypotheticalQuestions.generateQuestions(textSegment.text()).stream()
                                .map(question -> new TextSegment(
                                    question,
                                    new Metadata(Map.of(PARAGRAPH_METADATA_KEY, textSegment.text()))
                                ));
                        })
                        .toList();
                }

                @Override
                public TextSegment transform(TextSegment segment) {
                    return transformAll(Collections.singletonList(segment)).getFirst();
                }
            })
            .build();

        // don't attempt to reload data if set to "false", otherwise set it to "true"
        if(loadData){
          IngestionResult ingestionResult = ingestor.ingest(capitalDoc);

          System.out.println(
              cyan("ingested tokens: " + ingestionResult.tokenUsage().totalTokenCount()) + "\n");
        }

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
                .contentInjector((contents, userMessage) -> {
                    String excerpts = contents.stream()
                        .map(content ->
                            content
                                .textSegment()
                                .metadata()
                                .getString(PARAGRAPH_METADATA_KEY))
                        .collect(Collectors.joining("\n\n"));

                    return PromptTemplate.from("""
                        You are a helpful history and geography assistant knowing everything about the capitals of the world.
                        
                        Here's the question from the user:
                        <question>
                        {{userMessage}}
                        </question>
                        
                        Answer the question using the following information:
                        <excerpts>
                        {{contents}}
                        </excerpts>
                        """).apply(Map.of(
                        "userMessage", userMessage.singleText(),
                        "contents", excerpts
                    )).toUserMessage();
                })
                .build())
            .build();

        System.out.println("--- QUESTION ---\n");
        // String question = "Is Berlin located in Brandenburg state?";
        String question = String.format("How many inhabitants are there in %s?", capital);

        System.out.println(yellow(question) + "\n");

        Result<String> response = assistant.learnAboutCapitals(question);

        System.out.println("--- RESPONSE ---\n");

        System.out.println(green(response.content()));

        System.out.println("--- SOURCES ---");

        response.sources().forEach(content -> {
            System.out.println("\n- " + bold(blue(content.textSegment().text())));
            System.out.println(italic(content.textSegment().metadata().getString(PARAGRAPH_METADATA_KEY).toString()));
        });

    }
}
