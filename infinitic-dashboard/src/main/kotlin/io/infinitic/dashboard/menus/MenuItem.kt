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

package io.infinitic.dashboard.menus

import io.infinitic.dashboard.AppPanel
import io.infinitic.dashboard.AppState
import io.infinitic.dashboard.Panel
import io.infinitic.dashboard.routeTo
import kweb.Element
import kweb.ElementCreator
import kweb.a
import kweb.new
import kweb.state.property

sealed class MenuItem(val text: String, private val icon: ElementCreator<Element>.() -> Element) {
    abstract var current: Panel

    companion object {
        private const val selectedNavStyle =
            "bg-gray-100 text-gray-900 group flex items-center px-2 py-2 font-medium rounded-md "
        private const val unselectedNavStyle =
            "text-gray-600 hover:bg-gray-50 hover:text-gray-900 group flex items-center px-2 py-2 font-medium rounded-md "

        private const val selectNavIconStyle =
            "text-gray-500 mr-4 flex-shrink-0 h-6 w-6"
        private const val unselectNavIconStyle =
            "text-gray-400 group-hover:text-gray-500 mr-3 flex-shrink-0 h-6 w-6"
    }

    fun render(creator: ElementCreator<Element>, offCanvas: Boolean = false) = with(creator) {

        val a = a()
            .setAttribute("href", "#")
            .classes(
                AppPanel.appState.property(AppState::panel).map {
                    when (it.menu) {
                        this@MenuItem -> selectedNavStyle
                        else -> unselectedNavStyle
                    } + if (offCanvas) "text-base" else "text-sm"
                }
            )

        a.on.click { browser.routeTo(current) }

        a.new {
            icon().classes(
                AppPanel.appState.property(AppState::panel).map {
                    when (it.menu) {
                        this@MenuItem -> MenuItem.selectNavIconStyle
                        else -> MenuItem.unselectNavIconStyle
                    }
                }
            )
        }
        a.addText(text)
    }
}
