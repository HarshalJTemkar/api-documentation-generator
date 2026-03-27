package harshal.temkar.apidocgen.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import harshal.temkar.apidocgen.model.dto.BusinessLogicDoc;

/**
 * AI Service interface for business logic analysis.
 * 
 * LangChain4j automatically: - Sends prompts to LLM - Parses JSON responses -
 * Maps to BusinessLogicDoc - Handles retries
 * 
 * Benefits over manual WebClient: - Type-safe responses - Automatic JSON
 * parsing - Conversation memory support - Built-in error handling
 */
public interface BusinessLogicAnalyzer {

	/**
	 * Analyzes business logic from source code.
	 * 
	 * @param domainContext   Domain-specific context
	 * @param codingStandards Team coding standards
	 * @param sourceCode      Java method source code
	 * @param controllerName  Controller class name
	 * @param methodName      Method name
	 * @param httpMethod      HTTP method
	 * @param uri             API endpoint URI
	 * @return Structured business logic documentation
	 */
	@SystemMessage("""
			You are a senior business analyst with expertise in enterprise software documentation.

			Your task is to analyze REST API methods and extract comprehensive business logic documentation.

			Focus on:
			- Actual business rules in the code (not assumed)
			- Domain-specific terminology
			- Validations and constraints
			- Data transformations
			- Error handling strategies

			Always respond in valid JSON format matching BusinessLogicDoc structure.

			{{domainContext}}

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

			Extract business logic and provide structured JSON response.

			CRITICAL:
			- Do NOT hallucinate functionality
			- Only document what exists in the code
			- Use domain terminology provided in context
			- Be specific and precise
			""")
	BusinessLogicDoc analyzeBusinessLogic(@V("domainContext") String domainContext,
			@V("codingStandards") String codingStandards, @V("sourceCode") String sourceCode,
			@V("controllerName") String controllerName, @V("methodName") String methodName,
			@V("httpMethod") String httpMethod, @V("uri") String uri);
}