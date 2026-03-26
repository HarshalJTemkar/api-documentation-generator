package harshal.temkar.apidocgen.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import harshal.temkar.apidocgen.model.dto.ApiEndpointInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for testing and validating prompt quality.
 * 
 * Helps iterate on prompts by: - A/B testing different prompt versions -
 * Measuring LLM response quality - Tracking prompt effectiveness metrics
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptTestingService {

	private final PromptBuilderService promptBuilderService;
	private final OllamaService ollamaService;

	/**
	 * Test multiple prompt variations and compare outputs.
	 */
	public Map<String, String> comparePromptVariations(ApiEndpointInfo endpointInfo, String correlationId) {

		Map<String, String> results = new HashMap<>();

		// Version 1: Basic prompt
		String basicPrompt = buildBasicPrompt(endpointInfo);
		String basicResponse = ollamaService.generateCompletion(basicPrompt, correlationId + "-basic");
		results.put("basic", basicResponse);

		// Version 2: Domain-enhanced prompt
		String domainPrompt = promptBuilderService.buildBusinessLogicPrompt(endpointInfo, correlationId);
		String domainResponse = ollamaService.generateCompletion(domainPrompt, correlationId + "-domain");
		results.put("domain-enhanced", domainResponse);

		// Version 3: Example-driven prompt (few-shot learning)
		String fewShotPrompt = buildFewShotPrompt(endpointInfo);
		String fewShotResponse = ollamaService.generateCompletion(fewShotPrompt, correlationId + "-fewshot");
		results.put("few-shot", fewShotResponse);

		return results;
	}

	/**
	 * Measure prompt effectiveness metrics.
	 */
	public PromptMetrics measurePromptQuality(String prompt, String llmResponse) {
		return PromptMetrics.builder().promptLength(prompt.length()).responseLength(llmResponse.length())
				.hasStructuredOutput(llmResponse.contains("{") && llmResponse.contains("}"))
				.containsDomainTerms(countDomainTerms(llmResponse)).specificity(calculateSpecificity(llmResponse))
				.build();
	}

	private String buildBasicPrompt(ApiEndpointInfo endpointInfo) {
		return String.format("""
				Analyze this API method and document the business logic.

				Code:
				%s

				Provide business logic documentation in JSON format.
				""", endpointInfo.getMethodSourceCode());
	}

	private String buildFewShotPrompt(ApiEndpointInfo endpointInfo) {
		return String.format("""
				Analyze this API method and document the business logic.

				Example 1:
				Input: public void createUser(UserRequest req) { ... }
				Output: {"summary": "Creates new user account", "businessRules": [...]}

				Example 2:
				Input: public Order placeOrder(OrderRequest req) { ... }
				Output: {"summary": "Places customer order with payment", "businessRules": [...]}

				Now analyze:
				Code:
				%s

				Output:
				""", endpointInfo.getMethodSourceCode());
	}

	private int countDomainTerms(String response) {
		// Count occurrences of domain-specific terms
		// Implementation depends on your domain
		return 0;
	}

	private double calculateSpecificity(String response) {
		// Measure how specific vs generic the response is
		// Higher ratio of technical terms vs generic words
		return 0.0;
	}

	@lombok.Builder
	@lombok.Data
	public static class PromptMetrics {
		private int promptLength;
		private int responseLength;
		private boolean hasStructuredOutput;
		private int containsDomainTerms;
		private double specificity;
	}
}