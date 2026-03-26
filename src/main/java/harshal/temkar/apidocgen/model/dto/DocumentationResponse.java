package harshal.temkar.apidocgen.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing generated documentation.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationResponse {

    private String correlationId;
    private String apiUri;
    private String httpMethod;
    private ApiEndpointInfo endpointInfo;
    private BusinessLogicDoc businessLogic;
    private TechnicalWorkflowDoc technicalWorkflow;
    private LocalDateTime generatedAt;
    private long processingTimeMs;
}