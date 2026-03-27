package harshal.temkar.apidocgen.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.service.AiServices;
import harshal.temkar.apidocgen.ai.BusinessLogicAnalyzer;
import harshal.temkar.apidocgen.ai.TechnicalWorkflowAnalyzer;
import lombok.RequiredArgsConstructor;

/**
 * LangChain4j configuration for advanced AI features.
 * 
 * Provides:
 * - Chat models with conversation memory
 * - Streaming support for real-time responses
 * - AI Services for structured interactions
 * 
 * Performance Considerations:
 * - Timeout: 5 minutes for complex analysis
 * - Temperature: 0.1 for deterministic output
 * - Max retries: 3 for transient failures
 */
@Configuration
@RequiredArgsConstructor
public class LangChain4jConfig {

    private final OllamaConfig ollamaConfig;

    /**
     * Creates blocking chat model for synchronous operations.
     * 
     * Use for:
     * - Single-shot documentation generation
     * - Short analysis tasks
     * - Cached responses
     */
    @Bean
    ChatLanguageModel chatLanguageModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaConfig.getBaseUrl())
                .modelName(ollamaConfig.getModel())
                .temperature(ollamaConfig.getTemperature())
                .timeout(Duration.ofMillis(ollamaConfig.getReadTimeout()))
                .maxRetries(3)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * Creates streaming chat model for real-time token delivery.
     * 
     * Use for:
     * - Live documentation generation UI
     * - Progressive rendering
     * - Better user experience for long responses
     */
    @Bean
    OllamaStreamingChatModel streamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl(ollamaConfig.getBaseUrl())
                .modelName(ollamaConfig.getModel())
                .temperature(ollamaConfig.getTemperature())
                .timeout(Duration.ofMillis(ollamaConfig.getReadTimeout()))
                .build();
    }

    /**
     * AI Service for structured business logic analysis.
     * 
     * Automatically converts LLM responses to BusinessLogicDoc POJOs.
     */
    @Bean
    BusinessLogicAnalyzer businessLogicAnalyzer(ChatLanguageModel chatModel) {
        return AiServices.builder(BusinessLogicAnalyzer.class)
                .chatLanguageModel(chatModel)
                .build();
    }

    /**
     * AI Service for technical workflow documentation.
     */
    @Bean
    TechnicalWorkflowAnalyzer technicalWorkflowAnalyzer(ChatLanguageModel chatModel) {
        return AiServices.builder(TechnicalWorkflowAnalyzer.class)
                .chatLanguageModel(chatModel)
                .build();
    }
}