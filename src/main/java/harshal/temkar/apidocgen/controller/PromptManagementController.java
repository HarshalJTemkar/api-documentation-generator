package harshal.temkar.apidocgen.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import harshal.temkar.apidocgen.config.PromptConfig;
import harshal.temkar.apidocgen.model.dto.ApiEndpointInfo;
import harshal.temkar.apidocgen.service.PromptTestingService;
import harshal.temkar.apidocgen.util.CorrelationIdUtil;
import lombok.RequiredArgsConstructor;

/**
 * REST API for managing and testing prompts.
 * 
 * Endpoints: - GET /api/v1/prompts/config: Get current prompt configuration -
 * PUT /api/v1/prompts/config: Update prompt configuration - POST
 * /api/v1/prompts/test: Test prompt variations
 */

@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
public class PromptManagementController {

	private final PromptConfig promptConfig;
	private final PromptTestingService promptTestingService;

	/**
	 * Get current prompt configuration.
	 */
	@GetMapping("/config")
	public ResponseEntity<PromptConfig> getPromptConfig() {
		return ResponseEntity.ok(promptConfig);
	}

	/**
	 * Update specific domain context.
	 */
	@PutMapping("/config/domain-context")
	public ResponseEntity<Void> updateDomainContext(@RequestBody Map<String, String> domainContext) {

		promptConfig.getDomainContext().putAll(domainContext);
		return ResponseEntity.ok().build();
	}

	/**
	 * Update coding standards.
	 */
	@PutMapping("/config/coding-standards")
	public ResponseEntity<Void> updateCodingStandards(@RequestBody Map<String, String> codingStandards) {

		promptConfig.getCodingStandards().putAll(codingStandards);
		return ResponseEntity.ok().build();
	}

	/**
	 * Add entity-specific rules.
	 */
	@PutMapping("/config/entity-rules/{entityName}")
	public ResponseEntity<Void> updateEntityRules(@PathVariable String entityName,
			@RequestBody Map<String, Object> rules) {

		promptConfig.getEntitySpecificRules().put(entityName, (List<String>) rules.get("rules"));
		return ResponseEntity.ok().build();
	}

	/**
	 * Test prompt variations for quality comparison.
	 */
	@PostMapping("/test")
	public ResponseEntity<Map<String, String>> testPromptVariations(@RequestBody ApiEndpointInfo endpointInfo) {

		String correlationId = CorrelationIdUtil.generateCorrelationId();

		Map<String, String> results = promptTestingService.comparePromptVariations(endpointInfo, correlationId);

		return ResponseEntity.ok(results);
	}
}