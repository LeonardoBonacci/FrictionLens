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

- [ ] `POST /api/reports` – submit a friction report
  - [ ] Request validation (job_title, team, category, severity, blocker_text)
  - [ ] Sanitization pipeline: send blocker_text to Ollama to strip personal identifiers (names, emails)
  - [ ] Generate embedding for sanitized text via Ollama
  - [ ] Persist sanitized report with embedding to PostgreSQL
- [ ] `GET /api/reports` – list reports (paginated, filterable by role/team/category/date range)
- [ ] Unit and integration tests for ingestion flow

## Phase 4: Backend – Query & Analysis API

- [ ] `POST /api/query` – natural-language query endpoint
  - [ ] Send user question to Ollama for intent interpretation
  - [ ] Generate query embedding
  - [ ] Perform hybrid search: pgvector semantic similarity + structured filters
  - [ ] Cluster matching reports
  - [ ] Summarize clustered results via Ollama (grounded, no hallucination)
  - [ ] Return explainable summary + supporting report snippets
- [ ] `GET /api/trends` – time-based trend analysis (by role, team, category)
- [ ] Tests for query and trend endpoints

## Phase 5: LLM Integration (Ollama)

- [ ] Create Ollama client service (REST calls to local Ollama instance)
- [ ] Implement text sanitization prompt (remove names, normalize references)
- [ ] Implement embedding generation
- [ ] Implement natural-language query interpretation prompt
- [ ] Implement summarization prompt (grounded in retrieved data)
- [ ] Add fallback/error handling for Ollama unavailability

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
