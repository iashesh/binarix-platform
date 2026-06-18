package com.binarray.binarix.rca.agent;

import com.binarray.binarix.core.api.agent.AgentContext;
import com.binarray.binarix.core.api.agent.AgentMetadata;
import com.binarray.binarix.core.api.event.AgentEvent;
import com.binarray.binarix.core.impl.agent.AbstractAgent;
import com.binarray.binarix.core.impl.autoconfigure.AgentCoreProperties;
import com.binarray.binarix.rca.model.LogFindings;
import com.binarray.binarix.rca.model.RcaRequest;
import org.springframework.stereotype.Service;

/**
 * Agent responsible for reading and analysing application log files.
 * <p>
 * Uses the {@code log-reading} tool set to systematically:
 * <ol>
 *   <li>Read the raw log file via {@code read_log_file}.</li>
 *   <li>Search for ERROR and WARN entries via {@code grep_pattern}.</li>
 *   <li>Parse any exception stack traces via {@code parse_stack_trace}.</li>
 * </ol>
 * Produces a {@link LogFindings} record containing structured patterns, frames,
 * and a full raw analysis consumed by the {@code RcaSynthesizerAgent}.
 * </p>
 *
 * @author Ashesh
 */
@Service
public class LogAnalystAgent extends AbstractAgent<RcaRequest, LogFindings> {

    private final AgentCoreProperties coreProps;

    public LogAnalystAgent(AgentCoreProperties coreProps) {
        this.coreProps = coreProps;
    }

    @Override
    public String getName() { return "log-analyst"; }

    /**
     * Short-circuits the entire LLM call when the caller has supplied inline error text.
     * No file is read, no tokens are spent — the provided text is used directly as findings.
     */
    @Override
    public LogFindings execute(RcaRequest input, AgentContext ctx) {
        if (input.hasInlineError()) {
            eventBus.onAgentStarted(AgentEvent.started(getName(), ctx.correlationId()));
            log.info("[{}] Agent '{}' — using inline logErrorText, skipping log file read",
                    ctx.correlationId(), getName());
            LogFindings findings = LogFindings.fromRaw(input.logErrorText());
            eventBus.onAgentCompleted(AgentEvent.completed(getName(), ctx.correlationId(), findings));
            return findings;
        }
        return super.execute(input, ctx);
    }

    @Override
    public AgentMetadata getMetadata() {
        return AgentMetadata.builder()
                .model(coreProps.getAnthropic().getDefaultModel())
                .maxTokens(coreProps.getAnthropic().getDefaultMaxTokens())
                .systemPrompt(systemPrompt())
                .build();
    }

    @Override
    protected String[] getToolTags() { return new String[]{"log-reading"}; }

    @Override
    protected String buildUserMessage(RcaRequest input, AgentContext ctx) {
        return String.format(
            "Analyse the logs at: %s%n%n" +
            "Your job:%n" +
            "1. Use read_log_file to read the log content (maxLines=%d)%n" +
            "2. Use grep_pattern to find ERROR and WARN entries%n" +
            "3. Use parse_stack_trace for any exception stack traces you find%n" +
            "4. Summarise: what errors occurred, when, how many times, and what the stack traces point to%n%n" +
            "Return a detailed plain-text analysis covering:%n" +
            "- Error types and frequencies%n" +
            "- Timestamp of first and last error%n" +
            "- Key stack trace frames (class name, method, line number)%n" +
            "- Any recurring error signatures",
            input.logLocation(), input.maxLogLines());
    }

    @Override
    protected LogFindings parseResponse(String rawResponse, AgentContext ctx) {
        return LogFindings.fromRaw(rawResponse);
    }

    private String systemPrompt() {
        return "You are an expert log analyst specialising in Java application logs. " +
               "Use the tools available to read and search log files. " +
               "Be systematic: first read the log, then search for specific error patterns. " +
               "Always include timestamps, exception class names, and line numbers in your analysis.";
    }
}
