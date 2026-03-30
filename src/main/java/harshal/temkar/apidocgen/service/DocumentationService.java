package harshal.temkar.apidocgen.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import harshal.temkar.apidocgen.model.dto.DocumentationRequest;
import harshal.temkar.apidocgen.model.dto.DocumentationResponse;
import harshal.temkar.apidocgen.util.CorrelationIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Documentation service with both sync and async methods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentationService {

	private final AgenticOrchestrationService orchestrationService;

	/**
	 * SYNCHRONOUS: Blocks until documentation is generated.
	 * 
	 * Use for: - Single API documentation - When client can wait 5-10 seconds -
	 * Simpler error handling
	 */
	public DocumentationResponse generateDocumentationSync(DocumentationRequest request) {
		String correlationId = CorrelationIdUtil.getCorrelationId();

		log.info("[{}] Generating documentation SYNC for: {} {}", correlationId, request.getHttpMethod(),
				request.getApiUri());

		try {
			// Blocks until complete
			CompletableFuture<DocumentationResponse> future = orchestrationService.orchestrateDocumentationGeneration(
					request.getProjectPath(), request.getApiUri(), request.getHttpMethod(),
					request.isIncludeBusinessLogic(), request.isIncludeTechnicalWorkflow(), correlationId);

			// Wait for completion (blocks current thread)
			DocumentationResponse response = future.get();

			log.info("[{}] Documentation generation SYNC completed successfully", correlationId);
			return response;

		} catch (Exception e) {
			log.error("[{}] Documentation generation SYNC failed", correlationId, e);
			throw new RuntimeException("Documentation generation failed", e);
		}
	}

	/**
	 * ASYNCHRONOUS: Returns immediately, processes in background.
	 * 
	 * Use for: - Long-running operations - When client can't wait - Batch
	 * processing
	 */
	@Async("agentExecutor")
	public void generateDocumentationAsync(DocumentationRequest request, String correlationId) {
		log.info("[{}] Generating documentation ASYNC for: {} {}", correlationId, request.getHttpMethod(),
				request.getApiUri());

		try {
			CompletableFuture<DocumentationResponse> future = orchestrationService.orchestrateDocumentationGeneration(
					request.getProjectPath(), request.getApiUri(), request.getHttpMethod(),
					request.isIncludeBusinessLogic(), request.isIncludeTechnicalWorkflow(), correlationId);

			future.whenComplete((response, throwable) -> {
				if (throwable != null) {
					log.error("[{}] Documentation generation ASYNC failed", correlationId, throwable);
					// Store error in cache/database
				} else {
					log.info("[{}] Documentation generation ASYNC completed successfully", correlationId);
					// Store result in cache/database
				}
			});

		} catch (Exception e) {
			log.error("[{}] Documentation generation ASYNC failed to start", correlationId, e);
		} finally {
			CorrelationIdUtil.clearCorrelationId();
		}
	}
}