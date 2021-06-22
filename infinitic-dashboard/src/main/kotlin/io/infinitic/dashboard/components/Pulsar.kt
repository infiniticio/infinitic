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

package io.infinitic.dashboard.components

import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.dashboard.Infinitic
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import io.infinitic.pulsar.topics.WorkflowTopic
import kweb.Element
import kweb.ElementCreator
import kweb.a
import kweb.div
import kweb.h3
import kweb.new
import kweb.p
import kweb.span
import kweb.state.KVar
import kweb.table
import kweb.tbody
import kweb.td
import kweb.th
import kweb.thead
import kweb.tr
import org.apache.pulsar.common.policies.data.PartitionedTopicStats

object Pulsar {
    data class State(
        var detailType: DetailType? = null,
        var detailName: KVar<String>? = null
    )

    enum class DetailType {
        TASK, WORKFLOW
    }

    private val workflows by lazy { Infinitic.admin.workflows }
    private val tasks by lazy { Infinitic.admin.tasks }

    val topicName = Infinitic.admin.topicNamer
    val topics = Infinitic.admin.pulsarAdmin.topics()

    fun render(creator: ElementCreator<*>, state: State) = with(creator) {
        when (state.detailType) {
            DetailType.TASK -> renderDetailTask(state)
            DetailType.WORKFLOW -> renderDetailWorkflow(state)
            else -> renderMain()
        }
    }

    private fun ElementCreator<Element>.renderMain() {
        div().classes("py-6").new {
            div().classes("max-w-7xl mx-auto px-4 sm:px-6 md:px-8").new {
                div().classes("pb-5 border-b border-gray-200").new {
                    h3().classes("text-lg leading-6 font-medium text-gray-900")
                        .text("Workflows")
                    p().classes("mt-2 max-w-4xl text-sm text-gray-500").text(
                        """
                        Workcation is a property rental website. Etiam ullamcorper massa viverra consequat,
                        consectetur id nulla tempus. Fringilla egestas justo massa purus sagittis malesuada.
                    """
                    )
                }

                workflows.forEach {
                    div().classes("py-2").new {
                        div().classes("max-w-none mx-auto").new {
                            div().classes("bg-white overflow-hidden sm:rounded-lg sm:shadow").new {
                                div().classes("bg-white px-4 py-5 border-b border-gray-200 sm:px-6").new {
                                    h3().classes("text-lg leading-6 font-medium text-gray-900").text(it)
                                }
                                div().classes("flex flex-col").new {
                                    div().classes("-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8").new {
                                        div().classes("py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8").new {
                                            div().classes("shadow overflow-hidden border-b border-gray-200 sm:rounded-lg").new {
                                                table().classes("min-w-full divide-y divide-gray-200").new {
                                                    thead().classes("bg-gray-50").new {
                                                        tr().new {
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("Topic")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("NbConsumers")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("msgBacklog")
                                                            th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                                .setAttribute("scope", "col")
                                                                .text("msgRateOut")
                                                            th().classes("relative px-6 py-3")
                                                                .setAttribute("scope", "col")
                                                                .new {
                                                                    span().classes("sr-only").text("Detail")
                                                                }
                                                        }
                                                    }
                                                    tbody().new {
                                                        val workflowName = WorkflowName(it)

                                                        // new-workflow engine
                                                        var topic = topicName.of(WorkflowTopic.ENGINE_NEW, it)
                                                        var stats = topics.getPartitionedStats(topic, true, true, true)
                                                        displayStats(topic, stats, true)

                                                        // existing-workflow engine
                                                        topic = topicName.of(WorkflowTopic.ENGINE_EXISTING, it)
                                                        stats = topics.getPartitionedStats(topic, true, true, true)
                                                        displayStats(topic, stats, false)

                                                        // workflow-delay engine
                                                        topic = topicName.of(WorkflowTopic.DELAYS, it)
                                                        stats = topics.getPartitionedStats(topic, true, true, true)
                                                        displayStats(topic, stats, true)

                                                        // new-workflow-tag engine
                                                        topic = topicName.of(WorkflowTopic.TAG_NEW, it)
                                                        stats = topics.getPartitionedStats(topic, true, true, true)
                                                        displayStats(topic, stats, false)

                                                        // existing-workflow-tag engine
                                                        topic = topicName.of(WorkflowTopic.TAG_EXISTING, it)
                                                        stats = topics.getPartitionedStats(topic, true, true, true)
                                                        displayStats(topic, stats, true)

                                                        // new-workflowTask engine
                                                        topic = topicName.of(WorkflowTaskTopic.ENGINE_NEW, it)
                                                        stats = topics.getPartitionedStats(topic, true, true, true)
                                                        displayStats(topic, stats, false)

                                                        // existing-workflowTask engine
                                                        topic = topicName.of(WorkflowTaskTopic.ENGINE_EXISTING, it)
                                                        stats = topics.getPartitionedStats(topic, true, true, true)
                                                        displayStats(topic, stats, true)

                                                        // workflow executors
                                                        topic = topicName.of(WorkflowTaskTopic.EXECUTORS, it)
                                                        stats = topics.getPartitionedStats(topic, true, true, true)
                                                        displayStats(topic, stats, false)
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

                div().classes("pb-5 border-b border-gray-200").new {
                    h3().classes("text-lg leading-6 font-medium text-gray-900")
                        .text("Tasks")
                    p().classes("mt-2 max-w-4xl text-sm text-gray-500").text(
                        """
                        Workcation is a property rental website. Etiam ullamcorper massa viverra consequat,
                        consectetur id nulla tempus. Fringilla egestas justo massa purus sagittis malesuada.
                    """
                    )
                }

                tasks.forEach {
                    div().classes("py-2").new {
                        div().classes("max-w-none mx-auto").new {
                            div().classes("bg-white overflow-hidden sm:rounded-lg sm:shadow").new {
                                div().classes("bg-white px-4 py-5 border-b border-gray-200 sm:px-6").new {
                                    h3().classes("text-lg leading-6 font-medium text-gray-900").text(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ElementCreator<Element>.renderDetailWorkflow(state: State) {
        div().classes("py-6").new {
            div().classes("max-w-7xl mx-auto px-4 sm:px-6 md:px-8").new {
                div().classes("pb-5 border-b border-gray-200").new {
                    h3().classes("text-lg leading-6 font-medium text-gray-900")
                        .text(state.detailName!!)
                    p().classes("mt-2 max-w-4xl text-sm text-gray-500").text(
                        """
                        Workcation is a property rental website. Etiam ullamcorper massa viverra consequat,
                        consectetur id nulla tempus. Fringilla egestas justo massa purus sagittis malesuada.
                    """
                    )
                }

                div().classes("py-2").new {
                    div().classes("max-w-none mx-auto").new {
                        div().classes("bg-white overflow-hidden sm:rounded-lg sm:shadow").new {
                            div().classes("flex flex-col").new {
                                div().classes("-my-2 overflow-x-auto sm:-mx-6 lg:-mx-8").new {
                                    div().classes("py-2 align-middle inline-block min-w-full sm:px-6 lg:px-8").new {
                                        div().classes("shadow overflow-hidden border-b border-gray-200 sm:rounded-lg").new {
                                            table().classes("min-w-full divide-y divide-gray-200").new {
                                                thead().classes("bg-gray-50").new {
                                                    tr().new {
                                                        th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                            .setAttribute("scope", "col")
                                                            .text("Topic")
                                                        th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                            .setAttribute("scope", "col")
                                                            .text("NbConsumers")
                                                        th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                            .setAttribute("scope", "col")
                                                            .text("msgBacklog")
                                                        th().classes("px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider")
                                                            .setAttribute("scope", "col")
                                                            .text("msgRateOut")
                                                    }
                                                }
                                                tbody().new {
                                                    val workflowName = state.detailName!!.value
                                                    // workflow engine commands
                                                    var topic = topicName.of(WorkflowTopic.ENGINE_NEW, workflowName)
                                                    var stats = topics.getPartitionedStats(topic, true, true, true)
                                                    displayStatsRow(topic, stats, true)

                                                    // workflow engine events
                                                    topic = topicName.of(WorkflowTopic.ENGINE_EXISTING, workflowName)
                                                    stats = topics.getPartitionedStats(topic, true, true, true)
                                                    displayStatsRow(topic, stats, false)

                                                    // workflow delay engine
                                                    topic = topicName.of(WorkflowTopic.DELAYS, workflowName)
                                                    stats = topics.getPartitionedStats(topic, true, true, true)
                                                    displayStatsRow(topic, stats, true)

                                                    // tag workflow engine commands
                                                    topic = topicName.of(WorkflowTopic.TAG_NEW, workflowName)
                                                    stats = topics.getPartitionedStats(topic, true, true, true)
                                                    displayStatsRow(topic, stats, false)

                                                    // tag workflow engine events
                                                    topic = topicName.of(WorkflowTopic.TAG_EXISTING, workflowName)
                                                    stats = topics.getPartitionedStats(topic, true, true, true)
                                                    displayStatsRow(topic, stats, true)

                                                    // workflow tasks engine commands
                                                    topic = topicName.of(WorkflowTaskTopic.ENGINE_NEW, workflowName)
                                                    stats = topics.getPartitionedStats(topic, true, true, true)
                                                    displayStatsRow(topic, stats, false)

                                                    // workflow tasks engine events
                                                    topic = topicName.of(WorkflowTaskTopic.ENGINE_EXISTING, workflowName)
                                                    stats = topics.getPartitionedStats(topic, true, true, true)
                                                    displayStatsRow(topic, stats, true)

                                                    // workflow task executors
                                                    topic = topicName.of(WorkflowTaskTopic.EXECUTORS, workflowName)
                                                    stats = topics.getPartitionedStats(topic, true, true, true)
                                                    displayStatsRow(topic, stats, false)
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

    private fun ElementCreator<Element>.renderDetailTask(state: State) {
    }

    private fun ElementCreator<Element>.displayStatsRow(topic: String, stats: PartitionedTopicStats, isOdd: Boolean) {
        stats.subscriptions.map {
            tr().classes(if (isOdd) "bg-white" else "bg-gray-50").new {
                td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                    .text(topic)
                td().classes("px-6 py-4 text-sm text-gray-500")
                    .text(it.value.consumers.size.toString())
                td().classes("px-6 py-4 text-sm text-gray-500")
                    .text(it.value.msgBacklog.toString())
                td().classes("px-6 py-4 text-sm text-gray-500")
                    .text(it.value.msgRateOut.toString())
            }
        }
    }

    private fun ElementCreator<Element>.displayStats(topic: String, stats: PartitionedTopicStats, isOdd: Boolean) {
        stats.subscriptions.map {
            tr().classes(if (isOdd) "bg-white" else "bg-gray-50").new {
                td().classes("px-6 py-4 text-sm font-medium text-gray-900")
                    .text(topic)
                td().classes("px-6 py-4 text-sm text-gray-500")
                    .text(it.value.consumers.size.toString())
                td().classes("px-6 py-4 text-sm text-gray-500")
                    .text(it.value.msgBacklog.toString())
                td().classes("px-6 py-4 text-sm text-gray-500")
                    .text(it.value.msgRateOut.toString())
                td().classes("px-6 py-4 text-right text-sm font-medium")
                    .new {
                        a().classes("text-indigo-600 hover:text-indigo-900")
                            .setAttribute("href", "#")
                            .text("Detail")
                    }
            }
        }
    }
}
