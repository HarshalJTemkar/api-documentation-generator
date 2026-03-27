package harshal.temkar.apidocgen.agent;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import harshal.temkar.apidocgen.ai.TechnicalWorkflowAnalyzer;
import harshal.temkar.apidocgen.config.PromptConfig;
import harshal.temkar.apidocgen.model.dto.ApiEndpointInfo;
import harshal.temkar.apidocgen.model.dto.TechnicalWorkflowDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TechnicalWorkflowAgent implements Agent<ApiEndpointInfo, TechnicalWorkflowDoc> {

	private final TechnicalWorkflowAnalyzer analyzer;
	private final PromptConfig promptConfig;

	@Override
	@Cacheable(value = "llmResponses", key = "'workflow_' + #input.methodName + '_' + #input.controllerClassName")
	public TechnicalWorkflowDoc execute(ApiEndpointInfo input, String correlationId) {
		log.info("[{}] TechnicalWorkflowAgent: Analyzing {}.{}", correlationId, input.getControllerClassName(),
				input.getMethodName());

		long startTime = System.currentTimeMillis();

		try {
			String technicalPatterns = buildTechnicalPatternsString();
			String codingStandards = buildCodingStandardsString();

			TechnicalWorkflowDoc result = analyzer.analyzeTechnicalWorkflow(technicalPatterns, codingStandards,
					input.getMethodSourceCode(), input.getControllerClassName(), input.getMethodName(),
					input.getHttpMethod(), input.getUri());

			long duration = System.currentTimeMillis() - startTime;
			log.info("[{}] TechnicalWorkflowAgent completed in {}ms", correlationId, duration);

			return result;

		} catch (Exception e) {
			log.error("[{}] TechnicalWorkflowAgent failed: {}", correlationId, e.getMessage(), e);

			return TechnicalWorkflowDoc.builder().workflowSummary("Failed to analyze: " + e.getMessage()).build();
		}
	}

	@Override
	public String getAgentName() {
		return "TechnicalWorkflowAgent-LangChain4j";
	}

	private String buildTechnicalPatternsString() {
		StringBuilder patterns = new StringBuilder("## Technical Patterns\n");
		promptConfig.getTechnicalPatterns()
				.forEach((key, value) -> patterns.append("**").append(key).append("**: ").append(value).append("\n"));
		return patterns.toString();
	}

	private String buildCodingStandardsString() {
		StringBuilder standards = new StringBuilder("## Coding Standards\n");
		promptConfig.getCodingStandards()
				.forEach((key, value) -> standards.append("**").append(key).append("**: ").append(value).append("\n"));
		return standards.toString();
	}
}