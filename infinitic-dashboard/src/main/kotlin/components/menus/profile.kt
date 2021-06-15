package components.menus

import kotlinx.serialization.json.JsonPrimitive
import kweb.*

fun ElementCreator<Element>.profile(offCanvas: Boolean = false) {
    div().classes("flex-shrink-0 flex border-t border-gray-200 p-4").new {
        a().classes("flex-shrink-0 w-full group block").setAttribute("href", "#").new {
            div().classes("flex items-center").new {
                div {
                    img(mapOf(
                        "src" to JsonPrimitive("https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80"),
                        "class" to JsonPrimitive("inline-block h-9 w-9 rounded-full"),
                        "alt" to JsonPrimitive("")
                    ))
                }
                div().classes("ml-3").new {
                    p().classes("font-medium text-gray-700 group-hover:text-gray-900 " + if(offCanvas) "text-base" else "text-sm")
                        .text("Tom Cook")
                    p().classes("font-medium text-gray-500 group-hover:text-gray-700 " + if(offCanvas) "text-sm" else "text-xs")
                        .text("View Profile")
                }
            }
        }
    }
}
