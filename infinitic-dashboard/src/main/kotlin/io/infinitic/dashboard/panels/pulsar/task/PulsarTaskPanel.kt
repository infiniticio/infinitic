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

package io.infinitic.dashboard.panels.pulsar.task

import io.infinitic.dashboard.Infinitic.topicName
import io.infinitic.dashboard.Panel
import io.infinitic.dashboard.menus.PulsarMenu
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kweb.Element
import kweb.ElementCreator
import kweb.div
import kweb.h1
import kweb.h3
import kweb.new
import kweb.p
import kweb.state.KVar
import kweb.state.render
import kweb.table
import kweb.tbody
import kweb.td
import kweb.th
import kweb.thead
import kweb.tr
import org.apache.pulsar.common.policies.data.PartitionedTopicStats
import java.util.concurrent.ConcurrentHashMap

class PulsarTaskPanel private constructor(private val taskName: String) : Panel() {

    companion object {
        private val instances: ConcurrentHashMap<String, PulsarTaskPanel> = ConcurrentHashMap()

        fun from(taskName: String) = instances.computeIfAbsent(taskName) { PulsarTaskPanel(taskName) }
    }

    override val menu = PulsarMenu

    override val route = "/pulsar/t/$taskName"

    val state = KVar(PulsarTaskState(taskName))

    lateinit var job: Job

    override fun onEnter() {
        if (! this::job.isInitialized || job.isCancelled) {
            job = GlobalScope.launch {
                // update of task names every 30 seconds
                state.update(this)
            }
        }
    }

    override fun onLeave() {
        if (this::job.isInitialized) {
            job.cancel()
        }
    }

    override fun render(creator: ElementCreator<Element>): Unit = with(creator) {
        // Header
        div().classes("bg-white shadow").new {
            div().classes("border-b border-gray-200 px-4 py-4 sm:flex sm:items-center sm:justify-between sm:px-6 lg:px-8").new {
                div().classes("flex-1 min-w-0").new {
                    h1().classes("text-lg font-medium leading-6 text-gray-900 sm:truncate>").text(taskName)
                }
            }
        }
        // WORKFLOWS
        div().classes("pt-6").new {
            div().classes("max-w-7xl mx-auto px-4 sm:px-6 md:px-8").new {
                // Topics header
                h3().classes("text-lg leading-6 font-medium text-gray-900").text("Task's topics")
                p().classes("mt-2 max-w-4xl text-sm text-gray-500").text(
                    """
                        You should have at least one consumer / topic. If your backlog is non-zero,
                        then your msg rate should be non-zero as well (except for delays)
                    """
                )
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
                                                                .text("Type")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Nb Consumers")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Msg Backlog")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Msg Rate Out")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Name")
                                                        }
                                                    }
                                                    tbody().new {
                                                        state.topicsStats.forEach {
                                                            displayTopicStats(it.key.prefix, topicName.of(it.key, taskName), it.value)
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

    private fun ElementCreator<Element>.displayTopicStats(type: String, topic: String, stats: PartitionedTopicStats?) {
        when (stats) {
            null ->
                tr().classes("bg-white").new {
                    td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                        .text(type)
                    td().classes("px-6 py-4 text-sm text-gray-500")
                        .text("loading...")
                    td().classes("px-6 py-4 text-sm text-gray-500")
                        .text("loading...")
                    td().classes("px-6 py-4 text-sm text-gray-500")
                        .text("loading...")
                    td().classes("px-6 py-4 text-sm text-gray-500")
                        .text(topic)
                }
            else -> stats.subscriptions.map {
                tr().classes("bg-white").new {
                    td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                        .text(type)
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
    }
}
