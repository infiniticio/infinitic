import components.*
import components.menus.MenuItem
import kweb.*
import kweb.state.render
import plugins.tailwind.tailwindPlugin

fun main() {
    Kweb(port = 16097, plugins = listOf(tailwindPlugin)) {
        doc.body {

            route {
                path("/workflows") { params ->
                    MenuItem.selected.value = MenuItem.WORKFLOWS
                }
                path("/tasks") { params ->
                    MenuItem.selected.value = MenuItem.TASKS
                }
                path("/architecture") { params ->
                    MenuItem.selected.value = MenuItem.ARCHITECTURE
                }
                path("/settings") { params ->
                    MenuItem.selected.value = MenuItem.SETTINGS
                }
            }

            app()
        }
    }
}

fun ElementCreator<*>.routerView() {
    render(MenuItem.selected) { c ->
        when (c) {
            MenuItem.WORKFLOWS -> workflowsPanel()
            MenuItem.TASKS -> tasksPanel()
            MenuItem.ARCHITECTURE -> architecturePanel()
            MenuItem.SETTINGS -> settingsPanel()
        }
    }
}

fun ElementCreator<*>.routeTo(item: MenuItem) {
    browser.url.value = when(item) {
        MenuItem.WORKFLOWS -> "/workflows"
        MenuItem.TASKS -> "/tasks"
        MenuItem.ARCHITECTURE -> "/architecture"
        MenuItem.SETTINGS -> "/settings"
    }
}
