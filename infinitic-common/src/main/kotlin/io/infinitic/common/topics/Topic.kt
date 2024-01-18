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
package io.infinitic.common.topics

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.events.messages.ServiceEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.tasks.tags.messages.ServiceTagMessage
import io.infinitic.common.workflows.engine.events.WorkflowEventMessage
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.tags.messages.WorkflowTagMessage


sealed class Topic<S : Message>

/**
 * Topic used to get a unique producer name
 */
data object NamingTopic : Topic<Nothing>()

/****************************************************
 *
 *  Client Topics
 *
 ****************************************************/

data object ClientTopic : Topic<ClientMessage>()

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

data object ServiceTagTopic : ServiceTopic<ServiceTagMessage>()

data object ServiceExecutorTopic : ServiceTopic<ServiceExecutorMessage>()

data object DelayedServiceExecutorTopic : ServiceTopic<ServiceExecutorMessage>()

data object ServiceEventsTopic : ServiceTopic<ServiceEventMessage>()


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


data object WorkflowTagTopic : WorkflowTopic<WorkflowTagMessage>()

data object WorkflowCmdTopic : WorkflowTopic<WorkflowEngineMessage>()

data object WorkflowEngineTopic : WorkflowTopic<WorkflowEngineMessage>()

data object DelayedWorkflowEngineTopic : WorkflowTopic<WorkflowEngineMessage>()

data object WorkflowEventsTopic : WorkflowTopic<WorkflowEventMessage>()

data object WorkflowTaskExecutorTopic : WorkflowTopic<ServiceExecutorMessage>()

data object DelayedWorkflowTaskExecutorTopic : WorkflowTopic<ServiceExecutorMessage>()

data object WorkflowTaskEventsTopic : WorkflowTopic<ServiceEventMessage>()


/**
 * Property indicating whether a topic type receives delayed message
 * This is used to set up TTLInSeconds which mush be much higher for delayed messages
 *
 * @return `true` if the topic is delayed, `false` otherwise.
 */
val Topic<*>.isDelayed
  get() = when (this) {
    DelayedWorkflowEngineTopic,
    DelayedWorkflowTaskExecutorTopic,
    DelayedServiceExecutorTopic
    -> true

    else -> false
  }

/**
 * Returns a [Topic] without delay. If the current [Topic] is a delayed topic, it returns the corresponding
 * non-delayed [Topic]. If the current [Topic] is not a delayed topic, it returns itself.
 *
 * @return The [Topic] without delay.
 */
@Suppress("UNCHECKED_CAST")
val <S : Message> Topic<S>.withoutDelay
  get() = when (this) {
    DelayedWorkflowEngineTopic -> WorkflowEngineTopic
    DelayedWorkflowTaskExecutorTopic -> WorkflowTaskExecutorTopic
    DelayedServiceExecutorTopic -> ServiceExecutorTopic
    else -> this
  } as Topic<S>

/**
 * Returns a [Topic] relative to workflowTask
 *
 * @return The [Topic] without delay.
 */
@Suppress("UNCHECKED_CAST")
val <S : Message> Topic<S>.forWorkflow
  get() = when (this) {
    ServiceExecutorTopic -> WorkflowTaskExecutorTopic
    DelayedServiceExecutorTopic -> DelayedWorkflowTaskExecutorTopic
    ServiceEventsTopic -> WorkflowTaskEventsTopic
    else -> this
  } as Topic<S>
