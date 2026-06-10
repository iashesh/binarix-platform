package com.binarray.binarix.rca.entrypoint;

import com.binarray.binarix.rca.model.*;
import com.binarray.binarix.rca.model.RcaReport;
import com.binarray.binarix.rca.model.RcaRequest;
import com.binarray.binarix.rca.model.RcaResponse;
import com.binarray.binarix.rca.orchestration.RcaOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing the RCA Agent pipeline over HTTP.
 * <p>
 * Provides two endpoints:
 * <ul>
 *   <li>{@code POST /api/v1/analyze} — triggers a full RCA analysis for the
 *       given log and codebase coordinates and returns a structured report.</li>
 *   <li>{@code GET /api/v1/health} — lightweight liveness check for load
 *       balancers and Kubernetes readiness probes.</li>
 * </ul>
 * </p>
 *
 * @author Ashesh
 */
@RestController
@RequestMapping("/api/v1")
public class RcaController {

    private static final Logger log = LoggerFactory.getLogger(RcaController.class);
    private final RcaOrchestratorService orchestrator;

    /**
     * Constructs the controller with the RCA orchestrator.
     *
     * @param orchestrator the domain orchestrator that drives the two-phase RCA pipeline
     */
    public RcaController(RcaOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Triggers a full RCA analysis and returns a structured response.
     * <p>
     * The analysis is synchronous. Both the log reading and codebase exploration
     * phases run inside this request. Consider increasing the server request timeout
     * ({@code spring.mvc.async.request-timeout}) for very large codebases or logs.
     * </p>
     *
     * @param request the typed RCA input containing the log location, codebase type,
     *                codebase location, and optional tuning parameters
     * @return {@code 200 OK} with an {@link RcaResponse} containing the report on success,
     *         or {@code 500 Internal Server Error} with an error message on failure
     */
    @PostMapping("/analyze")
    public ResponseEntity<RcaResponse> analyze(@RequestBody RcaRequest request) {
        long start = System.currentTimeMillis();
        String correlationId = java.util.UUID.randomUUID().toString();
        log.info("[{}] REST request received for log: {}", correlationId, request.logLocation());
        try {
            RcaReport report = orchestrator.analyze(request);
            return ResponseEntity.ok(RcaResponse.success(correlationId, report,
                    System.currentTimeMillis() - start));
        } catch (Exception e) {
            log.error("[{}] Analysis failed: {}", correlationId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(RcaResponse.failure(correlationId, e.getMessage(),
                            System.currentTimeMillis() - start));
        }
    }

    /**
     * Returns a simple health check response confirming the service is running.
     * <p>
     * Intended for use by load balancers, Kubernetes liveness probes, and
     * monitoring systems. For richer health information, use Spring Actuator's
     * {@code /actuator/health} endpoint.
     * </p>
     *
     * @return {@code 200 OK} with the plain string {@code "RCA Agent is running"}
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("RCA Agent is running");
    }
}
