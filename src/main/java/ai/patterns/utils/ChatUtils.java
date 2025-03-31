/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.patterns.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChatUtils {

  public static @NotNull ChatOptions getDefaultChatOptions(String model) {
      @Nullable ChatUtils.@Nullable ChatOptions options;
      return new ChatOptions("",
          true,
          false,
          false,
           model,
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
