package harshal.temkar.apidocgen.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import harshal.temkar.apidocgen.config.OllamaConfig;
import harshal.temkar.apidocgen.service.OllamaService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

	private final OllamaService ollamaService;
	private final OllamaConfig ollamaConfig;
	private final WebClient ollamaWebClient;

	@GetMapping
	public ResponseEntity<Map<String, String>> health() {
		Map<String, String> health = new HashMap<>();
		health.put("status", "UP");
		health.put("service", "API Documentation Generator");
		return ResponseEntity.ok(health);
	}

	@GetMapping("/ollama")
	public ResponseEntity<Map<String, Object>> ollamaHealth() {
		boolean isAvailable = ollamaService.isOllamaAvailable();

		Map<String, Object> health = new HashMap<>();
		health.put("service", "Ollama LLM");
		health.put("status", isAvailable ? "UP" : "DOWN");
		health.put("available", isAvailable);
		health.put("configuredModel", ollamaConfig.getModel());

		// Check if model exists
		if (isAvailable) {
			boolean modelExists = checkModelExists(ollamaConfig.getModel());
			health.put("modelExists", modelExists);
			health.put("availableModels", getAvailableModels());

			if (!modelExists) {
				health.put("warning", "Configured model '" + ollamaConfig.getModel() + "' not found");
				health.put("status", "DEGRADED");
			}
		}

		return isAvailable && (boolean) health.getOrDefault("modelExists", false) ? ResponseEntity.ok(health)
				: ResponseEntity.status(503).body(health);
	}

	private boolean checkModelExists(String modelName) {
		try {
			Map<String, Object> response = ollamaWebClient.get().uri("/api/tags").retrieve().bodyToMono(Map.class)
					.block();

			if (response != null && response.containsKey("models")) {
				List<Map<String, Object>> models = (List<Map<String, Object>>) response.get("models");
				return models.stream().anyMatch(model -> {
					String name = (String) model.get("name");
					return name.equals(modelName) || name.startsWith(modelName.split(":")[0]);
				});
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	private List<String> getAvailableModels() {
		try {
			Map<String, Object> response = ollamaWebClient.get().uri("/api/tags").retrieve().bodyToMono(Map.class)
					.block();

			if (response != null && response.containsKey("models")) {
				List<Map<String, Object>> models = (List<Map<String, Object>>) response.get("models");
				return models.stream().map(model -> (String) model.get("name")).toList();
			}
			return List.of();
		} catch (Exception e) {
			return List.of();
		}
	}
}