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

package io.infinitic.inMemory.workers

import io.infinitic.client.Client
import io.infinitic.client.worker.startClientWorker
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.inMemory.transport.InMemoryOutput
import io.infinitic.metrics.global.engine.storage.MetricsGlobalStateStorage
import io.infinitic.metrics.global.engine.worker.startMetricsGlobalEngine
import io.infinitic.metrics.perName.engine.storage.MetricsPerNameStateStorage
import io.infinitic.metrics.perName.engine.worker.startMetricsPerNameEngine
import io.infinitic.tags.tasks.storage.TaskTagStorage
import io.infinitic.tags.tasks.worker.startTaskTagEngine
import io.infinitic.tags.workflows.storage.WorkflowTagStorage
import io.infinitic.tags.workflows.worker.startWorkflowTagEngine
import io.infinitic.tasks.TaskExecutorRegister
import io.infinitic.tasks.engine.storage.TaskStateStorage
import io.infinitic.tasks.engine.worker.startTaskEngine
import io.infinitic.tasks.executor.worker.startTaskExecutor
import io.infinitic.workflows.engine.storage.WorkflowStateStorage
import io.infinitic.workflows.engine.worker.startWorkflowEngine
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun CoroutineScope.startInMemory(
    taskExecutorRegister: TaskExecutorRegister,
    client: Client,
    output: InMemoryOutput,
    taskTagStorage: TaskTagStorage,
    taskStorage: TaskStateStorage,
    workflowTagStorage: WorkflowTagStorage,
    workflowStorage: WorkflowStateStorage,
    metricsPerNameStorage: MetricsPerNameStateStorage,
    metricsGlobalStorage: MetricsGlobalStateStorage,
    logFn: (_: MessageToProcess<*>) -> Unit
) = launch {

    launch(CoroutineName("logger")) {
        for (messageToProcess in output.logChannel) {
            logFn(messageToProcess)
        }
    }

    startClientWorker(
        client,
        inputChannel = output.clientEventsChannel,
        outputChannel = output.logChannel,
    )

    startTaskTagEngine(
        "in-memory-task-tag-engine",
        taskTagStorage,
        eventsInputChannel = output.taskTagEventsChannel,
        eventsOutputChannel = output.logChannel,
        commandsInputChannel = output.taskTagCommandsChannel,
        commandsOutputChannel = output.logChannel,
        output.sendCommandsToTaskEngine,
        output.sendEventsToClient
    )

    startTaskEngine(
        "in-memory-task-engine",
        taskStorage,
        eventsInputChannel = output.taskEventsChannel,
        eventsOutputChannel = output.logChannel,
        commandsInputChannel = output.taskCommandsChannel,
        commandsOutputChannel = output.logChannel,
        output.sendEventsToClient,
        output.sendEventsToTaskTagEngine,
        output.sendToTaskEngineAfter,
        output.sendEventsToWorkflowEngine,
        output.sendToTaskExecutors,
        output.sendToMetricsPerName
    )

    startWorkflowTagEngine(
        "in-memory-workflow-tag-engine",
        workflowTagStorage,
        eventsInputChannel = output.workflowTagEventsChannel,
        eventsOutputChannel = output.logChannel,
        commandsInputChannel = output.workflowTagCommandsChannel,
        commandsOutputChannel = output.logChannel,
        output.sendCommandsToWorkflowEngine,
        output.sendEventsToClient
    )

    startWorkflowEngine(
        "in-memory-workflow-engine",
        workflowStorage,
        eventsInputChannel = output.workflowEventsChannel,
        eventsOutputChannel = output.logChannel,
        commandsInputChannel = output.workflowCommandsChannel,
        commandsOutputChannel = output.logChannel,
        output.sendEventsToClient,
        output.sendCommandsToTaskTagEngine,
        output.sendCommandsToTaskEngine,
        output.sendEventsToWorkflowTagEngine,
        output.sendEventsToWorkflowEngine,
        output.sendToWorkflowEngineAfter
    )

    repeat(10) {
        @Suppress("MoveLambdaOutsideParentheses")
        startTaskExecutor(
            "in-memory-task-executor",
            taskExecutorRegister,
            inputChannel = output.executorChannel,
            outputChannel = output.logChannel,
            output.sendEventsToTaskEngine,
            { client }
        )
    }

    startMetricsPerNameEngine(
        "in-memory-metrics-per-name-engine",
        metricsPerNameStorage,
        inputChannel = output.metricsPerNameChannel,
        outputChannel = output.logChannel,
        output.sendToMetricsGlobal
    )

    startMetricsGlobalEngine(
        "in-memory-metrics-global-engine",
        metricsGlobalStorage,
        inputChannel = output.metricsGlobalChannel,
        outputChannel = output.logChannel
    )
}
