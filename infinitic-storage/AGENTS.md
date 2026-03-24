<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-storage

## Purpose
Provides persistent key-value and key-set storage abstractions with multiple backend implementations (Redis, PostgreSQL, MySQL, in-memory). Also supports optional Caffeine caching (via `infinitic-cache`) and LZ4/zstd value compression. Used by the workflow engine and tag services to persist state.

## Key Files

| File | Description |
|------|-------------|
| `src/main/kotlin/io/infinitic/storage/keyValue/KeyValueStorage.kt` | Core key-value storage interface |
| `src/main/kotlin/io/infinitic/storage/keySet/KeySetStorage.kt` | Core key-set storage interface (string set per key) |
| `src/main/kotlin/io/infinitic/storage/keyValue/CachedKeyValueStorage.kt` | Caffeine cache decorator for key-value storage |
| `src/main/kotlin/io/infinitic/storage/keySet/CachedKeySetStorage.kt` | Caffeine cache decorator for key-set storage |
| `src/main/kotlin/io/infinitic/storage/keyValue/CompressedKeyValueStorage.kt` | Compression decorator (LZ4/zstd) for key-value storage |
| `src/main/kotlin/io/infinitic/storage/keyValue/WrappedKeyValueStorage.kt` | Combines cache + compression decorators |
| `src/main/kotlin/io/infinitic/storage/keySet/WrappedKeySetStorage.kt` | Combines cache + compression decorators for key-set |
| `src/main/kotlin/io/infinitic/storage/config/StorageConfig.kt` | Top-level storage configuration sealed class |
| `src/main/kotlin/io/infinitic/storage/config/RedisConfig.kt` | Redis connection configuration |
| `src/main/kotlin/io/infinitic/storage/config/PostgresConfig.kt` | PostgreSQL connection configuration |
| `src/main/kotlin/io/infinitic/storage/config/MySQLConfig.kt` | MySQL connection configuration |
| `src/main/kotlin/io/infinitic/storage/config/InMemoryConfig.kt` | In-memory storage configuration |
| `src/main/kotlin/io/infinitic/storage/compression/CompressionConfig.kt` | Compression algorithm configuration (lz4, zstd, none) |
| `src/main/kotlin/io/infinitic/storage/databases/redis/RedisKeyValueStorage.kt` | Redis implementation of key-value storage |
| `src/main/kotlin/io/infinitic/storage/databases/redis/RedisKeySetStorage.kt` | Redis implementation of key-set storage |
| `src/main/kotlin/io/infinitic/storage/databases/postgres/PostgresKeyValueStorage.kt` | PostgreSQL implementation |
| `src/main/kotlin/io/infinitic/storage/databases/mysql/MySQLKeyValueStorage.kt` | MySQL implementation |
| `src/main/kotlin/io/infinitic/storage/databases/inMemory/InMemoryKeyValueStorage.kt` | Thread-safe in-memory implementation |
| `src/main/kotlin/io/infinitic/storage/StorageException.kt` | Base exception for storage errors |
| `src/main/kotlin/io/infinitic/storage/Flushable.kt` | Interface for flushing storage (used in tests) |
| `src/main/kotlin/io/infinitic/storage/data/Bytes.kt` | Byte array value type |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `src/main/kotlin/io/infinitic/storage/config/` | Per-backend configuration classes |
| `src/main/kotlin/io/infinitic/storage/databases/` | Backend implementations (redis, postgres, mysql, inMemory) |
| `src/main/kotlin/io/infinitic/storage/keyValue/` | Key-value interface, cache, compression decorators |
| `src/main/kotlin/io/infinitic/storage/keySet/` | Key-set interface and decorators |
| `src/main/kotlin/io/infinitic/storage/compression/` | Compression configuration |

## For AI Agents

### Working In This Directory
- All storage backends implement both `KeyValueStorage` and `KeySetStorage`
- Caching and compression are applied as decorators — the storage config determines which wrapping is applied
- Integration tests require Docker (TestContainers) for MySQL and PostgreSQL backends

### Testing Requirements
- `./gradlew :infinitic-storage:test`
- Integration tests spin up real databases via TestContainers — requires Docker

### Common Patterns
- `WrappedKeyValueStorage` is the production entry point: wraps any backend with optional cache + compression
- Storage config is loaded from YAML via Hoplite; `StorageConfig` is a sealed class per backend

## Dependencies

### Internal
- `infinitic-cache` — Caffeine cache decorators

### External
- Lettuce — Redis client
- HikariCP + JDBC drivers — PostgreSQL and MySQL
- Apache Commons Compress (LZ4, zstd) — compression
- TestContainers — integration test database provisioning

<!-- MANUAL: -->
