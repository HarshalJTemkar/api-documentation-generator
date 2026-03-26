package harshal.temkar.apidocgen.agent;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import harshal.temkar.apidocgen.model.dto.ApiEndpointInfo;
import harshal.temkar.apidocgen.model.dto.TechnicalWorkflowDoc;
import harshal.temkar.apidocgen.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Agent responsible for documenting technical workflow using LLM.
 * 
 * Analyzes: - Service method calls - Database operations - External API calls -
 * Transaction boundaries - Performance considerations
 * 
 * Output: Step-by-step technical flow.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class TechnicalWorkflowAgent implements Agent<ApiEndpointInfo, TechnicalWorkflowDoc> {

	private final OllamaService ollamaService;

	private static final String TECHNICAL_WORKFLOW_PROMPT = """
			Analyze the following Java REST API method and document the technical workflow.

			Controller: %s
			Method: %s
			HTTP: %s %s

			Source Code:
			```java
			%s
			```

			Provide:
			1. Workflow Summary: High-level technical flow
			2. Steps: Numbered step-by-step execution flow
			   - Step number
			   - Description
			   - Component involved (Controller/Service/Repository)
			   - Action performed
			3. Service Dependencies: List all injected services used
			4. Database Operations: Any DB queries/transactions
			5. External API Calls: Third-party integrations
			6. Transaction Boundary: @Transactional scope if any
			7. Performance Considerations: Potential bottlenecks, N+1 queries, etc.

			Respond in JSON format.

			IMPORTANT:
			- Document ONLY what is in the code
			- Do NOT assume or hallucinate functionality
			- If no database operation, state "No database operations"
			- Be precise and technical
			""";

	@Override
	@Cacheable(value = "llmResponses", key = "'workflow_' + #input.methodName + '_' + #input.controllerClassName")
	public TechnicalWorkflowDoc execute(ApiEndpointInfo input, String correlationId) {
		log.info("[{}] TechnicalWorkflowAgent: Analyzing workflow for {}.{}", correlationId,
				input.getControllerClassName(), input.getMethodName());

		String prompt = String.format(TECHNICAL_WORKFLOW_PROMPT, input.getControllerClassName(), input.getMethodName(),
				input.getHttpMethod(), input.getUri(), input.getMethodSourceCode());

		long startTime = System.currentTimeMillis();
		String llmResponse = ollamaService.generateCompletion(prompt, correlationId);
		long duration = System.currentTimeMillis() - startTime;

		log.info("[{}] TechnicalWorkflowAgent completed in {}ms", correlationId, duration);

		return parseTechnicalWorkflowResponse(llmResponse);
	}

	@Override
	public String getAgentName() {
		return "TechnicalWorkflowAgent";
	}

	private TechnicalWorkflowDoc parseTechnicalWorkflowResponse(String llmResponse) {
		// Parse LLM JSON response
		// Implementation would use Jackson ObjectMapper
		return TechnicalWorkflowDoc.builder().workflowSummary("Extracted from LLM").build();
	}
}