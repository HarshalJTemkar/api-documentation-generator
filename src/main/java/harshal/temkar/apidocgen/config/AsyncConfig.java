package harshal.temkar.apidocgen.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async configuration for non-blocking agent execution.
 * 
 * Enables parallel processing of multiple API documentation requests. Critical
 * for batch Excel processing with 100+ APIs.
 * 
 * Thread Pool Configuration: - Core pool size: 10 (minimum active threads) -
 * Max pool size: 50 (peak load handling) - Queue capacity: 100 (buffer for
 * burst traffic)
 * 
 * Performance Impact: - Supports concurrent analysis of 50 APIs - Non-blocking
 * LLM calls - Efficient CPU utilization
 */

@Configuration
public class AsyncConfig implements AsyncConfigurer {

	@Bean(name = "agentExecutor")
	Executor agentExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(10);
		executor.setMaxPoolSize(50);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("agent-exec-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60);
		executor.initialize();
		return executor;
	}

	@Override
	public Executor getAsyncExecutor() {
		return agentExecutor();
	}
}