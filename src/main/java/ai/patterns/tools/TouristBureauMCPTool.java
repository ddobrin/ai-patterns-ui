package ai.patterns.tools;

import static com.datastax.astra.internal.utils.AnsiUtils.cyan;
import static com.datastax.astra.internal.utils.AnsiUtils.magenta;
import static com.datastax.astra.internal.utils.AnsiUtils.yellow;

import ai.patterns.base.AbstractTest;
import ai.patterns.base.TopicReport;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.tool.ToolProvider;
import java.io.File;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TouristBureauMCPTool extends AbstractTest{

  interface TopicMCPAssistant {
    @SystemMessage("""
            You are a file system assistant. Your task is to:
            1. Read the content of the specified file
            2. Clean any non-printable or special characters from the content
            3. Ensure the content is valid UTF-8 text
            4. Return the cleaned content as a properly escaped JSON string
            5. If you encounter any issues with the file content, return a clear error message
            """)
    String find(String cityArticle);
  }

  @Tool("Get printable article about a city")
  TopicReport getPrintableArticle(String city){
    System.out.println(magenta(">>> Invoking `getPrintableArticle` tool with query: ") + city);

    String text = loadDocumentText(String.format("capitals/%s_article.txt", city)).text();

    return new TopicReport(city, text);
  }

  @Tool("Find article in the FileSystem")
  TopicReport findArticleInFilesystem(String city) throws Exception{
    System.out.println(magenta(">>> Invoking `findArticleInFilesystem` tool with city: ") + city);

    // Use NPX
    McpTransport transport = new StdioMcpTransport.Builder()
        .command(List.of("npx",
            "-y",
            "@modelcontextprotocol/server-filesystem",
            new File("src/resources/capitals").getAbsolutePath()
        ))
        .logEvents(true)
        .build();

    McpClient mcpClient = new DefaultMcpClient.Builder()
        .transport(transport)
        .build();

    // Pretty print available tools
    System.out.println(cyan("\nAvailable MCP Tools:"));
    mcpClient.listTools().forEach(tool -> {
      System.out.println(yellow("- Tool: ") + tool.name());
      System.out.println(cyan("  Description: ") + tool.description());
      System.out.println(cyan("  Parameters: "));
      tool.parameters().properties()
          .forEach((key, value) -> System.out.println(yellow("    - ") + key + ": " + value));

      System.out.println();
    });

    ToolProvider toolProvider = McpToolProvider.builder()
        .mcpClients(List.of(mcpClient))
        .build();

    TopicMCPAssistant topicMCPAssistant = AiServices.builder(
            TopicMCPAssistant.class)
        .chatLanguageModel(getChatLanguageModel(MODEL_GEMINI_FLASH))
        .toolProvider(toolProvider)
        .build();

    String foundArticle;
    try{
      File file = new File(String.format("src/test/resources/capitals/%s_article.txt", city));
      foundArticle = topicMCPAssistant.find(file.getAbsolutePath());

      System.out.println(yellow("\n-> Topic report: ") + foundArticle);

      return new TopicReport(city, foundArticle);
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();

      return new TopicReport(city, "No article found");
    } finally{
      mcpClient.close();
    }
  }
}
