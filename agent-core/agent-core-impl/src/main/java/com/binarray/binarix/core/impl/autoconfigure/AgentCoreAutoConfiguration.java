package com.binarray.binarix.core.impl.autoconfigure;

import com.binarray.binarix.core.api.memory.MemoryStore;
import com.binarray.binarix.core.api.retry.RetryPolicy;
import com.binarray.binarix.core.impl.anthropic.AnthropicGateway;
import com.binarray.binarix.core.impl.event.CompositeAgentEventListener;
import com.binarray.binarix.core.impl.memory.InMemoryStore;
import com.binarray.binarix.core.impl.orchestration.OrchestratorService;
import com.binarray.binarix.core.impl.orchestration.ParallelStrategy;
import com.binarray.binarix.core.impl.orchestration.SequentialStrategy;
import com.binarray.binarix.core.impl.retry.ExponentialBackoffRetryPolicy;
import com.binarray.binarix.core.impl.tool.ToolRegistry;
import com.binarray.binarix.core.api.event.AgentEventListener;
import com.binarray.binarix.core.api.orchestration.OrchestratorStrategy;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.List;

/**
 * Spring Boot auto-configuration for the Binarix Platform agent-core module.
 * <p>
 * Registers all core infrastructure beans with {@link ConditionalOnMissingBean} guards,
 * allowing domain applications to override any individual bean simply by declaring their
 * own implementation in their Spring context.
 * </p>
 *
 * <p>This class is registered in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * and is activated automatically when {@code agent-core-impl} is on the classpath.</p>
 *
 * @author Ashesh
 */
@AutoConfiguration
@EnableConfigurationProperties(AgentCoreProperties.class)
@EnableAspectJAutoProxy
public class AgentCoreAutoConfiguration {

    /**
     * Creates the {@link AnthropicClient} bean used by {@link AnthropicGateway}.
     * <p>Override this bean to customise timeout, proxy, or backend settings.</p>
     *
     * @param props the platform configuration containing the API key
     * @return a configured {@link AnthropicClient} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public AnthropicClient anthropicClient(AgentCoreProperties props) {
        return AnthropicOkHttpClient.builder()
                .apiKey(props.getAnthropic().getApiKey())
                .build();
    }

    /**
     * Creates the default {@link RetryPolicy} bean using exponential backoff.
     * <p>Override this bean to substitute a custom retry strategy.</p>
     *
     * @param props the platform configuration (unused by the default implementation)
     * @return an {@link ExponentialBackoffRetryPolicy} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public RetryPolicy retryPolicy(AgentCoreProperties props) {
        return new ExponentialBackoffRetryPolicy();
    }

    /**
     * Creates the {@link AnthropicGateway} bean that drives the Claude tool-call loop.
     *
     * @param client      the Anthropic API client
     * @param retryPolicy the retry policy for transient API failures
     * @param eventBus    the composite event listener for tool lifecycle events
     * @return a configured {@link AnthropicGateway} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public AnthropicGateway anthropicGateway(AnthropicClient client, RetryPolicy retryPolicy,
                                              CompositeAgentEventListener eventBus) {
        return new AnthropicGateway(client, retryPolicy, eventBus);
    }

    /**
     * Creates the {@link ToolRegistry} bean that scans and indexes all tool beans.
     *
     * @return a new {@link ToolRegistry} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public ToolRegistry toolRegistry() {
        return new ToolRegistry();
    }

    /**
     * Creates the default in-memory {@link MemoryStore} bean.
     * <p>Override with a Redis or database-backed implementation for persistent memory.</p>
     *
     * @return an {@link InMemoryStore} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public MemoryStore memoryStore() {
        return new InMemoryStore();
    }

    /**
     * Creates the {@link CompositeAgentEventListener} that fans events to all listeners.
     *
     * @param listeners all {@link AgentEventListener} beans in the application context
     * @return a configured {@link CompositeAgentEventListener}
     */
    @Bean
    @ConditionalOnMissingBean
    public CompositeAgentEventListener compositeAgentEventListener(List<AgentEventListener> listeners) {
        return new CompositeAgentEventListener(listeners);
    }

    /**
     * Registers the {@link ParallelStrategy} bean for concurrent agent execution.
     *
     * @return a new {@link ParallelStrategy} instance
     */
    @Bean
    public ParallelStrategy parallelStrategy(AgentCoreProperties props) {
        return new ParallelStrategy(props.getOrchestration().getAgentTimeout());
    }

    /**
     * Registers the {@link SequentialStrategy} bean for ordered agent execution.
     *
     * @param props the platform configuration providing the per-agent timeout
     * @return a new {@link SequentialStrategy} instance
     */
    @Bean
    public SequentialStrategy sequentialStrategy(AgentCoreProperties props) {
        return new SequentialStrategy(props.getOrchestration().getAgentTimeout());
    }

    /**
     * Creates the {@link OrchestratorService} that selects and invokes orchestration strategies.
     *
     * @param strategies all registered {@link OrchestratorStrategy} beans
     * @param props      the platform configuration providing the default strategy ID
     * @return a configured {@link OrchestratorService}
     */
    @Bean
    public OrchestratorService orchestratorService(List<OrchestratorStrategy> strategies,
                                                    AgentCoreProperties props) {
        return new OrchestratorService(strategies, props);
    }
}
