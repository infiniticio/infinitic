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
package io.infinitic.inMemory

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.executors.messages.TaskExecutorMessage
import io.infinitic.common.tasks.tags.messages.TaskTagMessage
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap

class InMemoryChannels {

  // Client channel
  private val clientChannel =
      Channel<ClientMessage>()

  // Task channels
  private val taskTagChannels = ConcurrentHashMap<ServiceName, Channel<TaskTagMessage>>()
  private val taskExecutorChannels = ConcurrentHashMap<ServiceName, Channel<TaskExecutorMessage>>()

  // Workflow channels
  private val workflowTagChannels = ConcurrentHashMap<WorkflowName, Channel<WorkflowTagMessage>>()
  private val workflowEngineChannels =
      ConcurrentHashMap<WorkflowName, Channel<WorkflowEngineMessage>>()
  private val workflowTaskExecutorChannels =
      ConcurrentHashMap<WorkflowName, Channel<TaskExecutorMessage>>()

  fun forClient() = clientChannel

  fun forTaskTag(serviceName: ServiceName) =
      taskTagChannels[serviceName]
        ?: run {
          val channel = Channel<TaskTagMessage>()
          taskTagChannels[serviceName] = channel
          channel
        }

  fun forTaskExecutor(serviceName: ServiceName) =
      taskExecutorChannels[serviceName]
        ?: run {
          val channel = Channel<TaskExecutorMessage>()
          taskExecutorChannels[serviceName] = channel
          channel
        }

  fun forWorkflowTag(workflowName: WorkflowName) =
      workflowTagChannels[workflowName]
        ?: run {
          val channel = Channel<WorkflowTagMessage>()
          workflowTagChannels[workflowName] = channel
          channel
        }

  fun forWorkflowEngine(workflowName: WorkflowName) =
      workflowEngineChannels[workflowName]
        ?: run {
          val channel = Channel<WorkflowEngineMessage>()
          workflowEngineChannels[workflowName] = channel
          channel
        }

  fun forWorkflowTaskExecutor(workflowName: WorkflowName) =
      workflowTaskExecutorChannels[workflowName]
        ?: run {
          val channel = Channel<TaskExecutorMessage>()
          workflowTaskExecutorChannels[workflowName] = channel
          channel
        }
}
