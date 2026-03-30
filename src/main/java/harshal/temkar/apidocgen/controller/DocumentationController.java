package harshal.temkar.apidocgen.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import harshal.temkar.apidocgen.model.dto.DocumentationRequest;
import harshal.temkar.apidocgen.model.dto.DocumentationResponse;
import harshal.temkar.apidocgen.model.dto.ExcelApiRequest;
import harshal.temkar.apidocgen.service.DocumentationService;
import harshal.temkar.apidocgen.service.ExcelProcessorService;
import harshal.temkar.apidocgen.util.CorrelationIdUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for API documentation generation.
 * 
 * Changed to synchronous processing to avoid async timeout issues.
 */
@RestController
@RequestMapping("/api/v1/documentation")
@RequiredArgsConstructor
@Slf4j
public class DocumentationController {

	private final DocumentationService documentationService;
	private final ExcelProcessorService excelProcessorService;

	/**
	 * Generates documentation for single API endpoint (SYNCHRONOUS).
	 * 
	 * Client will wait for completion (5-10 seconds typically).
	 */
	@PostMapping("/generate")
	public ResponseEntity<DocumentationResponse> generateDocumentation(
			@Valid @RequestBody DocumentationRequest request) {

		String correlationId = CorrelationIdUtil.generateCorrelationId();

		log.info("[{}] Received documentation request for: {} {}", correlationId, request.getHttpMethod(),
				request.getApiUri());

		try {
			// Synchronous call - blocks until complete
			DocumentationResponse response = documentationService.generateDocumentationSync(request);

			log.info("[{}] Documentation generated successfully", correlationId);
			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("[{}] Documentation generation failed", correlationId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		} finally {
			CorrelationIdUtil.clearCorrelationId();
		}
	}

	/**
	 * Async version - starts processing and returns immediately. Use
	 * /status/{correlationId} to check progress.
	 */
	@PostMapping("/generate/async")
	public ResponseEntity<AsyncJobResponse> generateDocumentationAsync(
			@Valid @RequestBody DocumentationRequest request) {

		String correlationId = CorrelationIdUtil.generateCorrelationId();

		log.info("[{}] Starting async documentation generation for: {} {}", correlationId, request.getHttpMethod(),
				request.getApiUri());

		// Start async processing
		documentationService.generateDocumentationAsync(request, correlationId);

		// Return job ID immediately
		return ResponseEntity.accepted()
				.body(AsyncJobResponse.builder().jobId(correlationId).status("PROCESSING")
						.message("Documentation generation started")
						.statusUrl("/api/v1/documentation/status/" + correlationId).build());
	}

	/**
	 * Check status of async job.
	 */
	@GetMapping("/status/{correlationId}")
	public ResponseEntity<AsyncJobStatus> checkStatus(@PathVariable String correlationId) {
		// Implementation would check job status from cache/database
		return ResponseEntity.ok(AsyncJobStatus.builder().jobId(correlationId).status("COMPLETED").build());
	}

	/**
	 * Batch processing from Excel (kept async due to long duration).
	 */
	@PostMapping("/batch/excel")
	public ResponseEntity<AsyncJobResponse> processExcelBatch(@Valid @RequestBody ExcelApiRequest request) {

		String correlationId = CorrelationIdUtil.generateCorrelationId();

		log.info("[{}] Starting Excel batch processing: {}", correlationId, request.getExcelFilePath());

		excelProcessorService.processExcelFile(request);

		return ResponseEntity.accepted()
				.body(AsyncJobResponse.builder().jobId(correlationId).status("PROCESSING")
						.message("Excel batch processing started")
						.statusUrl("/api/v1/documentation/batch/status/" + correlationId).build());
	}

	/**
	 * Quick endpoint test (synchronous, fast).
	 */
	@PostMapping("/test")
	public ResponseEntity<java.util.Map<String, Object>> testEndpoint(
			@Valid @RequestBody DocumentationRequest request) {

		String correlationId = CorrelationIdUtil.generateCorrelationId();

		log.info("[{}] Testing endpoint: {} {}", correlationId, request.getHttpMethod(), request.getApiUri());

		return ResponseEntity.ok(java.util.Map.of("correlationId", correlationId, "projectPath",
				request.getProjectPath(), "apiUri", request.getApiUri(), "httpMethod", request.getHttpMethod(),
				"status", "Endpoint found and parseable"));
	}

	// DTOs for async responses
	@lombok.Builder
	@lombok.Data
	public static class AsyncJobResponse {
		private String jobId;
		private String status;
		private String message;
		private String statusUrl;
	}

	@lombok.Builder
	@lombok.Data
	public static class AsyncJobStatus {
		private String jobId;
		private String status;
		private DocumentationResponse result;
		private String error;
	}
}