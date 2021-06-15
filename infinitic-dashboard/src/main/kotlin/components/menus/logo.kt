package components.menus

import kotlinx.serialization.json.JsonPrimitive
import kweb.*

fun ElementCreator<Element>.logo() {
    div().classes("flex-shrink-0 px-4 flex items-center").new {
        img(mapOf(
            "class" to JsonPrimitive("h-8 w-auto"),
            "src" to JsonPrimitive("https://tailwindui.com/img/logos/workflow-logo-indigo-600-mark-gray-800-text.svg"),
            "alt" to JsonPrimitive("Workflow")
        ))
    }
}

