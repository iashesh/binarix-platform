package com.binarray.binarix.rca;

import com.binarray.binarix.rca.config.RcaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot entry point for the Binarix RCA Agent system.
 * <p>
 * The RCA Agent performs automated Root Cause Analysis by orchestrating three
 * specialist agents — log analyst, code explorer, and context enricher — in parallel,
 * then synthesising their findings into a structured report via the RCA synthesizer agent.
 * </p>
 *
 * <p>Supports two run modes:</p>
 * <ul>
 *   <li><b>REST API mode</b> (default) — exposes {@code POST /api/v1/analyze} for
 *       integration with incident management tools.</li>
 *   <li><b>CLI mode</b> — activated via {@code rca.cli.enabled=true}; runs a single
 *       analysis and exits, suitable for CI/CD pipelines.</li>
 * </ul>
 *
 * @author Ashesh
 */
@SpringBootApplication
@EnableConfigurationProperties(RcaProperties.class)
public class RcaAgentApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments passed through to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(RcaAgentApplication.class, args);
    }
}
