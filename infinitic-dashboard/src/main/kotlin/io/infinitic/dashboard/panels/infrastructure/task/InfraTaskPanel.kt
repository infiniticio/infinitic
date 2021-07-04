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

package io.infinitic.dashboard.panels.infrastructure.task

import io.infinitic.common.serDe.json.Json
import io.infinitic.dashboard.Infinitic.topicName
import io.infinitic.dashboard.Panel
import io.infinitic.dashboard.icons.iconChevron
import io.infinitic.dashboard.icons.iconRefresh
import io.infinitic.dashboard.menus.InfraMenu
import io.infinitic.dashboard.panels.infrastructure.InfraPanel
import io.infinitic.dashboard.panels.infrastructure.InfraStatus
import io.infinitic.dashboard.panels.infrastructure.InfraTopicInfo
import io.infinitic.dashboard.panels.infrastructure.InfraTopicStats
import io.infinitic.dashboard.panels.infrastructure.lastUpdated
import io.infinitic.dashboard.routeTo
import io.infinitic.dashboard.slideovers.Slideover
import io.infinitic.exceptions.thisShouldNotHappen
import io.infinitic.pulsar.topics.TaskTopic
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kweb.Element
import kweb.ElementCreator
import kweb.a
import kweb.div
import kweb.h2
import kweb.h3
import kweb.img
import kweb.label
import kweb.li
import kweb.nav
import kweb.new
import kweb.ol
import kweb.option
import kweb.p
import kweb.select
import kweb.span
import kweb.state.KVar
import kweb.state.property
import kweb.state.render
import kweb.table
import kweb.tbody
import kweb.td
import kweb.th
import kweb.thead
import kweb.tr
import org.apache.pulsar.common.policies.data.PartitionedTopicStats
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

class InfraTaskPanel private constructor(private val taskName: String) : Panel() {

    companion object {
        private val instances: ConcurrentHashMap<String, InfraTaskPanel> = ConcurrentHashMap()

        fun from(taskName: String) = instances.computeIfAbsent(taskName) { InfraTaskPanel(taskName) }
    }

    override val menu = InfraMenu

    override val route = "/infra/t/$taskName"

    private val selectedInfo = KVar(InfraTopicInfo.STATS)
    private val state = KVar(InfraTaskState(taskName))

    private val lastUpdated  = state.property(InfraTaskState::lastUpdated)
    private val selectionTitle = state.property(InfraTaskState::selectedType).map { "${it.prefix} stats".replaceFirstChar { c -> c.uppercase() } }
    private val selectionStats = state.property(InfraTaskState::selectedStats)

    private val slideover = Slideover(selectionTitle, selectionStats) {
        p().classes("text-sm font-medium text-gray-900").new {
            span().text(lastUpdated(it.value.lastUpdated)  + " (cf.")
            a().classes("underline")
                .setAttribute("href", "https://pulsar.apache.org/docs/en/next/administration-stats/#partitioned-topics")
                .setAttribute("target", "_blank")
                .text("documentation")
            span().text(")")
        }
        p().classes("mt-7 text-sm text-gray-500").new {
            element("pre").text(
                when(it.value.status) {
                    InfraStatus.LOADING -> "Loading..."
                    InfraStatus.ERROR -> it.value.stackTrace!!
                    InfraStatus.COMPLETED -> Json.stringify(it.value.partitionedTopicStats, true)
                }
            )
        }
    }

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
        slideover.close()
    }

    override fun render(creator: ElementCreator<Element>): Unit = with(creator) {
        // Header
        div().classes("bg-white shadow py-8").new {
            div().classes("max-w-7xl mx-auto px-4 sm:px-6 md:px-8").new {
                div().classes("lg:flex lg:items-center lg:justify-between").new {
                    div().classes("flex-1 min-w-0").new {
                        // breadcrumbs
                        nav().classes("flex").setAttribute("aria-label", "Breadcrumb").new {
                            ol().classes("flex items-center space-x-4").setAttribute("role", "list").new {
                                li {
                                    div().classes("flex items-center").new {
                                        val a = a().classes("text-sm font-medium text-gray-500 hover:text-gray-700")
                                            .setAttribute("href", "#")
                                            .setAttribute("aria-current", "page")
                                        a.text("Infrastructure")
                                        a.on.click {
                                            browser.routeTo(InfraPanel)
                                        }
                                        span().classes("sr-only").text("Infrastructure")
                                    }
                                }
                                li {
                                    div().classes("flex items-center").new {
                                        iconChevron().classes("flex-shrink-0 h-5 w-5 text-gray-400")
                                    }
                                }
                            }
                        }
                        // title
                        h2().classes("mt-2 text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate")
                            .text(taskName)
                    }
                }
            }
        }
        // TASKS
        div().classes("pt-8 pb-8").new {
            div().classes("max-w-7xl mx-auto sm:px-6 md:px-8").new {
                // Workflows header
                div().classes("relative pb-5 border-b border-gray-200 sm:pb-0").new {
                    div().classes("md:flex md:items-center md:justify-between"). new {
                        h3().classes("text-lg leading-6 font-medium text-gray-900").text("Task's topics")
                        div().classes("mt-3 flex md:mt-0 md:absolute md:top-3 md:right-0").new {
                            render(lastUpdated) {
                                val div = div().classes("inline-flex items-center px-4 py-2 text-sm text-gray-500")
                                div.new {
                                    iconRefresh().classes("mr-1.5 h-5 w-5 text-gray-400")
                                }
                                div.addText(lastUpdated(it))
                            }
                        }
                    }
                    // header tabs
                    div().classes("mt-4 sm:block").new {
                        nav().classes("-mb-px flex space-x-8").new {
                            InfraTopicInfo.values().forEach { tab ->
                                val a = a()
                                a.setAttribute("href", "#").classes(
                                    selectedInfo.map { info ->
                                        "whitespace-nowrap pb-4 px-1 border-b-2 font-medium text-sm " +
                                            when(info) {
                                                tab -> "border-indigo-500 text-indigo-600"
                                                else -> "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                                            }
                                    }
                                ).text(tab.title)
                                a.on.click {
                                    selectedInfo.value = tab
                                }
                            }
                        }
                    }
                }
                render(selectedInfo) {
                    when(it) {
                        InfraTopicInfo.STATS -> displayStatsTab()
                        InfraTopicInfo.GRAPH -> displayGraphTab()
                    }
                }
            }
        }

        slideover.render(this)
    }

    private fun ElementCreator<Element>.displayGraphTab() {
        img().setAttribute("src", "/static/img/dashboard-tasks.png")
    }

    private fun ElementCreator<Element>.displayStatsTab() {
        p().classes("mt-7 text-sm text-gray-500").text(
            """
                        Here are the topics used to manage this task.
                        Click on a row to get more details.
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
                                                    displayTopicStats(it.key, topicName.of(it.key, taskName), it.value)
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

    private fun ElementCreator<Element>.displayTopicStats(type: TaskTopic, topic: String, stats: InfraTopicStats) {
        val row = tr()
        when (stats.status) {
            InfraStatus.LOADING ->
                row.classes("bg-white cursor-pointer hover:bg-gray-50").new {
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
            InfraStatus.ERROR ->
                row.classes("bg-white cursor-pointer hover:bg-gray-50").new {
                    td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                        .text(type.prefix)
                    td().classes("px-6 py-4 text-sm text-gray-500")
                        .text("error!")
                    td().classes("px-6 py-4 text-sm text-gray-500")
                        .text("error!")
                    td().classes("px-6 py-4 text-sm text-gray-500")
                        .text("error!")
                    td().classes("px-6 py-4 text-sm text-gray-500")
                        .text(topic)
                }
            InfraStatus.COMPLETED -> stats.partitionedTopicStats!!.subscriptions.map {
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
        row.on.click {
            if (state.value.selectedType != type) {
                state.value = state.value.copy(
                    selectedType = type,
                    selectedStats = state.value.topicsStats[type]!!
                )
            }

            slideover.open()
        }
    }
}
