/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.common.transport

import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.topics.Topic
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage

interface InfiniticConsumerAsync : AutoCloseable {

  // name used for logging
  var logName: String?

  fun join()

  suspend fun <S : Message> startConsumerAsync(
    topic: Topic<S>,
    handler: suspend (S, MillisInstant) -> Unit,
    beforeDlq: (suspend (S?, Exception) -> Unit),
    entity: String,
  )

  // Asynchronously start consumers of messages to client
  suspend fun startClientConsumerAsync(
    handler: suspend (ClientMessage, MillisInstant) -> Unit,
    beforeDlq: (suspend (ClientMessage?, Exception) -> Unit),
    clientName: ClientName
  )

  // Asynchronously start consumers of messages to workflow tag
  suspend fun startWorkflowTagConsumerAsync(
    handler: suspend (WorkflowTagMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowTagMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  )

  // Asynchronously start consumers of messages to workflow-cmd
  suspend fun startWorkflowCmdConsumerAsync(
    handler: suspend (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowEngineMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  )

  // Asynchronously start consumers of messages to workflow engine
  suspend fun startWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowEngineMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  )

  // Asynchronously start consumers of delayed messages to workflow engine
  suspend fun startDelayedWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage, MillisInstant) -> Unit,
    beforeDlq: suspend (WorkflowEngineMessage?, Exception) -> Unit,
    workflowName: WorkflowName,
    concurrency: Int
  )

  // Asynchronously start consumers of delayed messages to workflow engine
  suspend fun startWorkflowEventsConsumerAsync(
    handler: suspend (WorkflowEventMessage, MillisInstant) -> Unit,
    beforeDlq: (suspend (WorkflowEventMessage?, Exception) -> Unit),
    workflowName: WorkflowName,
    concurrency: Int
  )

  // Asynchronously start consumers of messages to task tags
  suspend fun startTaskTagConsumerAsync(
    handler: suspend (ServiceTagMessage, MillisInstant) -> Unit,
    beforeDlq: (suspend (ServiceTagMessage?, Exception) -> Unit),
    serviceName: ServiceName,
    concurrency: Int
  )

  // Asynchronously start consumers of messages to task executor
  suspend fun startTaskExecutorConsumerAsync(
    handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: (suspend (ServiceExecutorMessage?, Exception) -> Unit),
    serviceName: ServiceName,
    concurrency: Int
  )


  // Asynchronously start consumers of messages to task events
  suspend fun startTaskEventsConsumerAsync(
    handler: suspend (ServiceEventMessage, MillisInstant) -> Unit,
    beforeDlq: (suspend (ServiceEventMessage?, Exception) -> Unit),
    serviceName: ServiceName,
    concurrency: Int
  )

  // Asynchronously start consumers of delayed messages to task executor
  suspend fun startDelayedTaskExecutorConsumerAsync(
    handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: (suspend (ServiceExecutorMessage?, Exception) -> Unit),
    serviceName: ServiceName,
    concurrency: Int
  )

  // Asynchronously start consumers of messages to workflow task executor
  suspend fun startWorkflowTaskConsumerAsync(
    handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: (suspend (ServiceExecutorMessage?, Exception) -> Unit),
    workflowName: WorkflowName,
    concurrency: Int
  )

  // Asynchronously start consumers of messages to task events
  suspend fun startWorkflowTaskEventsConsumerAsync(
    handler: suspend (ServiceEventMessage, MillisInstant) -> Unit,
    beforeDlq: (suspend (ServiceEventMessage?, Exception) -> Unit),
    workflowName: WorkflowName,
    concurrency: Int
  )

  // Asynchronously start consumers of delayed messages to workflow task executor
  suspend fun startDelayedWorkflowTaskConsumerAsync(
    handler: suspend (ServiceExecutorMessage, MillisInstant) -> Unit,
    beforeDlq: (suspend (ServiceExecutorMessage?, Exception) -> Unit),
    workflowName: WorkflowName,
    concurrency: Int
  )
}
