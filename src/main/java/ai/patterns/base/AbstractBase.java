package ai.patterns.base;

import static com.datastax.astra.client.model.SimilarityMetric.COSINE;
import static ai.patterns.utils.Ansi.cyan;
import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

import com.datastax.astra.client.Collection;
import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.Database;
import com.datastax.astra.client.model.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.model.vertexai.HarmCategory;
import dev.langchain4j.model.vertexai.SafetyThreshold;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.model.vertexai.VertexAiGeminiStreamingChatModel;
import dev.langchain4j.model.vertexai.VertexAiScoringModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract Class for different tests and use cases to share configuration
 */
public abstract class AbstractBase {

    // ------------------------------------------------------------
    //                           GEMINI STUFF
    // ------------------------------------------------------------

    // Chat Models
    protected final String MODEL_GEMINI_PRO       = "gemini-2.0-pro-exp-02-05";
    protected final String MODEL_GEMINI_FLASH     = "gemini-2.0-flash-001";
    protected final String MODEL_GEMINI_FLASH_LITE = "gemini-2.0-flash-lite-preview-02-05";
    protected final String MODEL_GEMINI_FLASH_THINKING = "gemini-2.0-flash-thinking-exp-01-21";

    // Embedding Models
    // https://cloud.google.com/vertex-ai/generative-ai/docs/model-reference/text-embeddings-api?hl=en&authuser=2
    protected final String MODEL_EMBEDDING_MULTILINGUAL = "text-multilingual-embedding-002";
    protected final String MODEL_EMBEDDING_TEXT         = "text-embedding-005";
    protected final int    MODEL_EMBEDDING_DIMENSION    = 768;

    /** Create a chat model. */
    protected ChatLanguageModel getChatLanguageModel(final String modelName) {
        return VertexAiGeminiChatModel.builder()
                .project(System.getenv("GCP_PROJECT_ID"))
                .location(System.getenv("GCP_LOCATION"))
                .modelName(modelName)
                .maxRetries(3)
                .safetySettings(Map.of(
                    HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT, SafetyThreshold.BLOCK_NONE,
                    HarmCategory.HARM_CATEGORY_HARASSMENT, SafetyThreshold.BLOCK_NONE,
                    HarmCategory.HARM_CATEGORY_HATE_SPEECH, SafetyThreshold.BLOCK_NONE,
                    HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT, SafetyThreshold.BLOCK_NONE
                ))
                .build();
    }

    /** Create a streaming chat model. */
    protected StreamingChatLanguageModel getChatLanguageModelStreaming(final String modelName) {
        return VertexAiGeminiStreamingChatModel.builder()
                .project(System.getenv("GCP_PROJECT_ID"))
                .location(System.getenv("GCP_LOCATION"))
                .modelName(modelName)
                .safetySettings(Map.of(
                    HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT, SafetyThreshold.BLOCK_NONE,
                    HarmCategory.HARM_CATEGORY_HARASSMENT, SafetyThreshold.BLOCK_NONE,
                    HarmCategory.HARM_CATEGORY_HATE_SPEECH, SafetyThreshold.BLOCK_NONE,
                    HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT, SafetyThreshold.BLOCK_NONE
                ))
                .build();
    }

    /** Create an embedding model. */
    protected EmbeddingModel getEmbeddingModel(final String modelName) {
        return VertexAiEmbeddingModel.builder()
                .project(System.getenv("GCP_PROJECT_ID"))
                .endpoint(System.getenv("GCP_VERTEXAI_ENDPOINT"))
                .location(System.getenv("GCP_LOCATION"))
                .publisher("google")
                .modelName(modelName)
                .maxSegmentsPerBatch(100)
                .maxRetries(5)
                .build();
    }

    // ------------------------------------------------------------
    //                ASTRA / CASSANDRA STORE STUFF
    // ------------------------------------------------------------

    public static final String ASTRA_TOKEN        = System.getenv("ASTRA_TOKEN");
    // public static final String ASTRA_API_ENDPOINT = "https://68614272-9c81-4dca-ac49-43d4d969bdd2-us-east1.apps.astra.datastax.com";
    public static final String ASTRA_API_ENDPOINT = "https://1839c746-6788-481d-8413-7b7d49fa9ea3-us-east1.apps.astra.datastax.com";

    public Database getAstraDatabase() {
        // verbose
        //return new DataAPIClient(ASTRA_TOKEN).getDatabase(ASTRA_API_ENDPOINT);
        return new DataAPIClient(ASTRA_TOKEN)
                //DataAPIOptions.builder().withObserver(new LoggingCommandObserver(AbstractBase.class)).build())
                .getDatabase(ASTRA_API_ENDPOINT);
    }

    public Collection<Document> createCollection(String name, int dimension) {
        return getAstraDatabase().createCollection(name, dimension, COSINE);
    }

    public Collection<Document> getCollection(String name) {
        return getAstraDatabase().getCollection(name);
    }

    // ------------------------------------------------------------
    //               RAG STUFF
    // ------------------------------------------------------------

    protected void ingestDocument(String docName, EmbeddingModel model, EmbeddingStore<TextSegment> store) {
        Path path = new File(Objects.requireNonNull(AbstractBase.class
                .getResource("/" + docName)).getFile()).toPath();
        dev.langchain4j.data.document.Document document = FileSystemDocumentLoader
                .loadDocument(path, new TextDocumentParser());
        DocumentSplitter splitter = DocumentSplitters
                .recursive(300, 20);

        EmbeddingStoreIngestor.builder()
                .documentSplitter(splitter)
                .embeddingModel(model)
                .embeddingStore(store).build().ingest(document);
    }

    protected ContentRetriever createRetriever(String fileName) {
        URL fileURL = getClass().getResource(fileName);
        Path path = new File(fileURL.getFile()).toPath();
        dev.langchain4j.data.document.Document document = FileSystemDocumentLoader
                .loadDocument(path, new TextDocumentParser());
        DocumentSplitter splitter = DocumentSplitters
                .recursive(300, 0);
        List<TextSegment> segments = splitter.split(document);
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(getEmbeddingModel(MODEL_EMBEDDING_TEXT).embedAll(segments).content(), segments);
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(getEmbeddingModel(MODEL_EMBEDDING_TEXT))
                .maxResults(2)
                .minScore(0.6)
                .build();
    }

    protected EmbeddingStoreContentRetriever.EmbeddingStoreContentRetrieverBuilder createRetrieverBuilder(String fileName) {
        List<TextSegment> segments = DocumentSplitters
                .recursive(300, 20)
                .split(loadDocument(new File(Objects.requireNonNull(getClass()
                                .getResource(fileName))
                        .getFile()).toPath(), new TextDocumentParser()));
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        embeddingStore.addAll(getEmbeddingModel(MODEL_EMBEDDING_TEXT).embedAll(segments).content(), segments);
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(getEmbeddingModel(MODEL_EMBEDDING_TEXT))
                .maxResults(2)
                .minScore(0.6);
    }

    protected ScoringModel getScoringModel() {
        return VertexAiScoringModel.builder()
            .projectId(System.getenv("GCP_PROJECT_ID"))
            .projectNumber(System.getenv("GCP_PROJECT_NUM"))
            .location(System.getenv("GCP_LOCATION"))
            .model("semantic-ranker-512")
            .build();
    }

    // ------------------------------------------------------------
    //            DISPLAY STUFF
    // ------------------------------------------------------------

    /**
     * Utilities function to show the results in the console
     *
     * @param response
     *      AI Response
     */
    protected static void prettyPrint(Response<AiMessage> response) {
        System.out.println(cyan("\nRESPONSE TEXT:"));
        System.out.println(response.content().text().replaceAll("\\n", "\n"));
        System.out.println();
        prettyPrintMetadata(response);
    }

    protected static void prettyPrintMetadata(Response<AiMessage> response) {
        System.out.println(cyan("\nRESPONSE METADATA:"));
        if (response.finishReason() != null) {
            System.out.println("Finish Reason : " + cyan(response.finishReason().toString()));
        }
        if (response.tokenUsage() != null) {
            System.out.println("Tokens Input  : " + cyan(String.valueOf(response.tokenUsage().inputTokenCount())));
            System.out.println("Tokens Output : " + cyan(String.valueOf(response.tokenUsage().outputTokenCount())));
            System.out.println("Tokens Total  : " + cyan(String.valueOf(response.tokenUsage().totalTokenCount())));
        }
    }

    public dev.langchain4j.data.document.Document loadDocumentText(String fileName) {
        Path path = new File(Objects.requireNonNull(getClass().getResource("/" + fileName)).getFile()).toPath();
        return FileSystemDocumentLoader.loadDocument(path, new TextDocumentParser());
    }

    // @SuppressWarnings("unchecked")
    // public  List<Quote> loadQuotes(String filePath) throws IOException {
    //     URL fileURL = getClass().getResource(filePath);
    //     File inputFile = new File(fileURL.getFile());
    //     LinkedHashMap<String, Object> sampleQuotes = new ObjectMapper().readValue(inputFile, LinkedHashMap.class);
    //     List<Quote> result  = new ArrayList<>();
    //     AtomicInteger quote_idx = new AtomicInteger(0);
    //     ((LinkedHashMap<?,?>) sampleQuotes.get("quotes")).forEach((k,v) -> {
    //         ((ArrayList<?>)v).forEach(q -> {
    //             Map<String, Object> entry = (Map<String,Object>) q;
    //             String author = (String) k;//(String) entry.get("author");
    //             String body = (String) entry.get("body");
    //             List<String> tags = (List<String>) entry.get("tags");
    //             String rowId = "q_" + author + "_" + quote_idx.getAndIncrement();
    //             result.add(new Quote(rowId, author, tags, body));
    //         });
    //     });
    //     return result;
    // }

}
