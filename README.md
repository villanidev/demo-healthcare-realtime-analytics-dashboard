# demo-healthcare-realtime-analytics-dashboard

Lightweight healthcare real-time analytics dashboard POC following Kappa Architecture.

This repository contains a **small Spring Boot monolith** that demonstrates:

- Java 21 + Spring Boot 3 (Servlet stack, no WebFlux)
- PostgreSQL with logical schemas: `app` (OLTP) and `analytics` (derived facts and aggregates)
- A **Kappa-style event stream** based on the Outbox pattern, with a single ingestion path
- Server-Sent Events (SSE) for near real-time dashboard updates
- Minimal, privacy-aware analytics for healthcare appointment funnels and profile completion

The focus is on **open source, low operational overhead, and clear naming** from data architecture and streaming (Kappa) perspectives.
