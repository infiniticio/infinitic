<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# buildSrc

## Purpose
Contains the Gradle build logic shared across all modules: version constants, dependency catalog, and plugin configuration helpers. This is a standard Gradle `buildSrc` project that is compiled before the main build and its classes are available on the buildscript classpath of all subprojects.

## Key Files

| File | Description |
|------|-------------|
| `src/main/kotlin/Ci.kt` | Version constants — `BASE` is the current version string (e.g. `0.18.2`); set `RELEASE=true` env var to strip `-SNAPSHOT` suffix |
| `src/main/kotlin/Libs.kt` | Centralized dependency catalog as nested Kotlin objects (Kotlin, Pulsar, Avro4k, Coroutines, Kotest, Mockk, etc.) |
| `src/main/kotlin/Plugins.kt` | Plugin ID constants and plugin application helpers for Kotlin, Serialization, Spotless, TestLogger |
| `build.gradle.kts` | buildSrc own build file — declares its dependencies (Kotlin Gradle plugin, Spotless plugin) |

## For AI Agents

### Working In This Directory
- To bump the project version, update `Ci.BASE` in `Ci.kt`
- To add or update a dependency, add it to the appropriate nested object in `Libs.kt`
- All dependency versions are defined here — never hardcode versions in module `build.gradle.kts` files
- Plugin IDs are centralized in `Plugins.kt`

### Testing Requirements
- No tests in this module — it is build logic only
- Changes here affect every module; rebuild with `./gradlew build` to validate

### Common Patterns
- Dependencies referenced as `Libs.Kotlin.stdlib`, `Libs.Pulsar.client`, etc.
- Plugin IDs referenced as `Plugins.Kotlin.jvm`, etc.

## Dependencies

### External
- Kotlin Gradle plugin — for compiling Kotlin in subprojects
- Spotless Gradle plugin — for ktfmt code formatting enforcement

<!-- MANUAL: -->
