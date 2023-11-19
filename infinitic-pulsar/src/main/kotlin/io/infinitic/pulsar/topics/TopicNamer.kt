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

package io.infinitic.pulsar.topics

import io.infinitic.common.data.ClientName
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.workflows.data.workflows.WorkflowName

interface TopicNamer {
  val tenant: String

  val namespace: String

  fun fullName(topic: String) = "persistent://$tenant/$namespace/$topic"

  fun getProducerName(workerName: String, type: TopicType): String

  fun getConsumerName(workerName: String, type: TopicType): String

  fun getTopicName(type: GlobalTopics): String

  fun getTopicName(type: ClientTopics, clientName: ClientName): String

  fun getTopicName(type: WorkflowTopics, workflowName: WorkflowName): String

  fun getTopicDLQName(type: WorkflowTopics, workflowName: WorkflowName): String

  fun getTopicName(type: ServiceTopics, serviceName: ServiceName): String

  fun getTopicDLQName(type: ServiceTopics, serviceName: ServiceName): String

  fun getTopicName(type: WorkflowTaskTopics, workflowName: WorkflowName): String

  fun getTopicDLQName(type: WorkflowTaskTopics, workflowName: WorkflowName): String

  fun getTopicName(type: TopicType, name: String): String =
      when (type) {
        is ClientTopics -> getTopicName(type, ClientName(name))
        is GlobalTopics -> getTopicName(type)
        is WorkflowTopics -> getTopicName(type, WorkflowName(name))
        is WorkflowTaskTopics -> getTopicName(type, WorkflowName(name))
        is ServiceTopics -> getTopicName(type, ServiceName(name))
      }

  fun getTopicDLQName(type: TopicType, name: String): String? =
      when (type) {
        is WorkflowTopics -> getTopicDLQName(type, WorkflowName(name))
        is ServiceTopics -> getTopicDLQName(type, ServiceName(name))
        is WorkflowTaskTopics -> getTopicDLQName(type, WorkflowName(name))
        else -> null
      }

  /**
   * Get the name of the topic used to uniquely name clients and worker
   */
  fun getNamerTopic(): String = getTopicName(GlobalTopics.NAMER)
}

