package harshal.temkar.apidocgen.exception;

/**
 * Exception thrown during source code parsing failures.
 */

public class SourceCodeParseException extends ApiDocGenException {

	public SourceCodeParseException(
			ErrorCode errorCode, 
			String message, 
			String correlationId) {
		super(errorCode, message, correlationId);
	}

	public SourceCodeParseException(
			ErrorCode errorCode, 
			String message, 
			String correlationId, 
			Throwable cause) {
		super(errorCode, message, correlationId, cause);
	}
}