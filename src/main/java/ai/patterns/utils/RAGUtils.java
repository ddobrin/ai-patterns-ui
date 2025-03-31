package ai.patterns.utils;

import ai.patterns.dao.CapitalDataAccessDAO;
import ai.patterns.utils.ChatUtils.ChatOptions;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.CompressingQueryTransformer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RAGUtils {

  // Format vector data as a String to return as SOURCES to the UI
  public static String formatVectorSearchResults(List<CapitalDataAccessDAO.CapitalChunkRow> vectorData) {
    if (vectorData == null || vectorData.isEmpty()) {
      return "No sources found (no data matching your query in the vector store).";
    }

    return vectorData.stream()
        .sorted((o1, o2) -> o2.getDistance() < o1.getDistance() ? 1 : -1)
        .map(capitalChunk -> {
          if (capitalChunk.getDistance() != null && capitalChunk.getContent() != null && capitalChunk.getChunk() != null) {
            return String.format("""
                    * **Distance**: **%.3f** %s
                        * Embedded
                            > %s
                        * Context
                            > %s
                    """,
                capitalChunk.getDistance(),
                capitalChunk.getRerankingScore() != null ? " — _(Reranking score: %.3f)_".formatted(capitalChunk.getRerankingScore()) : "",
                capitalChunk.getContent(),
                capitalChunk.getChunk());
          } else {
            return "";
          }
        })
        .filter(formattedString -> formattedString != null) //filter out nulls
        .collect(Collectors.joining("\n\n")); //Join with double newlines.
  }

  // Format vector data to send to LLM as RAG data
  // return as List
  public static List<CapitalDataAccessDAO.CapitalChunkRow> augmentWithVectorDataList(
      String userMessage,
      ChatOptions options,
      CapitalDataAccessDAO dataAccess) {
    // handle the case when RAG is enabled in the UI
    // but no Chunking method has been selected
    String chunkingType = (options.chunkingType().name().equals(ChatUtils.ChunkingType.NONE.name()) ?
          ChatUtils.ChunkingType.HIERARCHICAL.name() :
          options.chunkingType().name())
              .toLowerCase();

    // search the vector store by query and embedding type !
    List<CapitalDataAccessDAO.CapitalChunkRow> vectorData = dataAccess.searchEmbeddings(
        userMessage,
        chunkingType,
        options.filtering() ? options.capital() : null,
        options.filtering() ? options.continent() : null);

    if (vectorData == null || vectorData.isEmpty()) {
      System.out.println("Vector data is empty or null.");
      return new ArrayList<>();
    }

    // post-processing filter
    // remove for a better solution
    if(options.filtering()) {
      if((options.capital() != null) && !options.capital().isEmpty())
        vectorData = vectorData.stream()
          .filter(row -> options.capital().equals(row.getCapital()))
          .collect(Collectors.toList());

      if((options.continent() != null) && !options.continent().isEmpty())
        vectorData = vectorData.stream()
          .filter(row -> options.continent().equals(row.getContinentName()))
          .collect(Collectors.toList());
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

  public static String prepareUserMessage(String userMessage,
      String messageAttachments,
      String additionalVectorData,
      String sources,
      String steps,
      boolean showDataSources){
    String returnSources = "";
    if (showDataSources && (!sources.trim().isEmpty() || !steps.isEmpty())) {
      returnSources = String.format("""
          Please add at the end of your answer, the following content as-is, for reference purposes:

          #### Sources

          %s
          %s

          """, steps, sources);
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

  public static String compressQuery(String chatId, String userMessage, MessageWindowChatMemory chatMemory, ChatLanguageModel chatLanguageModel) {
    CompressingQueryTransformer transformer = new CompressingQueryTransformer(chatLanguageModel);

    Query firstQuery = transformer.transform(new Query(userMessage,
            new Metadata(dev.langchain4j.data.message.UserMessage.from(userMessage),
                chatId,
                chatMemory.messages())))
        .stream()
        .findFirst()
        .orElse(new Query(userMessage));

    userMessage = firstQuery.text();
    return userMessage;
  }

  public static String hypotheticalAnswer(String chatId, String userMessage, MessageWindowChatMemory chatMemory, ChatLanguageModel chatLanguageModel) {
      return chatLanguageModel.chat("""
          Answer the following user question.
          Don't use pronouns, be explicit about the subject and object of the question and answer.
          
          Question: %s
          """.formatted(userMessage));
  }
}
