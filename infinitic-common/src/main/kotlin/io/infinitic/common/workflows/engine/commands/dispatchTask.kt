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
package io.infinitic.common.workflows.engine.commands

import io.infinitic.common.data.MillisInstant
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.requester.Requester
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.executors.errors.TaskTimedOutError
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.tasks.tags.messages.AddTaskIdToTag
import io.infinitic.common.transport.DelayedWorkflowEngineTopic
import io.infinitic.common.transport.InfiniticProducer
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagTopic
import io.infinitic.common.workflows.engine.messages.RemoteTaskTimedOut
import io.infinitic.common.workflows.engine.messages.data.TaskDispatched
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun InfiniticProducer.dispatchTask(
  taskDispatched: TaskDispatched,
  requester: Requester
) = coroutineScope {
  val emitterName = EmitterName(name)

  val executeTask = with(taskDispatched) {
    ExecuteTask(
        serviceName = serviceName,
        taskId = taskId,
        taskRetrySequence = taskRetrySequence,
        taskRetryIndex = TaskRetryIndex(),
        requester = requester,
        taskTags = taskTags,
        taskMeta = taskMeta,
        clientWaiting = false,
        methodName = methodName,
        methodParameterTypes = methodParameterTypes,
        methodParameters = methodParameters,
        lastError = null,
        emitterName = emitterName,
    )
  }

  // start task
  launch { executeTask.sendTo(ServiceExecutorTopic) }

  // add provided tags
  executeTask.taskTags.forEach {
    launch {
      val addTaskIdToTag = AddTaskIdToTag(
          serviceName = executeTask.serviceName,
          taskTag = it,
          taskId = executeTask.taskId,
          emitterName = emitterName,
      )
      addTaskIdToTag.sendTo(ServiceTagTopic)
    }
  }

  if (requester is WorkflowRequester) {
    // send task timeout if any
    taskDispatched.timeoutInstant?.let {
      launch {
        val remoteTaskTimedOut = RemoteTaskTimedOut(
            taskTimedOutError = TaskTimedOutError(
                serviceName = executeTask.serviceName,
                taskId = executeTask.taskId,
                methodName = executeTask.methodName,
            ),
            workflowName = requester.workflowName,
            workflowId = requester.workflowId,
            workflowVersion = requester.workflowVersion,
            workflowMethodName = requester.workflowMethodName,
            workflowMethodId = requester.workflowMethodId,
            emitterName = emitterName,
            emittedAt = it,
        )
        remoteTaskTimedOut.sendTo(DelayedWorkflowEngineTopic, it - MillisInstant.now())
      }
    }
  }
}
