<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-utils

## Purpose
Shared utility code used across all Infinitic modules. Provides configuration loading helpers, logging utilities, auto-close resource management, and property delegation utilities. This module has no domain logic — it is purely infrastructure.

## Key Files

| File | Description |
|------|-------------|
| `src/main/kotlin/io/infinitic/config/loaders.kt` | YAML/JSON configuration loading via Hoplite — used to load worker and client configs |
| `src/main/kotlin/io/infinitic/config/toString.kt` | Config-to-string utilities for debugging |
| `src/main/kotlin/io/infinitic/logger/IgnoreNullKLogger.kt` | Kotlin logging wrapper that silently ignores null messages |
| `src/main/kotlin/io/infinitic/autoclose/AutoClose.kt` | AutoCloseable resource management helpers |
| `src/main/kotlin/io/infinitic/properties/properties.kt` | Kotlin property delegation utilities |

## For AI Agents

### Working In This Directory
- All modules depend on this — changes here affect the entire codebase
- Config loaders use Hoplite with support for YAML, JSON, and `.properties` files
- Logging uses `kotlin-logging` (SLF4J facade)

### Testing Requirements
- `./gradlew :infinitic-utils:test`

### Common Patterns
- Use `loadConfigFromFile<T>()` / `loadConfigFromResource<T>()` from `loaders.kt` for config loading
- `AutoClose` pattern: register resources for cleanup when the enclosing component closes

## Dependencies

### External
- Hoplite — type-safe configuration loading from YAML/JSON
- kotlin-logging — Kotlin SLF4J logging facade
- SLF4J — logging API

<!-- MANUAL: -->
