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

package io.infinitic.dashboard.panels.infrastructure

import io.infinitic.dashboard.Panel
import io.infinitic.dashboard.icons.iconRefresh
import io.infinitic.dashboard.menus.InfraMenu
import io.infinitic.dashboard.panels.infrastructure.task.InfraTaskPanel
import io.infinitic.dashboard.panels.infrastructure.workflow.InfraWorkflowPanel
import io.infinitic.dashboard.routeTo
import io.infinitic.dashboard.slideovers.Slideover
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kweb.Element
import kweb.ElementCreator
import kweb.div
import kweb.h2
import kweb.h3
import kweb.new
import kweb.p
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

object InfraPanel : Panel() {
    override val menu = InfraMenu
    override val route = "/infra"

    private val infraTasksState = KVar(InfraTasksState())
    private val infraWorkflowsState = KVar(InfraWorkflowsState())

    lateinit var job: Job

    override fun onEnter() {
        if (! this::job.isInitialized || job.isCancelled) {
            job = GlobalScope.launch {
                // update of task names every 30 seconds
                infraTasksState.update(this)
                // update of workflow names every 30 seconds
                infraWorkflowsState.update(this)
            }
        }
    }

    override fun onLeave() {
        if (this::job.isInitialized) {
            job.cancel()
        }
    }

    override fun render(creator: ElementCreator<Element>) = with(creator) {
        // PAGE HEADER
        div().classes("bg-white shadow py-8").new {
            div().classes("max-w-7xl mx-auto px-4 sm:px-6 md:px-8").new {
                div().classes("lg:flex lg:items-center lg:justify-between").new {
                    div().classes("flex-1 min-w-0").new {
                        // title
                        h2().classes("mt-2 text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate")
                            .text("Infrastructure")
                    }
                }
            }
        }

        // WORKFLOWS
        val workflowsLastUpdated  = infraWorkflowsState.property(InfraWorkflowsState::lastUpdated)

        div().classes("pt-8 pb-8").new {
            div().classes("max-w-7xl mx-auto sm:px-6 md:px-8").new {
                // Workflows header
                div().classes("pb-5 border-b border-gray-200 sm:flex sm:items-center sm:justify-between").new {
                    h3().classes("text-lg leading-6 font-medium text-gray-900").text("Workflows")
                    div().classes("mt-3 sm:mt-0 sm:ml-4").new {
                        render(workflowsLastUpdated) {
                            val div = div().classes("inline-flex items-center px-4 py-2 text-sm text-gray-500")
                            div.new {
                                iconRefresh().classes("mr-1.5 h-5 w-5 text-gray-400")
                            }
                            div.addText(lastUpdated(it))
                        }
                    }
                }
                p().classes("mt-7 text-sm text-gray-500").text(
                    """
                        Here are your workflows, based on topics found in Pulsar.
                        Click on a row to get more details.
                    """
                )
                // Workflows table
                div().classes("pt-5").new {
                    div().classes("max-w-none mx-auto").new {
                        div().classes("bg-white overflow-hidden sm:rounded-lg sm:shadow").new {
                            div().classes("flex flex-col").new {
                                div().classes("-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8").new {
                                    div().classes("py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8").new {
                                        div().classes("shadow overflow-hidden border-b border-gray-200 sm:rounded-lg").new {
                                            render(infraWorkflowsState) { state ->
                                                table().classes("min-w-full divide-y divide-gray-200").new {
                                                    thead().classes("bg-gray-50").new {
                                                        tr().new {
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Name")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("# Executors")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Executors Backlog")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Executors Rate Out")
                                                        }
                                                    }
                                                    tbody().new {
                                                        when (val workflows = state.workflowNames) {
                                                            null -> displayLoading()
                                                            else -> workflows.forEach {
                                                                when (val stats = state.workflowTaskExecutorsStats[it]) {
                                                                    null -> displayExecutorLoading(it, isTask = false)
                                                                    else -> displayExecutorStats(it, stats, isTask = false)
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
        }

        // TASKS
        val tasksLastUpdated  = infraTasksState.property(InfraTasksState::lastUpdated)

        div().classes("pt-8 pb-8").new {
            div().classes("max-w-7xl mx-auto sm:px-6 md:px-8").new {
                // Tasks header
                div().classes("pb-5 border-b border-gray-200 sm:flex sm:items-center sm:justify-between").new {
                    h3().classes("text-lg leading-6 font-medium text-gray-900").text("Tasks")
                    div().classes("mt-3 sm:mt-0 sm:ml-4").new {
                        render(tasksLastUpdated) {
                            val div = div().classes("inline-flex items-center px-4 py-2 text-sm text-gray-500")
                            div.new {
                                iconRefresh().classes("mr-1.5 h-5 w-5 text-gray-400")
                            }
                            div.addText(lastUpdated(it))
                        }
                    }
                }
                p().classes("mt-7 text-sm text-gray-500").text(
                    """
                    Here are your tasks, based on topics found in Pulsar.
                        Click on a row to get more details.
                """
                )
                // Tasks table
                div().classes("pt-5").new {
                    div().classes("max-w-none mx-auto").new {
                        div().classes("bg-white overflow-hidden sm:rounded-lg sm:shadow").new {
                            div().classes("flex flex-col").new {
                                div().classes("-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8").new {
                                    div().classes("py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8").new {
                                        div().classes("shadow overflow-hidden border-b border-gray-200 sm:rounded-lg").new {
                                            render(infraTasksState) { state ->
                                                table().classes("min-w-full divide-y divide-gray-200").new {
                                                    thead().classes("bg-gray-50").new {
                                                        tr().new {
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Name")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("# Executors")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Executors Backlog")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Executors Rate Out")
                                                        }
                                                    }
                                                    tbody().new {
                                                        when (state.taskNames.status) {
                                                            InfraStatus.LOADING -> displayLoading()
                                                            InfraStatus.ERROR -> displayError()
                                                            InfraStatus.COMPLETED  -> state.taskNames.names!!.forEach {
                                                                when (state.taskStats[it]!!.status) {
                                                                    InfraStatus.LOADING -> displayExecutorLoading(it, isTask = true)
                                                                    InfraStatus.ERROR -> displayExecutorError(it, isTask = true)
                                                                    InfraStatus.COMPLETED -> displayExecutorStats(it, state.taskStats[it]!!.partitionedTopicStats!!, isTask = true)
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
        }
    }

    private fun ElementCreator<Element>.displayLoading() {
        tr().classes("bg-white").new {
            td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                .text("loading...")
        }
    }

    private fun ElementCreator<Element>.displayError() {
        tr().classes("bg-white").new {
            td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                .text("Error!")
        }
    }

    private fun ElementCreator<Element>.displayExecutorLoading(name: String, isTask: Boolean) {
        val row = tr()
        row.classes("bg-white cursor-pointer hover:bg-gray-50").new {
            td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                .text(name)
            td().classes("px-6 py-4 text-sm text-gray-500")
                .text("loading...")
            td().classes("px-6 py-4 text-sm text-gray-500")
                .text("loading...")
            td().classes("px-6 py-4 text-sm text-gray-500")
                .text("loading...")
        }
        row.on.click {
            if (isTask)
                browser.routeTo(InfraTaskPanel.from(name))
            else
                browser.routeTo(InfraWorkflowPanel.from(name))
        }
    }

    private fun ElementCreator<Element>.displayExecutorError(name: String, isTask: Boolean) {
        val row = tr()
        row.classes("bg-white cursor-pointer hover:bg-gray-50").new {
            td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                .text(name)
            td().classes("px-6 py-4 text-sm text-gray-500")
                .text("error!")
            td().classes("px-6 py-4 text-sm text-gray-500")
                .text("error!")
            td().classes("px-6 py-4 text-sm text-gray-500")
                .text("error!")
        }
        row.on.click {
            if (isTask)
                browser.routeTo(InfraTaskPanel.from(name))
            else
                browser.routeTo(InfraWorkflowPanel.from(name))
        }
    }
    private fun ElementCreator<Element>.displayExecutorStats(name: String, stats: PartitionedTopicStats, isTask: Boolean) {
        stats.subscriptions.map {
            val row = tr()
            row.classes("bg-white cursor-pointer hover:bg-gray-50").new {
                td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                    .text(name)
                td().classes("px-6 py-4 text-sm text-gray-500")
                    .text(it.value.consumers.size.toString())
                td().classes("px-6 py-4 text-sm text-gray-500")
                    .text(it.value.msgBacklog.toString())
                td().classes("px-6 py-4 text-sm text-gray-500")
                    .text("%.2f".format(it.value.msgRateOut) + " msg/s")
            }
            row.on.click {
                if (isTask)
                    browser.routeTo(InfraTaskPanel.from(name))
                else
                    browser.routeTo(InfraWorkflowPanel.from(name))
            }
        }
    }
}

internal fun lastUpdated(instant: Instant) = "Last updated at ${SimpleDateFormat("HH:mm:ss").format(Date.from(instant))}"
