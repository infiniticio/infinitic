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

package io.infinitic.pulsar.workers

import io.infinitic.common.monitoring.global.messages.MonitoringGlobalMessage
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEngineMessage
import io.infinitic.common.storage.keyValue.KeyValueStorage
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.workers.MessageToProcess
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.tasks.executor.register.TaskExecutorRegister
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.PulsarClient
import java.lang.RuntimeException

private const val N_WORKERS = 100

fun CoroutineScope.startPulsar(
    taskExecutorRegister: TaskExecutorRegister,
    pulsarClient: PulsarClient,
    keyValueStorage: KeyValueStorage
) = launch(Dispatchers.IO) {

    val logChannel = Channel<MessageToProcess<Any>>()

    launch(CoroutineName("logger")) {
        for (messageToProcess in logChannel) {
            when (val message = messageToProcess.message) {
                is MonitoringGlobalMessage ->
                    println("Monitoring Global  : $message")
                is MonitoringPerNameEngineMessage ->
                    println("Monitoring Per Name: $message")
                is TaskExecutorMessage ->
                    println("Task Executor      : $message")
                is TaskEngineMessage ->
                    println("Task engine        : $message")
                is WorkflowEngineMessage ->
                    println("Workflow engine    : $message")
                else -> throw RuntimeException("Unknown messageToProcess type: $messageToProcess")
            }
        }
    }

    startPulsarMonitoringGlobalWorker(
        pulsarClient,
        keyValueStorage,
        logChannel
    )

    startPulsarMonitoringPerNameWorker(
        pulsarClient,
        keyValueStorage,
        logChannel
    )

    startPulsarTaskExecutorWorker(
        pulsarClient,
        taskExecutorRegister,
        logChannel,
        N_WORKERS,
    )

    startPulsarTaskEngineWorker(
        pulsarClient,
        keyValueStorage,
        logChannel
    )

    startPulsarWorkflowEngineWorker(
        pulsarClient,
        keyValueStorage,
        logChannel
    )
}
