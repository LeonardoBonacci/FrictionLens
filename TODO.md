# FrictionLens – Implementation TODO

## Phase 1: Project Setup

- [x] Initialize Spring Boot project (Java 25, Maven)
- [x] Initialize React frontend (Vite + TypeScript)
- [x] Set up PostgreSQL with pgvector extension (Docker Compose)
- [x] Set up Ollama locally with a supported model (Llama 3)
- [x] Define project structure and module boundaries

## Phase 2: Data Layer

- [x] Design database schema
  - [x] `friction_reports` table: id, job_title, team, category, severity, blocker_text, embedding (vector), created_at
  - [x] Indexes on job_title, team, category, severity, created_at
  - [x] pgvector index on embedding column
- [x] Create JPA entities and repositories (Hibernate auto DDL)

## Phase 3: Backend – Ingestion API

- [x] `POST /api/reports` – submit a friction report
  - [x] Request validation (job_title, team, category, severity, blocker_text)
  - [x] Sanitization pipeline: send blocker_text to Ollama to strip personal identifiers (names, emails)
  - [x] Generate embedding for sanitized text via Ollama
  - [x] Persist sanitized report with embedding to PostgreSQL
- [x] `GET /api/reports` – list reports (paginated, filterable by role/team/category/date range)
- [x] Unit and integration tests for ingestion flow

## Phase 4: Backend – Query & Analysis API

- [x] `POST /api/query` – natural-language query endpoint
  - [x] Send user question to Ollama for intent interpretation
  - [x] Generate query embedding
  - [x] Perform hybrid search: pgvector semantic similarity + structured filters
  - [x] Cluster matching reports
  - [x] Summarize clustered results via Ollama (grounded, no hallucination)
  - [x] Return explainable summary + supporting report snippets
- [x] `GET /api/trends` – time-based trend analysis (by role, team, category)
- [x] Tests for query and trend endpoints

## Phase 5: LLM Integration (Ollama)

- [x] Create Ollama client service (REST calls to local Ollama instance)
- [x] Implement text sanitization prompt (remove names, normalize references)
- [x] Implement embedding generation
- [x] Implement natural-language query interpretation prompt
- [x] Implement summarization prompt (grounded in retrieved data)
- [x] Add fallback/error handling for Ollama unavailability

## Phase 6: Frontend – Core UI

- [x] Set up TanStack Query for server state management
- [ ] Report submission form
  - [ ] Fields: job title (dropdown/text), team, category, severity, blocker description
  - [ ] Client-side validation
  - [ ] Submit to `POST /api/reports`
- [ ] Reports list view (paginated, filterable)
- [ ] Natural-language query input
  - [ ] Text input for freeform questions
  - [ ] Display summarized results + supporting snippets

## Phase 7: Frontend – Dashboard

- [ ] Friction trends over time (line/bar charts)
- [ ] Breakdown by role, team, category
- [ ] Top recurring blockers (semantic clusters)
- [ ] Severity distribution

## Phase 8: Privacy & Security

- [ ] Validate sanitization pipeline strips all personal identifiers
- [ ] Ensure no PII in stored reports or logs
- [ ] Input validation and XSS prevention on frontend
- [ ] Rate limiting on report submission
- [x] CORS configuration

## Phase 9: DevOps & Deployment

- [x] Docker Compose for full stack (Spring Boot, PostgreSQL + pgvector, Ollama, React)
- [x] Environment configuration (profiles, secrets)
- [x] Health check endpoints
- [ ] README updates with setup and run instructions

## Phase 10: Polish & Testing

- [ ] End-to-end testing (submission → query → summary)
- [ ] Load testing for ingestion and query paths
- [ ] UI/UX refinements
- [ ] Error states and empty states in frontend
