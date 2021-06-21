/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.dashboard.components

import io.infinitic.dashboard.components.menus.MenuItem
import io.infinitic.dashboard.components.menus.logo
import io.infinitic.dashboard.components.menus.offCanvasMenuCloseButton
import io.infinitic.dashboard.components.menus.offCanvasMenuOverlay
import io.infinitic.dashboard.components.menus.profile
import io.infinitic.dashboard.routerView
import kweb.Element
import kweb.ElementCreator
import kweb.button
import kweb.div
import kweb.nav
import kweb.new
import kweb.span

fun ElementCreator<Element>.app() {
    div().classes("h-screen flex overflow-hidden bg-gray-100").new {
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
                            MenuItem.PULSAR.render(this, true)
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
                            MenuItem.PULSAR.render(this)
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
