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

  @Tool("List tools available in WeatherForecast server")
  TopicReport listToolsInWeatherForecast(String capital) throws Exception {
    System.out.println(blue(">>> Invoking `listToolsInWeatherForecast` tool with capital: ") + capital);

    var transport = new HttpClientSseClientTransport("http://localhost:8083");
    var mcpClient = McpClient.sync(transport).build();

    mcpClient.initialize();
    mcpClient.ping();

    // List the available MCP tools
    ListToolsResult toolsList = mcpClient.listTools();

    List<String> toolList = new ArrayList();
    // Pretty print available tools
    mcpClient.listTools().tools().forEach(tool -> {
      toolList.add(("* **Tool:** \n") + tool.name());
      toolList.add(("  * **Description:** \n") + tool.description());
      toolList.add(("  * **Parameters:** \n"));
      tool.inputSchema().properties()
          .forEach((key, value) -> toolList.add("    * "  + key + ": " + value));
    });

    // close  the client gracefully
    mcpClient.closeGracefully();

    return new TopicReport(capital, toolList.stream().collect(Collectors.joining("\n")));
  }

  @Tool("Get the temperature (in celsius) for a specific capital")
  TopicReport getTemperatureByCapital(String capital) throws Exception {
    System.out.println(
        blue(">>> Invoking `getTemperatureByCapital` tool with capital: ") + capital);

    var transport = new HttpClientSseClientTransport(System.getenv("MCP_WEATHER_SERVER"));
    var mcpClient = McpClient.sync(transport).build();

    mcpClient.initialize();
    mcpClient.ping();

    CallToolResult weatherForcastResult = mcpClient.callTool(
        new CallToolRequest("getTemperatureByCapital",
            Map.of("capital", capital)));

    String weatherForcastText = extractTextFromCallToolResult(weatherForcastResult);
    System.out.println("\nWeather Forecast: " + weatherForcastText);

    // close MCP client gracefully
    mcpClient.closeGracefully();

    return new TopicReport(capital, weatherForcastText);
  }

  @Tool("Get the temperature (in celsius) for a specific location")
  TopicReport getTemperatureByCoordinates(Double latitude, Double longitude) throws Exception {
    System.out.println(
        blue(">>> Invoking `getTemperatureByCoordinates` tool with latitude and longitude"));

    var transport = new HttpClientSseClientTransport("http://localhost:8083");
    var mcpClient = McpClient.sync(transport).build();

    mcpClient.initialize();
    mcpClient.ping();

    CallToolResult weatherForcastResult = mcpClient.callTool(
        new CallToolRequest("getTemperatureByLocation",
              Map.of("latitude", latitude,
                    "longitude", longitude))
    );

    String weatherForcastText = extractTextFromCallToolResult(weatherForcastResult);
    System.out.println("\nWeather Forecast: " + weatherForcastText);

    // close MCP client gracefully
    mcpClient.closeGracefully();

    return new TopicReport("capital", weatherForcastText);
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
