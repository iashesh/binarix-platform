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

### 2. Build
```bash
mvn clean install -DskipTests
```

### 3. Run — REST API mode
```bash
cd agent-matrix/rca-agent
mvn spring-boot:run
```

Call the API:
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
In `agent-matrix/rca-agent/src/main/resources/application.properties`:
```properties
rca.cli.enabled=true
rca.cli.log-location=test-data/sample-app.log
rca.cli.codebase-type=local
rca.cli.codebase-location=.
agent.core.security.log-base-path=.
```
Then `mvn spring-boot:run`.

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
mkdir -p agent-matrix/my-agent/src/main/java/com/agentplatform/myagent

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
