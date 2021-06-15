package components.menus

import kweb.*

fun ElementCreator<Element>.offCanvasMenuCloseButton() {
    div().classes(
        MenuItem.isMobileMenuVisible.map {
            "absolute top-0 right-0 -mr-12 pt-2 ease-in-out duration-300 " +
                    if (it) "opacity-0" else "opacity-100"
        }).new {
        val button = button()

        button.classes("ml-1 flex items-center justify-center h-10 w-10 rounded-full focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white").new {
            span().classes("sr-only").text("Close sidebar")
            element("svg").classes("h-6 w-6 text-white")
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
                        .setAttribute("d", "M6 18L18 6M6 6l12 12")
                }
        }

        button.on.click {
            MenuItem.isMobileMenuVisible.value = ! MenuItem.isMobileMenuVisible.value
        }
    }
}

fun ElementCreator<Element>.offCanvasMenuOverlay() {
    div()
        .setAttribute("aria-hidden", "true")
        .classes(MenuItem.isMobileMenuVisible.map {
            "fixed inset-0 bg-gray-600 bg-opacity-75 transition-opacity ease-linear duration-300 " +
                    if (it) "opacity-0" else "opacity-100"
        })
}