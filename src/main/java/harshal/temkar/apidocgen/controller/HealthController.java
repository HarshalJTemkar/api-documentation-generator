package harshal.temkar.apidocgen.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import harshal.temkar.apidocgen.service.OllamaService;
import lombok.RequiredArgsConstructor;

/**
 * Health check controller for monitoring.
 * 
 * Endpoints: - GET /health: Basic health check - GET /health/ollama: Ollama
 * service availability
 */

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

	private final OllamaService ollamaService;

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

		return isAvailable ? ResponseEntity.ok(health) : ResponseEntity.status(503).body(health);
	}
}