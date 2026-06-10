package com.binarray.binarix.core.impl.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Type-safe configuration properties for the Binarix Platform agent-core module.
 * <p>
 * All properties are bound from the {@code agent.core.*} namespace in
 * {@code application.properties} or {@code application.yml}. Nested static classes
 * group related settings logically.
 * </p>
 *
 * <p>Example configuration:</p>
 * <pre>{@code
 * agent.core.anthropic.api-key=${ANTHROPIC_API_KEY}
 * agent.core.orchestration.strategy=parallel
 * agent.core.retry.max-attempts=3
 * agent.core.security.log-base-path=/var/log/myapp
 * }</pre>
 *
 * @author Ashesh
 */
@ConfigurationProperties(prefix = "agent.core")
public class AgentCoreProperties {

    private Anthropic anthropic = new Anthropic();
    private Orchestration orchestration = new Orchestration();
    private Retry retry = new Retry();
    private Security security = new Security();

    /**
     * Returns the Anthropic API configuration group.
     *
     * @return the {@link Anthropic} configuration
     */
    public Anthropic getAnthropic() { return anthropic; }

    /** @param a the Anthropic configuration to set */
    public void setAnthropic(Anthropic a) { this.anthropic = a; }

    /**
     * Returns the orchestration configuration group.
     *
     * @return the {@link Orchestration} configuration
     */
    public Orchestration getOrchestration() { return orchestration; }

    /** @param o the orchestration configuration to set */
    public void setOrchestration(Orchestration o) { this.orchestration = o; }

    /**
     * Returns the retry configuration group.
     *
     * @return the {@link Retry} configuration
     */
    public Retry getRetry() { return retry; }

    /** @param r the retry configuration to set */
    public void setRetry(Retry r) { this.retry = r; }

    /**
     * Returns the security configuration group.
     *
     * @return the {@link Security} configuration
     */
    public Security getSecurity() { return security; }

    /** @param s the security configuration to set */
    public void setSecurity(Security s) { this.security = s; }

    /**
     * Anthropic Claude API connection settings.
     */
    public static class Anthropic {
        private String apiKey = "";
        private String defaultModel = "claude-sonnet-4-5";
        private int defaultMaxTokens = 8192;

        /**
         * Returns the Anthropic API key. Inject via {@code ${ANTHROPIC_API_KEY}} environment variable.
         *
         * @return the API key string
         */
        public String getApiKey() { return apiKey; }

        /** @param k the API key to set */
        public void setApiKey(String k) { this.apiKey = k; }

        /**
         * Returns the default Claude model identifier used when agents do not specify one.
         *
         * @return the model string, e.g. {@code "claude-sonnet-4-5"}
         */
        public String getDefaultModel() { return defaultModel; }

        /** @param m the model to set */
        public void setDefaultModel(String m) { this.defaultModel = m; }

        /**
         * Returns the default maximum output tokens per Claude API call.
         *
         * @return max tokens, default {@code 8192}
         */
        public int getDefaultMaxTokens() { return defaultMaxTokens; }

        /** @param t the max tokens to set */
        public void setDefaultMaxTokens(int t) { this.defaultMaxTokens = t; }
    }

    /**
     * Agent pipeline orchestration settings.
     */
    public static class Orchestration {
        private String strategy = "parallel";
        private Duration agentTimeout = Duration.ofMinutes(5);

        /**
         * Returns the default orchestration strategy ID used when pipelines do not specify one.
         *
         * @return the strategy ID, e.g. {@code "parallel"} or {@code "sequential"}
         */
        public String getStrategy() { return strategy; }

        /** @param s the strategy ID to set */
        public void setStrategy(String s) { this.strategy = s; }

        /**
         * Returns the maximum time to wait for a single agent pipeline to complete.
         *
         * @return the timeout duration, default {@code 5m}
         */
        public Duration getAgentTimeout() { return agentTimeout; }

        /** @param d the timeout duration to set */
        public void setAgentTimeout(Duration d) { this.agentTimeout = d; }
    }

    /**
     * Claude API retry settings applied via {@code ExponentialBackoffRetryPolicy}.
     */
    public static class Retry {
        private int maxAttempts = 3;
        private long baseDelayMs = 2000L;

        /**
         * Returns the maximum number of total attempts (including the initial try).
         *
         * @return max attempts, default {@code 3}
         */
        public int getMaxAttempts() { return maxAttempts; }

        /** @param m the max attempts to set */
        public void setMaxAttempts(int m) { this.maxAttempts = m; }

        /**
         * Returns the base delay in milliseconds before the first retry.
         * Subsequent retries double this value (exponential backoff).
         *
         * @return base delay in ms, default {@code 2000}
         */
        public long getBaseDelayMs() { return baseDelayMs; }

        /** @param d the base delay in ms to set */
        public void setBaseDelayMs(long d) { this.baseDelayMs = d; }
    }

    /**
     * Platform security constraints for file access and environment variable exposure.
     */
    public static class Security {
        private String logBasePath = "/var/log";
        private String codeBasePath = "/";
        private List<String> allowedEnvVars = List.of("APP_ENV", "REGION", "SERVICE_NAME");

        /**
         * Returns the base path used to jail log file reads.
         * Tool implementations must resolve all log paths relative to this directory.
         *
         * @return the log base path, default {@code "/var/log"}
         */
        public String getLogBasePath() { return logBasePath; }

        /** @param p the log base path to set */
        public void setLogBasePath(String p) { this.logBasePath = p; }

        /**
         * Returns the base path used to jail codebase file reads.
         *
         * @return the codebase base path
         */
        public String getCodeBasePath() { return codeBasePath; }

        /** @param p the codebase base path to set */
        public void setCodeBasePath(String p) { this.codeBasePath = p; }

        /**
         * Returns the whitelist of environment variable names that tools are permitted to read.
         *
         * @return the list of allowed variable names
         */
        public List<String> getAllowedEnvVars() { return allowedEnvVars; }

        /** @param v the list of allowed variable names to set */
        public void setAllowedEnvVars(List<String> v) { this.allowedEnvVars = v; }
    }
}
