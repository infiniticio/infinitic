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

import io.infinitic.dashboard.modals.ErrorModal
import io.infinitic.dashboard.panels.pulsar.PulsarPanel
import io.infinitic.dashboard.panels.pulsar.task.PulsarTaskPanel
import io.infinitic.dashboard.panels.pulsar.workflow.PulsarWorkflowPanel
import io.infinitic.dashboard.panels.settings.SettingsPanel
import io.infinitic.dashboard.panels.tasks.TasksPanel
import io.infinitic.dashboard.panels.workflows.WorkflowsPanel
import io.infinitic.dashboard.plugins.images.imagesPlugin
import io.infinitic.dashboard.plugins.tailwind.tailwindPlugin
import io.infinitic.pulsar.PulsarInfiniticAdmin
import kweb.Kweb
import kweb.WebBrowser
import kweb.route

fun main() {
    Kweb(port = 16097, plugins = listOf(tailwindPlugin, imagesPlugin)) {
        doc.body {
            lateinit var panel: Panel

            route {
                path("/workflows") {
                    panel = WorkflowsPanel
                }
                path("/tasks") {
                    panel = TasksPanel
                }
                path("/pulsar") {
                    panel = PulsarPanel
                }
                path("/pulsar/t/{name}") {
                    panel = PulsarTaskPanel.from(it.getValue("name").value)
                }
                path("/pulsar/w/{name}") {
                    panel = PulsarWorkflowPanel.from(it.getValue("name").value)
                }
                path("/settings") {
                    panel = SettingsPanel
                }
            }

            // entering hook
            panel.onEnter()
            // selecting panel
            AppPanel.appState.selectPanel(panel)
            // rendering the app
            AppPanel.render(this)

            ErrorModal.render(this)
        }
    }
}

fun WebBrowser.routeTo(to: Panel) {
    val from = AppPanel.appState.value.panel
    // leaving / entering hook
    if (from != to) {
        from.onLeave()
        to.onEnter()
    }
    // selecting panel - this will trigger a display update
    AppPanel.appState.selectPanel(to)
    // update current url
    url.value = to.route
}

object Infinitic {
    val admin by lazy {
        PulsarInfiniticAdmin.fromConfigFile("infinitic-dashboard/infinitic.yml")
    }
    val topicName by lazy { admin.topicNamer }
    val topics by lazy { admin.pulsarAdmin.topics() }
}
