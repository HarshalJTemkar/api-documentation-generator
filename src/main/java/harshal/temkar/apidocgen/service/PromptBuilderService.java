package harshal.temkar.apidocgen.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import harshal.temkar.apidocgen.config.PromptConfig;
import harshal.temkar.apidocgen.model.dto.ApiEndpointInfo;
import harshal.temkar.apidocgen.prompt.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds domain-aware prompts with context injection.
 * 
 * Features: - Automatic domain context detection - Entity-specific rule
 * application - Terminology standardization - Compliance checkpoint injection
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptBuilderService {

	private final PromptConfig promptConfig;

	/**
	 * Builds business logic prompt with domain context.
	 */
	public String buildBusinessLogicPrompt(ApiEndpointInfo endpointInfo, String correlationId) {
		log.debug("[{}] Building business logic prompt for: {}", correlationId, endpointInfo.getUri());

		// Detect relevant domain entities from URI and code
		List<String> detectedEntities = detectDomainEntities(endpointInfo);

		// Build base prompt
		String basePrompt = getBusinessLogicBasePrompt();

		// Create template
		PromptTemplate template = PromptTemplate.builder().templateName("business-logic").basePrompt(basePrompt)
				.variables(buildVariables(endpointInfo)).domainContext(filterRelevantContext(detectedEntities))
				.codingStandards(promptConfig.getCodingStandards()).outputFormat(getBusinessLogicOutputFormat())
				.exampleOutput(getBusinessLogicExample()).build();

		// Add entity-specific rules
		String renderedPrompt = template.render();
		renderedPrompt = injectEntitySpecificRules(renderedPrompt, detectedEntities);
		renderedPrompt = injectSecurityCheckpoints(renderedPrompt);
		renderedPrompt = injectComplianceRequirements(renderedPrompt, detectedEntities);

		log.debug("[{}] Generated prompt length: {} chars", correlationId, renderedPrompt.length());

		return renderedPrompt;
	}

	/**
	 * Builds technical workflow prompt with architectural patterns.
	 */
	public String buildTechnicalWorkflowPrompt(ApiEndpointInfo endpointInfo, String correlationId) {
		log.debug("[{}] Building technical workflow prompt for: {}", correlationId, endpointInfo.getUri());

		List<String> detectedEntities = detectDomainEntities(endpointInfo);

		String basePrompt = getTechnicalWorkflowBasePrompt();

		PromptTemplate template = PromptTemplate.builder().templateName("technical-workflow").basePrompt(basePrompt)
				.variables(buildVariables(endpointInfo)).domainContext(promptConfig.getTechnicalPatterns())
				.codingStandards(promptConfig.getCodingStandards()).outputFormat(getTechnicalWorkflowOutputFormat())
				.exampleOutput(getTechnicalWorkflowExample()).build();

		String renderedPrompt = template.render();
		renderedPrompt = injectPerformanceCheckpoints(renderedPrompt);
		renderedPrompt = applyTerminologyMapping(renderedPrompt);

		return renderedPrompt;
	}

	/**
	 * Detects domain entities from URI and source code.
	 * 
	 * Examples: - /api/v1/users/{id} -> ["User"] -
	 * /api/v1/orders/{orderId}/payments -> ["Order", "Payment"]
	 */
	private List<String> detectDomainEntities(ApiEndpointInfo endpointInfo) {
		List<String> entities = new java.util.ArrayList<>();

		// Extract from URI
		String uri = endpointInfo.getUri().toLowerCase();
		Pattern entityPattern = Pattern.compile("/([a-z]+)");
		Matcher matcher = entityPattern.matcher(uri);

		while (matcher.find()) {
			String entity = matcher.group(1);
			// Singularize and capitalize
			String standardizedEntity = singularize(capitalize(entity));
			if (!standardizedEntity.equals("Api") && !standardizedEntity.equals("V1")) {
				entities.add(standardizedEntity);
			}
		}

		// Extract from class names in source code
		String sourceCode = endpointInfo.getMethodSourceCode();
		Pattern classPattern = Pattern.compile("([A-Z][a-z]+)(Service|Repository|Entity|DTO)");
		Matcher classMatcher = classPattern.matcher(sourceCode);

		while (classMatcher.find()) {
			entities.add(classMatcher.group(1));
		}

		return entities.stream().distinct().collect(Collectors.toList());
	}

	/**
	 * Filters domain context relevant to detected entities.
	 */
	private Map<String, String> filterRelevantContext(List<String> entities) {
		Map<String, String> relevantContext = new HashMap<>();

		promptConfig.getDomainContext().forEach((key, value) -> {
			// Include if key mentions any detected entity
			boolean isRelevant = entities.stream().anyMatch(entity -> key.toLowerCase().contains(entity.toLowerCase()));

			if (isRelevant) {
				relevantContext.put(key, value);
			}
		});

		// Always include general context
		if (relevantContext.isEmpty()) {
			relevantContext.putAll(promptConfig.getDomainContext());
		}

		return relevantContext;
	}

	/**
	 * Injects entity-specific extraction rules.
	 */
	private String injectEntitySpecificRules(String prompt, List<String> entities) {
		StringBuilder rulesSection = new StringBuilder("\n\n## Entity-Specific Analysis Rules\n");

		entities.forEach(entity -> {
			List<String> rules = promptConfig.getEntitySpecificRules().get(entity);
			if (rules != null && !rules.isEmpty()) {
				rulesSection.append("\n### ").append(entity).append(" Entity:\n");
				rules.forEach(rule -> rulesSection.append("- ").append(rule).append("\n"));
			}
		});

		return prompt + rulesSection.toString();
	}

	/**
	 * Injects security checkpoints.
	 */
	private String injectSecurityCheckpoints(String prompt) {
		if (promptConfig.getSecurityCheckpoints().isEmpty()) {
			return prompt;
		}

		StringBuilder securitySection = new StringBuilder("\n\n## Security Checkpoints (Mandatory)\n");
		securitySection.append("Document the following if present in code:\n");

		promptConfig.getSecurityCheckpoints()
				.forEach(checkpoint -> securitySection.append("- ").append(checkpoint).append("\n"));

		return prompt + securitySection.toString();
	}

	/**
	 * Injects compliance requirements.
	 */
	private String injectComplianceRequirements(String prompt, List<String> entities) {
		// Inject compliance only for sensitive entities
		boolean hasSensitiveEntity = entities.stream()
				.anyMatch(e -> e.matches("(?i)(user|payment|medical|personal|financial).*"));

		if (!hasSensitiveEntity || promptConfig.getComplianceRequirements().isEmpty()) {
			return prompt;
		}

		StringBuilder complianceSection = new StringBuilder("\n\n## Compliance Requirements\n");
		complianceSection.append("Evaluate compliance with:\n");

		promptConfig.getComplianceRequirements()
				.forEach(req -> complianceSection.append("- ").append(req).append("\n"));

		return prompt + complianceSection.toString();
	}

	/**
	 * Injects performance checkpoints.
	 */
	private String injectPerformanceCheckpoints(String prompt) {
		if (promptConfig.getPerformanceCheckpoints().isEmpty()) {
			return prompt;
		}

		StringBuilder perfSection = new StringBuilder("\n\n## Performance Analysis (Required)\n");
		perfSection.append("Evaluate:\n");

		promptConfig.getPerformanceCheckpoints()
				.forEach(checkpoint -> perfSection.append("- ").append(checkpoint).append("\n"));

		return prompt + perfSection.toString();
	}

	/**
	 * Applies terminology mapping to standardize output.
	 */
	private String applyTerminologyMapping(String prompt) {
		String mappedPrompt = prompt;

		StringBuilder mappingSection = new StringBuilder("\n\n## Terminology Standards\n");
		mappingSection.append("Use this terminology in documentation:\n");

		promptConfig.getTerminologyMapping().forEach((internal, standard) -> {
			mappingSection.append("- Use \"").append(standard).append("\" instead of \"").append(internal)
					.append("\"\n");
		});

		return mappedPrompt + mappingSection.toString();
	}

	/**
	 * Builds variable map for template substitution.
	 */
	private Map<String, String> buildVariables(ApiEndpointInfo endpointInfo) {
		Map<String, String> variables = new HashMap<>();
		variables.put("controllerClassName", endpointInfo.getControllerClassName());
		variables.put("methodName", endpointInfo.getMethodName());
		variables.put("httpMethod", endpointInfo.getHttpMethod());
		variables.put("uri", endpointInfo.getUri());
		variables.put("sourceCode", endpointInfo.getMethodSourceCode());
		return variables;
	}

	// Helper methods
	private String singularize(String word) {
		if (word.endsWith("ies"))
			return word.substring(0, word.length() - 3) + "y";
		if (word.endsWith("es"))
			return word.substring(0, word.length() - 2);
		if (word.endsWith("s"))
			return word.substring(0, word.length() - 1);
		return word;
	}

	private String capitalize(String word) {
		return word.substring(0, 1).toUpperCase() + word.substring(1);
	}

	// Base prompt templates
	private String getBusinessLogicBasePrompt() {
	    return """
	        You are a senior business analyst with deep expertise in enterprise software documentation.
	        
	        Analyze the following REST API method and extract comprehensive business logic documentation.
	        
	        **API Details:**
	        - Controller: {controllerClassName}
	        - Method: {methodName}
	        - Endpoint: {httpMethod} {uri}
	        
	        **Source Code:**
	        ```java
	        {sourceCode}
	        ```
	        
	        **Analysis Instructions:**
	        1. Read the code carefully and identify actual business logic
	        2. Do NOT assume or hallucinate functionality
	        3. Focus on business rules, not technical implementation
	        4. Use domain terminology from the context provided below
	        5. Document compliance aspects if dealing with sensitive data
	        
	        **CRITICAL OUTPUT REQUIREMENTS:**
	        - Respond with VALID JSON only
	        - Use double quotes for all strings
	        - No comments in JSON
	        - No trailing commas
	        - Escape special characters in strings
	        - Do not wrap JSON in markdown code blocks
	        - Ensure all array and object brackets are properly closed
	        
	        **Expected JSON Structure (copy this exactly):**
	        {
	          "summary": "string value here",
	          "purpose": "string value here",
	          "businessRules": ["rule1", "rule2"],
	          "validations": ["validation1", "validation2"],
	          "dataTransformations": ["transformation1"],
	          "expectedBehavior": "string value here",
	          "errorScenarios": ["error1", "error2"]
	        }
	        
	        Respond with ONLY the JSON object, nothing else.
	        """;
	}

	private String getTechnicalWorkflowBasePrompt() {
		return """
				You are a senior software architect documenting technical execution flows.

				Analyze the following REST API method and document its technical workflow.

				**API Details:**
				- Controller: {controllerClassName}
				- Method: {methodName}
				- Endpoint: {httpMethod} {uri}

				**Source Code:**
				```java
				{sourceCode}
				```

				**Documentation Instructions:**
				1. Document step-by-step execution flow
				2. Identify all service/repository calls
				3. Note database operations and transactions
				4. Highlight external API integrations
				5. Apply architectural patterns from context below
				6. Evaluate performance implications

				""";
	}

	private String getBusinessLogicOutputFormat() {
		return """
				```json
				{
				  "summary": "2-3 sentence high-level summary",
				  "purpose": "Business purpose and use case",
				  "businessRules": ["Rule 1", "Rule 2"],
				  "validations": ["Validation 1", "Validation 2"],
				  "dataTransformations": ["Transformation 1"],
				  "expectedBehavior": "Success scenario outcome",
				  "errorScenarios": ["Error 1: handling", "Error 2: handling"],
				  "securityAspects": ["Authentication check", "Authorization"],
				  "complianceNotes": ["GDPR: data processing", "PCI: tokenization"]
				}
				```
				""";
	}

	private String getTechnicalWorkflowOutputFormat() {
		return """
				```json
				{
				  "workflowSummary": "High-level technical flow",
				  "steps": [
				    {
				      "stepNumber": 1,
				      "description": "Action description",
				      "componentInvolved": "Service/Repository name",
				      "action": "Specific method call"
				    }
				  ],
				  "serviceDependencies": ["UserService", "PaymentGateway"],
				  "databaseOperations": ["SELECT users", "INSERT orders"],
				  "externalApiCalls": ["PaymentProvider.charge()"],
				  "transactionBoundary": "@Transactional(REQUIRED)",
				  "performanceConsiderations": [
				    "N+1 query risk in line 45",
				    "Consider caching user details"
				  ],
				  "cachingStrategy": "Redis with 5min TTL"
				}
				```
				""";
	}

	private String getBusinessLogicExample() {
		return """
				{
				  "summary": "Creates a new customer order with payment processing and inventory reservation",
				  "purpose": "Allow customers to place orders for products with immediate payment",
				  "businessRules": [
				    "Order total must be greater than $10 minimum",
				    "Payment must be authorized before order confirmation",
				    "Inventory must be available for all order items"
				  ],
				  "validations": [
				    "Customer ID must exist and be active",
				    "Product IDs must be valid",
				    "Quantities must be positive integers",
				    "Shipping address must be complete"
				  ],
				  "dataTransformations": [
				    "OrderRequest DTO mapped to Order entity",
				    "Payment amount converted to cents for gateway",
				    "Inventory quantities decremented atomically"
				  ],
				  "expectedBehavior": "Order created with CONFIRMED status, payment charged, inventory reserved, confirmation email sent",
				  "errorScenarios": [
				    "Insufficient inventory: Order rejected, no charge",
				    "Payment declined: Order cancelled, inventory released",
				    "Invalid customer: 404 error returned"
				  ],
				  "securityAspects": [
				    "JWT authentication required",
				    "Customer can only create orders for self",
				    "Credit card data tokenized via PCI-compliant gateway"
				  ],
				  "complianceNotes": [
				    "PCI-DSS: Card data never stored, tokenization used",
				    "GDPR: Personal data (address) stored with consent"
				  ]
				}
				""";
	}

	private String getTechnicalWorkflowExample() {
		return """
				{
				  "workflowSummary": "Multi-step order creation with payment authorization and inventory management",
				  "steps": [
				    {
				      "stepNumber": 1,
				      "description": "Validate request and authenticate customer",
				      "componentInvolved": "OrderController",
				      "action": "Validate OrderRequest DTO, extract customer from JWT"
				    },
				    {
				      "stepNumber": 2,
				      "description": "Check inventory availability",
				      "componentInvolved": "InventoryService",
				      "action": "inventoryService.checkAvailability(productIds, quantities)"
				    },
				    {
				      "stepNumber": 3,
				      "description": "Authorize payment",
				      "componentInvolved": "PaymentService",
				      "action": "paymentService.authorizeCharge(amount, paymentMethod)"
				    },
				    {
				      "stepNumber": 4,
				      "description": "Create order and reserve inventory",
				      "componentInvolved": "OrderService",
				      "action": "orderService.createOrder(orderRequest) - @Transactional"
				    },
				    {
				      "stepNumber": 5,
				      "description": "Send confirmation email",
				      "componentInvolved": "NotificationService",
				      "action": "notificationService.sendOrderConfirmation(order) - Async"
				    }
				  ],
				  "serviceDependencies": [
				    "InventoryService",
				    "PaymentService",
				    "OrderService",
				    "NotificationService"
				  ],
				  "databaseOperations": [
				    "SELECT * FROM inventory WHERE product_id IN (...)",
				    "INSERT INTO orders (...)",
				    "UPDATE inventory SET quantity = quantity - ? WHERE product_id = ?"
				  ],
				  "externalApiCalls": [
				    "StripePaymentGateway.authorizeCharge()",
				    "SendGridEmailService.send()"
				  ],
				  "transactionBoundary": "@Transactional at OrderService.createOrder() with REQUIRED propagation",
				  "performanceConsiderations": [
				    "Inventory check could cause N+1 if not batched",
				    "Payment authorization adds 500-1000ms latency",
				    "Email sending is async to avoid blocking",
				    "Consider caching product details"
				  ],
				  "cachingStrategy": "Product catalog cached in Redis with 1-hour TTL"
				}
				""";
	}
}