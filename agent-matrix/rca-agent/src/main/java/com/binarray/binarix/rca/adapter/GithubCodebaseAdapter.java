package com.binarray.binarix.rca.adapter;

import com.binarray.binarix.core.api.adapter.CodebaseAdapter;
import com.binarray.binarix.rca.config.RcaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.*;
import java.net.http.*;
import java.util.*;

/**
 * {@link CodebaseAdapter} implementation that reads source code from a GitHub repository
 * via the GitHub raw content API.
 * <p>
 * Fetches files directly from the configured branch using
 * {@code https://raw.githubusercontent.com/{repo}/{branch}/{path}}.
 * An optional personal access token may be provided via
 * {@code rca.codebase.github-token} for private repository access.
 * </p>
 *
 * <p>Activated when {@code rca.codebase.type=github}.</p>
 *
 * @author Ashesh
 */
@Component
public class GithubCodebaseAdapter implements CodebaseAdapter {

    private static final Logger log = LoggerFactory.getLogger(GithubCodebaseAdapter.class);
    private final RcaProperties props;
    private final HttpClient httpClient;

    /**
     * Constructs the adapter with the RCA properties for repository configuration.
     *
     * @param props the RCA configuration properties containing the GitHub repo,
     *              branch, and optional access token
     */
    public GithubCodebaseAdapter(RcaProperties props) {
        this.props = props;
        this.httpClient = HttpClient.newBuilder().build();
    }

    /**
     * {@inheritDoc}
     *
     * @param type the codebase type string
     * @return {@code true} if {@code type} equals {@code "github"} (case-insensitive)
     */
    @Override
    public boolean supports(String type) { return "github".equalsIgnoreCase(type); }

    /**
     * {@inheritDoc}
     * <p>
     * Fetches the file content via the GitHub raw content URL.
     * Returns an error message string if the HTTP request fails or returns a
     * non-200 status code — never throws an exception.
     * </p>
     *
     * @param path the file path within the repository (e.g. {@code "src/main/java/App.java"})
     * @return the file content as a string, or a descriptive error message
     */
    @Override
    public String readFile(String path) {
        String url = String.format("https://raw.githubusercontent.com/%s/%s/%s",
                props.getCodebase().getGithubRepo(),
                props.getCodebase().getGithubBranch(),
                path);
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
            String token = props.getCodebase().getGithubToken();
            if (token != null && !token.isBlank()) {
                reqBuilder.header("Authorization", "token " + token);
            }
            HttpResponse<String> response = httpClient.send(reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) return response.body();
            return "Error: GitHub returned HTTP " + response.statusCode() + " for " + path;
        } catch (Exception e) {
            log.error("GitHub read failed for '{}': {}", path, e.getMessage());
            return "GitHub read error: " + e.getMessage();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Full directory listing is not supported via the raw content API.
     * Use the GitHub REST API ({@code /repos/{owner}/{repo}/contents/{path}}) for
     * directory enumeration.
     * </p>
     *
     * @param path the directory path (unused for this implementation)
     * @return a single-element list containing a guidance message
     */
    @Override
    public List<String> listDirectory(String path) {
        return List.of("GitHub directory listing requires the GitHub REST API — use searchText instead");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Full-text search is not supported via the raw content API.
     * Returns a guidance message directing callers to use the GitHub Code Search API.
     * </p>
     *
     * @param query   the search query (not executed in this implementation)
     * @param baseDir the base directory (not used in this implementation)
     * @return a single-element list containing a GitHub Search API guidance message
     */
    @Override
    public List<String> searchText(String query, String baseDir) {
        return List.of("GitHub code search: use GitHub Search API with query: " + query
                + " repo:" + props.getCodebase().getGithubRepo());
    }
}
