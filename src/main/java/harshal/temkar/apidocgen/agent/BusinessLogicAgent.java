package harshal.temkar.apidocgen.agent;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import harshal.temkar.apidocgen.ai.BusinessLogicAnalyzer;
import harshal.temkar.apidocgen.config.PromptConfig;
import harshal.temkar.apidocgen.model.dto.ApiEndpointInfo;
import harshal.temkar.apidocgen.model.dto.BusinessLogicDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Business logic agent using LangChain4j AI Services.
 * 
 * Benefits over direct WebClient: - Type-safe LLM responses - Automatic JSON
 * parsing - Built-in retry logic - Conversation memory support - Cleaner code
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessLogicAgent implements Agent<ApiEndpointInfo, BusinessLogicDoc> {

	private final BusinessLogicAnalyzer analyzer;
	private final PromptConfig promptConfig;

	@Override
	@Cacheable(value = "llmResponses", key = "#input.methodName + '_' + #input.controllerClassName")
	public BusinessLogicDoc execute(ApiEndpointInfo input, String correlationId) {
		log.info("[{}] BusinessLogicAgent: Analyzing {}.{}", correlationId, input.getControllerClassName(),
				input.getMethodName());

		long startTime = System.currentTimeMillis();

		try {
			// Build domain context string
			String domainContext = buildDomainContextString();
			String codingStandards = buildCodingStandardsString();

			// Call AI Service (LangChain4j handles everything)
			BusinessLogicDoc result = analyzer.analyzeBusinessLogic(domainContext, codingStandards,
					input.getMethodSourceCode(), input.getControllerClassName(), input.getMethodName(),
					input.getHttpMethod(), input.getUri());

			long duration = System.currentTimeMillis() - startTime;
			log.info("[{}] BusinessLogicAgent completed in {}ms", correlationId, duration);

			return result;

		} catch (Exception e) {
			log.error("[{}] BusinessLogicAgent failed: {}", correlationId, e.getMessage(), e);

			// Fallback
			return BusinessLogicDoc.builder().summary("Failed to analyze: " + e.getMessage()).build();
		}
	}

	@Override
	public String getAgentName() {
		return "BusinessLogicAgent-LangChain4j";
	}

	private String buildDomainContextString() {
		StringBuilder context = new StringBuilder("## Domain Context\n");
		promptConfig.getDomainContext()
				.forEach((key, value) -> context.append("**").append(key).append("**: ").append(value).append("\n"));
		return context.toString();
	}

	private String buildCodingStandardsString() {
		StringBuilder standards = new StringBuilder("## Coding Standards\n");
		promptConfig.getCodingStandards()
				.forEach((key, value) -> standards.append("**").append(key).append("**: ").append(value).append("\n"));
		return standards.toString();
	}
}