package com.binarray.binarix.rca.agent;

import com.binarray.binarix.core.api.agent.AgentContext;
import com.binarray.binarix.core.api.agent.AgentMetadata;
import com.binarray.binarix.core.impl.agent.AbstractAgent;
import com.binarray.binarix.core.impl.autoconfigure.AgentCoreProperties;
import com.binarray.binarix.rca.model.CodeFindings;
import com.binarray.binarix.rca.model.LogFindings;
import com.binarray.binarix.rca.model.RcaRequest;
import org.springframework.stereotype.Service;

/**
 * Agent responsible for exploring the application codebase to trace the root cause of errors.
 * <p>
 * Uses the {@code code-reading} tool set to navigate, search, and read source files.
 * Runs in parallel with {@code LogAnalystAgent} and {@code ContextEnricherAgent} during
 * Phase 1 of the RCA pipeline.
 * </p>
 *
 * @author Ashesh
 */
@Service
public class CodeExplorerAgent extends AbstractAgent<RcaRequest, CodeFindings> {

    private final AgentCoreProperties coreProps;

    public CodeExplorerAgent(AgentCoreProperties coreProps) {
        this.coreProps = coreProps;
    }

    @Override
    public String getName() { return "code-explorer"; }

    @Override
    public AgentMetadata getMetadata() {
        return AgentMetadata.builder()
                .model(coreProps.getAnthropic().getDefaultModel())
                .maxTokens(coreProps.getAnthropic().getDefaultMaxTokens())
                .systemPrompt(systemPrompt())
                .build();
    }

    @Override
    protected String[] getToolTags() { return new String[]{"code-reading"}; }

    @Override
    protected String buildUserMessage(RcaRequest input, AgentContext ctx) {
        // Prefer inline error text; fall back to log-analyst findings (only available in
        // sequential mode — in parallel mode ctx will not yet have log-analyst results).
        String errorContext;
        if (input.hasInlineError()) {
            errorContext = "\n\nError to investigate:\n" + input.logErrorText();
        } else {
            Object logFindings = ctx.findings().get("log-analyst");
            errorContext = (logFindings instanceof LogFindings lf)
                    ? "\n\nLog analyst found these issues:\n" + lf.rawAnalysis()
                    : "";
        }
        return String.format(
            "Explore the codebase at: %s (type: %s)%s%n%n" +
            "Your job:%n" +
            "1. Use list_directory to understand the project structure%n" +
            "2. Use search_codebase to find classes/methods mentioned in the error%n" +
            "3. Use read_file to read the relevant source files%n" +
            "4. Trace the call chain that led to the error%n" +
            "5. Identify the exact lines most likely responsible%n%n" +
            "Return a detailed analysis of the relevant code, the call chain, and suspicious lines.",
            input.codebaseLocation(), input.codebaseType(), errorContext);
    }

    @Override
    protected CodeFindings parseResponse(String rawResponse, AgentContext ctx) {
        return CodeFindings.fromRaw(rawResponse);
    }

    private String systemPrompt() {
        return "You are an expert Java code archaeologist. " +
               "Given error information, you explore the codebase to find the root cause. " +
               "Use list_directory to navigate, search_codebase to find relevant code, " +
               "and read_file to inspect it. Focus on the call chain leading to the error.";
    }
}
