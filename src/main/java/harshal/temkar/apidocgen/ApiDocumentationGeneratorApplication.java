package harshal.temkar.apidocgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for API Documentation Generator.
 * 
 * Features:
 * - Ollama LLM integration for AI-powered documentation
 * - Maven project source code analysis
 * - REST API endpoint documentation extraction
 * - Agentic AI architecture for scalable processing
 * - Excel batch processing support
 * 
 * @author Harshal Temkar
 * @version 1.0.0
 */

@SpringBootApplication
@EnableCaching
@EnableAsync
public class ApiDocumentationGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiDocumentationGeneratorApplication.class, args);
	}

}
