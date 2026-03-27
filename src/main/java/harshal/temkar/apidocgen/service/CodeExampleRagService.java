package harshal.temkar.apidocgen.service;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;

/**
 * RAG (Retrieval Augmented Generation) service for code examples.
 * 
 * Use case:
 * - Store previously analyzed code as examples
 * - Retrieve similar code patterns for few-shot learning
 * - Improve LLM accuracy with relevant examples
 * 
 * Performance:
 * - In-memory vector store (can switch to Pinecone/Weaviate for production)
 * - Embedding generation: 50-100ms per code snippet
 * - Similarity search: <10ms for 10K vectors
 */
@Service
@Slf4j
public class CodeExampleRagService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentSplitter documentSplitter;

    public CodeExampleRagService() {
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        this.documentSplitter = DocumentSplitters.recursive(500, 50);
    }

    /**
     * Stores code example for future retrieval.
     * 
     * @param sourceCode Java source code
     * @param documentation Generated documentation
     */
    public void storeCodeExample(String sourceCode, String documentation) {
        // Combine code and documentation for better context
        String combinedText = sourceCode + "\n\n" + documentation;
        
        Document document = Document.from(combinedText);
        List<TextSegment> segments = documentSplitter.split(document);

        segments.forEach(segment -> {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        });

        log.info("Stored code example in vector store");
    }

    /**
     * Retrieves similar code examples for few-shot learning.
     * 
     * @param sourceCode Target source code
     * @param maxResults Maximum number of similar examples
     * @return List of similar code snippets
     */
    public List<String> findSimilarExamples(String sourceCode, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(sourceCode).content();
        
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(
                queryEmbedding, 
                maxResults,
                0.7 // Minimum similarity threshold
        );

        return matches.stream()
                .map(match -> match.embedded().text())
                .toList();
    }

    /**
     * Enhances prompt with similar code examples (few-shot learning).
     * 
     * @param basePrompt Original prompt
     * @param sourceCode Target source code
     * @return Enhanced prompt with examples
     */
    public String enhancePromptWithExamples(String basePrompt, String sourceCode) {
        List<String> examples = findSimilarExamples(sourceCode, 2);
        
        if (examples.isEmpty()) {
            return basePrompt;
        }

        StringBuilder enhancedPrompt = new StringBuilder(basePrompt);
        enhancedPrompt.append("\n\n## Similar Code Examples\n");
        
        for (int i = 0; i < examples.size(); i++) {
            enhancedPrompt.append("\n### Example ").append(i + 1).append(":\n");
            enhancedPrompt.append("```\n").append(examples.get(i)).append("\n```\n");
        }

        return enhancedPrompt.toString();
    }
}