/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.dashboard.slideovers

import io.infinitic.dashboard.svgs.icons.iconClose
import kweb.Element
import kweb.ElementCreator
import kweb.button
import kweb.div
import kweb.h2
import kweb.new
import kweb.span
import kweb.state.KVal
import kweb.state.KVar
import kweb.state.render

class Slideover<T>(
    private val title: KVal<String> = KVar("Panel title"),
    private val state: KVar<T>,
    private val renderState: ElementCreator<Element>.(state: KVar<T>) -> Unit = {
      div().classes("h-full border-2 border-dashed border-gray-200")
    }
) {

  private val showModal = KVar(false)

  fun open() {
    showModal.value = true
  }

  fun close() {
    showModal.value = false
  }

  fun render(creator: ElementCreator<Element>): Unit =
      with(creator) {
        div()
            .classes("fixed overflow-hidden")
            .set("aria-labelledby", "slide-over-title")
            .set("role", "dialog")
            .set("aria-modal", "true")
            .new {
              div().classes("absolute inset-0 overflow-hidden").new {
                // Background overlay, show/hide based on slide-over state.
                div().classes("absolute inset-0").set("aria-hidden", "true")

                div()
                    .classes(
                        showModal.map {
                          "fixed inset-y-0 right-0 pl-10 pt-12 max-w-full flex sm:pl-16 md:pt-0 pt-0 transform transition ease-in-out duration-500 sm:duration-700 " +
                              if (it) "translate-x-0" else "translate-x-full"
                        })
                    .new {
                      div().classes("w-screen max-w-3xl").new {
                        div()
                            .classes(
                                "h-full flex flex-col py-6 bg-white shadow-xl overflow-y-scroll")
                            .new {
                              div().classes("px-4 sm:px-6").new {
                                div().classes("flex items-start justify-between").new {
                                  h2().classes("text-lg font-medium text-gray-900").text(title)
                                  div().classes("ml-3 h-7 flex items-center").new {
                                    val button =
                                        button()
                                            .classes(
                                                "bg-white rounded-md text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500")
                                    button.new {
                                      span().classes("sr-only").text("Close panel")
                                      iconClose()
                                    }
                                    button.on.click { close() }
                                  }
                                }
                              }
                              div().classes("mt-6 relative flex-1 px-4 sm:px-6").new {
                                // Replace with your content
                                div().classes("absolute inset-0 px-4 sm:px-6").new {
                                  render(state) { renderState(state) }
                                }
                              }
                            }
                      }
                    }
              }
            }
      }
}
