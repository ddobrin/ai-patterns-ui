package ai.patterns.utils;

import ai.patterns.dao.CapitalDataAccessDAO;
import ai.patterns.web.endpoints.ChatEndpoint.ChatOptions;
import ai.patterns.web.endpoints.ChatEndpoint.ChunkingType;
import dev.langchain4j.model.input.PromptTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RAGUtils
{
  // Format vector data as a String to return as SOURCES to the UI
  public static String formatVectorData(List<Map<String, Object>> vectorData) {
    if (vectorData == null || vectorData.isEmpty()) {
      return "Vector data is empty or null.";
    }

    return vectorData.stream()
        .map(map -> {
          Double distance = (Double) map.get("distance");
          String content = (String) map.get("content");
          String chunk = (String) map.get("chunk");

          if (distance != null && content != null && chunk != null) {
            return String.format("[Distance: %.10f]\n\n--> CONTENT: %s\nCHUNK: %s [...]",
                distance, content, chunk.substring(0, 200));
          } else {
            return "";
          }
        })
        .filter(formattedString -> formattedString != null) //filter out nulls
        .collect(Collectors.joining("\n\n")); //Join with double newlines.
  }

  // Format vector data to send to LLM as RAG data
  public static String augmentWithVectorData(String userMessage, ChatOptions options, CapitalDataAccessDAO dataAccess) {
    // no RAG? ok
    if (!options.enableRAG() || (options.chunkingType() == ChunkingType.NONE)) {
      return "";
    }

    // search the vector store by query and embedding type !
    List<Map<String, Object>> vectorData = dataAccess.searchEmbeddings(userMessage, options.chunkingType().name().toLowerCase());

    if (vectorData == null || vectorData.isEmpty()) {
      System.out.println("Vector data is empty or null.");
      return "No data found in the vector store";
    }

    return vectorData.stream()
        .map(map -> Optional.ofNullable(map.get("chunk")))
        .filter(Optional::isPresent) //filter out optionals that are empty
        .map(Optional::get) //get the value from the optional.
        .map(Object::toString) //convert each object to string.
        .collect(Collectors.joining("\n"));
  }

  // Format vector data to send to LLM as RAG data
  // return as List
  public static List<Map<String, Object>> augmentWithVectorDataList(String userMessage, ChatOptions options, CapitalDataAccessDAO dataAccess) {
    // no RAG? ok
    if (!options.enableRAG() || (options.chunkingType() == ChunkingType.NONE)) {
      return new ArrayList<>();
    }

    // search the vector store by query and embedding type !
    List<Map<String, Object>> vectorData = dataAccess.searchEmbeddings(userMessage, options.chunkingType().name().toLowerCase());

    if (vectorData == null || vectorData.isEmpty()) {
      System.out.println("Vector data is empty or null.");
      return new ArrayList<>();
    }

    return vectorData;
  }

  // prepare UserMessage for LLM without appending sources
  public static String prepareUserMessagesNoSources(String userMessage, String additionalVectorData){
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
        "userMessage", userMessage,
        "contents", additionalVectorData
    )).toUserMessage().singleText();
  }

  // prepare UserMessage for LLM with soruces appended
  public static String prepareUserMessagesWithSources(String userMessage, String additionalVectorData, String sources){
    return PromptTemplate.from("""
        Here's the question from the user:
        <question>
        {{userMessage}}
        </question>
        
        Answer the question using the following information:
        <excerpts>
        {{contents}}
        </excerpts>
        
        Please add at the end of your answer, the following String as-is, for reference purposes:
        ---------------------
        ===== SOURCES =====
        
        {{sources}}
        
        ---------------------
        """).apply(Map.of(
        "userMessage", userMessage,
        "contents", additionalVectorData,
        "sources", sources
    )).toUserMessage().singleText();
  }
}
