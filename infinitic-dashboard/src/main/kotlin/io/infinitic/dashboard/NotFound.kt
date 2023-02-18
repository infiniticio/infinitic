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
package io.infinitic.dashboard

import io.infinitic.dashboard.panels.infrastructure.AllJobsPanel
import kweb.Element
import kweb.ElementCreator
import kweb.a
import kweb.div
import kweb.h1
import kweb.img
import kweb.new
import kweb.p
import kweb.span

object NotFound {
  fun render(creator: ElementCreator<Element>): Unit =
      with(creator) {
        div().classes("min-h-screen pt-16 pb-12 flex flex-col bg-white").new {
          element("main")
              .classes(
                  "flex-grow flex flex-col justify-center max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8")
              .new {
                div().classes("flex-shrink-0 flex justify-center").new {
                  with(a()) {
                    href = AllJobsPanel.url
                    new {
                      span().classes("sr-only").text("Infinitic")
                      img()
                          .classes("h-12 w-auto")
                          .set("src", "https://docs.infinitic.io/logo-light.svg")
                          .set("alt", "Infinitic")
                    }
                  }
                }
                div().classes("py-16").new {
                  div().classes("text-center").new {
                    p().classes("text-sm font-semibold text-indigo-600 uppercase tracking-wide")
                        .text("404 error")
                    h1()
                        .classes(
                            "mt-2 text-4xl font-extrabold text-gray-900 tracking-tight sm:text-5xl")
                        .text("Page not found.")
                    p().classes("mt-2 text-base text-gray-500")
                        .text("Sorry, we couldn’t find the page you’re looking for.")
                    div().classes("mt-6").new {
                      with(a()) {
                        href = AllJobsPanel.url
                        classes("text-base font-medium text-indigo-600 hover:text-indigo-500")
                        new {
                          span().text("Go back home")
                          span().text("→").set("aria-hidden", "true")
                        }
                      }
                    }
                  }
                }
              }
        }
      }
}
