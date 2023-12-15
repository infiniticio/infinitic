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
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import java.util.concurrent.CompletableFuture

interface InfiniticConsumer : AutoCloseable {

  // Asynchronously start consumers of messages to client
  fun startClientConsumerAsync(
    handler: suspend (ClientMessage) -> Unit,
    beforeDlq: (suspend (ClientMessage, Exception) -> Unit)?,
    clientName: ClientName
  ): CompletableFuture<Unit>

  // Asynchronously start consumers of messages to task tags
  fun startTaskTagConsumerAsync(
    handler: suspend (TaskTagMessage) -> Unit,
    beforeDlq: (suspend (TaskTagMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit>

  // Asynchronously start consumers of messages to workflow tag
  fun startWorkflowTagConsumerAsync(
    handler: suspend (WorkflowTagMessage) -> Unit,
    beforeDlq: (suspend (WorkflowTagMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit>

  // Asynchronously start consumers of messages to workflow engine
  fun startWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    beforeDlq: (suspend (WorkflowEngineMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit>

  // Asynchronously start consumers of delayed messages to workflow engine
  fun startDelayedWorkflowEngineConsumerAsync(
    handler: suspend (WorkflowEngineMessage) -> Unit,
    beforeDlq: (suspend (WorkflowEngineMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit>

  // Asynchronously start consumers of messages to task executor
  fun startTaskExecutorConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit>

  // Asynchronously start consumers of delayed messages to task executor
  fun startDelayedTaskExecutorConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit)?,
    serviceName: ServiceName,
    concurrency: Int
  ): CompletableFuture<Unit>

  // Asynchronously start consumers of messages to workflow task executor
  fun startWorkflowTaskConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit>

  // Asynchronously start consumers of delayed messages to workflow task executor
  fun startDelayedWorkflowTaskConsumerAsync(
    handler: suspend (TaskExecutorMessage) -> Unit,
    beforeDlq: (suspend (TaskExecutorMessage, Exception) -> Unit)?,
    workflowName: WorkflowName,
    concurrency: Int
  ): CompletableFuture<Unit>

}
