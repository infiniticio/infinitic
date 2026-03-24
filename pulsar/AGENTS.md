<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# pulsar

## Purpose
Contains Docker Compose configuration and supporting data for running a local Apache Pulsar instance during development and integration testing. This directory is not a Gradle module — it is a developer convenience for standing up the Pulsar broker locally.

## Key Files

| File | Description |
|------|-------------|
| `docker-compose.yml` | Docker Compose file that starts a standalone Pulsar broker |
| `README.md` | Instructions for starting and stopping the local Pulsar instance |
| `data/` | Persistent data directory mounted into the Pulsar container |

## For AI Agents

### Working In This Directory
- Start Pulsar: `cd pulsar && docker compose up -d`
- Stop Pulsar: `cd pulsar && docker compose down`
- Integration tests in `infinitic-tests` and `infinitic-transport-pulsar` require a running Pulsar instance (or use TestContainers automatically)
- Do not commit files from the `data/` directory

### Testing Requirements
- No tests in this directory
- For integration tests, either use `docker compose up` here or let TestContainers manage Pulsar automatically

### Common Patterns
- Standalone Pulsar mode (single broker, no ZooKeeper) is sufficient for development

## Dependencies

### External
- Docker + Docker Compose
- Apache Pulsar Docker image

<!-- MANUAL: -->
