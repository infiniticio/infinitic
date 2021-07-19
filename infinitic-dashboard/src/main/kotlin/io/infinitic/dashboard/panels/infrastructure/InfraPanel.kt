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
import io.infinitic.dashboard.menus.InfraMenu
import io.infinitic.dashboard.panels.infrastructure.jobs.displayJobSectionHeader
import io.infinitic.dashboard.panels.infrastructure.requests.Completed
import io.infinitic.dashboard.panels.infrastructure.requests.Failed
import io.infinitic.dashboard.panels.infrastructure.requests.JobNames
import io.infinitic.dashboard.panels.infrastructure.requests.Loading
import io.infinitic.dashboard.panels.infrastructure.task.InfraTaskPanel
import io.infinitic.dashboard.panels.infrastructure.workflow.InfraWorkflowPanel
import io.infinitic.dashboard.routeTo
import io.infinitic.dashboard.slideovers.Slideover
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kweb.Element
import kweb.ElementCreator
import kweb.div
import kweb.h2
import kweb.new
import kweb.p
import kweb.state.KVal
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
    override val url = "/infra"

    lateinit var job: Job

    private val infraTasksState = KVar(InfraTasksState())
    private val infraWorkflowsState = KVar(InfraWorkflowsState())

    private var selectionType = InfraType.TASK
    private val selectionTitle = KVal("Error!")
    private val selectionNames = KVar(JobNames())

    private val slideover = Slideover(selectionTitle, selectionNames) {
        p().classes("text-sm font-medium text-gray-900").text(lastUpdated(it.value.lastUpdated))
        p().classes("mt-7 text-sm text-gray-500").new {
            element("pre").text(it.value.text)
        }
    }

    init {
        // this listener ensures that the slideover appear/disappear with right content
        infraTasksState.addListener { old, new ->
            if (selectionType == InfraType.TASK) {
                when (new.taskNames.request) {
                    is Failed -> {
                        selectionNames.value = new.taskNames
                        if (old.taskNames.request !is Failed) {
                            slideover.open()
                        }
                    }
                    else -> slideover.close()
                }
            }
        }

        // this listener ensures that the slideover appear/disappear with right content
        infraWorkflowsState.addListener { old, new ->
            if (selectionType == InfraType.WORKFLOW) {
                when (new.workflowNames.request) {
                    is Failed -> {
                        selectionNames.value = new.workflowNames
                        if (old.workflowNames.request !is Failed) {
                            slideover.open()
                        }
                    }
                    else -> slideover.close()
                }
            }
        }
    }

    override fun onEnter() {
        if (! this::job.isInitialized || job.isCancelled) {
            job = GlobalScope.launch {
                // update of task names every 30 seconds
                infraTasksState.update(this)
                // shift the updates
                delay(2000)
                // update of workflow names every 30 seconds
                infraWorkflowsState.update(this)
            }
        }
    }

    override fun onLeave() {
        if (this::job.isInitialized) {
            job.cancel()
        }
        slideover.close()
    }

    override fun render(creator: ElementCreator<Element>) = with(creator) {
        // PAGE HEADER
        div().classes("bg-white shadow py-8").new {
            div().classes("max-w-7xl mx-auto px-4 sm:px-6 md:px-8").new {
                div().classes("lg:flex lg:items-center lg:justify-between").new {
                    div().classes("flex-1 min-w-0").new {
                        // title
                        h2().classes("mt-2 text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate")
                            .text(InfraMenu.title)
                    }
                }
            }
        }

        // WORKFLOWS
        val workflowsLastUpdated = infraWorkflowsState.property(InfraWorkflowsState::lastUpdated)

        div().classes("pt-8 pb-8").new {
            div().classes("max-w-7xl mx-auto sm:px-6 md:px-8").new {
                // Workflows header
                displayJobSectionHeader("Workflows", workflowsLastUpdated)

                p().classes("mt-7 text-sm text-gray-500").text(
                    """
                        Here is the list of your workflows, based on existing topics found in your Pulsar cluster.
                        Click on a row for a complete view of the topics used during the execution of a workflow.
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
                                                                .text("Executors Msg Rate Out")
                                                        }
                                                    }
                                                    tbody().new {
                                                        when (val request = state.workflowNames.request) {
                                                            is Loading -> displayNamesLoading()
                                                            is Failed -> displayNamesError(state.workflowNames, InfraType.WORKFLOW)
                                                            is Completed -> request.result.forEach {
                                                                when (val request = state.workflowStats[it]!!.request) {
                                                                    is Loading -> displayExecutorLoading(it, InfraType.WORKFLOW)
                                                                    is Failed -> displayExecutorError(it, InfraType.WORKFLOW)
                                                                    is Completed -> displayExecutorStats(it, request.result, InfraType.WORKFLOW)
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
        val tasksLastUpdated = infraTasksState.property(InfraTasksState::lastUpdated)

        div().classes("pt-8 pb-8").new {
            div().classes("max-w-7xl mx-auto sm:px-6 md:px-8").new {
                // Tasks header
                displayJobSectionHeader("Tasks", tasksLastUpdated)

                p().classes("mt-7 text-sm text-gray-500").text(
                    """
                        Here is the list of your tasks, based on existing topics found in your Pulsar cluster.
                        Click on a row for a complete view of the topics used during the execution of a task.
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
                                                                .text("Executors Msg Rate Out")
                                                        }
                                                    }
                                                    tbody().new {
                                                        when (val request = state.taskNames.request) {
                                                            is Loading -> displayNamesLoading()
                                                            is Failed -> displayNamesError(state.taskNames, InfraType.TASK)
                                                            is Completed -> request.result.forEach {
                                                                when (val request = state.taskStats[it]!!.request) {
                                                                    is Loading -> displayExecutorLoading(it, InfraType.TASK)
                                                                    is Failed -> displayExecutorError(it, InfraType.TASK)
                                                                    is Completed -> displayExecutorStats(it, request.result, InfraType.TASK)
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

        slideover.render(this)
    }

    private fun ElementCreator<Element>.displayNamesLoading() {
        tr().classes("bg-white").new {
            td().setAttribute("colspan", "4").classes("px-6 py-4 text-sm font-medium text-gray-900")
                .text("Loading...")
        }
    }

    private fun ElementCreator<Element>.displayNamesError(names: JobNames, type: InfraType) {
        val row = tr()
        row.classes("bg-white cursor-pointer hover:bg-gray-50").new {
            td().setAttribute("colspan", "4").classes("px-6 py-4 text-sm font-medium text-gray-900")
                .text("Error!")
        }
        row.on.click {
            selectionType = type
            selectionNames.value = names

            slideover.open()
        }
    }

    private fun ElementCreator<Element>.displayExecutorLoading(name: String, type: InfraType) {
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
            when (type) {
                InfraType.TASK -> browser.routeTo(InfraTaskPanel.from(name))
                InfraType.WORKFLOW -> browser.routeTo(InfraWorkflowPanel.from(name))
            }
        }
    }

    private fun ElementCreator<Element>.displayExecutorError(name: String, type: InfraType) {
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
            when (type) {
                InfraType.TASK -> browser.routeTo(InfraTaskPanel.from(name))
                InfraType.WORKFLOW -> browser.routeTo(InfraWorkflowPanel.from(name))
            }
        }
    }
    private fun ElementCreator<Element>.displayExecutorStats(name: String, stats: PartitionedTopicStats, type: InfraType) {
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
                when (type) {
                    InfraType.TASK -> browser.routeTo(InfraTaskPanel.from(name))
                    InfraType.WORKFLOW -> browser.routeTo(InfraWorkflowPanel.from(name))
                }
            }
        }
    }
}

internal fun lastUpdated(instant: Instant) = "Last updated at ${SimpleDateFormat("HH:mm:ss").format(Date.from(instant))}"
