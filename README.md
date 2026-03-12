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

### 1) Create a .env file from example

### 2) Start the service

```bash
docker compose up -d --build
```

- The API listens on `http://localhost:8080`
- Reports are written to `research_reports/`

---

## How it works
Main modules:
- `src/main/kotlin/com/core/deepsearch/api/` → Ktor server, HTTP/WS routes
- `src/main/kotlin/com/core/deepsearch/` → orchestration (`DeepResearchAgent`, `Main.kt`)
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

## Browser extension (Chrome side panel)

An optional Chrome MV3 side panel extension lets you start research, monitor progress in real time, and browse saved reports directly from the browser. It connects to the local API.

### Install
- Start the backend first: `docker compose up -d --build`
- In Chrome: open `chrome://extensions`, enable Developer mode, click “Load unpacked”, and select the `extension/` folder
- Click the extension icon to open the side panel

### Usage
- The top status shows backend connectivity (`/api/health`)
- Pick or create an Agent (name, model, optional params)
- Enter a topic and click “Start research”
- Progress updates stream live; when finished, you get a Markdown preview and can view past reports per agent

### Connectivity & permissions
- API base: `http://127.0.0.1:8080`
- WebSocket: `ws://127.0.0.1:8080/api/research/{id}/stream`
- Endpoints used: `/api/health`, `/api/agents` (GET/POST/PUT/DELETE), `/api/research` (POST), `/api/agents/{id}/reports` (GET), `/api/reports/{id}` (GET)
- Extension permissions: `storage`, `sidePanel`; host permissions for `http://localhost:8080/*`
