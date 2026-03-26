package harshal.temkar.apidocgen.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Utility for loading and formatting LLM prompt templates.
 * 
 * Templates stored in resources/prompts/ directory. Supports variable
 * substitution for dynamic prompts.
 */

public final class PromptTemplateUtil {

	private PromptTemplateUtil() {
		throw new UnsupportedOperationException("Utility class");
	}

	public static String loadTemplate(String templateName) {
		try {
			return Files.readString(Paths.get("src/main/resources/prompts", templateName));
		} catch (IOException e) {
			throw new RuntimeException("Failed to load prompt template: " + templateName, e);
		}
	}

	public static String formatTemplate(String template, Object... args) {
		return String.format(template, args);
	}
}