package harshal.temkar.apidocgen.prompt;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * Flexible prompt template with variable substitution and context injection.
 * 
 * Enables: - Domain-specific terminology - Coding standard enforcement - Custom
 * extraction patterns - Company-specific guidelines
 */
@Data
@Builder
public class PromptTemplate {

	private String templateName;
	private String basePrompt;

	@Builder.Default
	private Map<String, String> variables = new HashMap<>();

	@Builder.Default
	private Map<String, String> domainContext = new HashMap<>();

	@Builder.Default
	private Map<String, String> codingStandards = new HashMap<>();

	private String outputFormat;
	private String exampleOutput;

	/**
	 * Renders final prompt with all substitutions.
	 */
	public String render() {
		String renderedPrompt = basePrompt;

		// Substitute variables
		for (Map.Entry<String, String> entry : variables.entrySet()) {
			renderedPrompt = renderedPrompt.replace("{" + entry.getKey() + "}", entry.getValue());
		}

		// Inject domain context
		if (!domainContext.isEmpty()) {
			StringBuilder contextSection = new StringBuilder("\n\n## Domain Context\n");
			domainContext.forEach((key, value) -> contextSection.append("- **").append(key).append("**: ").append(value)
					.append("\n"));
			renderedPrompt += contextSection.toString();
		}

		// Inject coding standards
		if (!codingStandards.isEmpty()) {
			StringBuilder standardsSection = new StringBuilder("\n\n## Coding Standards to Recognize\n");
			codingStandards.forEach((key, value) -> standardsSection.append("- **").append(key).append("**: ")
					.append(value).append("\n"));
			renderedPrompt += standardsSection.toString();
		}

		// Add output format
		if (outputFormat != null) {
			renderedPrompt += "\n\n## Output Format\n" + outputFormat;
		}

		// Add example
		if (exampleOutput != null) {
			renderedPrompt += "\n\n## Example Output\n```json\n" + exampleOutput + "\n```";
		}

		return renderedPrompt;
	}
}