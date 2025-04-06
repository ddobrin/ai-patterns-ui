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
package ai.patterns.tools;

import static ai.patterns.utils.Ansi.blue;

import ai.patterns.base.AbstractBase;
import ai.patterns.data.TopicReport;
import dev.langchain4j.agent.tool.Tool;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.ToolCapabilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TouristBureauMCPTool extends AbstractBase {

  @Tool("List capabilities available in TouristBureau server")
  TopicReport listCapabilitiesInFileArchive(String capital) throws Exception {
    System.out.println(blue(">>> Invoking `listCapabilitiesInFileArchive` tool with capital: ") + capital);

    var transport = new HttpClientSseClientTransport(System.getenv("MCP_FILE_SERVER"));
    var mcpClient = McpClient.sync(transport).build();

    mcpClient.initialize();
    mcpClient.ping();

    ServerCapabilities capabilities = mcpClient.getServerCapabilities();
    if(capabilities == null)
      return new TopicReport(capital, "No capabilities available for the TouristBureau MCP Server");

    // List the available MCP tools
    ToolCapabilities toolsList = capabilities.tools();

    List<String> toolList = new ArrayList();
    // Pretty print available tools
    mcpClient.listTools().tools().forEach(tool -> {
      toolList.add(("* **Tool:** \n") + tool.name());
      toolList.add(("  * **Description:** \n") + tool.description());
      toolList.add(("  * **Parameters:** \n"));
      tool.inputSchema().properties()
          .forEach((key, value) -> toolList.add("    * "  + key + ": " + value));
    });

    if(capabilities.prompts() == null){
      toolList.add(("* **Prompt:** ") + "No prompts available for the TouristBureau MCP Server");
    } else {
      mcpClient.listPrompts().prompts().forEach(prompt -> {
        toolList.add(("* **Prompt:** \n") + prompt.name());
        toolList.add(("  * **Description:** \n") + prompt.description());

        prompt.arguments().forEach(arg -> {
          String requiredText = arg.required() ? "(Required)" : "(Optional)";
          toolList.add("    * " + arg.name() + " " + requiredText + ": " + arg.description());
        });
      });
    };

    if(capabilities.resources() == null){
      toolList.add(("* **Resources:** \n") + "No resources available for the TouristBureau MCP Server");
    }

    // close  the client gracefully
    mcpClient.closeGracefully();

    return new TopicReport(capital, toolList.stream().collect(Collectors.joining("\n")));
  }

  @Tool("Find article in the Archives")
  TopicReport findArticleInArchives(String capital) throws Exception {
    System.out.println(blue(">>> Invoking `findArticleInArchives` tool with capital: ") + capital);

    var transport = new HttpClientSseClientTransport(System.getenv("MCP_FILE_SERVER"));
    var mcpClient = McpClient.sync(transport).build();

    mcpClient.initialize();
    mcpClient.ping();

    CallToolResult fileResult = mcpClient.callTool(
        new CallToolRequest("findArticleInArchives",
            Map.of("capital", capital)));

    String fileText = extractTextFromCallToolResult(fileResult);
    System.out.println("\nArchived file: " + fileText);

    // close MCP client gracefully
    mcpClient.closeGracefully();

    return new TopicReport(capital, fileText);
  }

  // extract the text content from the CallTool
  public static String extractTextFromCallToolResult(CallToolResult result) {
    return result.content().stream()
        .map(content -> {
          if (content instanceof McpSchema.TextContent textContent) {
            return textContent.text();
          } else if (content instanceof McpSchema.ImageContent) {
            return "[Image content]";
          } else if (content instanceof McpSchema.EmbeddedResource embeddedResource) {
            McpSchema.ResourceContents resourceContents = embeddedResource.resource();
            if (resourceContents instanceof McpSchema.TextResourceContents textResource) {
              return textResource.text();
            } else {
              return "[Binary resource]";
            }
          }
          return "";
        })
        .filter(text -> !text.isEmpty())
        .collect(Collectors.joining("\n"));
  }
}
