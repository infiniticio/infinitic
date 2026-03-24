<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-cache

## Purpose
Provides a Caffeine-based caching layer for key-value and key-set storage abstractions. Used by `infinitic-storage` to wrap database-backed storage implementations with an in-process cache, reducing latency for frequently accessed state (workflow state, tag mappings).

## Key Files

| File | Description |
|------|-------------|
| `src/main/kotlin/io/infinitic/cache/config/CacheConfig.kt` | Top-level cache configuration sealed class |
| `src/main/kotlin/io/infinitic/cache/config/CacheConfigInterface.kt` | Interface for cache configuration |
| `src/main/kotlin/io/infinitic/cache/config/CaffeineCacheConfig.kt` | Caffeine-specific config (maximumSize, expireAfterAccess, etc.) |
| `src/main/kotlin/io/infinitic/cache/keyValue/CachedKeyValue.kt` | Cache abstraction for key-value stores |
| `src/main/kotlin/io/infinitic/cache/keySet/CachedKeySet.kt` | Cache abstraction for key-set stores |
| `src/main/kotlin/io/infinitic/cache/caches/caffeine/CaffeineCachedKeyValue.kt` | Caffeine implementation of key-value cache |
| `src/main/kotlin/io/infinitic/cache/caches/caffeine/CaffeineCachedKeySet.kt` | Caffeine implementation of key-set cache |
| `src/main/kotlin/io/infinitic/cache/caches/caffeine/setup.kt` | Caffeine builder setup utilities |
| `src/main/kotlin/io/infinitic/cache/data/Bytes.kt` | Byte array wrapper type |
| `src/main/kotlin/io/infinitic/cache/Flushable.kt` | Interface for flushing cache state (used in tests) |

## For AI Agents

### Working In This Directory
- Cache is purely in-process — no external service required
- Used as a decorator around `KeyValueStorage` and `KeySetStorage` implementations
- Configuration is loaded from YAML worker config via Hoplite

### Testing Requirements
- `./gradlew :infinitic-cache:test`
- Tests verify cache eviction and invalidation behavior

### Common Patterns
- `CaffeineCacheConfig` wraps a Caffeine builder with sensible defaults
- `Flushable.flush()` used in integration tests to reset cache between test cases

## Dependencies

### Internal
- None (standalone module, depended upon by `infinitic-storage`)

### External
- Caffeine — high-performance Java caching library

<!-- MANUAL: -->
