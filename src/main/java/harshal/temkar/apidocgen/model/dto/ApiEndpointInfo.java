package harshal.temkar.apidocgen.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Contains extracted API endpoint metadata.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiEndpointInfo {

    private String controllerClassName;
    private String methodName;
    private String uri;
    private String httpMethod;
    private List<String> requestParams;
    private String requestBodyType;
    private String responseType;
    private List<String> pathVariables;
    private Map<String, String> annotations;
    private String methodSourceCode;
}