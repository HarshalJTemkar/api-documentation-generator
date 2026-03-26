package harshal.temkar.apidocgen.util;

import java.util.UUID;

import org.slf4j.MDC;

/**
 * Utility for managing correlation IDs across request lifecycle.
 * 
 * Used for: - Distributed tracing - Log aggregation - Request tracking across
 * microservices
 */

public final class CorrelationIdUtil {

	private static final String CORRELATION_ID_KEY = "correlationId";

	private CorrelationIdUtil() {
		throw new UnsupportedOperationException("Utility class");
	}

	public static String generateCorrelationId() {
		String correlationId = UUID.randomUUID().toString();
		MDC.put(CORRELATION_ID_KEY, correlationId);
		return correlationId;
	}

	public static String getCorrelationId() {
		String correlationId = MDC.get(CORRELATION_ID_KEY);
		if (correlationId == null) {
			correlationId = generateCorrelationId();
		}
		return correlationId;
	}

	public static void setCorrelationId(String correlationId) {
		MDC.put(CORRELATION_ID_KEY, correlationId);
	}

	public static void clearCorrelationId() {
		MDC.remove(CORRELATION_ID_KEY);
	}
}