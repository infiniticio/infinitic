<!-- Parent: ../AGENTS.md -->
<!-- Generated: 2026-03-24 | Updated: 2026-03-24 -->

# infinitic-dashboard

## Purpose
Web-based monitoring dashboard for Infinitic built with Kweb (server-side reactive UI framework). Provides real-time visibility into running workflows, services, tasks, and infrastructure health. Uses Tailwind CSS for styling. Deployed as part of the worker or as a standalone service.

## Key Files

| File | Description |
|------|-------------|
| `src/main/kotlin/io/infinitic/dashboard/InfiniticDashboard.kt` | Entry point — starts the Kweb server and registers routes |
| `src/main/kotlin/io/infinitic/dashboard/AppPanel.kt` | Root application panel component |
| `src/main/kotlin/io/infinitic/dashboard/AppState.kt` | Global reactive application state |
| `src/main/kotlin/io/infinitic/dashboard/Panel.kt` | Base panel interface |
| `src/main/kotlin/io/infinitic/dashboard/NotFound.kt` | 404 panel |
| `src/main/kotlin/io/infinitic/dashboard/config/DashboardConfig.kt` | Dashboard configuration (port, Pulsar connection) |
| `src/main/kotlin/io/infinitic/dashboard/config/DashboardConfigInterface.kt` | Dashboard config interface |
| `src/main/kotlin/io/infinitic/dashboard/config/DashboardSettingsConfig.kt` | Dashboard settings (theme, etc.) |
| `src/main/kotlin/io/infinitic/dashboard/panels/infrastructure/AllJobsPanel.kt` | Panel showing all running jobs |
| `src/main/kotlin/io/infinitic/dashboard/panels/infrastructure/AllJobsState.kt` | Reactive state for jobs panel |
| `src/main/kotlin/io/infinitic/dashboard/panels/infrastructure/AllServicesState.kt` | Reactive state for services list |
| `src/main/kotlin/io/infinitic/dashboard/panels/infrastructure/AllWorkflowsState.kt` | Reactive state for workflows list |
| `src/main/kotlin/io/infinitic/dashboard/panels/services/ServicesPanel.kt` | Services overview panel |
| `src/main/kotlin/io/infinitic/dashboard/panels/workflows/WorkflowsPanel.kt` | Workflows overview panel |
| `src/main/kotlin/io/infinitic/dashboard/panels/settings/SettingsPanel.kt` | Settings panel |
| `src/main/kotlin/io/infinitic/dashboard/menus/` | Navigation menu components |
| `src/main/kotlin/io/infinitic/dashboard/svgs/icons/` | SVG icon components |
| `src/main/kotlin/io/infinitic/dashboard/plugins/tailwind/tailwindPlugin.kt` | Kweb plugin that injects Tailwind CSS |

## Subdirectories

| Directory | Purpose |
|-----------|---------|
| `src/main/kotlin/io/infinitic/dashboard/config/` | Dashboard configuration |
| `src/main/kotlin/io/infinitic/dashboard/menus/` | Navigation menu components |
| `src/main/kotlin/io/infinitic/dashboard/modals/` | Modal dialog components |
| `src/main/kotlin/io/infinitic/dashboard/panels/` | Page panels (infrastructure, services, workflows, settings) |
| `src/main/kotlin/io/infinitic/dashboard/panels/infrastructure/` | Infrastructure monitoring panels |
| `src/main/kotlin/io/infinitic/dashboard/panels/infrastructure/jobs/` | Job stats display components |
| `src/main/kotlin/io/infinitic/dashboard/plugins/` | Kweb plugins (tailwind, images) |
| `src/main/kotlin/io/infinitic/dashboard/slideovers/` | Slide-over panel components |
| `src/main/kotlin/io/infinitic/dashboard/svgs/` | SVG icon and logo components |
| `src/main/resources/` | Static assets (CSS, images) |

## For AI Agents

### Working In This Directory
- UI is built with Kweb — server-side reactive Kotlin UI (not a JS framework)
- Tailwind CSS is pre-compiled into `src/main/resources/css/compiled.css`
- To rebuild CSS after modifying Tailwind config: `cd infinitic-dashboard && npm i && node_modules/.bin/postcss tailwind.css -o src/main/resources/css/compiled.css`
- State is managed as Kweb `ObservableValue` — UI updates reactively when state changes

### Testing Requirements
- `./gradlew :infinitic-dashboard:test`
- Limited unit tests — primarily integration tested manually

### Common Patterns
- Each panel is a Kotlin object/class with a `render()` function that returns Kweb HTML
- Reactive state: `ObservableValue<T>` — observe with `value.addListener { ... }`
- Icons are Kotlin SVG builders (type-safe, no raw HTML strings)

## Dependencies

### Internal
- `infinitic-common` — Pulsar admin access for monitoring data
- `infinitic-transport-pulsar` — stats and topic information

### External
- Kweb — Kotlin server-side reactive web framework
- Tailwind CSS — utility-first CSS framework
- Node.js / PostCSS — CSS build toolchain (dev only)

<!-- MANUAL: -->
