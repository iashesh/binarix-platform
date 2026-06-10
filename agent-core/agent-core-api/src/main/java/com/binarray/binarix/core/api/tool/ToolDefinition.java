package com.binarray.binarix.core.api.tool;

import java.lang.annotation.*;

/**
 * Annotation that declares a class as a Binarix Platform agent tool.
 * <p>
 * Applied at the class level on any Spring component that implements {@link Tool}.
 * The {@code ToolRegistry} scans for all beans carrying this annotation at application
 * startup and registers them with the Claude SDK under the declared {@link #name()}.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @ToolDefinition(
 *     name = "read_log_file",
 *     description = "Reads a log file from the configured base path.",
 *     tags = {"log-reading"},
 *     readOnly = true
 * )
 * @Component
 * public class ReadLogFileTool implements Tool, Supplier<String> { ... }
 * }</pre>
 *
 * @author Ashesh
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolDefinition {

    /**
     * The unique snake_case tool name registered with Claude.
     * <p>Must match the name Claude uses when invoking the tool in a tool-call response.</p>
     *
     * @return the tool name, e.g. {@code "read_log_file"}
     */
    String name();

    /**
     * A human-readable description of what this tool does.
     * <p>This description is sent to Claude as part of the tool schema and directly
     * influences when Claude decides to invoke the tool.</p>
     *
     * @return the tool description
     */
    String description();

    /**
     * Optional tags used to group tools.
     * <p>
     * The {@code ToolRegistry} uses these tags to filter which tools are provided to a
     * given agent. For example, an agent with tags {@code {"log-reading"}} only receives
     * tools tagged with {@code "log-reading"}.
     * </p>
     *
     * @return array of tag strings, empty by default
     */
    String[] tags() default {};

    /**
     * Whether this tool is read-only (i.e. it does not modify any external state).
     * <p>Write-capable tools (e.g. those that write files or call external APIs) should
     * set this to {@code false} to signal that elevated caution is required.</p>
     *
     * @return {@code true} if read-only (default), {@code false} if the tool mutates state
     */
    boolean readOnly() default true;
}
