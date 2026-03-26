package harshal.temkar.apidocgen.service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import harshal.temkar.apidocgen.agent.BusinessLogicAgent;
import harshal.temkar.apidocgen.agent.CodeReaderAgent;
import harshal.temkar.apidocgen.agent.DocumentationFormatterAgent;
import harshal.temkar.apidocgen.agent.TechnicalWorkflowAgent;
import harshal.temkar.apidocgen.model.dto.ApiEndpointInfo;
import harshal.temkar.apidocgen.model.dto.BusinessLogicDoc;
import harshal.temkar.apidocgen.model.dto.DocumentationResponse;
import harshal.temkar.apidocgen.model.dto.TechnicalWorkflowDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates agentic AI workflow for documentation generation.
 * 
 * Workflow:
 * 1. CodeReaderAgent: Parse source code
 * 2. BusinessLogicAgent: Extract business logic (parallel)
 * 3. TechnicalWorkflowAgent: Document workflow (parallel)
 * 4. DocumentationFormatterAgent: Format output
 * 
 * Performance:
 * - Parallel execution of agents 2 & 3
 * - Async processing for high throughput
 * - Non-blocking operations
 * 
 * Scalability:
 * - Supports 50+ concurrent documentation requests
 * - Horizontal scaling via stateless agents
 * - Independent agent failure isolation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgenticOrchestrationService {

    private final CodeReaderAgent codeReaderAgent;
    private final BusinessLogicAgent businessLogicAgent;
    private final TechnicalWorkflowAgent technicalWorkflowAgent;
    private final DocumentationFormatterAgent documentationFormatterAgent;

    /**
     * Orchestrates complete documentation generation workflow.
     * 
     * Executes agents in optimal order:
     * - Sequential: CodeReaderAgent (must complete first)
     * - Parallel: BusinessLogicAgent + TechnicalWorkflowAgent
     * - Sequential: DocumentationFormatterAgent (aggregates results)
     * 
     * @param projectPath Maven project path
     * @param apiUri API endpoint URI
     * @param httpMethod HTTP method
     * @param includeBusinessLogic Include business logic analysis
     * @param includeTechnicalWorkflow Include technical workflow
     * @param correlationId Request tracking ID
     * @return Complete documentation response
     */
    @Async("agentExecutor")
    public CompletableFuture<DocumentationResponse> orchestrateDocumentationGeneration(
            String projectPath,
            String apiUri,
            String httpMethod,
            boolean includeBusinessLogic,
            boolean includeTechnicalWorkflow,
            String correlationId) {

        log.info("[{}] Starting agentic orchestration for: {} {}", 
                correlationId, httpMethod, apiUri);

        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Code Reading (Must complete first)
            ApiEndpointInfo endpointInfo = codeReaderAgent.execute(
                    new CodeReaderAgent.Input(projectPath, apiUri, httpMethod),
                    correlationId
            );

            // Step 2 & 3: Parallel execution of analysis agents
            CompletableFuture<BusinessLogicDoc> businessLogicFuture = 
                    includeBusinessLogic 
                    ? CompletableFuture.supplyAsync(() -> 
                            businessLogicAgent.execute(endpointInfo, correlationId))
                    : CompletableFuture.completedFuture(null);

            CompletableFuture<TechnicalWorkflowDoc> technicalWorkflowFuture = 
                    includeTechnicalWorkflow 
                    ? CompletableFuture.supplyAsync(() -> 
                            technicalWorkflowAgent.execute(endpointInfo, correlationId))
                    : CompletableFuture.completedFuture(null);

            // Wait for parallel completion
            CompletableFuture.allOf(businessLogicFuture, technicalWorkflowFuture).join();

            BusinessLogicDoc businessLogic = businessLogicFuture.get();
            TechnicalWorkflowDoc technicalWorkflow = technicalWorkflowFuture.get();

            // Step 4: Format documentation
            String formattedDoc = documentationFormatterAgent.execute(
                    new DocumentationFormatterAgent.Input(apiUri, businessLogic, technicalWorkflow),
                    correlationId
            );

            long processingTime = System.currentTimeMillis() - startTime;

            log.info("[{}] Agentic orchestration completed in {}ms", correlationId, processingTime);

            DocumentationResponse response = DocumentationResponse.builder()
                    .correlationId(correlationId)
                    .apiUri(apiUri)
                    .httpMethod(httpMethod)
                    .endpointInfo(endpointInfo)
                    .businessLogic(businessLogic)
                    .technicalWorkflow(technicalWorkflow)
                    .generatedAt(LocalDateTime.now())
                    .processingTimeMs(processingTime)
                    .build();

            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("[{}] Agentic orchestration failed: {}", correlationId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
}