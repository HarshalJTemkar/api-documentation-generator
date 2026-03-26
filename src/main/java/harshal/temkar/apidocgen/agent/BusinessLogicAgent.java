package harshal.temkar.apidocgen.agent;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import harshal.temkar.apidocgen.model.dto.ApiEndpointInfo;
import harshal.temkar.apidocgen.model.dto.BusinessLogicDoc;
import harshal.temkar.apidocgen.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Agent responsible for extracting business logic using LLM.
 * 
 * Uses Ollama to analyze:
 * - Business rules
 * - Validations
 * - Data transformations
 * - Expected behavior
 * - Error scenarios
 * 
 * Caching: Results cached by method signature hash.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessLogicAgent implements Agent<ApiEndpointInfo, BusinessLogicDoc> {

    private final OllamaService ollamaService;

    private static final String BUSINESS_LOGIC_PROMPT = """
            Analyze the following Java REST API method and provide business logic documentation.
            
            Controller: %s
            Method: %s
            HTTP: %s %s
            
            Source Code:
            ```java
            %s
            ```
            
            Extract:
            1. Summary: Brief description of what this API does
            2. Purpose: Business purpose of this endpoint
            3. Business Rules: List all business rules implemented
            4. Validations: Input validations and constraints
            5. Data Transformations: How data is transformed/mapped
            6. Expected Behavior: Normal execution flow outcome
            7. Error Scenarios: Possible error cases and handling
            
            Respond in JSON format with keys: summary, purpose, businessRules, validations, 
            dataTransformations, expectedBehavior, errorScenarios
            
            IMPORTANT:
            - Do NOT provide generic/default descriptions
            - Analyze ONLY the actual code provided
            - If no business logic exists, state "No business logic found"
            - Be specific and code-driven in your analysis
            """;

    @Override
    @Cacheable(value = "llmResponses", key = "#input.methodName + '_' + #input.controllerClassName")
    public BusinessLogicDoc execute(ApiEndpointInfo input, String correlationId) {
        log.info("[{}] BusinessLogicAgent: Analyzing business logic for {}.{}", 
                correlationId, input.getControllerClassName(), input.getMethodName());

        String prompt = String.format(
                BUSINESS_LOGIC_PROMPT,
                input.getControllerClassName(),
                input.getMethodName(),
                input.getHttpMethod(),
                input.getUri(),
                input.getMethodSourceCode()
        );

        long startTime = System.currentTimeMillis();
        String llmResponse = ollamaService.generateCompletion(prompt, correlationId);
        long duration = System.currentTimeMillis() - startTime;

        log.info("[{}] BusinessLogicAgent completed in {}ms", correlationId, duration);

        return parseBusinessLogicResponse(llmResponse);
    }

    @Override
    public String getAgentName() {
        return "BusinessLogicAgent";
    }

    private BusinessLogicDoc parseBusinessLogicResponse(String llmResponse) {
        // Parse LLM JSON response
        // Implementation would use Jackson ObjectMapper
        // Simplified for brevity
        return BusinessLogicDoc.builder()
                .summary("Extracted from LLM")
                .build();
    }
}