package harshal.temkar.apidocgen.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Enhanced async configuration with proper timeout handling.
 * 
 * Timeouts: - Agent execution: 10 minutes (LLM can be slow) - Web async
 * requests: 10 minutes - Connection timeout: 10 seconds
 * 
 * Performance: - Core pool: 10 threads - Max pool: 50 threads - Queue: 100
 * requests
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer, WebMvcConfigurer {

	@Value("${async.core-pool-size:10}")
	private int corePoolSize;

	@Value("${async.max-pool-size:50}")
	private int maxPoolSize;

	@Value("${async.queue-capacity:100}")
	private int queueCapacity;

	@Value("${async.timeout:600000}")
	private long asyncTimeout;

	/**
	 * Thread pool for agent execution.
	 */
	@Bean(name = "agentExecutor")
	Executor agentExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("agent-exec-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60);
		executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}

	@Override
	public Executor getAsyncExecutor() {
		return agentExecutor();
	}

	/**
	 * Configure async support with custom timeout.
	 */
	@Override
	public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
		configurer.setDefaultTimeout(asyncTimeout);
		configurer.registerCallableInterceptors(timeoutInterceptor());
	}

	@Bean
	TimeoutCallableProcessingInterceptor timeoutInterceptor() {
		return new TimeoutCallableProcessingInterceptor();
	}
}