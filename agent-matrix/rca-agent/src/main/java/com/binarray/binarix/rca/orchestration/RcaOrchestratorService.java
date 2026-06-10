package com.binarray.binarix.rca.orchestration;

import com.binarray.binarix.core.api.agent.AgentContext;
import com.binarray.binarix.core.api.orchestration.AgentNode;
import com.binarray.binarix.core.api.orchestration.AgentPipeline;
import com.binarray.binarix.core.impl.orchestration.OrchestratorService;
import com.binarray.binarix.rca.agent.*;
import com.binarray.binarix.rca.model.*;
import com.binarray.binarix.rca.agent.CodeExplorerAgent;
import com.binarray.binarix.rca.agent.ContextEnricherAgent;
import com.binarray.binarix.rca.agent.LogAnalystAgent;
import com.binarray.binarix.rca.agent.RcaSynthesizerAgent;
import com.binarray.binarix.rca.model.RcaReport;
import com.binarray.binarix.rca.model.RcaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Domain orchestrator for the RCA Agent pipeline.
 * <p>
 * Coordinates a two-phase execution:
 * </p>
 * <ol>
 *   <li><b>Phase 1 — parallel analysis:</b> {@code LogAnalystAgent},
 *       {@code CodeExplorerAgent}, and {@code ContextEnricherAgent} all run
 *       concurrently using the {@code parallel} strategy. Each receives the same
 *       {@link RcaRequest} and stores its output in the shared
 *       {@link AgentContext}.</li>
 *   <li><b>Phase 2 — sequential synthesis:</b> {@code RcaSynthesizerAgent} runs
 *       alone, reading all Phase 1 findings from the enriched context and
 *       producing the final {@link RcaReport}.</li>
 * </ol>
 *
 * @author Ashesh
 */
@Service
public class RcaOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(RcaOrchestratorService.class);

    private final OrchestratorService coreOrchestrator;
    private final LogAnalystAgent logAnalyst;
    private final CodeExplorerAgent codeExplorer;
    private final ContextEnricherAgent contextEnricher;
    private final RcaSynthesizerAgent synthesizer;

    /**
     * Constructs the orchestrator with all required agent dependencies.
     *
     * @param coreOrchestrator the platform {@link OrchestratorService} that drives pipeline execution
     * @param logAnalyst       the {@link LogAnalystAgent} for Phase 1 log analysis
     * @param codeExplorer     the {@link CodeExplorerAgent} for Phase 1 codebase exploration
     * @param contextEnricher  the {@link ContextEnricherAgent} for Phase 1 git context enrichment
     * @param synthesizer      the {@link RcaSynthesizerAgent} for Phase 2 report synthesis
     */
    public RcaOrchestratorService(OrchestratorService coreOrchestrator,
                                   LogAnalystAgent logAnalyst,
                                   CodeExplorerAgent codeExplorer,
                                   ContextEnricherAgent contextEnricher,
                                   RcaSynthesizerAgent synthesizer) {
        this.coreOrchestrator = coreOrchestrator;
        this.logAnalyst = logAnalyst;
        this.codeExplorer = codeExplorer;
        this.contextEnricher = contextEnricher;
        this.synthesizer = synthesizer;
    }

    /**
     * Runs the full two-phase RCA pipeline for the given request.
     * <p>
     * Phase 1 executes the three analyst agents in parallel, then Phase 2 runs
     * the synthesizer with all accumulated findings. If synthesis does not produce
     * a report, a fallback report containing an error message is returned rather
     * than propagating an exception.
     * </p>
     *
     * @param request the typed RCA input containing log location and codebase coordinates
     * @return the completed {@link RcaReport}; never {@code null}
     */
    public RcaReport analyze(RcaRequest request) {
        log.info("Starting RCA analysis for log: {}", request.logLocation());
        AgentContext ctx = AgentContext.create(request);

        // Phase 1: run all three analyst agents concurrently
        AgentPipeline analysisPhase = AgentPipeline.builder()
                .strategy("parallel")
                .addNode(AgentNode.of(logAnalyst, request))
                .addNode(AgentNode.of(codeExplorer, request))
                .addNode(AgentNode.of(contextEnricher, request))
                .build();

        log.info("[{}] Phase 1: parallel analysis started", ctx.correlationId());
        AgentContext enrichedCtx = coreOrchestrator.orchestrate(analysisPhase, ctx);
        log.info("[{}] Phase 1 complete. Findings: {}", ctx.correlationId(), enrichedCtx.findings().keySet());

        // Phase 2: synthesize all findings into the final report
        AgentPipeline synthesisPhase = AgentPipeline.builder()
                .strategy("sequential")
                .addNode(AgentNode.of(synthesizer, request))
                .build();

        log.info("[{}] Phase 2: synthesis started", ctx.correlationId());
        AgentContext finalCtx = coreOrchestrator.orchestrate(synthesisPhase, enrichedCtx);

        RcaReport report = (RcaReport) finalCtx.findings().get("rca-synthesizer");
        if (report == null) {
            log.error("[{}] Synthesizer produced no report", ctx.correlationId());
            return RcaReport.fromRaw(ctx.correlationId(), "RCA synthesis failed - check logs");
        }
        log.info("[{}] RCA analysis complete", ctx.correlationId());
        return report;
    }
}
