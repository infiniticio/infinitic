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

package io.infinitic.workflows.engine.worker

import io.infinitic.workflows.engine.WorkflowEngine
import io.infinitic.workflows.engine.storage.events.WorkflowEventStorage
import io.infinitic.workflows.engine.storage.states.WorkflowStateStorage
import io.infinitic.workflows.engine.transport.WorkflowEngineInputChannels
import io.infinitic.workflows.engine.transport.WorkflowEngineMessageToProcess
import io.infinitic.workflows.engine.transport.WorkflowEngineOutput
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

fun <T : WorkflowEngineMessageToProcess> CoroutineScope.startWorkflowEngine(
    coroutineName: String,
    workflowStateStorage: WorkflowStateStorage,
    workflowEventStorage: WorkflowEventStorage,
    workflowEngineInputChannels: WorkflowEngineInputChannels<T>,
    workflowEngineOutput: WorkflowEngineOutput
) = launch(CoroutineName(coroutineName)) {

    val workflowEngine = WorkflowEngine(
        workflowStateStorage,
        workflowEventStorage,
        workflowEngineOutput
    )

    val out = workflowEngineInputChannels.workflowResultsChannel
    val events = workflowEngineInputChannels.workflowEventsChannel
    val commands = workflowEngineInputChannels.workflowCommandsChannel

    while (true) {
        select<Unit> {
            events.onReceive {
                try {
                    it.output = workflowEngine.handle(it.message)
                    out.send(it)
                } catch (e: Exception) {
                    it.exception = e
                    out.send(it)
                }
            }
            commands.onReceive {
                try {
                    it.output = workflowEngine.handle(it.message)
                    out.send(it)
                } catch (e: Exception) {
                    it.exception = e
                    out.send(it)
                }
            }
        }
    }
}
