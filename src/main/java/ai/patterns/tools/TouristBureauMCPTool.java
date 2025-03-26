package ai.patterns.tools;

import static ai.patterns.utils.Ansi.cyan;
import static ai.patterns.utils.Ansi.blue;
import static ai.patterns.utils.Ansi.yellow;
import static ai.patterns.utils.Models.MODEL_GEMINI_FLASH;

import ai.patterns.base.AbstractBase;
import ai.patterns.data.TopicReport;
import ai.patterns.utils.ChatUtils;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TouristBureauMCPTool extends AbstractBase {

  interface TopicMCPAssistant {
    @SystemMessage("""
            You are an Archive assistant. Your task is to:
            1. Read the content of the specified file
            2. Clean any non-printable or special characters from the content
            3. Ensure the content is valid UTF-8 text
            4. Return the cleaned content as a properly escaped JSON string
            5. If you encounter any issues with the file content, return a clear error message
            """)
    String find(String cityArticle);
  }

  // @Tool("Get printable article about a city")
  // TopicReport getPrintableArticle(String city){
  //   System.out.println(blue(">>> Invoking `getPrintableArticle` tool with query: ") + city);
  //
  //   String text = loadDocumentText(String.format("capitals/%s_article.txt", city)).text();
  //
  //   return new TopicReport(city, text);
  // }

  @Tool("List tools available in TouristBureau server")
  TopicReport listToolsInTouristBureau(String city) throws Exception {
    System.out.println(blue(">>> Invoking `listToolsInTouristBureau` tool with city: ") + city);

    // Use NPX
    McpTransport transport = new StdioMcpTransport.Builder()
        .command(List.of("npx",
            "-y",
            "@modelcontextprotocol/server-filesystem@0.6.2",
            new File("src/main/resources/capitals").getAbsolutePath()
        ))
        .logEvents(true)
        .build();

    McpClient mcpClient = new DefaultMcpClient.Builder()
        .transport(transport)
        .build();

    List<String> toolList = new ArrayList();
    // Pretty print available tools
    mcpClient.listTools().forEach(tool -> {
      toolList.add(("* **Tool:** \n") + tool.name());
      toolList.add(("  * **Description:** \n") + tool.description());
      toolList.add(("  * **Parameters:** \n"));
      tool.parameters().properties()
          .forEach((key, value) -> toolList.add("    * "  + key + ": " + value));
    });

    return new TopicReport(city, toolList.stream().collect(Collectors.joining("\n")));
  }

  @Tool("Find article in the Archives")
  TopicReport findArticleInArchives(String city) throws Exception{
    System.out.println(blue(">>> Invoking `findArticleInArchives` tool with city: ") + city);

    // Use NPX
    McpTransport transport = new StdioMcpTransport.Builder()
        .command(List.of("npx",
            "-y",
            "@modelcontextprotocol/server-filesystem@0.6.2",
            new File("src/main/resources/capitals").getAbsolutePath()
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
        .chatLanguageModel(getChatLanguageModel(ChatUtils.getDefaultChatOptions()))
        .toolProvider(toolProvider)
        .build();

    String foundArticle;
    try{
      File file = new File(String.format("src/main/resources/capitals/%s_article.txt", city));
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
