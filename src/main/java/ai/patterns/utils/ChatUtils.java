package ai.patterns.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChatUtils {

  public static @NotNull ChatOptions getDefaultChatOptions() {
      @Nullable ChatUtils.@Nullable ChatOptions options;
      return new ChatOptions("",
          true,
          false,
          false,
           Models.MODEL_GEMINI_FLASH,
          false,
          true,
          false,
          false,
           ChunkingType.NONE,
           false,
           false,
           false,
           false,
           false,
          false,
           true,
           "",
          ""
          );
  }

  public enum ChunkingType {
      NONE,
      HIERARCHICAL,
      HYPOTHETICAL,
      CONTEXTUAL,
      LATE
  }

  public record ChatOptions(
      String systemMessage,
      boolean useVertex,
      boolean useAgents,
      boolean useWebsearch,
      String model,
      boolean enableSafety,
      boolean useGuardrails,
      boolean evaluateResponse,
      boolean enableRAG,
      ChunkingType chunkingType,
      boolean filtering,
      boolean queryCompression,
      boolean queryRouting,
      boolean hyde,
      boolean reranking,
      boolean writeActions,
      boolean showDataSources,
      String capital,
      String continent) {
  }
}
