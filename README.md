## Deep Research Agent

Deep web research agent in Kotlin (Ktor). It plans queries, searches the web, extracts and cross‑checks information, and generates a Markdown report. It exposes an HTTP API and a WebSocket to track progress in real time.

### Key features
- **Ktor HTTP API**: endpoints under `/api` to start and track research
- **Controlled iterations**: planning, Brave search, HTML content extraction, source evaluation, claim extraction, cross‑checking, synthesis
- **OpenAI**: plan generation, extraction, synthesis, and report via `OpenAIClient`
- **Markdown reports**: saved to `research_reports/`
- **WebSocket**: real‑time progress streaming

---

## Quickstart (Docker / Docker Compose)

### Prerequisites
- Docker and Docker Compose installed
- Valid API keys:
  - **OPENAI_API_KEY** (OpenAI)
  - **BRAVE_API_KEY** (Brave Search API)

### 1) Create a .env file (recommended)
Place it at the project root:

```bash
OPENAI_API_KEY=sk-xxx
BRAVE_API_KEY=brv-xxx

# Optional (defaults in parentheses)
OPENAI_MODEL=gpt-4o              # (gpt-4o)
MAX_ITERATIONS=3                 # (3)
MAX_SOURCES_PER_QUERY=10         # (10)
MIN_SOURCE_CREDIBILITY_SCORE=0.5 # (0.5)
# DB_PATH=deep_research.db       # SQLite path inside the container
```

You may also export these variables in your shell instead of using `.env`.

### 2) Start the service

```bash
docker compose up -d --build
```

- The API listens on `http://localhost:8080`
- Reports are written to `research_reports/` (bind mounted)

### 3) Health and logs

```bash
# API health
curl http://localhost:8080/api/health

# Application logs
docker logs -f deep-research-agent
```

Note: The Dockerfile defines an internal healthcheck. The public health endpoint is `/api/health`.

---

## API overview
Base path: `http://localhost:8080/api`

### Health
- GET `/api/health` → `{ status: "ok", timestamp: <ms> }`

### Start a research job
- POST `/api/research`

Example request:

```bash
curl -X POST http://localhost:8080/api/research \
  -H 'Content-Type: application/json' \
  -d '{
    "topic": "Impact of LLMs on developer productivity",
    "maxIterations": 3,
    "model": "gpt-4o"
  }'
```

Example response:

```json
{ "id": "<researchId>", "status": "started", "message": "Research started successfully" }
```

### Check research status
- GET `/api/research/{id}/status`

```bash
curl http://localhost:8080/api/research/<researchId>/status
```

Example response:

```json
{
  "id": "<researchId>",
  "status": "RUNNING",
  "progress": {
    "currentIteration": 1,
    "totalIterations": 3,
    "sourcesFound": 5,
    "claimsExtracted": 8,
    "currentPhase": "SYNTHESIS"
  }
}
```

### Real‑time stream (WebSocket)
- WS `/api/research/{id}/stream`

Example with `websocat`:

```bash
websocat ws://localhost:8080/api/research/<researchId>/stream
```

### Saved reports
- GET `/api/reports` → list
- GET `/api/reports/{id}` → specific report

### Agent management
- POST `/api/agents` → create
- GET `/api/agents` → list
- GET `/api/agents/{id}` → read
- PUT `/api/agents/{id}` → update
- DELETE `/api/agents/{id}` → delete
- GET `/api/agents/{id}/reports` → agent reports

Create agent example:

```bash
curl -X POST http://localhost:8080/api/agents \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "default",
    "model": "gpt-4o",
    "maxIterations": 3
  }'
```

---

## Configuration (environment variables)
- **OPENAI_API_KEY**: required
- **BRAVE_API_KEY**: required
- **OPENAI_MODEL**: OpenAI model (default: `gpt-4o`)
- **MAX_ITERATIONS**: research iterations (default: `3`, API bounds [1..5])
- **MAX_SOURCES_PER_QUERY**: max sources per query (default: `10`)
- **MIN_SOURCE_CREDIBILITY_SCORE**: source credibility threshold (default: `0.5`)
- **DB_PATH**: SQLite file path inside the container (default: `deep_research.db`)

The service first loads `.env`, then overrides with system environment variables.

Default volume (Compose):
- `./research_reports:/app/research_reports` (persisted on host)

If you want to persist SQLite outside the container, mount a volume for `DB_PATH`.

---

## How it works (high level)
Main modules:
- `src/main/kotlin/com/jetbrains/deepsearch/api/` → Ktor server, HTTP/WS routes
- `src/main/kotlin/com/jetbrains/deepsearch/` → orchestration (`DeepResearchAgent`, `Main.kt`)
- `clients/` → OpenAI, Brave, and HTML extraction
- `core/` → planning and iteration loop (`ResearchPlanner`, `IterativeResearcher`)
- `eval/` → source evaluation, claim extraction, cross‑checking
- `report/` → report generation and export to Markdown

Simplified flow:
1. API receives a research request (topic, parameters)
2. `DeepResearchAgent` plans iterations via `ResearchPlanner`
3. `SearchClient` queries Brave; `HtmlContentExtractor` fetches main content
4. `SourceEvaluator` filters/weights sources
5. `ClaimExtractor` extracts claims via OpenAI
6. `CrossChecker` compares claims across sources
7. `IterativeResearcher` iterates; `OpenAIClient` synthesizes
8. `ReportGenerator` produces a Markdown report under `research_reports/`
9. API exposes status/results and streams updates via WebSocket

---

## Local development (optional)

### Build & tests
```bash
./gradlew build
./gradlew test
```

### Run without Docker
```bash
export OPENAI_API_KEY=sk-xxx
export BRAVE_API_KEY=brv-xxx
./gradlew run
```

The app listens on `http://localhost:8080`.

---

## License
This project is provided as is. Check the repository for a license file if present.


