/* 
* Copyright 2025 - 2025 the original author or authors.
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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.ModelPreferences;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestClient;

/**
 * @author Christian Tzolov, Dan Dobrin
 */
@Service
public class FileService {

	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(FileService.class);

	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

	@Tool(description = "Find article in the Archives")
	public String findArticleInArchives(@ToolParam(description = "The capital to find the article for") String capital,
			ToolContext toolContext) {

    try {
			System.out.println("Reading file from the Archive for capital: " + capital);

			String data = String.format("File not found in the archive for capital %s", capital);
			ClassPathResource resource = new ClassPathResource(String.format("capitals/%s_article.txt", capital));
			try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
				data = FileCopyUtils.copyToString(reader);
			}

			System.out.println("File content: " + data.substring(0, Math.min(data.length(), 250)));
      return data;
    } catch (IOException e) {
      return "No article found in the Archive";
    }
  }
}