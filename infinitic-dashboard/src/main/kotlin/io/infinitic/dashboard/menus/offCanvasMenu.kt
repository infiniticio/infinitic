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
package io.infinitic.dashboard.menus

import io.infinitic.dashboard.AppPanel
import io.infinitic.dashboard.svgs.icons.iconClose
import io.infinitic.dashboard.toggleMobileMenu
import kweb.Element
import kweb.ElementCreator
import kweb.button
import kweb.div
import kweb.new
import kweb.span
import kweb.state.KVar

fun ElementCreator<Element>.offCanvasMenuCloseButton(isMobileMenuVisible: KVar<Boolean>) {
  div()
      .classes(
          isMobileMenuVisible.map {
            "absolute top-0 right-0 -mr-12 pt-2 ease-in-out duration-300 " +
                if (it) "opacity-100" else "opacity-0"
          })
      .new {
        val button = button()
        button
            .classes(
                "ml-1 flex items-center justify-center h-10 w-10 rounded-full focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white")
            .new {
              span().classes("sr-only").text("Close sidebar")
              iconClose().addClasses("text-white")
            }
        button.on.click { AppPanel.appState.toggleMobileMenu() }
      }
}

fun ElementCreator<Element>.offCanvasMenuOverlay(isMobileMenuVisible: KVar<Boolean>) {
  div()
      .set("aria-hidden", "true")
      .classes(
          isMobileMenuVisible.map {
            "fixed inset-0 bg-gray-600 bg-opacity-75 transition-opacity ease-linear duration-300 " +
                if (it) "opacity-100" else "opacity-0"
          })
}
