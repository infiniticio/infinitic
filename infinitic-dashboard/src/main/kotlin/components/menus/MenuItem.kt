package components.menus

import kweb.*
import kweb.state.KVar
import routeTo

enum class MenuItem(val text: String, val icon: String) {
    WORKFLOWS("Workflows", "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"),
    TASKS("Tasks", "M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"),
    ARCHITECTURE("Architecture", "M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"),
    SETTINGS("Settings", "M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z");

    companion object {
        var selected =  KVar(WORKFLOWS)

        val isMobileMenuVisible = KVar(false)

        private const val selectedNavStyle =
            "bg-gray-100 text-gray-900 group flex items-center px-2 py-2 font-medium rounded-md "
        private const val unselectedNavStyle =
            "text-gray-600 hover:bg-gray-50 hover:text-gray-900 group flex items-center px-2 py-2 font-medium rounded-md "

        private const val selectNavIconStyle =
            "text-gray-500 mr-4 flex-shrink-0 h-6 w-6"
        private const val unselectNavIconStyle =
            "text-gray-400 group-hover:text-gray-500 mr-3 flex-shrink-0 h-6 w-6"
    }

    fun render(creator: ElementCreator<Element>, offCanvas: Boolean = false) {

        val a = creator.a()
            .setAttribute("href", "#")
            .classes(selected.map {
                when (it) {
                    this -> selectedNavStyle
                    else -> unselectedNavStyle
                } + if (offCanvas) "text-base" else "text-sm"
            })

        a.on.click {
            creator.routeTo(this)
        }

        val item = this
        a.new {
            element("svg").classes(selected.map {
                when (it) {
                    item -> selectNavIconStyle
                    else -> unselectNavIconStyle
                }
            })
                .setAttribute("xmlns", "http://www.w3.org/2000/svg")
                .setAttribute("fill", "none")
                .setAttribute("viewBox", "0 0 24 24")
                .setAttribute("stroke", "currentColor")
                .setAttribute("aria-hidden", "true")
                .new {
                    element("path")
                        .setAttribute("stroke-linecap", "round")
                        .setAttribute("stroke-linejoin", "round")
                        .setAttribute("stroke-width", "2")
                        .setAttribute("d", item.icon)
                }
        }
        a.addText(text)
    }
}
