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
package io.infinitic.tasks.executor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.requester.WorkflowRequester
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.events.messages.TaskCompletedEvent
import io.infinitic.common.tasks.events.messages.TaskFailedEvent
import io.infinitic.common.tasks.events.messages.TaskRetriedEvent
import io.infinitic.common.tasks.events.messages.TaskStartedEvent
import io.infinitic.common.tasks.tags.messages.SetDelegatedTaskData
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.ServiceTagEngineTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.interfaces.InfiniticProducer
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.infinitic.common.workflows.data.commands.DispatchNewMethodPastCommand
import io.infinitic.common.workflows.data.commands.DispatchNewWorkflowPastCommand
import io.infinitic.common.workflows.data.commands.DispatchTaskPastCommand
import io.infinitic.common.workflows.data.commands.InlineTaskPastCommand
import io.infinitic.common.workflows.data.commands.ReceiveSignalPastCommand
import io.infinitic.common.workflows.data.commands.SendSignalPastCommand
import io.infinitic.common.workflows.data.commands.StartDurationTimerPastCommand
import io.infinitic.common.workflows.data.commands.StartInstantTimerPastCommand
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.tasks.executor.events.dispatchDurationTimerCmd
import io.infinitic.tasks.executor.events.dispatchInstantTimerCmd
import io.infinitic.tasks.executor.events.dispatchRemoteMethodCmd
import io.infinitic.tasks.executor.events.dispatchRemoteSignalCmd
import io.infinitic.tasks.executor.events.dispatchRemoteWorkflowCmd
import io.infinitic.tasks.executor.events.dispatchTaskCmd
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class TaskEventHandler(val producer: InfiniticProducer) {

  suspend fun process(msg: ServiceExecutorEventMessage, publishedAt: MillisInstant) {
    when (msg) {
      is TaskCompletedEvent -> sendTaskCompleted(msg, publishedAt)
      is TaskFailedEvent -> sendTaskFailed(msg, publishedAt)
      is TaskRetriedEvent,
      is TaskStartedEvent -> Unit
    }
  }

  private suspend fun sendTaskFailed(msg: TaskFailedEvent, publishedAt: MillisInstant): Unit =
      coroutineScope {
        // send to parent client
        msg.getEventForClient(producer.emitterName)?.let {
          launch { with(producer) { it.sendTo(ClientTopic) } }
        }
        // send to parent workflow
        msg.getEventForWorkflow(producer.emitterName, publishedAt)?.let {
          launch { with(producer) { it.sendTo(WorkflowStateEngineTopic) } }
        }
      }

  private suspend fun sendTaskCompleted(msg: TaskCompletedEvent, publishedAt: MillisInstant) {
    coroutineScope {
      when (msg.isDelegated) {
        // if this task is marked as asynchronous, we do not forward the result, add a tag.
        // this tag is a convenient way to memorize workflowId and workflowName
        // so that the user just has to complete task by its Id
        true -> launch {

          val addTaskToTag = SetDelegatedTaskData(
              serviceName = msg.serviceName,
              delegatedTaskData = msg.getDelegatedTaskData(),
              taskId = msg.taskId,
              emitterName = producer.emitterName,
          )
          with(producer) { addTaskToTag.sendTo(ServiceTagEngineTopic) }
        }

        false -> {
          // send to parent client
          msg.getEventForClient(producer.emitterName)?.let {
            launch { with(producer) { it.sendTo(ClientTopic) } }
          }
          // send to parent workflow
          msg.getEventForWorkflow(producer.emitterName, publishedAt)?.let {
            launch { with(producer) { it.sendTo(WorkflowStateEngineTopic) } }
          }
          // remove tags
          msg.getEventsForTag(producer.emitterName).forEach {
            launch { with(producer) { it.sendTo(ServiceTagEngineTopic) } }
          }
        }
      }
    }
    // If we are dealing with a workflowTask, we ensure that new commands are dispatched only AFTER
    // the workflow task's completion is forwarded to the engine. This is a safeguard against potential
    // race conditions that may arise if the engine receives the outcomes of the dispatched tasks earlier
    // than the result of the workflowTask.
    if (msg.isWorkflowTask()) completeWorkflowTask(msg, publishedAt)
  }

  private suspend fun completeWorkflowTask(msg: TaskCompletedEvent, publishedAt: MillisInstant) =
      coroutineScope {

        val result = msg.returnValue.deserialize(
            type = WorkflowTaskReturnValue::class.java,
            jsonViewClass = null,
        ) as WorkflowTaskReturnValue

        // Note: After 0.13.0, workflowTaskInstant should not be null anymore
        val workflowTaskInstant = result.workflowTaskInstant ?: publishedAt

        // from there, workflowVersion is defined
        val current =
            (msg.requester as WorkflowRequester).copy(workflowVersion = result.workflowVersion)

        result.newCommands.forEach {
          when (it) {
            is DispatchNewWorkflowPastCommand ->
              dispatchRemoteWorkflowCmd(current, it, workflowTaskInstant, producer)

            is DispatchNewMethodPastCommand ->
              dispatchRemoteMethodCmd(current, it, workflowTaskInstant, producer)

            is DispatchTaskPastCommand ->
              dispatchTaskCmd(current, it, workflowTaskInstant, producer)

            is SendSignalPastCommand ->
              dispatchRemoteSignalCmd(current, it, workflowTaskInstant, producer)

            is StartDurationTimerPastCommand ->
              dispatchDurationTimerCmd(current, it, workflowTaskInstant, producer)

            is StartInstantTimerPastCommand ->
              dispatchInstantTimerCmd(current, it, workflowTaskInstant, producer)

            is ReceiveSignalPastCommand,
            is InlineTaskPastCommand -> Unit // Nothing to do
          }
        }
      }

  companion object {
    val logger = LoggerWithCounter(KotlinLogging.logger {})
  }
}
