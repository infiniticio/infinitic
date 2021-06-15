package components

import kweb.*

fun ElementCreator<Element>.architecturePanel() {
    div().classes("py-6").new {
        div().classes("max-w-7xl mx-auto px-4 sm:px-6 md:px-8").new {
            h1().classes("text-2xl font-semibold text-gray-900")
                .text("Architecture")
        }
        div().classes("max-w-7xl mx-auto px-4 sm:px-6 md:px-8").new {
            div().classes("py-4").new {
                div().classes("border-4 border-dashed border-gray-200 rounded-lg h-96")
            }
        }
    }
}

