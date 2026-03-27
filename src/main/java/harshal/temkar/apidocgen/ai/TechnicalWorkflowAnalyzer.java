package harshal.temkar.apidocgen.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import harshal.temkar.apidocgen.model.dto.TechnicalWorkflowDoc;

/**
 * AI Service for technical workflow analysis.
 */
public interface TechnicalWorkflowAnalyzer {

    @SystemMessage("""
            You are a senior software architect documenting technical execution flows.
            
            Your task is to analyze REST API methods and document their technical workflow.
            
            Focus on:
            - Step-by-step execution flow
            - Service/repository dependencies
            - Database operations
            - External API integrations
            - Transaction boundaries
            - Performance implications
            
            Always respond in valid JSON format matching TechnicalWorkflowDoc structure.
            
            {{technicalPatterns}}
            
            {{codingStandards}}
            """)
    @UserMessage("""
            Analyze the following REST API method:
            
            **Controller:** {{controllerName}}
            **Method:** {{methodName}}
            **Endpoint:** {{httpMethod}} {{uri}}
            
            **Source Code:**
            ```java
            {{sourceCode}}
            ```
            
            Document the technical workflow with step-by-step execution flow.
            
            CRITICAL:
            - Document only what exists in the code
            - Identify all service/repository calls
            - Note database operations
            - Highlight performance considerations
            """)
    TechnicalWorkflowDoc analyzeTechnicalWorkflow(
            @V("technicalPatterns") String technicalPatterns,
            @V("codingStandards") String codingStandards,
            @V("sourceCode") String sourceCode,
            @V("controllerName") String controllerName,
            @V("methodName") String methodName,
            @V("httpMethod") String httpMethod,
            @V("uri") String uri
    );
}