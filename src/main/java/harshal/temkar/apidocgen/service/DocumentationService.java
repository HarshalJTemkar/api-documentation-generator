package harshal.temkar.apidocgen.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import harshal.temkar.apidocgen.model.dto.DocumentationRequest;
import harshal.temkar.apidocgen.model.dto.DocumentationResponse;
import harshal.temkar.apidocgen.util.CorrelationIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Main service facade for documentation generation.
 * 
 * Delegates to AgenticOrchestrationService for actual processing. Handles
 * correlation ID management.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentationService {

	private final AgenticOrchestrationService orchestrationService;

	/**
	 * Generates documentation for single API endpoint.
	 * 
	 * @param request Documentation request
	 * @return Documentation response
	 */

	public CompletableFuture<DocumentationResponse> generateDocumentation(DocumentationRequest request) {
		String correlationId = CorrelationIdUtil.generateCorrelationId();

		log.info("[{}] Generating documentation for: {} {}", correlationId, request.getHttpMethod(),
				request.getApiUri());

		return orchestrationService.orchestrateDocumentationGeneration(request.getProjectPath(), request.getApiUri(),
				request.getHttpMethod(), request.isIncludeBusinessLogic(), request.isIncludeTechnicalWorkflow(),
				correlationId).whenComplete((response, throwable) -> {
					if (throwable != null) {
						log.error("[{}] Documentation generation failed", correlationId, throwable);
					} else {
						log.info("[{}] Documentation generation completed successfully", correlationId);
					}
					CorrelationIdUtil.clearCorrelationId();
				});
	}
}