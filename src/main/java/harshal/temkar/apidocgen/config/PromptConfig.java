package harshal.temkar.apidocgen.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Externalized prompt configuration for easy domain customization.
 * 
 * Allows fine-tuning without code changes: - Domain terminology - Coding
 * standards - Company-specific patterns - Industry regulations
 */

@Configuration
@ConfigurationProperties(prefix = "apidocgen.prompts")
@Data
public class PromptConfig {

	/**
	 * Domain-specific context to inject into prompts.
	 * 
	 * Example for E-commerce: - "Payment Processing": "All payment APIs must
	 * document PCI-DSS compliance" - "Order Management": "Orders follow state
	 * machine pattern"
	 */

	private Map<String, String> domainContext = new HashMap<>();

	/**
	 * Coding standards to recognize in code.
	 * 
	 * Example: - "Error Handling": "We use @ControllerAdvice with custom ErrorCode
	 * enums" - "Validation": "JSR-303 Bean Validation on DTOs"
	 */

	private Map<String, String> codingStandards = new HashMap<>();

	/**
	 * Business rule patterns specific to your domain.
	 * 
	 * Example for Banking: - "Transaction Limits": "Daily limits enforced at
	 * service layer" - "Audit Trail": "All financial transactions logged to audit
	 * DB"
	 */

	private Map<String, String> businessRulePatterns = new HashMap<>();

	/**
	 * Technical patterns to document.
	 * 
	 * Example: - "Caching Strategy": "Redis for session, Caffeine for reference
	 * data" - "Database Pattern": "Read replicas for queries, master for writes"
	 */

	private Map<String, String> technicalPatterns = new HashMap<>();

	/**
	 * Terminology mappings (internal term -> documentation term).
	 * 
	 * Example: - "usr" -> "User" - "txn" -> "Transaction"
	 */

	private Map<String, String> terminologyMapping = new HashMap<>();

	/**
	 * Security aspects to always document.
	 */

	private List<String> securityCheckpoints = new ArrayList<>();

	/**
	 * Performance aspects to highlight.
	 */

	private List<String> performanceCheckpoints = new ArrayList<>();

	/**
	 * Compliance requirements (GDPR, HIPAA, PCI-DSS, etc.).
	 */

	private List<String> complianceRequirements = new ArrayList<>();

	/**
	 * Custom extraction rules per domain entity.
	 * 
	 * Example for User entity: - "Personal Data": "Identify all PII fields" -
	 * "Access Control": "Document permission checks"
	 */

	private Map<String, List<String>> entitySpecificRules = new HashMap<>();
}