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

package io.infinitic.dashboard.modals

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kweb.Element
import kweb.ElementCreator
import kweb.button
import kweb.div
import kweb.new
import kweb.p
import kweb.span
import kweb.state.KVar

object Modal {

    private val showModal = KVar(false)
    private val display = KVar(false)

    fun open() {
        display.value = true
        showModal.value = true
    }

    private fun close() {
        GlobalScope.launch {
            showModal.value = false
            delay(1000)
            display.value = false
        }
    }

    fun render(creator: ElementCreator<Element>): Unit = with(creator) {
        div().classes(
            display.map {
                "fixed z-10 inset-0 overflow-y-auto " + if (it) "" else "hidden"
            }
        )
            .setAttribute("aria-labelledby", "modal-title")
            .setAttribute("role", "dialog")
            .setAttribute("aria-modal", "true")
            .new {
                div().classes(
                    showModal.map {
                        "flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0 transition-opacity " +
                            if (it) "ease-out duration-300 opacity-100" else "ease-in duration-200 opacity-0"
                    }
                ).new {
                    div()
                        .classes("fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity")
                        .setAttribute("aria-hidden", "true")
                    // This element is to trick the browser into centering the modal contents.
                    span()
                        .classes("hidden sm:inline-block sm:align-middle sm:h-screen")
                        .setAttribute("aria-hidden", "true")
                    div().classes(
                        showModal.map {
                            "inline-block align-bottom bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full sm:p-6 " +
                                if (it) "ease-out duration-300 opacity-100 translate-y-0 sm:scale-100" else "ease-in duration-200 opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
                        }
                    ).new {
                        // Close cross
                        val close = div()
                        close.on.click { close() }
                        close.classes("hidden sm:block absolute top-0 right-0 pt-4 pr-4").new {
                            button()
                                .classes("bg-white rounded-md text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500")
                                .setAttribute("type", "button")
                                .new {
                                    span().classes("sr-only").text("Close")
                                    // Heroicon name: outline/x
                                    element("svg")
                                        .classes("h-6 w-6")
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
                        }
                        div().classes("sm:flex sm:items-start").new {
                            div().classes("mx-auto flex-shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-red-100 sm:mx-0 sm:h-10 sm:w-10").new {
                                // Heroicon name: outline/exclamation
                                element("svg")
                                    .classes("h-6 w-6 text-red-600")
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
                                            .setAttribute("d", "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z")
                                    }
                            }
                            div().classes("mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left").new {
                                element("h3")
                                    .classes("text-lg leading-6 font-medium text-gray-900")
                                    .setAttribute("idd", "modal-title")
                                    .text("Deactivate account")
                                div().classes("mt-2").new {
                                    p()
                                        .classes("text-sm text-gray-500")
                                        .text("Are you sure you want to deactivate your account? All of your data will be permanently removed from our servers forever. This action cannot be undone.")
                                }
                            }
                        }
                        // buttons
                        div().classes("mt-5 sm:mt-4 sm:flex sm:flex-row-reverse").new {
                            button()
                                .setAttribute("type", "button")
                                .classes("w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-red-600 text-base font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 sm:ml-3 sm:w-auto sm:text-sm")
                                .text("Deactivate")
                            button()
                                .setAttribute("type", "button")
                                .classes("mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:w-auto sm:text-sm")
                                .text("Cancel")
                        }
                    }
                }
            }
    }
}
