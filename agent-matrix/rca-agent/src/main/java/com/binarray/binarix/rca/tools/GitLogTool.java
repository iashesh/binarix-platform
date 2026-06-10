package com.binarray.binarix.rca.tools;

import com.binarray.binarix.core.api.tool.AgentTool;
import com.binarray.binarix.core.api.tool.ToolDefinition;
import com.binarray.binarix.rca.config.RcaProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.function.Supplier;

/**
 * This is tool to get recent git commits for a file or directory.
 *
 * @author Ashesh
 */
@ToolDefinition(
    name = "git_log",
    description = "Returns recent git commits for a file or directory. Shows what changed recently.",
    tags = {"git"},
    readOnly = true
)
@Component
public class GitLogTool implements AgentTool, Supplier<String> {
    private static final Logger log = LoggerFactory.getLogger(GitLogTool.class);

    @JsonPropertyDescription("File or directory path to get git log for. Use '.' for all recent commits.")
    public String path = ".";

    @JsonPropertyDescription("Number of recent commits to return")
    public int count = 10;

    private final RcaProperties props;

    /**
     * Constructs the tool with the RCA properties for codebase base path resolution.
     *
     * @param props the RCA configuration properties providing the local codebase base path
     */
    public GitLogTool(RcaProperties props) { this.props = props; }

    /**
     * Executes {@code git log} for {@link #path} and returns recent commit history.
     * <p>
     * Runs as a subprocess within the configured codebase base path. If git is
     * unavailable or the path has no history, a descriptive message is returned
     * rather than throwing an exception.
     * </p>
     *
     * @return multi-line git log output in {@code hash date author: message} format,
     *         or a descriptive unavailability message
     */
    @Override
    public String get() {
        try {
            String basePath = props.getCodebase().getLocalBasePath();
            ProcessBuilder pb = new ProcessBuilder(
                    "git", "log", "--oneline", "--format=%H %ad %an: %s",
                    "--date=short", "-n", String.valueOf(count), "--", path
            );
            pb.directory(new File(basePath));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exit = process.waitFor();
            if (exit != 0 || output.isBlank()) {
                return "No git history found for path: " + path + " (exit=" + exit + ")";
            }
            return output;
        } catch (Exception e) {
            log.warn("git log failed: {}", e.getMessage());
            return "git log unavailable: " + e.getMessage();
        }
    }
}
