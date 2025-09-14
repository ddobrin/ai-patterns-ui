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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TouristBureauMCPTool extends AbstractBase {

  private final Tracer tracer;

  public TouristBureauMCPTool(Tracer tracer) {
    this.tracer = tracer;
  }

  @Tool("List capabilities available in TouristBureau server")
  TopicReport listCapabilitiesInFileArchive(String capital) throws Exception {
    Span span = tracer.spanBuilder("TouristBureauMCPTool.listCapabilities")
        .setAttribute("mcp.server", "file")
        .setAttribute("mcp.tool", "listCapabilitiesInFileArchive")
        .setAttribute("capital", capital)
        .setAttribute("mcp.server.url", System.getenv("MCP_FILE_SERVER"))
        .startSpan();

    try (Scope scope = span.makeCurrent()) {
      System.out.println(blue(">>> Invoking `listCapabilitiesInFileArchive` tool with capital: ") + capital);

      span.addEvent("Creating MCP transport");
      var transport = new HttpClientSseClientTransport(System.getenv("MCP_FILE_SERVER"));
      var mcpClient = McpClient.sync(transport).build();

      span.addEvent("Initializing MCP client");
      mcpClient.initialize();
      mcpClient.ping();

      span.addEvent("Getting server capabilities");
      ServerCapabilities capabilities = mcpClient.getServerCapabilities();
      if(capabilities == null) {
        span.setAttribute("capabilities.available", false);
        return new TopicReport(capital, "No capabilities available for the TouristBureau MCP Server");
      }

      span.setAttribute("capabilities.available", true);
      // List the available MCP tools
      ToolCapabilities toolsList = capabilities.tools();

      List<String> toolList = new ArrayList();
      // Pretty print available tools
      span.addEvent("Listing available tools");
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
        span.addEvent("Listing available prompts");
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

      span.addEvent("Closing MCP client");
      // close  the client gracefully
      mcpClient.closeGracefully();

      String result = toolList.stream().collect(Collectors.joining("\n"));
      span.setAttribute("response.length", result.length());
      return new TopicReport(capital, result);
    } catch (Exception e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  @Tool("Find article in the Archives")
  TopicReport findArticleInArchives(String capital) throws Exception {
    Span span = tracer.spanBuilder("TouristBureauMCPTool.findArticleInArchives")
        .setAttribute("mcp.server", "file")
        .setAttribute("mcp.tool", "findArticleInArchives")
        .setAttribute("capital", capital)
        .setAttribute("mcp.server.url", System.getenv("MCP_FILE_SERVER"))
        .startSpan();

    try (Scope scope = span.makeCurrent()) {
      System.out.println(blue(">>> Invoking `findArticleInArchives` tool with capital: ") + capital);

      span.addEvent("Creating MCP transport");
      var transport = new HttpClientSseClientTransport(System.getenv("MCP_FILE_SERVER"));
      var mcpClient = McpClient.sync(transport).build();

      span.addEvent("Initializing MCP client");
      mcpClient.initialize();
      mcpClient.ping();

      span.addEvent("Calling MCP tool: findArticleInArchives");
      CallToolResult fileResult = mcpClient.callTool(
          new CallToolRequest("findArticleInArchives",
              Map.of("capital", capital)));

      span.addEvent("Extracting response text");
      String fileText = extractTextFromCallToolResult(fileResult);
      System.out.println("\nArchived file: " + fileText);

      span.addEvent("Closing MCP client");
      // close MCP client gracefully
      mcpClient.closeGracefully();

      span.setAttribute("response.length", fileText.length());
      return new TopicReport(capital, fileText);
    } catch (Exception e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
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
