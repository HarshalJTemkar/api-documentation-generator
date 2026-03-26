package harshal.temkar.apidocgen.parser;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

import harshal.temkar.apidocgen.exception.ErrorCode;
import harshal.temkar.apidocgen.exception.SourceCodeParseException;
import harshal.temkar.apidocgen.model.dto.ApiEndpointInfo;
import harshal.temkar.apidocgen.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Parser for Spring Boot REST controllers.
 * 
 * Extracts:
 * - @RestController / @Controller classes
 * - @RequestMapping URIs
 * - HTTP method mappings (@GetMapping, @PostMapping, etc.)
 * - Request/Response types
 * - Path variables and request parameters
 * - Method source code
 * 
 * Performance:
 * - Cached parsing results
 * - Parallel file scanning for large projects
 * - Optimized AST traversal
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ControllerParser {

    private final JavaFileParser javaFileParser;
    private final MethodExtractor methodExtractor;

    /**
     * Parses API endpoint from project.
     * 
     * Algorithm:
     * 1. Scan all Java files
     * 2. Filter controllers (@RestController/@Controller)
     * 3. Find method matching URI and HTTP method
     * 4. Extract metadata
     * 
     * @param projectPath Maven project root
     * @param apiUri Target API URI
     * @param httpMethod HTTP method
     * @param correlationId Request tracking
     * @return Endpoint information
     */
    @Cacheable(value = "parsedCode", key = "#projectPath + '_' + #apiUri + '_' + #httpMethod")
    public ApiEndpointInfo parseEndpoint(String projectPath, String apiUri, 
                                        String httpMethod, String correlationId) {
        
        log.info("[{}] Parsing endpoint: {} {} in {}", correlationId, httpMethod, apiUri, projectPath);

        List<Path> javaFiles = FileUtil.findAllJavaFiles(projectPath);
        
        for (Path javaFile : javaFiles) {
            try {
                String sourceCode = FileUtil.readFileContent(javaFile);
                CompilationUnit cu = javaFileParser.parseSourceCode(sourceCode, correlationId);

                Optional<ApiEndpointInfo> endpointInfo = parseControllerFile(
                        cu, apiUri, httpMethod, correlationId);

                if (endpointInfo.isPresent()) {
                    return endpointInfo.get();
                }

            } catch (Exception e) {
                log.debug("[{}] Skipping file {}: {}", correlationId, javaFile, e.getMessage());
            }
        }

        throw new SourceCodeParseException(
                ErrorCode.CONTROLLER_NOT_FOUND,
                "No controller found for: " + httpMethod + " " + apiUri,
                correlationId
        );
    }

    private Optional<ApiEndpointInfo> parseControllerFile(CompilationUnit cu, String apiUri, 
                                                         String httpMethod, String correlationId) {
        
        return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(this::isController)
                .flatMap(controller -> parseControllerMethods(controller, apiUri, httpMethod, correlationId).stream())
                .findFirst();
    }

    private boolean isController(ClassOrInterfaceDeclaration classDecl) {
        return classDecl.getAnnotations().stream()
                .anyMatch(ann -> ann.getNameAsString().equals("RestController") 
                        || ann.getNameAsString().equals("Controller"));
    }

    private Optional<ApiEndpointInfo> parseControllerMethods(ClassOrInterfaceDeclaration controller, 
                                                            String apiUri, String httpMethod, 
                                                            String correlationId) {
        
        String baseUri = extractBaseUri(controller);

        return controller.getMethods().stream()
                .filter(method -> matchesEndpoint(method, baseUri, apiUri, httpMethod))
                .map(method -> buildEndpointInfo(controller, method, baseUri, correlationId))
                .findFirst();
    }

    private String extractBaseUri(ClassOrInterfaceDeclaration controller) {
        return controller.getAnnotationByName("RequestMapping")
                .flatMap(ann -> ann.toNormalAnnotationExpr()
                        .flatMap(norm -> norm.getPairs().stream()
                                .filter(pair -> pair.getNameAsString().equals("value") 
                                        || pair.getNameAsString().equals("path"))
                                .map(pair -> pair.getValue().toString().replaceAll("\"", ""))
                                .findFirst()))
                .orElse("");
    }

    private boolean matchesEndpoint(MethodDeclaration method, String baseUri, 
                                   String apiUri, String httpMethod) {
        
        Optional<AnnotationExpr> mappingAnnotation = findMappingAnnotation(method, httpMethod);
        
        if (mappingAnnotation.isEmpty()) {
            return false;
        }

        String methodUri = extractMethodUri(mappingAnnotation.get());
        String fullUri = (baseUri + methodUri).replaceAll("//", "/");

        return normalizeUri(fullUri).equals(normalizeUri(apiUri));
    }

    private Optional<AnnotationExpr> findMappingAnnotation(MethodDeclaration method, String httpMethod) {
        String annotationName = httpMethod.toLowerCase() + "Mapping";
        
        return method.getAnnotations().stream()
                .filter(ann -> ann.getNameAsString().toLowerCase().contains(annotationName.toLowerCase())
                        || ann.getNameAsString().equals("RequestMapping"))
                .findFirst();
    }

    private String extractMethodUri(AnnotationExpr annotation) {
        return annotation.toNormalAnnotationExpr()
                .flatMap(norm -> norm.getPairs().stream()
                        .filter(pair -> pair.getNameAsString().equals("value") 
                                || pair.getNameAsString().equals("path"))
                        .map(pair -> pair.getValue().toString().replaceAll("\"", ""))
                        .findFirst())
                .orElse(annotation.toSingleMemberAnnotationExpr()
                        .map(single -> single.getMemberValue().toString().replaceAll("\"", ""))
                        .orElse(""));
    }

    private String normalizeUri(String uri) {
        return uri.replaceAll("\\{[^}]+\\}", "*").replaceAll("/+", "/");
    }

    private ApiEndpointInfo buildEndpointInfo(ClassOrInterfaceDeclaration controller, 
                                             MethodDeclaration method, 
                                             String baseUri, 
                                             String correlationId) {
        
        log.info("[{}] Building endpoint info for: {}.{}", 
                correlationId, controller.getNameAsString(), method.getNameAsString());

        return ApiEndpointInfo.builder()
                .controllerClassName(controller.getNameAsString())
                .methodName(method.getNameAsString())
                .uri(baseUri + extractMethodUri(method.getAnnotations().get(0)))
                .httpMethod(extractHttpMethod(method))
                .requestParams(methodExtractor.extractRequestParams(method))
                .pathVariables(methodExtractor.extractPathVariables(method))
                .requestBodyType(methodExtractor.extractRequestBodyType(method))
                .responseType(methodExtractor.extractResponseType(method))
                .annotations(extractAnnotations(method))
                .methodSourceCode(method.toString())
                .build();
    }

    private String extractHttpMethod(MethodDeclaration method) {
        return method.getAnnotations().stream()
                .map(ann -> ann.getNameAsString().toLowerCase())
                .filter(name -> name.contains("mapping"))
                .map(name -> name.replace("mapping", "").toUpperCase())
                .findFirst()
                .orElse("GET");
    }

    private Map<String, String> extractAnnotations(MethodDeclaration method) {
        return method.getAnnotations().stream()
                .collect(Collectors.toMap(
                        ann -> ann.getNameAsString(),
                        AnnotationExpr::toString
                ));
    }
}