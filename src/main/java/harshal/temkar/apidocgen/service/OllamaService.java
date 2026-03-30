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

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaService {

	private final WebClient ollamaWebClient;
	private final OllamaConfig ollamaConfig;

	public String generateCompletion(String prompt, String correlationId) {
		log.info("[{}] Calling Ollama LLM with model: {}", correlationId, ollamaConfig.getModel());

		// Verify model exists first
		if (!isModelAvailable(ollamaConfig.getModel())) {
			throw new OllamaServiceException(ErrorCode.OLLAMA_MODEL_NOT_AVAILABLE,
					String.format("Model '%s' not found. Please pull the model first using: ollama pull %s",
							ollamaConfig.getModel(), ollamaConfig.getModel()),
					correlationId);
		}

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
			log.error("[{}] Ollama API error: {} - {}", correlationId, e.getStatusCode(), e.getResponseBodyAsString());

			// Better error message for 404
			if (e.getStatusCode().value() == 404 && e.getResponseBodyAsString().contains("not found")) {
				throw new OllamaServiceException(ErrorCode.OLLAMA_MODEL_NOT_AVAILABLE,
						String.format("Model '%s' not found. Available models: %s", ollamaConfig.getModel(),
								getAvailableModels()),
						correlationId, e);
			}

			throw new OllamaServiceException(ErrorCode.OLLAMA_CONNECTION_FAILED,
					"Ollama API request failed: " + e.getMessage(), correlationId, e);
		} catch (Exception e) {
			log.error("[{}] Unexpected error calling Ollama: {}", correlationId, e.getMessage(), e);
			throw new OllamaServiceException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate LLM completion",
					correlationId, e);
		}
	}

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

	public boolean isModelAvailable(String modelName) {
		try {
			Map<String, Object> response = ollamaWebClient.get().uri("/api/tags").retrieve().bodyToMono(Map.class)
					.timeout(Duration.ofSeconds(5)).block();

			if (response != null && response.containsKey("models")) {
				java.util.List<Map<String, Object>> models = (java.util.List<Map<String, Object>>) response
						.get("models");
				return models.stream().anyMatch(model -> {
					String name = (String) model.get("name");
					return name.equals(modelName) || name.startsWith(modelName.split(":")[0]);
				});
			}
			return false;
		} catch (Exception e) {
			log.warn("Failed to check model availability: {}", e.getMessage());
			return false;
		}
	}

	private String getAvailableModels() {
		try {
			Map<String, Object> response = ollamaWebClient.get().uri("/api/tags").retrieve().bodyToMono(Map.class)
					.block();

			if (response != null && response.containsKey("models")) {
				java.util.List<Map<String, Object>> models = (java.util.List<Map<String, Object>>) response
						.get("models");
				return models.stream().map(model -> (String) model.get("name"))
						.collect(java.util.stream.Collectors.joining(", "));
			}
			return "none";
		} catch (Exception e) {
			return "unable to fetch";
		}
	}

	private boolean isRetryableException(Throwable throwable) {
		if (throwable instanceof WebClientResponseException e) {
			int statusCode = e.getStatusCode().value();
			// Don't retry 404 (model not found)
			if (statusCode == 404) {
				return false;
			}
			return statusCode >= 500 || statusCode == 429;
		}
		return true;
	}
}