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

import kweb.Element
import kweb.ElementCreator
import kweb.div

fun ElementCreator<Element>.profile(offCanvas: Boolean = false) {
//    div().classes("flex-shrink-0 flex border-t border-gray-200 p-4").new {
//        a().classes("flex-shrink-0 w-full group block").setAttribute("href", "#").new {
//            div().classes("flex items-center").new {
//                div {
//                    img(
//                        mapOf(
//                            "src" to JsonPrimitive("https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=facearea&facepad=2&w=256&h=256&q=80"),
//                            "class" to JsonPrimitive("inline-block h-9 w-9 rounded-full"),
//                            "alt" to JsonPrimitive("")
//                        )
//                    )
//                }
//                div().classes("ml-3").new {
//                    p().classes("font-medium text-gray-700 group-hover:text-gray-900 " + if (offCanvas) "text-base" else "text-sm")
//                        .text("Tom Cook")
//                    p().classes("font-medium text-gray-500 group-hover:text-gray-700 " + if (offCanvas) "text-sm" else "text-xs")
//                        .text("View Profile")
//                }
//            }
//        }
//    }
}
