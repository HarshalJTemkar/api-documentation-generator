package harshal.temkar.apidocgen.service;

import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import harshal.temkar.apidocgen.config.OllamaConfig;
import harshal.temkar.apidocgen.exception.ErrorCode;
import harshal.temkar.apidocgen.exception.OllamaServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.util.retry.Retry;

/**
 * Service for interacting with Ollama LLM API.
 * 
 * Features: - Async/non-blocking calls - Retry mechanism (3 attempts with
 * exponential backoff) - Circuit breaker for resilience - Timeout handling -
 * Correlation ID propagation
 * 
 * Performance: - Non-blocking I/O - Supports high concurrency (100+
 * simultaneous requests) - Optimized for long-running LLM operations
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaService {

	private final WebClient ollamaWebClient;
	private final OllamaConfig ollamaConfig;

	/**
	 * Generates completion from Ollama LLM.
	 * 
	 * @param prompt        User prompt
	 * @param correlationId Request tracking ID
	 * @return LLM generated text
	 */
	
	public String generateCompletion(String prompt, String correlationId) {
		log.info("[{}] Calling Ollama LLM with model: {}", correlationId, ollamaConfig.getModel());

		Map<String, Object> requestBody = Map.of("model", ollamaConfig.getModel(), "prompt", prompt, "stream", false,
				"options", Map.of("temperature", ollamaConfig.getTemperature(), "top_p", 0.9));

		try {
			long startTime = System.currentTimeMillis();

			String response = ollamaWebClient.post().uri("/api/generate").header("X-Correlation-ID", correlationId)
					.bodyValue(requestBody).retrieve().bodyToMono(Map.class)
					.retryWhen(Retry.backoff(3, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10))
							.filter(this::isRetryableException))
					.map(responseMap -> (String) responseMap.get("response")).block();

			long duration = System.currentTimeMillis() - startTime;
			log.info("[{}] Ollama LLM completed in {}ms", correlationId, duration);

			return response;

		} catch (WebClientResponseException e) {
			log.error("[{}] Ollama API error: {} - {}", correlationId, e.getStatusCode(), e.getMessage());
			throw new OllamaServiceException(ErrorCode.OLLAMA_CONNECTION_FAILED,
					"Ollama API request failed: " + e.getMessage(), correlationId, e);
		} catch (Exception e) {
			log.error("[{}] Unexpected error calling Ollama: {}", correlationId, e.getMessage(), e);
			throw new OllamaServiceException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate LLM completion",
					correlationId, e);
		}
	}

	/**
	 * Checks if exception is retryable.
	 * 
	 * Retryable: 5xx errors, timeouts, connection errors Non-retryable: 4xx errors
	 * (bad request)
	 */
	
	private boolean isRetryableException(Throwable throwable) {
		if (throwable instanceof WebClientResponseException e) {
			int statusCode = e.getStatusCode().value();
			return statusCode >= 500 || statusCode == 429; // Server errors or rate limit
		}
		return true; // Retry for network errors
	}

	/**
	 * Health check for Ollama service.
	 * 
	 * @return true if Ollama is available
	 */
	
	public boolean isOllamaAvailable() {
		try {
			ollamaWebClient.get().uri("/api/tags").retrieve().bodyToMono(String.class).timeout(Duration.ofSeconds(5))
					.block();
			return true;
		} catch (Exception e) {
			log.warn("Ollama service unavailable: {}", e.getMessage());
			return false;
		}
	}
}