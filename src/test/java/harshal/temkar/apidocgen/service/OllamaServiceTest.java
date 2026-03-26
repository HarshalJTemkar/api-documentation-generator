package harshal.temkar.apidocgen.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OllamaServiceTest {

	@Autowired
	private OllamaService ollamaService;

	@Test
	void testOllamaAvailability() {
		boolean isAvailable = ollamaService.isOllamaAvailable();
		assertTrue(isAvailable, "Ollama service should be available for tests");
	}

	@Test
	void testGenerateCompletion() {
		String prompt = "What is 2+2?";
		String response = ollamaService.generateCompletion(prompt, "test-correlation-id");

		assertNotNull(response);
		assertFalse(response.isBlank());
	}
}