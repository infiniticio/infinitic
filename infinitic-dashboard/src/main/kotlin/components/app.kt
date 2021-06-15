package components

import components.menus.*
import kweb.*
import kweb.state.render
import routerView

fun ElementCreator<Element>.app() {
    div().classes("h-screen flex overflow-hidden bg-white").new {
        // offCanvas menu
        div().classes("fixed inset-0 flex z-40 md:hidden")
            .setAttribute("role", "Dialog")
            .setAttribute("aria-modal", "true")
            .new {
                // Off-canvas menu overlay, show/hide based on off-canvas menu state.
                offCanvasMenuOverlay()
                // Off-canvas menu, show/hide based on off-canvas menu state.
                div().classes(
                    MenuItem.isMobileMenuVisible.map {
                        "relative flex-1 flex flex-col max-w-xs w-full bg-white transition ease-in-out duration-300 transform translate-x-0 " +
                                if (it) "-translate-x-full" else "translate-x-0"
                    }
                ).new {
                    // Close button, show/hide based on off-canvas menu state.
                    offCanvasMenuCloseButton()
                    // off-canvas menu
                    div().classes("flex-1 h-0 pt-5 pb-4 overflow-y-auto").new {
                        // logo
                        logo()
                        // navigation
                        nav().classes("mt-5 px-2 space-y-1").new {
                            // Dashboard
                            MenuItem.WORKFLOWS.render(this, true)
                            // Team
                            MenuItem.TASKS.render(this, true)
                            // Projects
                            MenuItem.ARCHITECTURE.render(this, true)
                            // Calendar
                            MenuItem.SETTINGS.render(this, true)
                        }
                    }
                    // profile
                    profile(true)
                }
                // Force sidebar to shrink to fit close icon
                div().classes("flex-shrink-0 w-14")
                    .setAttribute("aria-hidden", "true")

            }
        // static sidebar for desktop
        div().classes("hidden md:flex md:flex-shrink-0").new {
            div().classes("flex flex-col w-64").new {
                // Sidebar component
                div().classes("flex flex-col h-0 flex-1 border-r border-gray-200 bg-white").new {
                    // logo && navigation
                    div().classes("flex-1 flex flex-col pt-5 pb-4 overflow-y-auto").new {
                        // logo
                        logo()
                        // navigation
                        nav().classes("mt-5 flex-1 px-2 bg-white space-y-1").new {
                            // Dashboard
                            MenuItem.WORKFLOWS.render(this)
                            // Team
                            MenuItem.TASKS.render(this)
                            // Projects
                            MenuItem.ARCHITECTURE.render(this)
                            // Calendar
                            MenuItem.SETTINGS.render(this)
                        }
                    }
                    // Profile
                    profile()
                }
            }
        }
        // right part
        div().classes("flex flex-col w-0 flex-1 overflow-hidden").new {
            // mobile hamburger menu
            div().classes("md:hidden pl-1 pt-1 sm:pl-3 sm:pt-3").new {
                val hamburger = button()
                hamburger.classes("-ml-0.5 -mt-0.5 h-12 w-12 inline-flex items-center justify-center rounded-md text-gray-500 hover:text-gray-900 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-indigo-500").new {
                    span().classes("sr-only").text("Open sidebar")
                    element("svg").classes("h-6 w-6")
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
                                .setAttribute("d", "M4 6h16M4 12h16M4 18h16")
                        }
                }
                hamburger.on.click {
                    MenuItem.isMobileMenuVisible.value = ! MenuItem.isMobileMenuVisible.value
                }
            }
            // right-panel content
            element("main").classes("flex-1 relative z-0 overflow-y-auto focus:outline-none").new {
                routerView()
            }
        }
    }
}

