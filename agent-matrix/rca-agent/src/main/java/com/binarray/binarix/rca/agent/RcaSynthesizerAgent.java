package com.binarray.binarix.rca.agent;

import com.binarray.binarix.core.api.agent.AgentContext;
import com.binarray.binarix.core.api.agent.AgentMetadata;
import com.binarray.binarix.core.impl.agent.AbstractAgent;
import com.binarray.binarix.core.impl.autoconfigure.AgentCoreProperties;
import com.binarray.binarix.rca.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    private final AgentCoreProperties coreProps;
    private final ObjectMapper objectMapper;

    public RcaSynthesizerAgent(AgentCoreProperties coreProps, ObjectMapper objectMapper) {
        this.coreProps = coreProps;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() { return "rca-synthesizer"; }

    @Override
    public AgentMetadata getMetadata() {
        return AgentMetadata.builder()
                .model(coreProps.getAnthropic().getDefaultModel())
                .maxTokens(coreProps.getAnthropic().getDefaultMaxTokens())
                .systemPrompt(systemPrompt())
                .build();
    }

    @Override
    protected String[] getToolTags() { return new String[0]; }

    @Override
    protected String buildUserMessage(RcaRequest input, AgentContext ctx) {
        StringBuilder sb = new StringBuilder(
            "Synthesise a Root Cause Analysis from the following agent findings.\n\n" +
            "RULES:\n" +
            "- Every root cause MUST be directly evidenced by an actual error in the LOG ANALYSIS.\n" +
            "- Use code and git findings ONLY to explain errors that appear in the logs.\n" +
            "- Do NOT infer problems from git history that are absent from the logs.\n" +
            "- If the log shows a simple error, report it simply — do not over-engineer.\n\n"
        );

        Object logFindings = ctx.findings().get("log-analyst");
        if (logFindings instanceof LogFindings lf) {
            sb.append("=== LOG ANALYSIS ===\n").append(cap(lf.rawAnalysis())).append("\n\n");
        }
        Object codeFindings = ctx.findings().get("code-explorer");
        if (codeFindings instanceof CodeFindings cf) {
            sb.append("=== CODE ANALYSIS ===\n").append(cap(cf.rawAnalysis())).append("\n\n");
        }
        Object contextFindings = ctx.findings().get("context-enricher");
        if (contextFindings instanceof ContextFindings ctxf) {
            sb.append("=== CONTEXT / GIT ANALYSIS ===\n").append(cap(ctxf.rawAnalysis())).append("\n\n");
        }

        sb.append("""
            Respond with a single JSON object — no prose, no markdown fences — matching this schema exactly:
            {
              "executiveSummary": "<2-3 sentence summary of the incident and its cause>",
              "rootCauses": [
                {
                  "description": "<explanation of this root cause>",
                  "confidenceScore": <0.0–1.0>,
                  "evidence": ["<log line, file:line, or commit hash>"],
                  "category": "<auth|config|db|null-pointer|oom|race-condition|network|...>",
                  "affectedFile": "<path/to/File.java or null>",
                  "affectedLine": <line number or 0>
                }
              ],
              "affectedComponent": "<service, class, or method that is the epicentre>",
              "severity": "<CRITICAL|HIGH|MEDIUM|LOW>",
              "remediationSteps": ["<Step 1>", "<Step 2>"]
            }
            """);

        return sb.toString();
    }

    @Override
    protected RcaReport parseResponse(String rawResponse, AgentContext ctx) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(rawResponse));

            List<RootCause> rootCauses = new ArrayList<>();
            for (JsonNode rc : root.path("rootCauses")) {
                List<String> evidence = new ArrayList<>();
                for (JsonNode e : rc.path("evidence")) evidence.add(e.asText());
                rootCauses.add(new RootCause(
                    rc.path("description").asText(""),
                    rc.path("confidenceScore").asDouble(0.0),
                    evidence,
                    rc.path("category").asText(""),
                    rc.path("affectedFile").isNull() ? null : rc.path("affectedFile").asText(null),
                    rc.path("affectedLine").asInt(0)
                ));
            }

            List<String> remediationSteps = new ArrayList<>();
            for (JsonNode step : root.path("remediationSteps")) remediationSteps.add(step.asText());

            return new RcaReport(
                ctx.correlationId(),
                rootCauses,
                root.path("executiveSummary").asText(""),
                remediationSteps,
                root.path("affectedComponent").asText("Unknown"),
                root.path("severity").asText("UNKNOWN"),
                Instant.now(),
                rawResponse
            );
        } catch (Exception e) {
            log.warn("[{}] Structured JSON parse failed, falling back to raw: {}",
                    ctx.correlationId(), e.getMessage());
            return RcaReport.fromRaw(ctx.correlationId(), rawResponse);
        }
    }

    /** Caps a finding section at 4 000 chars to keep the synthesizer prompt lean. */
    private static String cap(String text) {
        if (text == null) return "";
        return text.length() <= 4_000 ? text : text.substring(0, 4_000) + "\n… [truncated]";
    }

    /** Strips markdown code fences that Claude sometimes wraps around JSON output. */
    private String extractJson(String response) {
        String trimmed = response.strip();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) return trimmed.substring(start, end).strip();
        }
        return trimmed;
    }

    private String systemPrompt() {
        return "You are a senior Site Reliability Engineer and Root Cause Analysis expert. " +
               "You receive structured findings from log analysis, code exploration, and git history. " +
               "Ground every root cause in actual log evidence — never infer problems from git history alone. " +
               "Respond ONLY with the JSON object specified in the user message — no prose, no markdown.";
    }
}
