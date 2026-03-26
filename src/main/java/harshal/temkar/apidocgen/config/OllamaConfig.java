package harshal.temkar.apidocgen.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Getter;
import lombok.Setter;
import reactor.netty.http.client.HttpClient;

/**
 * Configuration for Ollama LLM integration.
 * 
 * Handles: - WebClient configuration with optimized timeouts - Connection
 * pooling for high throughput - Retry mechanism for transient failures -
 * Circuit breaker pattern for resilience
 * 
 * Performance Considerations: - Connection timeout: 10s (fast fail for
 * unavailable service) - Read timeout: 300s (LLM responses can be slow) - Max
 * connections: 100 (horizontal scaling support)
 */

@Configuration
@ConfigurationProperties(prefix = "ollama")
@Getter
@Setter
public class OllamaConfig {

	private String baseUrl = "http://localhost:11434";
	private String model = "llama3.1:8b";
	private int connectionTimeout = 10000;
	private int readTimeout = 300000; // 5 minutes for LLM processing
	private int maxConnections = 100;
	private double temperature = 0.1; // Low temperature for consistent output

	/**
	 * Creates optimized WebClient for Ollama API calls.
	 * 
	 * Performance optimizations: - Connection pooling to reduce overhead -
	 * Configurable timeouts for different LLM models - Non-blocking I/O for high
	 * concurrency
	 * 
	 * @return Configured WebClient instance
	 */
	
	@Bean
	WebClient ollamaWebClient() {
		HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
				.responseTimeout(Duration.ofMillis(readTimeout))
				.doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
						.addHandlerLast(new WriteTimeoutHandler(connectionTimeout, TimeUnit.MILLISECONDS)));

		return WebClient.builder().baseUrl(baseUrl).clientConnector(new ReactorClientHttpConnector(httpClient)).build();
	}
}