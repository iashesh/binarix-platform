package com.binarray.binarix.rca.agent;

import com.binarray.binarix.core.api.agent.AgentContext;
import com.binarray.binarix.core.api.agent.AgentMetadata;
import com.binarray.binarix.core.impl.agent.AbstractAgent;
import com.binarray.binarix.rca.model.*;
import com.binarray.binarix.rca.model.*;
import org.springframework.stereotype.Service;

/**
 * Agent responsible for synthesising all Phase 1 findings into a final RCA report.
 * <p>
 * Runs as the sole node in Phase 2 of the pipeline after the parallel analysis phase has
 * completed. Uses no tools — relies entirely on Claude's reasoning applied to the findings
 * provided in the prompt via {@link AgentContext}.
 * </p>
 *
 * @author Ashesh
 */
@Service
public class RcaSynthesizerAgent extends AbstractAgent<RcaRequest, RcaReport> {

    @Override
    public String getName() { return "rca-synthesizer"; }

    @Override
    public AgentMetadata getMetadata() {
        return AgentMetadata.builder()
                .model("claude-sonnet-4-5")
                .maxTokens(8192)
                .systemPrompt(systemPrompt())
                .build();
    }

    @Override
    protected String[] getToolTags() { return new String[0]; }

    @Override
    protected String buildUserMessage(RcaRequest input, AgentContext ctx) {
        StringBuilder sb = new StringBuilder("Synthesise a Root Cause Analysis from the following agent findings:\n\n");
        Object logFindings = ctx.findings().get("log-analyst");
        if (logFindings instanceof LogFindings lf) {
            sb.append("=== LOG ANALYSIS ===\n").append(lf.rawAnalysis()).append("\n\n");
        }
        Object codeFindings = ctx.findings().get("code-explorer");
        if (codeFindings instanceof CodeFindings cf) {
            sb.append("=== CODE ANALYSIS ===\n").append(cf.rawAnalysis()).append("\n\n");
        }
        Object contextFindings = ctx.findings().get("context-enricher");
        if (contextFindings instanceof ContextFindings ctxf) {
            sb.append("=== CONTEXT / GIT ANALYSIS ===\n").append(ctxf.rawAnalysis()).append("\n\n");
        }
        sb.append("Based on the above, produce a complete Root Cause Analysis report with:\n");
        sb.append("1. EXECUTIVE SUMMARY (2-3 sentences)\n");
        sb.append("2. ROOT CAUSES (ranked by confidence, with evidence for each)\n");
        sb.append("3. AFFECTED COMPONENT (which service/class/method is the epicentre)\n");
        sb.append("4. SEVERITY (CRITICAL/HIGH/MEDIUM/LOW with justification)\n");
        sb.append("5. REMEDIATION STEPS (numbered, actionable steps to fix the issue)\n");
        sb.append("6. PREVENTION (what to do to prevent recurrence)");
        return sb.toString();
    }

    @Override
    protected RcaReport parseResponse(String rawResponse, AgentContext ctx) {
        return RcaReport.fromRaw(ctx.correlationId(), rawResponse);
    }

    private String systemPrompt() {
        return "You are a senior Site Reliability Engineer and Root Cause Analysis expert. " +
               "You receive structured findings from log analysis, code exploration, and git history. " +
               "Synthesise these into a clear, actionable RCA report. " +
               "Be specific: cite actual class names, line numbers, and commit hashes where available. " +
               "Rank root causes by confidence. Make remediation steps concrete and actionable.";
    }
}
