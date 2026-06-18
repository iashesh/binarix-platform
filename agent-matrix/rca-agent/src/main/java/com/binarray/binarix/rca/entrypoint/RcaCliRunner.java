package com.binarray.binarix.rca.entrypoint;

import com.binarray.binarix.rca.config.RcaProperties;
import com.binarray.binarix.rca.model.RcaReport;
import com.binarray.binarix.rca.model.RcaRequest;
import com.binarray.binarix.rca.orchestration.RcaOrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * Spring Boot {@link CommandLineRunner} that triggers an RCA analysis when the
 * application is started in CLI mode.
 * <p>
 * CLI mode is activated by setting {@code rca.cli.enabled=true} and
 * {@code rca.cli.log-location} in {@code application.properties} (or via
 * command-line arguments such as {@code --rca.cli.enabled=true}).
 * When not activated, the runner logs an informational message and returns
 * immediately, leaving the application running in REST API mode.
 * </p>
 *
 * <p>Process exit codes: {@code 0} on successful analysis, {@code 1} on failure.</p>
 *
 * @author Ashesh
 */
@Component
public class RcaCliRunner implements CommandLineRunner, ExitCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(RcaCliRunner.class);

    private final RcaOrchestratorService orchestrator;
    private final RcaProperties rcaProps;
    private int exitCode = 0;

    @Value("${rca.cli.log-location:}")
    private String  cliLogLocation;

    @Value("${rca.cli.log-error-text:}")
    private String  cliLogErrorText;

    @Value("${rca.cli.codebase-type:local}")
    private String  cliCodebaseType;

    @Value("${rca.cli.codebase-location:.}")
    private String  cliCodebaseLocation;

    @Value("${rca.cli.enabled:false}")
    private boolean cliEnabled;

    /**
     * Constructs the CLI runner with the RCA orchestrator and configuration properties.
     *
     * @param orchestrator the domain orchestrator that drives the two-phase RCA pipeline
     * @param rcaProps     the RCA configuration properties providing the default max log lines
     */
    public RcaCliRunner(RcaOrchestratorService orchestrator, RcaProperties rcaProps) {
        this.orchestrator = orchestrator;
        this.rcaProps = rcaProps;
    }

    /**
     * Runs the RCA analysis if CLI mode is enabled, then prints the report to stdout.
     * <p>
     * If CLI mode is not enabled (the default), logs an informational message and
     * returns immediately so the REST API can continue serving requests.
     * Sets {@link #getExitCode()} to {@code 1} if the analysis throws an exception.
     * </p>
     *
     * @param args the Spring Boot command-line arguments (unused directly)
     */
    @Override
    public void run(String... args) {
        boolean hasInput = !cliLogLocation.isBlank() || !cliLogErrorText.isBlank();
        if (!cliEnabled || !hasInput) {
            log.info("CLI mode not activated. Use POST /api/v1/analyze, or set rca.cli.enabled=true " +
                     "with rca.cli.log-location (file) or rca.cli.log-error-text (inline error)");
            return;
        }
        try {
            log.info("=== RCA Agent Starting (CLI mode) ===");
            if (!cliLogErrorText.isBlank()) {
                log.info("Mode: inline error text | Codebase: {} ({})", cliCodebaseLocation, cliCodebaseType);
            } else {
                log.info("Mode: log file {} | Codebase: {} ({})", cliLogLocation, cliCodebaseLocation, cliCodebaseType);
            }

            String logErrorText = cliLogErrorText.isBlank() ? null : cliLogErrorText;
            RcaRequest request = new RcaRequest(cliLogLocation, cliCodebaseType, cliCodebaseLocation,
                    rcaProps.getLog().getMaxLines(), "main", logErrorText);
            RcaReport report = orchestrator.analyze(request);

            System.out.println("\n" + "=".repeat(80));
            System.out.println("ROOT CAUSE ANALYSIS REPORT");
            System.out.println("=".repeat(80));
            System.out.println("Correlation ID : " + report.correlationId());
            System.out.println("Generated At   : " + report.generatedAt());
            System.out.println("Severity       : " + report.severity());
            System.out.println("Component      : " + report.affectedComponent());
            System.out.println("=".repeat(80));
            System.out.println(report.executiveSummary());
            System.out.println("=".repeat(80));
        } catch (Exception e) {
            log.error("RCA analysis failed: {}", e.getMessage(), e);
            exitCode = 1;
        }
    }

    /**
     * Returns the process exit code after the CLI run completes.
     *
     * @return {@code 0} on success, {@code 1} if the analysis threw an exception
     */
    @Override
    public int getExitCode() { return exitCode; }
}
