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

package io.infinitic.dashboard

import io.infinitic.dashboard.components.Pulsar
import io.infinitic.dashboard.components.app
import io.infinitic.dashboard.components.menus.MenuItem
import io.infinitic.dashboard.components.settingsPanel
import io.infinitic.dashboard.components.tasksPanel
import io.infinitic.dashboard.components.workflowsPanel
import io.infinitic.dashboard.plugins.tailwind.tailwindPlugin
import io.infinitic.pulsar.PulsarInfiniticAdmin
import io.infinitic.pulsar.topics.WorkflowTaskTopic
import io.infinitic.pulsar.topics.WorkflowTopic
import kweb.ElementCreator
import kweb.Kweb
import kweb.route
import kweb.state.KVar
import kweb.state.render
import org.apache.pulsar.client.admin.Topics
import org.apache.pulsar.common.policies.data.PartitionedTopicStats

val appState = KVar(AppState(MenuItem.WORKFLOWS))

fun main() {

    class WorkflowStats(val name: String, topics: Topics) {

        val allStats = mutableMapOf<String, PartitionedTopicStats>()

//        val name: String by stats
//
//        val salutedName: String by lazy {
//            "Hi $name"
//        }

        private fun updateTopicStats(topic: String) {
            allStats[topic] = Pulsar.topics.getPartitionedStats(topic, true, true, true)
        }

        fun update() {
            // new-workflow engine
            var topic = Pulsar.topicName.of(WorkflowTopic.ENGINE_NEW, name)
            var stats = Pulsar.topics.getPartitionedStats(topic, true, true, true)

            // existing-workflow engine
            topic = Pulsar.topicName.of(WorkflowTopic.ENGINE_EXISTING, name)
            stats = Pulsar.topics.getPartitionedStats(topic, true, true, true)

            // workflow-delay engine
            topic = Pulsar.topicName.of(WorkflowTopic.DELAYS, name)
            stats = Pulsar.topics.getPartitionedStats(topic, true, true, true)

            // new-workflow-tag engine
            topic = Pulsar.topicName.of(WorkflowTopic.TAG_NEW, name)
            stats = Pulsar.topics.getPartitionedStats(topic, true, true, true)

            // existing-workflow-tag engine
            topic = Pulsar.topicName.of(WorkflowTopic.TAG_EXISTING, name)
            stats = Pulsar.topics.getPartitionedStats(topic, true, true, true)

            // new-workflowTask engine
            topic = Pulsar.topicName.of(WorkflowTaskTopic.ENGINE_NEW, name)
            stats = Pulsar.topics.getPartitionedStats(topic, true, true, true)

            // existing-workflowTask engine
            topic = Pulsar.topicName.of(WorkflowTaskTopic.ENGINE_EXISTING, name)
            stats = Pulsar.topics.getPartitionedStats(topic, true, true, true)

            // workflow executors
            topic = Pulsar.topicName.of(WorkflowTaskTopic.EXECUTORS, name)
            stats = Pulsar.topics.getPartitionedStats(topic, true, true, true)
        }
    }

    Kweb(port = 16097, plugins = listOf(tailwindPlugin)) {
        doc.body {
            route {
                path("/workflows") { params ->
                    appState.value = appState.value.copy(
                        menuItem = MenuItem.WORKFLOWS
                    )
                }
                path("/tasks") { params ->
                    appState.value = appState.value.copy(
                        menuItem = MenuItem.TASKS
                    )
                }
                path("/pulsar") { params ->
                    appState.value = appState.value.copy(
                        menuItem = MenuItem.PULSAR,
                        pulsarState = Pulsar.State()
                    )
                }
                path("/pulsar/w/{name}") { params ->
                    appState.value = appState.value.copy(
                        menuItem = MenuItem.PULSAR,
                        pulsarState = Pulsar.State(
                            Pulsar.DetailType.WORKFLOW,
                            params.getValue("name")
                        )
                    )
                }
                path("/settings") { params ->
                    appState.value = AppState(MenuItem.SETTINGS)
                }
            }

            app()
        }
    }
}

data class AppState(
    val menuItem: MenuItem,
    val isMobileMenuVisible: Boolean = false,
    val pulsarState: Pulsar.State = Pulsar.State()
)

fun ElementCreator<*>.routerView() {
    render(appState) { s ->
        when (s.menuItem) {
            MenuItem.WORKFLOWS -> workflowsPanel()
            MenuItem.TASKS -> tasksPanel()
            MenuItem.PULSAR -> Pulsar.render(this, s.pulsarState)
            MenuItem.SETTINGS -> settingsPanel()
        }
    }
}

fun ElementCreator<*>.routeTo(item: MenuItem) {
    browser.url.value = when (item) {
        MenuItem.WORKFLOWS -> "/workflows"
        MenuItem.TASKS -> "/tasks"
        MenuItem.PULSAR -> "/pulsar"
        MenuItem.SETTINGS -> "/settings"
    }
}

object Infinitic {
    val admin by lazy {
        PulsarInfiniticAdmin.fromConfigFile("infinitic-dashboard/infinitic.yml")
    }
}
