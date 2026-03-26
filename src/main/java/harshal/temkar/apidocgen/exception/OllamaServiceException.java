package harshal.temkar.apidocgen.exception;

/**
 * Exception thrown during Ollama LLM service failures.
 */

public class OllamaServiceException extends ApiDocGenException {

	public OllamaServiceException(
			ErrorCode errorCode, 
			String message, 
			String correlationId) {
		super(errorCode, message, correlationId);
	}

	public OllamaServiceException(
			ErrorCode errorCode, 
			String message, 
			String correlationId, 
			Throwable cause) {
		super(errorCode, message, correlationId, cause);
	}
}