# Binarix Platform

> **Extensible multi-agent AI platform for Java & Spring Boot**
> Built by [binarray.com](https://binarray.com) · Powered by [Anthropic Claude](https://anthropic.com)

```
binarix.binarray.com  |  github.com/binarray/binarix-platform
```

---

## What is Binarix?

Binarix is a downloadable, extensible platform for building **multi-agent AI systems** in Java.
Drop in `agent-core` as a Maven dependency, write your tools and prompts, and you have a
production-ready agentic system — with parallel orchestration, retry, tracing, and Claude
integration included.

Think of it as **Spring Boot for AI agents**.

---

## Project Structure

```
binarix-platform/
├── agent-core/                   ← The reusable framework (add as Maven dep)
│   ├── agent-core-api/           ← Pure interfaces & contracts (no Spring)
│   └── agent-core-impl/          ← Spring Boot auto-configuration & runtime
│
└── agent-matrix/                 ← All domain agent systems live here
    └── rca-agent/                ← Root Cause Analysis agent system
        ├── tools/                ← ReadLogFile, GrepPattern, SearchCodebase ...
        ├── agent/                ← LogAnalyst, CodeExplorer, RcaSynthesizer ...
        └── orchestration/        ← RcaOrchestratorService (parallel → synthesize)
```

---

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
- Anthropic API key (`sk-ant-...`)

### 1. Set your API key
```bash
export ANTHROPIC_API_KEY=sk-ant-YOUR_KEY_HERE
```

### 2. Load Maven modules (IntelliJ IDEA)
Open the project in IntelliJ IDEA, then reload Maven so IDEA generates the module structure:

**View → Tool Windows → Maven → Reload All Maven Projects** (circular arrow icon)

> IDEA uses external storage for module files (`.iml`, `modules.xml`), so these won't appear
> in the project tree — Maven is the authoritative source for module structure and dependencies.
> Committing `.iml` files would create a second source of truth that can drift.

### 3. Build
```bash
mvn clean install -DskipTests
```

### 3. Run — REST API mode
```bash
cd agent-matrix/rca-agent
mvn spring-boot:run
```

**Endpoint:** `POST http://localhost:8080/api/v1/analyze`

The API supports two modes:

#### Mode A — inline error text (no log file needed)

Supply the error message or stack trace directly in `logErrorText`. The log file is skipped entirely — the agent goes straight to codebase + git analysis.

```bash
curl -X POST http://localhost:8080/api/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "logLocation": "",
    "logErrorText": "Invalid Session. Please login again",
    "codebaseType": "local",
    "codebaseLocation": "IDEA/trajor",
    "maxLogLines": 5000
  }'
```

<details>
<summary>Sample response</summary>

```json
{
  "correlationId": "5c6779bb-b92e-47bc-9097-26aed39c74ab",
  "status": "SUCCESS",
  "report": {
    "correlationId": "d4637247-f6e3-4980-8b7c-02096366742e",
    "rootCauses": [
      {
        "description": "JWT token validation failure caught by AuthenticationFilter. The filter catches any JwtException thrown during token parsing (line 48) and returns a generic 'Invalid Session' message to the client. Common causes include expired tokens (1-hour TTL configured), invalid signatures, or malformed tokens.",
        "confidenceScore": 1.0,
        "evidence": [
          "LOG: Invalid Session. Please login again",
          "AuthenticationFilter.java:65 - response.getWriter().write(\"Invalid Session. Please login again.\")",
          "AuthenticationFilter.java:63-66 - catch (JwtException jwtException) block",
          "TokenManager.java:78 - parseSignedClaims(token) throws JwtException"
        ],
        "category": "auth",
        "affectedFile": "trajor-core/src/main/java/com/trajor/config/AuthenticationFilter.java",
        "affectedLine": 65
      }
    ],
    "executiveSummary": "The application returns 'Invalid Session. Please login again' when JWT token validation fails. The error is caught and handled in AuthenticationFilter.java at line 65, triggered by any JwtException during token parsing (expired, invalid signature, malformed). The logs show only the user-facing error message; the actual JWT exception details are suppressed by the catch block.",
    "remediationSteps": [
      "Add debug logging in AuthenticationFilter catch block (line 63-66) to log the actual JwtException type and message before returning generic error",
      "Review application logs with debug level enabled to identify specific JWT failure reason (expired vs invalid signature vs malformed)",
      "If tokens are expiring too quickly, adjust jwt.expiration property (currently 3600000ms = 1 hour) in application.properties",
      "Implement token refresh mechanism if not already present to handle expiration gracefully",
      "Verify JWT secret key consistency across all application instances if distributed deployment"
    ],
    "affectedComponent": "AuthenticationFilter.doFilterInternal() -> TokenManager.extractAllClaims()",
    "severity": "MEDIUM",
    "generatedAt": "2026-06-18T20:59:36.014394Z",
    "rawAnalysis": "..."
  },
  "errorMessage": null,
  "durationMs": 178243
}
```
</details>

#### Mode B — log file on disk

Point `logLocation` at a log file (relative to `rca.log.base-path`). The agent reads the file, extracts errors, then analyses the codebase.

```bash
curl -X POST http://localhost:8080/api/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "logLocation": "test-data/sample-app.log",
    "codebaseType": "local",
    "codebaseLocation": ".",
    "maxLogLines": 1000,
    "gitBranch": "main"
  }'
```

### 4. Run — CLI mode

CLI mode runs a single analysis on startup, prints the report to stdout, and exits (`0` = success, `1` = error). No REST server stays running. Useful for scripts, CI pipelines, or a quick one-shot check.

Two input modes — same as the REST API:

**Inline error text** (no log file needed — paste the error or stack trace directly):

```bash
cd agent-matrix/rca-agent
mvn spring-boot:run -Dspring-boot.run.arguments="\
  --rca.cli.enabled=true \
  --rca.cli.log-error-text='Invalid Session. Please login again' \
  --rca.cli.codebase-type=local \
  --rca.cli.codebase-location=/absolute/path/to/your/project"
```

**Log file on disk:**

```bash
cd agent-matrix/rca-agent
mvn spring-boot:run -Dspring-boot.run.arguments="\
  --rca.cli.enabled=true \
  --rca.cli.log-location=/absolute/path/to/app.log \
  --rca.cli.codebase-type=local \
  --rca.cli.codebase-location=/absolute/path/to/your/project \
  --agent.core.security.log-base-path=/absolute/path/to/logs"
```

Or set them permanently in `application.properties` if you always run in CLI mode:

```properties
rca.cli.enabled=true
# Pick one of the two input modes:
rca.cli.log-error-text=Invalid Session. Please login again
# rca.cli.log-location=/var/log/myapp/app.log
rca.cli.codebase-type=local
rca.cli.codebase-location=/home/user/projects/myapp
agent.core.security.log-base-path=/var/log/myapp
```

The report is printed to stdout in a readable format:

```
================================================================================
ROOT CAUSE ANALYSIS REPORT
================================================================================
Correlation ID : 5c6779bb-b92e-47bc-9097-26aed39c74ab
Generated At   : 2026-06-18T20:59:36Z
Severity       : MEDIUM
Component      : AuthenticationFilter.doFilterInternal()
================================================================================
The application returns 'Invalid Session...' when JWT token validation fails...
================================================================================
```

> **Note:** `agent.core.security.log-base-path` must be a parent of your log file path — it acts as a security jail preventing the agent from reading files outside that directory.

---

## Configuration

| Property | Default | Description |
|---|---|---|
| `ANTHROPIC_API_KEY` env | — | **Required.** Your Anthropic API key |
| `agent.core.anthropic.default-model` | `claude-sonnet-4-5` | Claude model |
| `agent.core.orchestration.strategy` | `parallel` | `parallel` or `sequential` |
| `rca.codebase.type` | `local` | `local` or `github` |
| `rca.codebase.local-base-path` | `.` | Your codebase root |
| `rca.log.base-path` | `/var/log` | Log file base path |

---

## Adding a New Agent System

```bash
# 1. Create a new module under agent-matrix/
mkdir -p agent-matrix/my-agent/src/main/java/com/binarray/binarix/myagent

# 2. Add pom.xml (depend on agent-core-impl)
# 3. Write @ToolDefinition tools
# 4. Write agents extending AbstractAgent<I, O>
# 5. Wire RcaOrchestratorService pattern
# Done — all retry, tracing, Claude API wiring inherited
```

**Time to first working agent: ~4 hours.**

---

## Built With

- **Java 21** — Virtual threads for parallel agent execution
- **Spring Boot 3.3** — Auto-configuration, Actuator, DI
- **Anthropic Java SDK 2.1** — Claude API, full tool-call loop
- **Jackson** — Tool input/output serialisation

---

*Binarix Platform is open for extension. Fork it, build on it, ship agents.*
*© binarray.com*
