/*
* Copyright 2024 - 2024 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package mcp.file.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import java.util.stream.Collectors;

/**
 * @author Christian Tzolov
 */

public class SampleClient {

	private final McpClientTransport transport;

	public SampleClient(McpClientTransport transport) {
		this.transport = transport;
	}

	public void run() throws Exception {

		var client = McpClient.sync(this.transport).build();

		client.initialize();

		client.ping();

		// List and demonstrate tools
		ListToolsResult toolsList = client.listTools();
		System.out.println("Available Tools = " + prettyPrintTools(toolsList.tools()));

		CallToolResult fileResult = client.callTool(new CallToolRequest("findArticleInArchives",
				Map.of("capital", "Berlin")));

		System.out.println("\nFile Result: " + extractTextFromCallToolResult(fileResult));
	}

	public String prettyPrintTools(List<Tool> tools) {
		StringBuilder sb = new StringBuilder("Tools:\n");

		for (Tool tool : tools) {
			// Tool name
			sb.append("* ").append(tool.name()).append("\n");

			// Description
			sb.append("  * Description: ").append(tool.description()).append("\n");

			// Input Schema
			sb.append("  * Input Schema:\n");
			sb.append("    * Type: ").append(tool.inputSchema().type()).append("\n");

			// Properties
			sb.append("    * Properties:\n");
			Map<String, Object> properties = tool.inputSchema().properties();
			for (Map.Entry<String, Object> property : properties.entrySet()) {
				String propName = property.getKey();
				Map<String, Object> propDetails = (Map<String, Object>) property.getValue();

				StringBuilder propDescription = new StringBuilder();
				propDescription.append(propName).append(" (").append(propDetails.get("type"));

				// Add format if available
				if (propDetails.containsKey("format")) {
					propDescription.append(", format: ").append(propDetails.get("format"));
				}

				// Add required if applicable
				if (tool.inputSchema().required().contains(propName)) {
					propDescription.append(", required");
				}

				propDescription.append(")");
				sb.append("      * ").append(propDescription).append("\n");
			}

			// Additional Properties
			sb.append("    * Additional Properties: ")
					.append(tool.inputSchema().additionalProperties()).append("\n\n");
		}

		return sb.toString();
	}

	public static String extractTextFromCallToolResult(CallToolResult result) {
		return result.content().stream()
				.map(content -> {
					if (content instanceof TextContent textContent) {
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
