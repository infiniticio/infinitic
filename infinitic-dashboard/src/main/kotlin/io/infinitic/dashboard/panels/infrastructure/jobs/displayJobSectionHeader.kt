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

package io.infinitic.dashboard.panels.infrastructure.jobs

import io.infinitic.dashboard.icons.iconRefresh
import io.infinitic.dashboard.panels.infrastructure.lastUpdated
import kweb.Element
import kweb.ElementCreator
import kweb.div
import kweb.h3
import kweb.new
import kweb.span
import kweb.state.KVar
import java.time.Instant

internal fun ElementCreator<Element>.displayJobSectionHeader(
    title: String,
    lastUpdated: KVar<Instant>
) {
    div().classes("pb-5 border-b border-gray-200 sm:flex sm:items-center sm:justify-between").new {
        h3().classes("text-lg leading-6 font-medium text-gray-900").text(title)
        div().classes("mt-3 sm:mt-0").new {
            val div = div().classes("inline-flex items-center py-2 text-sm text-gray-500")
            div.new {
                iconRefresh().classes("mr-1.5 h-5 w-5 text-gray-400")
                span().text(lastUpdated.map { lastUpdated(it) })
            }
        }
    }
}
