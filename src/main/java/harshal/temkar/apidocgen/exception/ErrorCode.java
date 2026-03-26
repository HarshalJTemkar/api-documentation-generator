package harshal.temkar.apidocgen.exception;

import lombok.Getter;

/**
 * Centralized error codes for API Documentation Generator.
 * 
 * Benefits: - Consistent error identification across services - I18N support
 * for multi-language error messages - Client-friendly error handling
 */

@Getter
public enum ErrorCode {

	// Source Code Parsing Errors (1000-1099)
	SOURCE_CODE_NOT_FOUND("ERR-1000", "error.source.code.not.found"),
	INVALID_MAVEN_PROJECT("ERR-1001", "error.invalid.maven.project"),
	JAVA_FILE_PARSE_ERROR("ERR-1002", "error.java.file.parse"),
	CONTROLLER_NOT_FOUND("ERR-1003", "error.controller.not.found"),
	METHOD_NOT_FOUND("ERR-1004", "error.method.not.found"),

	// Ollama Service Errors (2000-2099)
	OLLAMA_CONNECTION_FAILED("ERR-2000", "error.ollama.connection.failed"),
	OLLAMA_MODEL_NOT_AVAILABLE("ERR-2001", "error.ollama.model.not.available"),
	OLLAMA_TIMEOUT("ERR-2002", "error.ollama.timeout"),
	OLLAMA_INVALID_RESPONSE("ERR-2003", "error.ollama.invalid.response"),

	// Excel Processing Errors (3000-3099)
	EXCEL_FILE_INVALID("ERR-3000", "error.excel.file.invalid"), EXCEL_READ_ERROR("ERR-3001", "error.excel.read"),
	EXCEL_WRITE_ERROR("ERR-3002", "error.excel.write"),

	// Validation Errors (4000-4099)
	INVALID_REQUEST("ERR-4000", "error.invalid.request"),
	MISSING_REQUIRED_FIELD("ERR-4001", "error.missing.required.field"),
	INVALID_URI_FORMAT("ERR-4002", "error.invalid.uri.format"),

	// General Errors (9000-9099)
	INTERNAL_SERVER_ERROR("ERR-9000", "error.internal.server"),
	SERVICE_UNAVAILABLE("ERR-9001", "error.service.unavailable");

	private final String code;
	private final String messageKey;

	ErrorCode(String code, String messageKey) {
		this.code = code;
		this.messageKey = messageKey;
	}
}