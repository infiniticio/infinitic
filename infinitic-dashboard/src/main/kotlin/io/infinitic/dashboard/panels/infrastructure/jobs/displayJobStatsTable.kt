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

import io.infinitic.dashboard.Infinitic
import io.infinitic.dashboard.panels.infrastructure.requests.Completed
import io.infinitic.dashboard.panels.infrastructure.requests.Failed
import io.infinitic.dashboard.panels.infrastructure.requests.Loading
import io.infinitic.dashboard.panels.infrastructure.requests.TopicStats
import io.infinitic.dashboard.slideovers.Slideover
import io.infinitic.pulsar.topics.TopicSet
import kweb.Element
import kweb.ElementCreator
import kweb.div
import kweb.new
import kweb.state.KVar
import kweb.state.render
import kweb.table
import kweb.tbody
import kweb.td
import kweb.th
import kweb.thead
import kweb.tr

internal fun ElementCreator<Element>.displayJobStatsTable(
    name: String,
    state: KVar<out InfraJobState<out TopicSet>>,
    selectionSlide: Slideover<*>,
    selectionType: KVar<TopicSet>,
    selectionStats: KVar<TopicStats>
) {
    // Topics table
    div().classes("pt-5").new {
        div().classes("max-w-none mx-auto").new {
            div().classes("bg-white overflow-hidden sm:rounded-lg sm:shadow").new {
                div().classes("flex flex-col").new {
                    div().classes("-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8").new {
                        div().classes("py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8").new {
                            div().classes("shadow overflow-hidden border-b border-gray-200 sm:rounded-lg").new {
                                render(state) { state ->
                                    table().classes("min-w-full divide-y divide-gray-200").new {
                                        thead().classes("bg-gray-50").new {
                                            tr().new {
                                                th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                    .setAttribute("scope", "col")
                                                    .text("Topic's type")
                                                th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                    .setAttribute("scope", "col")
                                                    .text("# Consumers")
                                                th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                    .setAttribute("scope", "col")
                                                    .text("Msg Backlog")
                                                th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                    .setAttribute("scope", "col")
                                                    .text("Msg Rate Out")
                                                th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                    .setAttribute("scope", "col")
                                                    .text("Topic's name")
                                            }
                                        }
                                        tbody().new {
                                            state.topicsStats.forEach {
                                                val type = it.key
                                                val stats = it.value
                                                val topic = Infinitic.topicName.of(type, name)
                                                val row = tr()

                                                when (val status = stats.request) {
                                                    is Loading ->
                                                        row.classes("bg-white").new {
                                                            td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                                                                .text(type.prefix)
                                                            td().classes("px-6 py-4 text-sm text-gray-500")
                                                                .text("loading...")
                                                            td().classes("px-6 py-4 text-sm text-gray-500")
                                                                .text("loading...")
                                                            td().classes("px-6 py-4 text-sm text-gray-500")
                                                                .text("loading...")
                                                            td().classes("px-6 py-4 text-sm text-gray-500")
                                                                .text(topic)
                                                        }
                                                    is Failed ->
                                                        row.classes("bg-white cursor-pointer hover:bg-gray-50").new {
                                                            td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                                                                .text(type.prefix)
                                                            td().classes("px-6 py-4 text-sm text-gray-500 text-center italic")
                                                                .setAttribute("colspan", 3)
                                                                .text(status.title)
                                                            td().classes("px-6 py-4 text-sm text-gray-500")
                                                                .text(topic)
                                                        }
                                                    is Completed -> status.result.subscriptions.map {
                                                        row.classes("bg-white cursor-pointer hover:bg-gray-50").new {
                                                            td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                                                                .text(type.prefix)
                                                            td().classes("px-6 py-4 text-sm text-gray-500")
                                                                .text(it.value.consumers.size.toString())
                                                            td().classes("px-6 py-4 text-sm text-gray-500")
                                                                .text(it.value.msgBacklog.toString())
                                                            td().classes("px-6 py-4 text-sm text-gray-500")
                                                                .text("%.2f".format(it.value.msgRateOut) + " msg/s")
                                                            td().classes("px-6 py-4 text-sm text-gray-500")
                                                                .text(topic)
                                                        }
                                                    }
                                                }
                                                if (stats.request !is Loading<*>) {
                                                    row.on.click {
                                                        if (selectionType.value != type) {
                                                            selectionType.value = type
                                                            selectionStats.value = stats
                                                        }

                                                        selectionSlide.open()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
