package ai.patterns.base;

import static ai.patterns.utils.Models.MODEL_EMBEDDING_TEXT;
import static ai.patterns.utils.Ansi.cyan;
import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

import ai.patterns.utils.ChatUtils.ChatOptions;
import ai.patterns.utils.Models;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
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
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Abstract Class for different tests and use cases to share configuration
 */
public abstract class AbstractBase {

    // ------------------------------------------------------------
    //                           GEMINI STUFF
    // ------------------------------------------------------------

    /** Create a chat model. */
    protected ChatLanguageModel getChatLanguageModel(final ChatOptions chatOptions) {
        return VertexAiGeminiChatModel.builder()
                .project(System.getenv("GCP_PROJECT_ID"))
                .location(System.getenv("GCP_LOCATION"))
                .modelName(chatOptions.model())
                .maxRetries(3)
                .safetySettings(chatOptions.enableSafety() ? Models.SAFETY_SETTINGS_ON : Models.SAFETY_SETTINGS_OFF)
                .build();
    }

    /** Create a streaming chat model. */
    protected StreamingChatLanguageModel getChatLanguageModelStreaming(final ChatOptions chatOptions) {
        return VertexAiGeminiStreamingChatModel.builder()
                .project(System.getenv("GCP_PROJECT_ID"))
                .location(System.getenv("GCP_LOCATION"))
                .modelName(chatOptions.model())
                .safetySettings(chatOptions.enableSafety() ? Models.SAFETY_SETTINGS_ON : Models.SAFETY_SETTINGS_OFF)
                .useGoogleSearch(chatOptions.useWebsearch())
                .build();
    }

    /** Create an Ollama chat model */
    protected ChatLanguageModel getChatLanguageModelOllama(final ChatOptions chatOptions) {
        return OllamaChatModel.builder()
            .baseUrl("https://ollama-gemma4b-360922367561.us-central1.run.app")
            .modelName(chatOptions.model())
            .maxRetries(3)
            .timeout(Duration.ofSeconds(30))
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
}
