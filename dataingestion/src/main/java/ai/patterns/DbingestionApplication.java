package ai.patterns;

import ai.patterns.dbingestion.CapitalService;
import ai.patterns.dbingestion.ContextualRetrievalEmbeddingService;
import ai.patterns.dbingestion.HierarchicalEmbeddingService;
import ai.patterns.dbingestion.HypotheticalQuestionsEmbeddingService;
import ai.patterns.utils.CapitalsFileLoader;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.vertexai.VertexAiEmbeddingModel;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class DbingestionApplication {

  private EmbeddingModel embeddingModel;
  private CapitalService capitalService;
  private HypotheticalQuestionsEmbeddingService hypotheticalQuestionsEmbeddingService;
  private ContextualRetrievalEmbeddingService contextualRetrievalEmbeddingService;
  private HierarchicalEmbeddingService hierarchicalEmbeddingService;

  public static void main(String[] args) {
    SpringApplication.run(DbingestionApplication.class, args);
  }

  @Bean
  public ApplicationRunner commandLineRunner(
        CapitalService capitalService,
        EmbeddingModel embeddingModel,
        HypotheticalQuestionsEmbeddingService hypotheticalQuestionsEmbeddingService,
        ContextualRetrievalEmbeddingService contextualRetrievalEmbeddingService,
        HierarchicalEmbeddingService hierarchicalEmbeddingService,
        ApplicationContext context) {

    this.embeddingModel = embeddingModel;
    this.capitalService = capitalService;
    this.hypotheticalQuestionsEmbeddingService = hypotheticalQuestionsEmbeddingService;
    this.contextualRetrievalEmbeddingService = contextualRetrievalEmbeddingService;
    this.hierarchicalEmbeddingService = hierarchicalEmbeddingService;


    return args -> {
      try {
        System.out.println("Starting data ingestion process...");

        List<CapitalData> capitalsData = CapitalsFileLoader.loadFiles("txt");

        // filter a subset of capitals
        List<CapitalData> filteredCapitalsData = filterCapitals(capitalsData);

        filteredCapitalsData.forEach(capitalData -> {
          System.out.println("Processing capital: " + capitalData.capitalCity().city());
          System.out.println("Current date and time: " + LocalDateTime.now());

          //***********************************************************
          // wire in a different chunking method, rest remains the same
          //***********************************************************
          List<CapitalDataRAG> capitalDataRAGList = hierarchicalEmbeddingService.generateHierarchicalEmbeddingCapitals(
              capitalData);
          // List<CapitalDataRAG> capitalDataRAGList = contextualRetrievalEmbeddingService.generateContextualEmbeddingsForCapitals(
          //     capitalData);

          // List<CapitalDataRAG> capitalDataRAGList = hypotheticalQuestionsEmbeddingService.generateHypotheticalQuestionsForCapitals(
          //     capitalData);
          //***********************************************************

          System.out.println("Persisting data for capital: " + capitalData.capitalCity().city() + "...");
          System.out.println("Current date and time: " + LocalDateTime.now());
          capitalDataRAGList.parallelStream()
              .forEach(capitalRAG -> capitalService.addCapital(capitalRAG));
        });
      } catch (Exception e) {
        System.err.println("Error during data ingestion: " + e.getMessage());
        e.printStackTrace();
      } finally {
        // Gracefully exit when done
        SpringApplication.exit(context, () -> 0);
      }
    };
  }

  private List<CapitalData> filterCapitals(List<CapitalData> capitalsData) {
    // Define the list of 10 capital names to keep
    Set<String> capitalsToKeep = new HashSet<>(Arrays.asList(
        "London", "Paris", "Berlin", "Rome", "Madrid",
        "Tokyo", "Beijing", "Bern", "Vienna", "Ottawa"
    ));

    // Filter the list to keep only capitals in our set
    return capitalsData.stream()
        .filter(capital -> capitalsToKeep.contains(capital.capitalCity().city()))
        .collect(Collectors.toList());
  }


  @Bean
  protected EmbeddingModel embeddingModel() {
    return VertexAiEmbeddingModel.builder()
        .project(System.getenv("GCP_PROJECT_ID"))
        .endpoint(System.getenv("GCP_VERTEXAI_ENDPOINT"))
        .location(System.getenv("GCP_LOCATION"))
        .publisher("google")
        .modelName(System.getenv("GCP_TEXTEMBEDDING_MODEL"))
        .maxSegmentsPerBatch(100)
        .maxRetries(5)
        .build();
  }
}
