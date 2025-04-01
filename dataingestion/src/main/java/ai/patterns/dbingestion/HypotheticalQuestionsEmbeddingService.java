package ai.patterns.dbingestion;

import static com.datastax.astra.internal.utils.AnsiUtils.yellow;

import ai.patterns.CapitalData;
import ai.patterns.CapitalDataRAG;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Type;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.vertexai.VertexAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HypotheticalQuestionsEmbeddingService {

  private static final String PARAGRAPH_METADATA_KEY = "paragraph";

  public List<CapitalDataRAG> generateHypotheticalQuestionsForCapitals(CapitalData capital) {
    List<CapitalDataRAG> capitalDataRAGList = new ArrayList<>();

    String text = capital.article().text();

    // text files
    Document capitalDoc = Document.from(text);

    // chat model
    VertexAiGeminiChatModel questionModel = VertexAiGeminiChatModel.builder()
        .project(System.getenv("GCP_PROJECT_ID"))
        .location(System.getenv("GCP_LOCATION"))
        .modelName("gemini-2.0-flash-001")
        .responseSchema(Schema.newBuilder()
            .setType(Type.ARRAY)
            .setItems(Schema.newBuilder().setType(Type.STRING).build())
            .build())
        .build();

    interface QuestionGenerator {

      @SystemMessage("""
          Suggest 20 clear questions whose answer could be given by the user provided text.
          Don't use pronouns, be explicit about the subjects and objects of the question.
          """)
      List<String> generateQuestions(String text);
    }

    // create AIService
    QuestionGenerator hypotheticalQuestions = AiServices.create(QuestionGenerator.class,
        questionModel);

    // split the text for a capital
    DocumentSplitter splitter = DocumentSplitters.recursive(2000, 200);
    List<TextSegment> textSegments = splitter.split(capitalDoc);

    // for (TextSegment segment : textSegments) {
    textSegments.parallelStream()
        .forEach(segment -> {
            List<TextSegment> questions = hypotheticalQuestions.generateQuestions(segment.text())
                .stream()
                .map(question -> new TextSegment(
                    question,
                    new Metadata(Map.of(PARAGRAPH_METADATA_KEY, segment.text()))
                )).toList();

            List<String> questionList = new ArrayList<>();
            System.out.println(yellow("\nQUESTIONS:\n"));
            for (int i = 1; i < questions.size() - 1; i++) {
              String question = questions.get(i).text();
              System.out.println((i) + ") " + question);

              questionList.add(question);
            }

            capitalDataRAGList.add(new CapitalDataRAG(
                capital,
                CapitalDataRAG.EmbedType.HYPOTHETICAL,
                questionList,
                segment.text()));
          }
        );

    return capitalDataRAGList;
  }
}