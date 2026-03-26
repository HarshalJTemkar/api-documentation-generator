package harshal.temkar.apidocgen.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for single API documentation generation.
 * 
 * Validation: - projectPath: Must be valid file system path - apiUri: Must be
 * valid REST endpoint pattern
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationRequest {

	@NotBlank(message = "Project path is required")
	private String projectPath;

	@NotBlank(message = "API URI is required")
	@Pattern(regexp = "^/(api/)?[a-zA-Z0-9/_\\-{}]+$", message = "Invalid API URI format")
	private String apiUri;

	private String httpMethod; // GET, POST, PUT, DELETE, PATCH

	@Builder.Default
	private boolean includeBusinessLogic = true;

	@Builder.Default
	private boolean includeTechnicalWorkflow = true;

	private String preferredLanguage; // en, de
}