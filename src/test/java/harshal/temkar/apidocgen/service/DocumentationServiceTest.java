package harshal.temkar.apidocgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import harshal.temkar.apidocgen.model.dto.DocumentationRequest;
import harshal.temkar.apidocgen.model.dto.DocumentationResponse;

@SpringBootTest
class DocumentationServiceTest {

	@Autowired
	private DocumentationService documentationService;

	private DocumentationRequest testRequest;

	@BeforeEach
	void setUp() {
		testRequest = DocumentationRequest.builder().projectPath("/path/to/test/project").apiUri("/api/v1/test")
				.httpMethod("GET").includeBusinessLogic(true).includeTechnicalWorkflow(true).preferredLanguage("en")
				.build();
	}

	@Test
	void testGenerateDocumentation_Success() throws Exception {
		CompletableFuture<DocumentationResponse> future = documentationService.generateDocumentation(testRequest);

		DocumentationResponse response = future.get();

		assertNotNull(response);
		assertNotNull(response.getCorrelationId());
		assertEquals(testRequest.getApiUri(), response.getApiUri());
	}
}