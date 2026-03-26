package harshal.temkar.apidocgen.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for batch Excel processing.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelApiRequest {

	@NotBlank(message = "Project path is required")
	private String projectPath;

	@NotBlank(message = "Excel file path is required")
	private String excelFilePath;

	@Builder.Default
	private String apiUriColumnName = "API_URI";

	@Builder.Default
	private String httpMethodColumnName = "HTTP_METHOD";

	@Builder.Default
	private String businessLogicColumnName = "BUSINESS_LOGIC";

	@Builder.Default
	private String technicalWorkflowColumnName = "TECHNICAL_WORKFLOW";

	@Builder.Default
	private int startRow = 1; // Skip header

	private String preferredLanguage;
}