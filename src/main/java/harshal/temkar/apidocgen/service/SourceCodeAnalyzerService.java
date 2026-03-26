package harshal.temkar.apidocgen.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import harshal.temkar.apidocgen.exception.ErrorCode;
import harshal.temkar.apidocgen.exception.SourceCodeParseException;
import harshal.temkar.apidocgen.model.dto.ApiEndpointInfo;
import harshal.temkar.apidocgen.parser.ControllerParser;
import harshal.temkar.apidocgen.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for analyzing Spring Boot source code.
 * 
 * Features:
 * - Maven project validation
 * - Controller detection
 * - Endpoint mapping extraction
 * - Source code retrieval
 * 
 * Performance:
 * - Cached analysis results
 * - Efficient file system traversal
 * - Parallel processing for large projects
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SourceCodeAnalyzerService {

    private final ControllerParser controllerParser;

    /**
     * Analyzes API endpoint from Maven project.
     * 
     * Steps:
     * 1. Validate Maven project structure
     * 2. Locate controller class
     * 3. Find method matching URI
     * 4. Extract method metadata
     * 5. Read method source code
     * 
     * @param projectPath Root project path
     * @param apiUri API endpoint URI
     * @param httpMethod HTTP method (GET, POST, etc.)
     * @param correlationId Request tracking ID
     * @return Endpoint information
     */
    
    @Cacheable(value = "parsedCode", key = "#projectPath + '_' + #apiUri + '_' + #httpMethod")
    public ApiEndpointInfo analyzeEndpoint(
            String projectPath,
            String apiUri,
            String httpMethod,
            String correlationId) {

        log.info("[{}] Analyzing endpoint: {} {} in project: {}", 
                correlationId, httpMethod, apiUri, projectPath);

        // Validate Maven project
        if (!FileUtil.isValidMavenProject(projectPath)) {
            throw new SourceCodeParseException(
                    ErrorCode.INVALID_MAVEN_PROJECT,
                    "Invalid Maven project structure: " + projectPath,
                    correlationId
            );
        }

        // Parse endpoint
        ApiEndpointInfo endpointInfo = controllerParser.parseEndpoint(
                projectPath,
                apiUri,
                httpMethod,
                correlationId
        );

        log.info("[{}] Successfully analyzed endpoint: {}.{}", 
                correlationId, 
                endpointInfo.getControllerClassName(), 
                endpointInfo.getMethodName());

        return endpointInfo;
    }
}