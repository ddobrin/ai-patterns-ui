# Spring AI MCP Weather Server Sample with WebMVC Starter

## Dependencies

The project requires the Spring AI MCP Server WebMVC Boot Starter:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

## Building the Project

Build the project using Maven:
```bash
./mvnw clean install -DskipTests
```

## Running the Server

The server supports two transport modes:

### WebMVC SSE Mode (Default)
```bash
java -jar target/mcp-weather-server-0.0.1.jar
```

## Configuration

Configure the server through `application.properties`:

```properties
# Server identification
spring.ai.mcp.server.name=my-weather-server
spring.ai.mcp.server.version=0.0.1

# Server type (SYNC/ASYNC)
spring.ai.mcp.server.type=SYNC

# Transport configuration
spring.ai.mcp.server.stdio=false
spring.ai.mcp.server.sse-message-endpoint=/mcp/message

# Change notifications
spring.ai.mcp.server.resource-change-notification=true
spring.ai.mcp.server.tool-change-notification=true
spring.ai.mcp.server.prompt-change-notification=true

# Logging (required for STDIO transport)
spring.main.banner-mode=off
logging.file.name=./target/starter-webmvc-server.log
```
