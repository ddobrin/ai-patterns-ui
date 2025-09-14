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
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class WeatherForecastMCPTool extends AbstractBase {

  private final Tracer tracer;

  public WeatherForecastMCPTool(Tracer tracer) {
    this.tracer = tracer;
  }

  @Tool("List capabilities available in WeatherForecast server")
  TopicReport listCapabilitiesInWeatherForecast(String capital) throws Exception {
    Span span = tracer.spanBuilder("WeatherForecastMCPTool.listCapabilities")
        .setAttribute("mcp.server", "weather")
        .setAttribute("mcp.tool", "listCapabilitiesInWeatherForecast")
        .setAttribute("capital", capital)
        .setAttribute("mcp.server.url", System.getenv("MCP_WEATHER_SERVER"))
        .startSpan();

    try (Scope scope = span.makeCurrent()) {
      System.out.println(blue(">>> Invoking `listCapabilitiesInWeatherForecast` tool with capital: ") + capital);

      span.addEvent("Creating MCP transport");
      var transport = new HttpClientSseClientTransport(System.getenv("MCP_WEATHER_SERVER"));
      var mcpClient = McpClient.sync(transport).build();

      span.addEvent("Initializing MCP client");
      mcpClient.initialize();
      mcpClient.ping();

      span.addEvent("Getting server capabilities");
      ServerCapabilities capabilities = mcpClient.getServerCapabilities();
      if(capabilities == null) {
        span.setAttribute("capabilities.available", false);
        return new TopicReport(capital, "No capabilities available for the WeatherForecast MCP Server");
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
        toolList.add(("* **Prompt:** ") + "No prompts available for the WeatherForecast MCP Server");
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
        toolList.add(("* **Resources:** \n") + "No resources available for the WeatherForecast MCP Server");
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

  @Tool("Get the temperature (in celsius) for a specific capital")
  TopicReport getTemperatureByCapital(String capital) throws Exception {
    Span span = tracer.spanBuilder("WeatherForecastMCPTool.getTemperatureByCapital")
        .setAttribute("mcp.server", "weather")
        .setAttribute("mcp.tool", "getTemperatureByCapital")
        .setAttribute("capital", capital)
        .setAttribute("mcp.server.url", System.getenv("MCP_WEATHER_SERVER"))
        .startSpan();

    try (Scope scope = span.makeCurrent()) {
      System.out.println(
          blue(">>> Invoking `getTemperatureByCapital` tool with capital: ") + capital);

      span.addEvent("Creating MCP transport");
      var transport = new HttpClientSseClientTransport(System.getenv("MCP_WEATHER_SERVER"));
      var mcpClient = McpClient.sync(transport).build();

      span.addEvent("Initializing MCP client");
      mcpClient.initialize();
      mcpClient.ping();

      span.addEvent("Calling MCP tool: getTemperatureByCapital");
      CallToolResult weatherForcastResult = mcpClient.callTool(
          new CallToolRequest("getTemperatureByCapital",
              Map.of("capital", capital)));

      span.addEvent("Extracting response text");
      String weatherForcastText = extractTextFromCallToolResult(weatherForcastResult);
      System.out.println("\nWeather Forecast: " + weatherForcastText);

      span.addEvent("Closing MCP client");
      // close MCP client gracefully
      mcpClient.closeGracefully();

      span.setAttribute("response.length", weatherForcastText.length());
      return new TopicReport(capital, weatherForcastText);
    } catch (Exception e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  @Tool("Get the temperature (in celsius) for a specific location")
  TopicReport getTemperatureByCoordinates(Double latitude, Double longitude) throws Exception {
    Span span = tracer.spanBuilder("WeatherForecastMCPTool.getTemperatureByCoordinates")
        .setAttribute("mcp.server", "weather")
        .setAttribute("mcp.tool", "getTemperatureByLocation")
        .setAttribute("latitude", latitude)
        .setAttribute("longitude", longitude)
        .setAttribute("mcp.server.url", System.getenv("MCP_WEATHER_SERVER"))
        .startSpan();

    try (Scope scope = span.makeCurrent()) {
      System.out.println(
          blue(">>> Invoking `getTemperatureByCoordinates` tool with latitude and longitude"));

      span.addEvent("Creating MCP transport");
      var transport = new HttpClientSseClientTransport(System.getenv("MCP_WEATHER_SERVER"));
      var mcpClient = McpClient.sync(transport).build();

      span.addEvent("Initializing MCP client");
      mcpClient.initialize();
      mcpClient.ping();

      span.addEvent("Calling MCP tool: getTemperatureByLocation");
      CallToolResult weatherForcastResult = mcpClient.callTool(
          new CallToolRequest("getTemperatureByLocation",
                Map.of("latitude", latitude,
                      "longitude", longitude))
      );

      span.addEvent("Extracting response text");
      String weatherForcastText = extractTextFromCallToolResult(weatherForcastResult);
      System.out.println("\nWeather Forecast: " + weatherForcastText);

      span.addEvent("Closing MCP client");
      // close MCP client gracefully
      mcpClient.closeGracefully();

      span.setAttribute("response.length", weatherForcastText.length());
      return new TopicReport("capital", weatherForcastText);
    } catch (Exception e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }

  // get coordinates for a specific capital
  public static Map<String, Object> getCoordinatesForCapital(String capital) throws Exception {
    // URL encode the city name to handle spaces and special characters
    String encodedCityName = URLEncoder.encode(capital, StandardCharsets.UTF_8.toString());
    String geocodingUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedCityName;

    // Setup HTTP connection
    URL url = new URL(geocodingUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");

    // Read the response
    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    StringBuilder response = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      response.append(line);
    }
    reader.close();

    // Parse JSON response
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> jsonResponse = mapper.readValue(response.toString(), Map.class);

    // Extract the first result (assuming it's the most relevant)
    if (jsonResponse.containsKey("results") && ((java.util.List) jsonResponse.get("results")).size() > 0) {
      return (Map<String, Object>) ((java.util.List) jsonResponse.get("results")).get(0);
    } else {
      throw new Exception("No location found for capital: " + capital);
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
