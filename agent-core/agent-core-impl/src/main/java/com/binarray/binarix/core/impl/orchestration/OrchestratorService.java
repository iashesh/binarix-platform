package com.binarray.binarix.core.impl.orchestration;

import com.binarray.binarix.core.api.agent.AgentContext;
import com.binarray.binarix.core.api.exception.AgentException;
import com.binarray.binarix.core.api.orchestration.AgentPipeline;
import com.binarray.binarix.core.api.orchestration.OrchestratorStrategy;
import com.binarray.binarix.core.impl.autoconfigure.AgentCoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central orchestration service for the Binarix Platform.
 * <p>
 * Selects the appropriate {@link OrchestratorStrategy} for a given {@link AgentPipeline}
 * and delegates execution to it. The strategy is resolved in priority order:
 * <ol>
 *   <li>The strategy declared on the pipeline itself ({@link AgentPipeline#getStrategy()}).</li>
 *   <li>The global default from {@code agent.core.orchestration.strategy} in application properties.</li>
 * </ol>
 * </p>
 *
 * <p>All {@link OrchestratorStrategy} Spring beans are auto-discovered at startup and
 * stored in an internal map keyed by {@link OrchestratorStrategy#strategyId()}.</p>
 *
 * @author Ashesh
 */
@Service
public class OrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);

    /** All registered strategies keyed by their {@link OrchestratorStrategy#strategyId()}. */
    private final Map<String, OrchestratorStrategy> strategies;

    /** Global platform configuration for fallback strategy and timeout settings. */
    private final AgentCoreProperties props;

    /**
     * Constructs the service by indexing all available strategy implementations.
     *
     * @param strategyList all {@link OrchestratorStrategy} beans discovered by Spring
     * @param props        the platform configuration properties
     */
    public OrchestratorService(List<OrchestratorStrategy> strategyList, AgentCoreProperties props) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(OrchestratorStrategy::strategyId, s -> s));
        this.props = props;
        log.info("OrchestratorService loaded strategies: {}", this.strategies.keySet());
    }

    /**
     * Orchestrates the given pipeline by selecting and invoking the appropriate strategy.
     * <p>
     * Returns an {@link AgentContext} enriched with findings from every agent node
     * that executed within the pipeline.
     * </p>
     *
     * @param pipeline the pipeline to execute
     * @param ctx      the current agent context for this pipeline run
     * @return an updated {@link AgentContext} with all findings merged in
     * @throws AgentException if no strategy is registered for the resolved strategy ID
     */
    public AgentContext orchestrate(AgentPipeline pipeline, AgentContext ctx) {
        String stratId = pipeline.getStrategy() != null
                ? pipeline.getStrategy()
                : props.getOrchestration().getStrategy();
        OrchestratorStrategy strategy = strategies.get(stratId);
        if (strategy == null) {
            throw new AgentException("No orchestration strategy registered for id: " + stratId);
        }
        log.info("[{}] Orchestrating {} nodes with strategy '{}'",
                ctx.correlationId(), pipeline.nodes().size(), stratId);
        return strategy.execute(pipeline, ctx);
    }
}
