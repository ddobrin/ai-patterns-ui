package ai.patterns.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.api.common.AttributeKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(value = "opentelemetry.traces.enabled", havingValue = "true", matchIfMissing = true)
public class OpenTelemetryConfig {

    @Value("${spring.application.name:ai-patterns-ui}")
    private String applicationName;

    @Value("${gcp.project.id:}")
    private String projectId;

    @Bean
    public OpenTelemetry openTelemetry() throws IOException {
        Resource resource = Resource.getDefault().toBuilder()
                .put(AttributeKey.stringKey("service.name"), applicationName)
                .put(AttributeKey.stringKey("service.version"), "1.0.0")
                .build();

        // Create Google Cloud Trace exporter
        io.opentelemetry.sdk.trace.export.SpanExporter googleCloudTraceExporter = 
            com.google.cloud.opentelemetry.trace.TraceExporter.createWithDefaultConfiguration();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(googleCloudTraceExporter).build())
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(applicationName);
    }
}
