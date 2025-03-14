package ai.patterns.dao;

import java.util.List;

public record CapitalDataRAG(
    CapitalData capitalData,
    EmbedType embedType,
    List<String> content,
    String chunk
) {
  public enum EmbedType{
    HIERARCHICAL,
    HYPOTHETICAL,
    CONTEXTUAL,
    LATE
  }
}
