package harshal.temkar.apidocgen.exception;

import lombok.Getter;

/**
 * Base exception for API Documentation Generator.
 * 
 * Includes correlation ID for distributed tracing.
 */

@Getter
public class ApiDocGenException extends RuntimeException {

	private final ErrorCode errorCode;
	private final String correlationId;

	public ApiDocGenException(
			ErrorCode errorCode, 
			String message, 
			String correlationId) {
		super(message);
		this.errorCode = errorCode;
		this.correlationId = correlationId;
	}

	public ApiDocGenException(
			ErrorCode errorCode, 
			String message, 
			String correlationId, 
			Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
		this.correlationId = correlationId;
	}
}