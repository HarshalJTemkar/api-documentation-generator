package harshal.temkar.apidocgen.parser;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts method-level metadata from parsed Java methods.
 * 
 * Extracts: - Request parameters (@RequestParam) - Path variables
 * (@PathVariable) - Request body (@RequestBody) - Response type
 */

@Component
@Slf4j
public class MethodExtractor {

	public List<String> extractRequestParams(MethodDeclaration method) {
		return method.getParameters().stream().filter(
				param -> param.getAnnotations().stream().anyMatch(ann -> ann.getNameAsString().equals("RequestParam")))
				.map(Parameter::getNameAsString).collect(Collectors.toList());
	}

	public List<String> extractPathVariables(MethodDeclaration method) {
		return method.getParameters().stream().filter(
				param -> param.getAnnotations().stream().anyMatch(ann -> ann.getNameAsString().equals("PathVariable")))
				.map(Parameter::getNameAsString).collect(Collectors.toList());
	}

	public String extractRequestBodyType(MethodDeclaration method) {
		return method.getParameters().stream()
				.filter(param -> param.getAnnotations().stream()
						.anyMatch(ann -> ann.getNameAsString().equals("RequestBody")))
				.map(param -> param.getType().asString()).findFirst().orElse(null);
	}

	public String extractResponseType(MethodDeclaration method) {
		return method.getType().asString();
	}
}