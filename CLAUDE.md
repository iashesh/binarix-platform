
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

Requires Java 21+ and Maven 3.9+.

```bash
# Build all modules
mvn clean install -DskipTests

# Build with tests
mvn clean install

# Run the RCA agent as a REST API (from rca-agent directory)
cd agent-matrix/rca-agent && mvn spring-boot:run

# Run a single test class
mvn test -pl agent-matrix/rca-agent -Dtest=ClassName
```

`ANTHROPIC_API_KEY` must be set before running.

## Module Structure

This is a Maven multi-module monorepo split into framework and applications:

```
agent-core/
  agent-core-api/     # Pure interfaces, annotations, records — no Spring
  agent-core-impl/    # Spring Boot auto-configuration, runtime, Anthropic SDK integration
agent-matrix/
  rca-agent/          # Root Cause Analysis application (REST + CLI entrypoints)
```

`agent-core` is a reusable framework; `agent-matrix` contains domain-specific applications built on it. New agent systems go under `agent-matrix/`.

## Architecture

### Agent Execution

`AbstractAgent<I,O>` (in `agent-core-impl`) is the base class for all agents. When `execute(input, context)` is called:

1. Resolves tools from `ToolRegistry` by tag
2. Builds system + user message
3. Delegates to `AnthropicGateway.runToolLoop()`, which drives a conversation loop with Claude until no more tool calls are requested
4. Returns parsed `AgentOutput<O>`

### Tool System

Tools are Spring beans annotated with `@ToolDefinition`. Public fields become JSON Schema properties sent to Claude. The tool itself implements `Supplier<String>` — when Claude calls the tool, its public fields are set via reflection, then `get()` is called.

```java
@ToolDefinition(name = "read_file", tags = {"code-analysis"})
public class ReadFileTool implements AgentTool, Supplier<String> {
    @JsonPropertyDescription("Absolute path to the file")
    public String filePath = "";

    public String get() { /* reads file, returns content */ }
}
```

`ToolRegistry` auto-discovers all `@ToolDefinition` beans and indexes them by name and tag. Agents filter tools by tag to restrict which tools they can use.

### Orchestration

`OrchestratorService` runs agent pipelines. Two strategies:
- **Parallel** (default): uses `Executors.newVirtualThreadPerTaskExecutor()` + `CompletableFuture.allOf()`
- **Sequential**: each agent's context feeds the next

`AgentContext` is an immutable record holding a findings map shared across agents in a pipeline.

### RCA Agent Pipeline (2-phase)

`RcaOrchestratorService` orchestrates:
- **Phase 1 (parallel)**: `LogAnalystAgent`, `CodeExplorerAgent`, `ContextEnricherAgent` run concurrently
- **Phase 2 (sequential)**: `RcaSynthesizerAgent` consumes all Phase 1 findings to produce `RcaReport`

REST endpoint: `POST /api/v1/analyze` → `RcaController` → `RcaOrchestratorService`

## Key Classes

| Class | Module | Purpose |
|---|---|---|
| `AnthropicGateway` | impl | Single Anthropic SDK integration point; drives tool-call loop |
| `ToolRegistry` | impl | Discovers and indexes `@ToolDefinition` beans |
| `OrchestratorService` | impl | Runs `AgentPipeline` via parallel or sequential strategy |
| `AgentCoreAutoConfiguration` | impl | Spring Boot auto-config entry point |
| `AbstractAgent<I,O>` | impl | Base agent with retry, events, tool resolution |
| `AgentContext` | api | Immutable record; use `context.withFinding(key, value)` to add findings |
| `RcaOrchestratorService` | rca-agent | 2-phase RCA pipeline |

## Configuration Reference

Set in `application.properties` or as environment variables:

| Property | Default | Purpose |
|---|---|---|
| `agent.core.anthropic.api-key` | — | Claude API key (required) |
| `agent.core.anthropic.default-model` | `claude-sonnet-4-5` | Model for all agents |
| `agent.core.anthropic.default-max-tokens` | `8192` | Token budget per call |
| `agent.core.orchestration.strategy` | `parallel` | `parallel` or `sequential` |
| `agent.core.orchestration.agent-timeout` | `5m` | Max duration per agent |
| `agent.core.retry.max-attempts` | `3` | Retry attempts on failure |
| `agent.core.retry.base-delay-ms` | `2000` | Exponential backoff base |
| `agent.core.security.log-base-path` | `/var/log` | Path jail for log reads |
| `agent.core.security.code-base-path` | `/` | Path jail for code reads |
| `rca.codebase.type` | `local` | `local` or `github` |
| `rca.codebase.local-base-path` | `.` | Local codebase root |
| `rca.log.base-path` | `/var/log` | Log file base directory |
| `rca.log.max-lines` | `5000` | Max lines read from logs |

## Adding a New Agent System

1. Create a new Maven module under `agent-matrix/` with a dependency on `agent-core-impl`
2. Define tool classes with `@ToolDefinition` and appropriate tags
3. Extend `AbstractAgent<InputType, OutputType>` for each agent role, filtering tools by tag
4. Wire orchestration in a service extending or using `OrchestratorService`
5. Expose via `@RestController` or `CommandLineRunner`

Spring Boot auto-configuration in `agent-core-impl` registers all framework beans automatically — no manual bean registration needed.
