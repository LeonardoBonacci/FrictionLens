# FrictionLens

A lightweight, privacy-preserving system that helps organizations understand where and why work gets stuck—without tracking individuals.

---

## Overview

FrictionLens is a voluntary “stuck signal” platform where employees can quickly report when their work is blocked. Each report captures the *type of role experiencing friction* (not identity), the context, and a short description of the blocker.

The system aggregates these signals across the organization to reveal recurring bottlenecks, delays, and process inefficiencies. Admins can explore this data through dashboards or a natural-language interface that lets them ask questions like:

- “Why are backend engineers getting stuck this week?”
- “What’s slowing down onboarding?”
- “Where are approval bottlenecks occurring?”

The goal is not monitoring people, but mapping structural friction in how work flows through the organization.

---

## Core Principles

- No personal identifiers (no names in free text)
- Only job titles, teams, and system-level context
- Fully voluntary input (no passive tracking)
- Emphasis on aggregation and abstraction, not attribution

---

## Architecture

### Backend
- Spring Boot (Java)
- REST API for ingestion and querying
- Sanitization and normalization pipeline for incoming reports

### Data Layer
- PostgreSQL
- pgvector extension for semantic search
- Hybrid schema:
  - Structured fields (job_title, timestamp, severity, category)
  - Unstructured fields (blocker text + embeddings)

### LLM Layer (Local-first)
- Ollama (Llama 3 / Mistral / Qwen)
- Responsibilities:
  - Sanitizing free text (removing names, normalizing references)
  - Generating embeddings
  - Interpreting natural language queries
  - Summarizing clustered results

### Frontend
- React (Vite)
- TanStack Query for server state management
- Minimal UI (no Redux for MVP)

---

## Key Features

- Semantic clustering of blocker reports
- Natural-language querying over aggregated friction data
- Time-based trend analysis by role or team
- Explainable summaries grounded in retrieved data (no hallucinated reasoning)

---

## Core Idea

FrictionLens turns subjective “I got stuck” moments into a structured map of organizational bottlenecks—without ever identifying individuals.