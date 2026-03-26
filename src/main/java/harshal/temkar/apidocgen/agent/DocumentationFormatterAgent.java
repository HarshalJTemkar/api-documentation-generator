package harshal.temkar.apidocgen.agent;

import org.springframework.stereotype.Component;

import harshal.temkar.apidocgen.model.dto.BusinessLogicDoc;
import harshal.temkar.apidocgen.model.dto.TechnicalWorkflowDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Agent responsible for formatting documentation output.
 * 
 * Formats: - Plain English documentation - Markdown format - HTML format
 * (future) - PDF export (future)
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentationFormatterAgent implements Agent<DocumentationFormatterAgent.Input, String> {

	@Override
	public String execute(Input input, String correlationId) {
		log.info("[{}] DocumentationFormatterAgent: Formatting documentation", correlationId);

		StringBuilder formatted = new StringBuilder();

		formatted.append("# API Documentation\n\n");
		formatted.append("## Endpoint: ").append(input.apiUri()).append("\n\n");

		if (input.businessLogic() != null) {
			formatted.append("## Business Logic\n\n");
			formatted.append("**Summary:** ").append(input.businessLogic().getSummary()).append("\n\n");
			formatted.append("**Purpose:** ").append(input.businessLogic().getPurpose()).append("\n\n");

			if (input.businessLogic().getBusinessRules() != null) {
				formatted.append("**Business Rules:**\n");
				input.businessLogic().getBusinessRules()
						.forEach(rule -> formatted.append("- ").append(rule).append("\n"));
				formatted.append("\n");
			}
		}

		if (input.technicalWorkflow() != null) {
			formatted.append("## Technical Workflow\n\n");
			formatted.append(input.technicalWorkflow().getWorkflowSummary()).append("\n\n");

			if (input.technicalWorkflow().getSteps() != null) {
				formatted.append("**Execution Steps:**\n");
				input.technicalWorkflow().getSteps().forEach(step -> formatted.append(step.getStepNumber()).append(". ")
						.append(step.getDescription()).append("\n"));
			}
		}

		return formatted.toString();
	}

	@Override
	public String getAgentName() {
		return "DocumentationFormatterAgent";
	}

	public record Input(String apiUri, BusinessLogicDoc businessLogic, TechnicalWorkflowDoc technicalWorkflow) {
	}
}