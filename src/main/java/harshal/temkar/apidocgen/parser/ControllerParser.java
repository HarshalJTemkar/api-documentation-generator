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
import com.github.javaparser.ast.expr.MemberValuePair;

import harshal.temkar.apidocgen.exception.ErrorCode;
import harshal.temkar.apidocgen.exception.SourceCodeParseException;
import harshal.temkar.apidocgen.model.dto.ApiEndpointInfo;
import harshal.temkar.apidocgen.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Enhanced parser for Spring Boot REST controllers.
 * 
 * Improvements: - Handles context path in application.properties - Supports
 * various @RequestMapping patterns - Better URI matching with wildcards -
 * Handles @GetMapping, @PostMapping, etc. - Supports class-level and
 * method-level mappings
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ControllerParser {

	private final JavaFileParser javaFileParser;
	private final MethodExtractor methodExtractor;

	@Cacheable(value = "parsedCode", key = "#projectPath + '_' + #apiUri + '_' + #httpMethod")
	public ApiEndpointInfo parseEndpoint(String projectPath, String apiUri, String httpMethod, String correlationId) {

		log.info("[{}] Parsing endpoint: {} {} in {}", correlationId, httpMethod, apiUri, projectPath);

		List<Path> javaFiles = FileUtil.findAllJavaFiles(projectPath);

		log.debug("[{}] Found {} Java files to scan", correlationId, javaFiles.size());

		// Try exact match first
		for (Path javaFile : javaFiles) {
			try {
				String sourceCode = FileUtil.readFileContent(javaFile);
				CompilationUnit cu = javaFileParser.parseSourceCode(sourceCode, correlationId);

				Optional<ApiEndpointInfo> endpointInfo = parseControllerFile(cu, apiUri, httpMethod, correlationId,
						false);

				if (endpointInfo.isPresent()) {
					log.info("[{}] Found exact match in: {}", correlationId, javaFile.getFileName());
					return endpointInfo.get();
				}

			} catch (Exception e) {
				log.debug("[{}] Skipping file {}: {}", correlationId, javaFile.getFileName(), e.getMessage());
			}
		}

		// Try fuzzy match (ignore context path)
		log.debug("[{}] Exact match not found, trying fuzzy match", correlationId);

		for (Path javaFile : javaFiles) {
			try {
				String sourceCode = FileUtil.readFileContent(javaFile);
				CompilationUnit cu = javaFileParser.parseSourceCode(sourceCode, correlationId);

				Optional<ApiEndpointInfo> endpointInfo = parseControllerFile(cu, apiUri, httpMethod, correlationId,
						true);

				if (endpointInfo.isPresent()) {
					log.info("[{}] Found fuzzy match in: {}", correlationId, javaFile.getFileName());
					return endpointInfo.get();
				}

			} catch (Exception e) {
				log.debug("[{}] Skipping file {}: {}", correlationId, javaFile.getFileName(), e.getMessage());
			}
		}

		throw new SourceCodeParseException(ErrorCode.CONTROLLER_NOT_FOUND,
				"No controller found for: " + httpMethod + " " + apiUri, correlationId);
	}

	private Optional<ApiEndpointInfo> parseControllerFile(CompilationUnit cu, String apiUri, String httpMethod,
			String correlationId, boolean fuzzyMatch) {

		return cu.findAll(ClassOrInterfaceDeclaration.class).stream().filter(this::isController).flatMap(controller -> {
			log.debug("[{}] Checking controller: {}", correlationId, controller.getNameAsString());
			return parseControllerMethods(controller, apiUri, httpMethod, correlationId, fuzzyMatch).stream();
		}).findFirst();
	}

	private boolean isController(ClassOrInterfaceDeclaration classDecl) {
		return classDecl.getAnnotations().stream().anyMatch(ann -> {
			String name = ann.getNameAsString();
			return name.equals("RestController") || name.equals("Controller") || name.contains("RestController")
					|| name.contains("Controller");
		});
	}

	private Optional<ApiEndpointInfo> parseControllerMethods(ClassOrInterfaceDeclaration controller, String apiUri,
			String httpMethod, String correlationId, boolean fuzzyMatch) {

		String baseUri = extractBaseUri(controller);

		log.debug("[{}] Controller {} has base URI: {}", correlationId, controller.getNameAsString(), baseUri);

		return controller.getMethods().stream().filter(method -> {
			boolean matches = matchesEndpoint(method, baseUri, apiUri, httpMethod, fuzzyMatch);
			if (matches) {
				log.debug("[{}] Method {} matches endpoint", correlationId, method.getNameAsString());
			}
			return matches;
		}).map(method -> buildEndpointInfo(controller, method, baseUri, correlationId)).findFirst();
	}

	private String extractBaseUri(ClassOrInterfaceDeclaration controller) {
		return controller.getAnnotationByName("RequestMapping").flatMap(ann -> {
			// Handle @RequestMapping(value = "...")
			if (ann.isNormalAnnotationExpr()) {
				return ann.asNormalAnnotationExpr().getPairs().stream()
						.filter(pair -> pair.getNameAsString().equals("value") || pair.getNameAsString().equals("path"))
						.map(MemberValuePair::getValue).map(value -> cleanQuotes(value.toString())).findFirst();
			}
			// Handle @RequestMapping("...")
			else if (ann.isSingleMemberAnnotationExpr()) {
				return Optional.of(cleanQuotes(ann.asSingleMemberAnnotationExpr().getMemberValue().toString()));
			}
			return Optional.empty();
		}).orElse("");
	}

	private boolean matchesEndpoint(MethodDeclaration method, String baseUri, String apiUri, String httpMethod,
			boolean fuzzyMatch) {

		Optional<AnnotationExpr> mappingAnnotation = findMappingAnnotation(method);

		if (mappingAnnotation.isEmpty()) {
			return false;
		}

		AnnotationExpr annotation = mappingAnnotation.get();

		// Check HTTP method match
		if (!matchesHttpMethod(annotation, httpMethod)) {
			return false;
		}

		String methodUri = extractMethodUri(annotation);
		String fullUri = combineUris(baseUri, methodUri);

		log.debug("Comparing: fullUri={}, apiUri={}, fuzzyMatch={}", fullUri, apiUri, fuzzyMatch);

		if (fuzzyMatch) {
			return fuzzyUriMatch(fullUri, apiUri);
		} else {
			return exactUriMatch(fullUri, apiUri);
		}
	}

	private Optional<AnnotationExpr> findMappingAnnotation(MethodDeclaration method) {
		return method.getAnnotations().stream().filter(ann -> {
			String name = ann.getNameAsString();
			return name.contains("Mapping");
		}).findFirst();
	}

	private boolean matchesHttpMethod(AnnotationExpr annotation, String httpMethod) {
		String annotationName = annotation.getNameAsString().toLowerCase();

		// Direct mapping annotations
		if (annotationName.equals("getmapping") && httpMethod.equalsIgnoreCase("GET"))
			return true;
		if (annotationName.equals("postmapping") && httpMethod.equalsIgnoreCase("POST"))
			return true;
		if (annotationName.equals("putmapping") && httpMethod.equalsIgnoreCase("PUT"))
			return true;
		if (annotationName.equals("deletemapping") && httpMethod.equalsIgnoreCase("DELETE"))
			return true;
		if (annotationName.equals("patchmapping") && httpMethod.equalsIgnoreCase("PATCH"))
			return true;

		// Generic @RequestMapping with method attribute
		if (annotationName.equals("requestmapping")) {
			if (annotation.isNormalAnnotationExpr()) {
				Optional<String> methodAttr = annotation.asNormalAnnotationExpr().getPairs().stream()
						.filter(pair -> pair.getNameAsString().equals("method")).map(pair -> pair.getValue().toString())
						.findFirst();

				if (methodAttr.isPresent()) {
					return methodAttr.get().toUpperCase().contains(httpMethod.toUpperCase());
				}
			}
			// Default to GET if no method specified
			return httpMethod.equalsIgnoreCase("GET");
		}

		return false;
	}

	private String extractMethodUri(AnnotationExpr annotation) {
		// Handle @GetMapping("/path")
		if (annotation.isSingleMemberAnnotationExpr()) {
			return cleanQuotes(annotation.asSingleMemberAnnotationExpr().getMemberValue().toString());
		}

		// Handle @GetMapping(value = "/path") or @GetMapping(path = "/path")
		if (annotation.isNormalAnnotationExpr()) {
			return annotation.asNormalAnnotationExpr().getPairs().stream()
					.filter(pair -> pair.getNameAsString().equals("value") || pair.getNameAsString().equals("path"))
					.map(pair -> cleanQuotes(pair.getValue().toString())).findFirst().orElse("");
		}

		return "";
	}

	private String combineUris(String baseUri, String methodUri) {
		if (baseUri.isEmpty() && methodUri.isEmpty()) {
			return "/";
		}

		String combined = baseUri + methodUri;

		// Normalize slashes
		combined = combined.replaceAll("/+", "/");

		// Ensure starts with /
		if (!combined.startsWith("/")) {
			combined = "/" + combined;
		}

		// Remove trailing slash
		if (combined.length() > 1 && combined.endsWith("/")) {
			combined = combined.substring(0, combined.length() - 1);
		}

		return combined;
	}

	private boolean exactUriMatch(String fullUri, String apiUri) {
		// Normalize both URIs
		String normalizedFull = normalizeUri(fullUri);
		String normalizedApi = normalizeUri(apiUri);

		return normalizedFull.equals(normalizedApi);
	}

	private boolean fuzzyUriMatch(String fullUri, String apiUri) {
		// Remove leading context paths for fuzzy matching
		String normalizedFull = normalizeUri(fullUri);
		String normalizedApi = normalizeUri(apiUri);

		// Try exact match first
		if (normalizedFull.equals(normalizedApi)) {
			return true;
		}

		// Try removing first path segment (potential context path)
		String fullUriWithoutContext = removeFirstSegment(normalizedFull);
		String apiUriWithoutContext = removeFirstSegment(normalizedApi);

		if (fullUriWithoutContext.equals(normalizedApi)) {
			return true;
		}

		if (normalizedFull.equals(apiUriWithoutContext)) {
			return true;
		}

		if (fullUriWithoutContext.equals(apiUriWithoutContext)) {
			return true;
		}

		// Match with wildcards for path variables
		return wildcardMatch(normalizedFull, normalizedApi);
	}

	private String normalizeUri(String uri) {
		// Remove trailing slash
		if (uri.length() > 1 && uri.endsWith("/")) {
			uri = uri.substring(0, uri.length() - 1);
		}

		// Ensure starts with /
		if (!uri.startsWith("/")) {
			uri = "/" + uri;
		}

		// Replace path variables {id} with wildcard for matching
		uri = uri.replaceAll("\\{[^}]+\\}", "*");

		// Normalize multiple slashes
		uri = uri.replaceAll("/+", "/");

		return uri;
	}

	private String removeFirstSegment(String uri) {
		if (uri.length() <= 1) {
			return uri;
		}

		int secondSlash = uri.indexOf('/', 1);
		if (secondSlash == -1) {
			return "/";
		}

		return uri.substring(secondSlash);
	}

	private boolean wildcardMatch(String pattern, String text) {
		String[] patternParts = pattern.split("/");
		String[] textParts = text.split("/");

		if (patternParts.length != textParts.length) {
			return false;
		}

		for (int i = 0; i < patternParts.length; i++) {
			if (!patternParts[i].equals("*") && !patternParts[i].equals(textParts[i])) {
				return false;
			}
		}

		return true;
	}

	private String cleanQuotes(String value) {
		return value.replaceAll("[\"\\[\\]{}]", "").trim();
	}

	private ApiEndpointInfo buildEndpointInfo(ClassOrInterfaceDeclaration controller, MethodDeclaration method,
			String baseUri, String correlationId) {

		log.info("[{}] Building endpoint info for: {}.{}", correlationId, controller.getNameAsString(),
				method.getNameAsString());

		String methodUri = method.getAnnotations().stream().filter(ann -> ann.getNameAsString().contains("Mapping"))
				.findFirst().map(this::extractMethodUri).orElse("");

		return ApiEndpointInfo.builder().controllerClassName(controller.getNameAsString())
				.methodName(method.getNameAsString()).uri(combineUris(baseUri, methodUri))
				.httpMethod(extractHttpMethod(method)).requestParams(methodExtractor.extractRequestParams(method))
				.pathVariables(methodExtractor.extractPathVariables(method))
				.requestBodyType(methodExtractor.extractRequestBodyType(method))
				.responseType(methodExtractor.extractResponseType(method)).annotations(extractAnnotations(method))
				.methodSourceCode(method.toString()).build();
	}

	private String extractHttpMethod(MethodDeclaration method) {
		return method.getAnnotations().stream().map(ann -> ann.getNameAsString().toLowerCase())
				.filter(name -> name.contains("mapping")).map(name -> {
					if (name.contains("get"))
						return "GET";
					if (name.contains("post"))
						return "POST";
					if (name.contains("put"))
						return "PUT";
					if (name.contains("delete"))
						return "DELETE";
					if (name.contains("patch"))
						return "PATCH";
					return "GET"; // Default
				}).findFirst().orElse("GET");
	}

	private Map<String, String> extractAnnotations(MethodDeclaration method) {
		return method.getAnnotations().stream().collect(Collectors.toMap(AnnotationExpr::getNameAsString,
				AnnotationExpr::toString, (existing, replacement) -> existing));
	}
}