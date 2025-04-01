package ai.patterns.dbingestion;

import static com.datastax.astra.internal.utils.AnsiUtils.yellow;

import ai.patterns.CapitalData;
import ai.patterns.CapitalDataRAG;
import ai.patterns.CapitalDataRAG.EmbedType;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Type;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.vertexai.HarmCategory;
import dev.langchain4j.model.vertexai.SafetyThreshold;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ContextualRetrievalEmbeddingService {

  private static final String PARAGRAPH_METADATA_KEY = "paragraph";

  public List<CapitalDataRAG> generateContextualEmbeddingsForCapitals(CapitalData capital) {
    List<CapitalDataRAG> capitalDataRAGList = new ArrayList<>();

    String text = capital.article().text();

    // text files
    Document capitalDoc = Document.from(text);

    // chat model
    VertexAiGeminiChatModel geminiChatModel = VertexAiGeminiChatModel.builder()
        .project(System.getenv("GCP_PROJECT_ID"))
        .location(System.getenv("GCP_LOCATION"))
        .modelName("gemini-2.0-flash-001")
        .maxRetries(3)
        .safetySettings(Map.of(
            HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT, SafetyThreshold.BLOCK_NONE,
            HarmCategory.HARM_CATEGORY_HARASSMENT, SafetyThreshold.BLOCK_NONE,
            HarmCategory.HARM_CATEGORY_HATE_SPEECH, SafetyThreshold.BLOCK_NONE,
            HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT, SafetyThreshold.BLOCK_NONE
        ))
        // .responseSchema(Schema.newBuilder()
        //     .setType(Type.ARRAY)
        //     .setItems(Schema.newBuilder().setType(Type.STRING).build())
        //     .build())
        .build();

    interface ContextualGenerator {

      @UserMessage("""
          <document>
          {{wholeDocument}}
          </document>
          Here is the chunk we want to situate within the whole document
          <chunk>
          {{chunk}}
          </chunk>
          Please give a short succinct context to situate this chunk within the overall document \
          for the purposes of improving search retrieval of the chunk. \
          Answer only with the succinct context and nothing else.
          """)
      String generateContext(@V("chunk") String chunk, @V("wholeDocument") String wholeDocument);
    }

    // create AIService
    ContextualGenerator contextualGenerator = AiServices.create(
        ContextualGenerator.class,
        geminiChatModel);

    // split the text for a capital
    DocumentSplitter splitter = DocumentSplitters.recursive(2000, 200);
    List<TextSegment> textSegments = splitter.split(capitalDoc);

    // for (TextSegment segment : textSegments) {
    textSegments
        // .parallelStream()
        .forEach(segment -> {
          String contextualizedChunk = contextualGenerator
            .generateContext(segment.text(), text);

          System.out.println("\n" + "-".repeat(100));
          System.out.println(yellow("ORIGINAL:\n") + segment.text());
          System.out.println(yellow("\nCONTEXTUALIZED CHUNK:\n") + contextualizedChunk);
            capitalDataRAGList.add(new CapitalDataRAG(
                capital,
                EmbedType.CONTEXTUAL,
                List.of(contextualizedChunk),
                segment.text()));
          }
        );

    return capitalDataRAGList;
  }
}