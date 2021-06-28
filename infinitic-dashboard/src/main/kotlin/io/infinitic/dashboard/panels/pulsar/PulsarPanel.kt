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

package io.infinitic.dashboard.panels.pulsar

import io.infinitic.dashboard.Infinitic
import io.infinitic.dashboard.Panel
import io.infinitic.dashboard.menus.PulsarMenu
import io.infinitic.dashboard.panels.pulsar.task.PulsarTaskPanel
import io.infinitic.dashboard.routeTo
import io.infinitic.pulsar.topics.TaskTopic
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kweb.Element
import kweb.ElementCreator
import kweb.div
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

object PulsarPanel : Panel() {
    override val menu = PulsarMenu
    override val route = "/pulsar"

    private val tasksState = KVar(PulsarTasksState())
    private val workflowsState = KVar(PulsarWorkflowsState())

    lateinit var job: Job

    override fun onEnter() {
        if (! this::job.isInitialized || job.isCancelled) {
            job = GlobalScope.launch {
                // update of task names every 30 seconds
                launch {
                    while (isActive) {
                        val delayJob = launch { delay(30000) }

                        try {
                            println("UPDATING TASKs NAMES")
                            // request Pulsar
                            val taskNames = Infinitic.admin.tasks
                            val taskExecutorsStats = tasksState.value.taskExecutorsStats
                            tasksState.value = tasksState.value.copy(
                                taskNames = taskNames,
                                taskExecutorsStats = taskNames.associateWith {
                                    if (taskExecutorsStats.containsKey(it)) taskExecutorsStats[it] else null
                                }
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // update task stats every 3 seconds
                        val updateJob = launch {
                            while (isActive) {
                                val delay = launch { delay(3000) }
                                var taskExecutorsStats = tasksState.value.taskExecutorsStats
                                tasksState.value.taskNames?.map {
                                    println("updating stats for $it")
                                    val topic = Infinitic.topicName.of(TaskTopic.EXECUTORS, it)
                                    try {
                                        val stats = Infinitic.topics.getPartitionedStats(topic, true, true, true)
                                        taskExecutorsStats = taskExecutorsStats.plus(it to stats)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                delay.join()
                                tasksState.value = tasksState.value.copy(taskExecutorsStats = taskExecutorsStats)
                            }
                        }
                        // wait for at least 30s
                        delayJob.join()
                        // cancel updateJob before updating taskNames
                        updateJob.cancelAndJoin()
                    }
                }

                // update of workflow names every 30 seconds
                launch {
                    while (isActive) {
                        val delayJob = launch { delay(30000) }

                        // get set of workflow names
                        try {
                            println("UPDATING TASKs NAMES")
                            // request Pulsar
                            val workflowNames = Infinitic.admin.workflows
                            val workflowTaskExecutorsStats = workflowsState.value.workflowTaskExecutorsStats
                            workflowsState.value = workflowsState.value.copy(
                                workflowNames = workflowNames,
                                workflowTaskExecutorsStats = workflowNames.associateWith {
                                    if (workflowTaskExecutorsStats.containsKey(it)) workflowTaskExecutorsStats[it] else null
                                }
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // update workflow stats every 3 seconds
                        val updateJob = launch {
                            while (isActive) {
                                val delay = launch { delay(3000) }
                                var workflowTaskExecutorsStats = workflowsState.value.workflowTaskExecutorsStats
                                workflowsState.value.workflowNames?.map {
                                    println("updating stats for $it")
                                    val topic = Infinitic.topicName.of(WorkflowTaskTopic.EXECUTORS, it)
                                    try {
                                        val stats = Infinitic.topics.getPartitionedStats(topic, true, true, true)
                                        workflowTaskExecutorsStats = workflowTaskExecutorsStats.plus(it to stats)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                delay.join()
                                workflowsState.value = workflowsState.value.copy(workflowTaskExecutorsStats = workflowTaskExecutorsStats)
                            }
                        }
                        // wait for at least 30s
                        delayJob.join()
                        // cancel updateJob before updating workflowNames
                        updateJob.cancelAndJoin()
                    }
                }
            }
        }
    }

    override fun onLeave() {
        if (this::job.isInitialized) {
            job.cancel()
        }
    }

    override fun render(creator: ElementCreator<Element>) = with(creator) {
        // WORKFLOWS
        div().classes("pt-6").new {
            div().classes("max-w-7xl mx-auto px-4 sm:px-6 md:px-8").new {
                // Workflows header
                h3().classes("text-lg leading-6 font-medium text-gray-900").text("Workflows")
                p().classes("mt-2 max-w-4xl text-sm text-gray-500").text(
                    """
                        Here is the list of the workflows processed in Pulsar based on existence of topics.
                        For a quick check you can see here the number of executors and their msg rate.
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
                                            render(workflowsState) { state ->
                                                table().classes("min-w-full divide-y divide-gray-200").new {
                                                    thead().classes("bg-gray-50").new {
                                                        tr().new {
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Name")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Nb Executors")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Msg Rate Out")
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
        div().classes("pt-6").new {
            div().classes("max-w-7xl mx-auto px-4 sm:px-6 md:px-8").new {
                // Workflows header
                h3().classes("text-lg leading-6 font-medium text-gray-900").text("Tasks")
                p().classes("mt-2 max-w-4xl text-sm text-gray-500").text(
                    """
                        Here is the list of the tasks processed in Pulsar based on existence of topics.
                        For a quick check you can see here the number of executors and their msg rate.
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
                                            render(tasksState) { state ->
                                                table().classes("min-w-full divide-y divide-gray-200").new {
                                                    thead().classes("bg-gray-50").new {
                                                        tr().new {
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Name")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Nb Executors")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Msg Rate Out")
                                                        }
                                                    }
                                                    tbody().new {
                                                        when (val taskNames = state.taskNames) {
                                                            null -> displayLoading()
                                                            else -> taskNames.forEach {
                                                                when (val stats = state.taskExecutorsStats[it]) {
                                                                    null -> displayExecutorLoading(it, isTask = true)
                                                                    else -> displayExecutorStats(it, stats, isTask = true)
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

    private fun ElementCreator<Element>.displayExecutorLoading(name: String, isTask: Boolean) {
        val row = tr()
        row.classes("bg-white cursor-pointer hover:bg-gray-50").new {
            td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                .text(name)
            td().classes("px-6 py-4 text-sm text-gray-500")
                .text("loading...")
            td().classes("px-6 py-4 text-sm text-gray-500")
                .text("loading...")
        }
        row.on.click {
            browser.routeTo(PulsarTaskPanel(name))
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
                    .text(it.value.msgRateOut.toString())
            }
            row.on.click {
                browser.routeTo(PulsarTaskPanel(name))
            }
        }
    }
}
