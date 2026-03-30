package harshal.temkar.apidocgen.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import harshal.temkar.apidocgen.util.CorrelationIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final MessageSource messageSource;

	/**
	 * Handle async request timeout.
	 */
	@ExceptionHandler(AsyncRequestTimeoutException.class)
	public ResponseEntity<Map<String, Object>> handleAsyncTimeout(AsyncRequestTimeoutException ex) {
		String correlationId = CorrelationIdUtil.getCorrelationId();

		log.error("[{}] Async request timeout", correlationId, ex);

		Map<String, Object> response = new HashMap<>();
		response.put("timestamp", LocalDateTime.now());
		response.put("correlationId", correlationId);
		response.put("errorCode", "ERR-TIMEOUT");
		response.put("message", "Request processing timed out. LLM analysis takes 5-10 seconds. "
				+ "Please use /api/v1/documentation/generate/async endpoint for long-running operations.");
		response.put("suggestion", "Use async endpoint: POST /api/v1/documentation/generate/async");

		return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(response);
	}

	@ExceptionHandler(ApiDocGenException.class)
	public ResponseEntity<Map<String, Object>> handleApiDocGenException(ApiDocGenException ex, Locale locale) {

		log.error("API Documentation Generation Error [{}]: {}", ex.getCorrelationId(), ex.getMessage(), ex);

		String message = messageSource.getMessage(ex.getErrorCode().getMessageKey(), null, ex.getMessage(), locale);

		return buildErrorResponse(ex.getErrorCode().getCode(), message, ex.getCorrelationId(),
				determineHttpStatus(ex.getErrorCode()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {

		String correlationId = CorrelationIdUtil.getCorrelationId();
		log.error("Validation Error [{}]: {}", correlationId, ex.getMessage());

		Map<String, String> fieldErrors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

		Map<String, Object> response = new HashMap<>();
		response.put("timestamp", LocalDateTime.now());
		response.put("correlationId", correlationId);
		response.put("errorCode", ErrorCode.INVALID_REQUEST.getCode());
		response.put("message", "Validation failed");
		response.put("fieldErrors", fieldErrors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
		String correlationId = CorrelationIdUtil.getCorrelationId();
		log.error("Unexpected Error [{}]: {}", correlationId, ex.getMessage(), ex);

		return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
				"An unexpected error occurred: " + ex.getMessage(), correlationId, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private ResponseEntity<Map<String, Object>> buildErrorResponse(String errorCode, String message,
			String correlationId, HttpStatus status) {

		Map<String, Object> response = new HashMap<>();
		response.put("timestamp", LocalDateTime.now());
		response.put("correlationId", correlationId);
		response.put("errorCode", errorCode);
		response.put("message", message);

		return ResponseEntity.status(status).body(response);
	}

	private HttpStatus determineHttpStatus(ErrorCode errorCode) {
		return switch (errorCode) {
		case SOURCE_CODE_NOT_FOUND, CONTROLLER_NOT_FOUND, METHOD_NOT_FOUND -> HttpStatus.NOT_FOUND;
		case INVALID_REQUEST, MISSING_REQUIRED_FIELD, INVALID_URI_FORMAT, EXCEL_FILE_INVALID, INVALID_MAVEN_PROJECT ->
			HttpStatus.BAD_REQUEST;
		case OLLAMA_CONNECTION_FAILED, SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
		case OLLAMA_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
		default -> HttpStatus.INTERNAL_SERVER_ERROR;
		};
	}
}