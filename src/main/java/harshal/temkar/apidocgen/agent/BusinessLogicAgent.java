package harshal.temkar.apidocgen.agent;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import harshal.temkar.apidocgen.model.dto.ApiEndpointInfo;
import harshal.temkar.apidocgen.model.dto.BusinessLogicDoc;
import harshal.temkar.apidocgen.service.OllamaService;
import harshal.temkar.apidocgen.service.PromptBuilderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Enhanced agent with domain-aware prompt generation.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessLogicAgent implements Agent<ApiEndpointInfo, BusinessLogicDoc> {

	private final OllamaService ollamaService;
	private final PromptBuilderService promptBuilderService;
	private final ObjectMapper objectMapper;

	@Override
	@Cacheable(value = "llmResponses", key = "#input.methodName + '_' + #input.controllerClassName")
	public BusinessLogicDoc execute(ApiEndpointInfo input, String correlationId) {
		log.info("[{}] BusinessLogicAgent: Analyzing with domain context: {}.{}", correlationId,
				input.getControllerClassName(), input.getMethodName());

		// Build domain-aware prompt
		String prompt = promptBuilderService.buildBusinessLogicPrompt(input, correlationId);

		log.debug("[{}] Generated prompt:\n{}", correlationId, prompt);

		long startTime = System.currentTimeMillis();
		String llmResponse = ollamaService.generateCompletion(prompt, correlationId);
		long duration = System.currentTimeMillis() - startTime;

		log.info("[{}] BusinessLogicAgent completed in {}ms", correlationId, duration);

		return parseBusinessLogicResponse(llmResponse, correlationId);
	}

	@Override
	public String getAgentName() {
		return "BusinessLogicAgent";
	}

	private BusinessLogicDoc parseBusinessLogicResponse(String llmResponse, String correlationId) {
		try {
			// Extract JSON from LLM response (may include markdown)
			String jsonContent = extractJsonFromResponse(llmResponse);

			return objectMapper.readValue(jsonContent, BusinessLogicDoc.class);

		} catch (Exception e) {
			log.error("[{}] Failed to parse LLM response: {}", correlationId, e.getMessage());

			// Fallback: return raw response in summary
			return BusinessLogicDoc.builder().summary(llmResponse).build();
		}
	}

	private String extractJsonFromResponse(String response) {
		// LLM might wrap JSON in markdown code blocks
		if (response.contains("```json")) {
			int start = response.indexOf("```json") + 7;
			int end = response.lastIndexOf("```");
			return response.substring(start, end).trim();
		}

		// Or return as-is if already JSON
		return response.trim();
	}
}