package harshal.temkar.apidocgen.agent;

/**
 * Base interface for agentic AI components.
 * 
 * Each agent has a specific responsibility:
 * - CodeReaderAgent: Extracts source code
 * - BusinessLogicAgent: Analyzes business logic
 * - TechnicalWorkflowAgent: Documents technical flow
 * - DocumentationFormatterAgent: Formats output
 * 
 * Benefits:
 * - Separation of concerns
 * - Independent scaling
 * - Parallel execution
 * - Easy to add new agents
 */

public interface Agent<I, O> {
    
    /**
     * Executes agent's specific task.
     * 
     * @param input Agent input
     * @param correlationId Request tracking ID
     * @return Agent output
     */
	
    O execute(I input, String correlationId);
    
    /**
     * Returns agent name for logging/monitoring.
     */
    
    String getAgentName();
}