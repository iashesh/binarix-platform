package com.binarray.binarix.rca.agent;

import com.binarray.binarix.core.api.agent.AgentContext;
import com.binarray.binarix.core.api.agent.AgentMetadata;
import com.binarray.binarix.core.impl.agent.AbstractAgent;
import com.binarray.binarix.core.impl.autoconfigure.AgentCoreProperties;
import com.binarray.binarix.rca.model.ContextFindings;
import com.binarray.binarix.rca.model.RcaRequest;
import org.springframework.stereotype.Service;

/**
 * Agent responsible for gathering deployment and change context to enrich the RCA.
 * <p>
 * Uses the {@code git} tool set to examine recent commits and identify changes that
 * may have introduced the observed errors. Runs in parallel with {@code LogAnalystAgent}
 * and {@code CodeExplorerAgent} during Phase 1 of the RCA pipeline.
 * </p>
 *
 * @author Ashesh
 */
@Service
public class ContextEnricherAgent extends AbstractAgent<RcaRequest, ContextFindings> {

    private final AgentCoreProperties coreProps;

    public ContextEnricherAgent(AgentCoreProperties coreProps) {
        this.coreProps = coreProps;
    }

    @Override
    public String getName() { return "context-enricher"; }

    @Override
    public AgentMetadata getMetadata() {
        return AgentMetadata.builder()
                .model(coreProps.getAnthropic().getDefaultModel())
                .maxTokens(coreProps.getAnthropic().getDefaultMaxTokens())
                .systemPrompt(systemPrompt())
                .build();
    }

    @Override
    protected String[] getToolTags() { return new String[]{"git"}; }

    @Override
    protected String buildUserMessage(RcaRequest input, AgentContext ctx) {
        String errorHint = input.hasInlineError()
                ? "\n\nThe error being investigated — focus your search on commits related to this:\n"
                  + input.logErrorText()
                : "";
        return String.format(
            "Enrich the context for root cause analysis of the application at: %s%s%n%n" +
            "Your job:%n" +
            "1. Use git_log to find recent commits (last 20) that might have introduced the error%n" +
            "2. Look for commits with keywords: fix, bug, refactor, update, change, remove%n" +
            "3. Identify which files were recently changed that might be relevant%n%n" +
            "Return a summary of recent changes that could be related to the observed errors.",
            input.codebaseLocation(), errorHint);
    }

    @Override
    protected ContextFindings parseResponse(String rawResponse, AgentContext ctx) {
        return ContextFindings.fromRaw(rawResponse);
    }

    private String systemPrompt() {
        return "You are a DevOps detective specialising in deployment and change analysis. " +
               "Use git_log to identify recent changes that might have caused issues. " +
               "Focus on commits that touched critical files or had unusual change patterns.";
    }
}
