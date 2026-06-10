package com.binarray.binarix.rca.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for the RCA Agent system.
 * <p>
 * Bound from the {@code rca.*} namespace in {@code application.properties} or
 * {@code application.yml}. Groups settings for codebase access and log file reading.
 * </p>
 *
 * <p>Example configuration:</p>
 * <pre>{@code
 * rca.codebase.type=local
 * rca.codebase.local-base-path=/path/to/your/project
 * rca.log.base-path=/var/log/myapp
 * rca.log.max-lines=5000
 * }</pre>
 *
 * @author Ashesh
 */
@ConfigurationProperties(prefix = "rca")
public class RcaProperties {

    private Codebase codebase = new Codebase();
    private Log log = new Log();

    /**
     * Returns the codebase access configuration group.
     *
     * @return the {@link Codebase} configuration
     */
    public Codebase getCodebase() { return codebase; }

    /** @param c the codebase configuration to set */
    public void setCodebase(Codebase c) { this.codebase = c; }

    /**
     * Returns the log source configuration group.
     *
     * @return the {@link Log} configuration
     */
    public Log getLog() { return log; }

    /** @param l the log configuration to set */
    public void setLog(Log l) { this.log = l; }

    /**
     * Codebase access settings — controls which adapter is used and where the
     * source code is located.
     */
    public static class Codebase {
        private String type = "local";
        private String localBasePath = ".";
        private String githubRepo = "";
        private String githubBranch = "main";
        private String githubToken = "";

        /**
         * Returns the codebase adapter type. Supported values: {@code "local"}, {@code "github"}.
         *
         * @return the adapter type string
         */
        public String getType() { return type; }

        /** @param t the adapter type to set */
        public void setType(String t) { this.type = t; }

        /**
         * Returns the local filesystem base path for the codebase.
         * All file reads are jailed within this directory.
         *
         * @return the local base path, default {@code "."}
         */
        public String getLocalBasePath() { return localBasePath; }

        /** @param p the local base path to set */
        public void setLocalBasePath(String p) { this.localBasePath = p; }

        /**
         * Returns the GitHub repository identifier in {@code org/repo} format.
         * Only used when {@link #getType()} is {@code "github"}.
         *
         * @return the repository identifier
         */
        public String getGithubRepo() { return githubRepo; }

        /** @param r the GitHub repository to set */
        public void setGithubRepo(String r) { this.githubRepo = r; }

        /**
         * Returns the GitHub branch to read from.
         *
         * @return the branch name, default {@code "main"}
         */
        public String getGithubBranch() { return githubBranch; }

        /** @param b the branch name to set */
        public void setGithubBranch(String b) { this.githubBranch = b; }

        /**
         * Returns the GitHub personal access token for authenticated API requests.
         * Inject via {@code ${GITHUB_TOKEN}} environment variable.
         *
         * @return the token string
         */
        public String getGithubToken() { return githubToken; }

        /** @param t the token to set */
        public void setGithubToken(String t) { this.githubToken = t; }
    }

    /**
     * Log source settings — controls where log files are read from and how many lines
     * are processed per analysis run.
     */
    public static class Log {
        private int maxLines = 5000;
        private String basePath = "/var/log";

        /**
         * Returns the maximum number of log lines to read per analysis.
         *
         * @return max lines, default {@code 5000}
         */
        public int getMaxLines() { return maxLines; }

        /** @param m the max lines to set */
        public void setMaxLines(int m) { this.maxLines = m; }

        /**
         * Returns the base directory path for log file reads.
         * All log reads are jailed within this directory.
         *
         * @return the base path, default {@code "/var/log"}
         */
        public String getBasePath() { return basePath; }

        /** @param p the base path to set */
        public void setBasePath(String p) { this.basePath = p; }
    }
}
