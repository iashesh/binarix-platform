package com.binarray.binarix.core.impl.orchestration;

import com.binarray.binarix.core.api.agent.Agent;
import com.binarray.binarix.core.api.agent.AgentContext;
import com.binarray.binarix.core.api.orchestration.AgentNode;
import com.binarray.binarix.core.api.orchestration.AgentPipeline;
import com.binarray.binarix.core.api.orchestration.OrchestratorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Orchestration strategy that executes pipeline nodes one at a time in declaration order.
 * <p>
 * Each node receives the {@link AgentContext} enriched by all previously-run nodes,
 * so later agents can reference earlier agents' findings in their prompts.
 * </p>
 *
 * <p>Use this strategy when nodes have a strict dependency order — for example, a
 * synthesis agent that must wait for analysis agents to complete before it can run.</p>
 *
 * @author Ashesh
 */
@Component
public class SequentialStrategy implements OrchestratorStrategy {

    private static final Logger log = LoggerFactory.getLogger(SequentialStrategy.class);

    /**
     * {@inheritDoc}
     *
     * @return {@code "sequential"}
     */
    @Override
    public String strategyId() { return "sequential"; }

    /**
     * {@inheritDoc}
     * <p>
     * Iterates over the pipeline nodes in order. After each node completes, its output
     * is stored in the context under the agent's name before the next node is invoked.
     * </p>
     *
     * @param pipeline the pipeline whose nodes to execute in order
     * @param ctx      the initial context; enriched after each node completes
     * @return a fully enriched {@link AgentContext} after all nodes have run
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public AgentContext execute(AgentPipeline pipeline, AgentContext ctx) {
        AgentContext current = ctx;
        for (AgentNode node : pipeline.nodes()) {
            log.info("[{}] Sequential: executing node '{}'", ctx.correlationId(), node.nodeId());
            Object result = ((Agent) node.agent())
                    .execute(node.input(), current);
            current = current.withFinding(node.agent().getName(), result);
        }
        return current;
    }
}
