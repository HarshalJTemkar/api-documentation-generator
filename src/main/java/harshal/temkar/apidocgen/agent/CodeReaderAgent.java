package harshal.temkar.apidocgen.agent;

import org.springframework.stereotype.Component;

import harshal.temkar.apidocgen.model.dto.ApiEndpointInfo;
import harshal.temkar.apidocgen.parser.ControllerParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Agent responsible for reading and parsing source code.
 * 
 * Responsibilities: - Locate controller files - Extract method signatures -
 * Parse annotations - Extract method body
 * 
 * Performance: Cached results for repeated requests.
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class CodeReaderAgent implements Agent<CodeReaderAgent.Input, ApiEndpointInfo> {

	private final ControllerParser controllerParser;

	@Override
	public ApiEndpointInfo execute(Input input, String correlationId) {
		log.info("[{}] CodeReaderAgent: Parsing API endpoint: {}", correlationId, input.apiUri());

		long startTime = System.currentTimeMillis();

		ApiEndpointInfo endpointInfo = controllerParser.parseEndpoint(input.projectPath(), input.apiUri(),
				input.httpMethod(), correlationId);

		long duration = System.currentTimeMillis() - startTime;
		log.info("[{}] CodeReaderAgent completed in {}ms", correlationId, duration);

		return endpointInfo;
	}

	@Override
	public String getAgentName() {
		return "CodeReaderAgent";
	}

	public record Input(String projectPath, String apiUri, String httpMethod) {
	}
}