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
      return "No sources found (no data matching your query in the vector store).";
    }

    return vectorData.stream()
        .sorted((o1, o2) -> ((Double)o2.get("distance")).compareTo((Double)o1.get("distance")))
        .map(map -> {
          Double distance = (Double) map.get("distance");
          String content = (String) map.get("content");
          String chunk = (String) map.get("chunk");

          if (distance != null && content != null && chunk != null) {
//            if(chunk.length() > 500)
//              chunk = chunk.substring(0, 500) + " [...]";

            return String.format("""
                    * Similarity: **%.3f**
                        * Embedded
                            > %s
                        * Context
                            > %s
                    """,
                distance, content, chunk);
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
  public static List<Map<String, Object>> augmentWithVectorDataList(String userMessage, String embedType, CapitalDataAccessDAO dataAccess) {
    // search the vector store by query and embedding type !
    List<Map<String, Object>> vectorData = dataAccess.searchEmbeddings(userMessage, embedType);

    if (vectorData == null || vectorData.isEmpty()) {
      System.out.println("Vector data is empty or null.");
      return new ArrayList<>();
    }

    return vectorData;
  }

  // prepare UserMessage for LLM with sources appended
  public static String prepareUserMessage(String userMessage,
                                                      String messageAttachments,
                                                      String additionalVectorData,
                                                      String sources,
                                                      boolean showDataSources){
    String returnSources = "";
    if (showDataSources && !sources.trim().isEmpty()) {
        returnSources = String.format("""
          Please add at the end of your answer, the following content as-is, for reference purposes:

          #### Sources

          %s

          """, sources);
    }

    String vectorData = additionalVectorData;
    if(additionalVectorData != null && ! additionalVectorData.isEmpty()) {
      vectorData = String.format("""
          Answer the question using the following information:
          <excerpts>
          %s
          </excerpts>          
          """, additionalVectorData);
    }

    return PromptTemplate.from("""
        Here's the question from the user:
        <question>
        {{userMessage}}
        </question>
        
        {{attachments}}
        
        {{contents}}
        
        {{sources}}
        """).apply(Map.of(
        "userMessage", userMessage,
        "attachments", messageAttachments,
        "contents", vectorData,
        "sources", returnSources
    )).toUserMessage().singleText();
  }
}
