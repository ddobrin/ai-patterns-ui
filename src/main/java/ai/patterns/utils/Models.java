package ai.patterns.utils;

import dev.langchain4j.model.vertexai.HarmCategory;
import dev.langchain4j.model.vertexai.SafetyThreshold;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class Models {
  public static final String MODEL_GEMINI_FLASH     = "gemini-2.0-flash-001";
  public static final String MODEL_GEMINI_PRO       = "gemini-2.0-pro-exp-02-05";
  public static final String MODEL_GEMINI_FLASH_THINKING = "gemini-2.0-flash-thinking-exp-01-21";
  public static final String MODEL_GEMINI_FLASH_LITE = "gemini-2.0-flash-exp";
  public static final String MODEL_GEMMA3_27B = "gemma3:27b";

  // Embedding Models
  // https://cloud.google.com/vertex-ai/generative-ai/docs/model-reference/text-embeddings-api?hl=en&authuser=2
  public static final String MODEL_EMBEDDING_MULTILINGUAL = "text-multilingual-embedding-002";
  public static final String MODEL_EMBEDDING_TEXT         = "text-embedding-005";
  public static final int    MODEL_EMBEDDING_DIMENSION    = 768;

  public static final int MAX_RETRIES = 3;
  public static final int DB_RETRIEVAL_LIMIT = 5;
  public static final double RERANKING_SCORE_THRESHOLD = 0.6;

  public static final Map<@NotNull HarmCategory, @NotNull SafetyThreshold> SAFETY_SETTINGS_OFF = Map.of(
      HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT, SafetyThreshold.BLOCK_NONE,
      HarmCategory.HARM_CATEGORY_HARASSMENT, SafetyThreshold.BLOCK_NONE,
      HarmCategory.HARM_CATEGORY_HATE_SPEECH, SafetyThreshold.BLOCK_NONE,
      HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT, SafetyThreshold.BLOCK_NONE
  );

  public static final Map<@NotNull HarmCategory, @NotNull SafetyThreshold> SAFETY_SETTINGS_ON = Map.of(
      HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT, SafetyThreshold.BLOCK_LOW_AND_ABOVE,
      HarmCategory.HARM_CATEGORY_HARASSMENT, SafetyThreshold.BLOCK_LOW_AND_ABOVE,
      HarmCategory.HARM_CATEGORY_HATE_SPEECH, SafetyThreshold.BLOCK_LOW_AND_ABOVE,
      HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT, SafetyThreshold.BLOCK_LOW_AND_ABOVE
  );
}
