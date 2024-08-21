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

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.workflows.engine.messages.WorkflowCmdMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagEngineMessage


sealed class Topic<S : Message> {
  abstract val prefix: String
}

/**
 * Topic used to get a unique producer name
 */
data object NamingTopic : Topic<Nothing>() {
  override val prefix = "namer"
}

/****************************************************
 *
 *  Client Topics
 *
 ****************************************************/

data object ClientTopic : Topic<ClientMessage>() {
  override val prefix = "response"
}

/****************************************************
 *
 *  Service Topics
 *
 ****************************************************/
sealed class ServiceTopic<S : Message> : Topic<S>() {
  companion object {
    val entries: List<ServiceTopic<*>>
      get() = ServiceTopic::class.sealedSubclasses.map { it.objectInstance!! }
  }
}

data object ServiceTagTopic : ServiceTopic<ServiceTagMessage>() {
  override val prefix = "task-tag"
}

data object ServiceExecutorTopic : ServiceTopic<ServiceExecutorMessage>() {
  override val prefix = "task-executor"
}

data object RetryServiceExecutorTopic : ServiceTopic<ServiceExecutorMessage>() {
  override val prefix = "task-executor"
}


data object ServiceEventsTopic : ServiceTopic<ServiceEventMessage>() {
  override val prefix = "task-events"
}


/****************************************************
 *
 *  Workflow Topics
 *
 ****************************************************/

sealed class WorkflowTopic<S : Message> : Topic<S>() {
  companion object {
    val entries: List<WorkflowTopic<*>>
      get() = WorkflowTopic::class.sealedSubclasses.map { it.objectInstance!! }
  }
}


data object WorkflowTagEngineTopic : WorkflowTopic<WorkflowTagEngineMessage>() {
  override val prefix = "workflow-tag"
}

data object WorkflowCmdTopic : WorkflowTopic<WorkflowCmdMessage>() {
  override val prefix = "workflow-cmd"
}

data object WorkflowStateEngineTopic : WorkflowTopic<WorkflowStateEngineMessage>() {
  override val prefix = "workflow-engine"
}

data object TimerWorkflowStateEngineTopic : WorkflowTopic<WorkflowStateEngineMessage>() {
  override val prefix = "workflow-delay"
}

data object WorkflowEventsTopic : WorkflowTopic<WorkflowEventMessage>() {
  override val prefix = "workflow-events"
}

data object WorkflowTaskExecutorTopic : WorkflowTopic<ServiceExecutorMessage>() {
  override val prefix = "workflow-task-executor"
}

data object RetryWorkflowTaskExecutorTopic : WorkflowTopic<ServiceExecutorMessage>() {
  override val prefix = "workflow-task-executor"
}

data object WorkflowTaskEventsTopic : WorkflowTopic<ServiceEventMessage>() {
  override val prefix = "workflow-task-events"
}


/**
 * Property indicating whether a topic type receives delayed message used for timers or timeout
 * This is used to set up TTLInSeconds which mush be much higher for delayed messages
 */
val Topic<*>.isTimer
  get() = when (this) {
    TimerWorkflowStateEngineTopic -> true
    else -> false
  }

/**
 * Property indicating whether a topic type receives delayed message
 */
val Topic<*>.acceptDelayed
  get() = when (this) {
    TimerWorkflowStateEngineTopic -> true
    RetryWorkflowTaskExecutorTopic -> true
    RetryServiceExecutorTopic -> true

    else -> false
  }

/**
 * @return The [Topic] without delay.
 * If the current [Topic] is a delayed topic, it returns the corresponding non-delayed [Topic].
 * If the current [Topic] is not a delayed topic, it returns itself.
 */
@Suppress("UNCHECKED_CAST")
val <S : Message> Topic<S>.withoutDelay
  get() = when (this) {
    TimerWorkflowStateEngineTopic -> WorkflowStateEngineTopic
    RetryWorkflowTaskExecutorTopic -> WorkflowTaskExecutorTopic
    RetryServiceExecutorTopic -> ServiceExecutorTopic
    else -> this
  } as Topic<S>

/**
 * @return a [Topic] relative to workflowTask.
 */
@Suppress("UNCHECKED_CAST")
val <S : Message> Topic<S>.forWorkflow
  get() = when (this) {
    ServiceExecutorTopic -> WorkflowTaskExecutorTopic
    RetryServiceExecutorTopic -> RetryWorkflowTaskExecutorTopic
    ServiceEventsTopic -> WorkflowTaskEventsTopic
    else -> this
  } as Topic<S>
