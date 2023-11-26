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

package io.infinitic.pulsar.resources

open class TopicNamerDefault(override val tenant: String, override val namespace: String) :
  TopicNamer {

  override fun getProducerName(name: String, topicDescription: TopicDescription) =
      "$name>>${topicDescription.subscriptionPrefix}"

  override fun getConsumerName(name: String, topicDescription: TopicDescription) =
      "$name<<${topicDescription.subscriptionPrefix}"

  override fun getTopicName(name: String, topicDescription: TopicDescription): String =
      when (topicDescription) {
        is GlobalTopicDescription -> fullNameTopic(topicDescription.subscriptionPrefix)
        else -> fullNameTopic("${topicDescription.subscriptionPrefix}:$name")
      }

  override fun getDlqTopicName(name: String, topicDescription: TopicDescription): String? =
      when (topicDescription.hasDeadLetter) {
        true -> fullNameTopic("${topicDescription.subscriptionPrefix}-dlq:$name")
        false -> null
      }

  override fun getServiceName(topic: String): String? {
    for (type in ServiceTopicDescription.entries) {
      var prefix = getTopicName("", type)
      if (topic.startsWith(prefix)) return topic.removePrefix(prefix)

      prefix = getDlqTopicName("", type)!!
      if (topic.startsWith(prefix)) return topic.removePrefix(prefix)
    }

    return null
  }

  override fun getWorkflowName(topic: String): String? {
    val workflowTypes: List<TopicDescription> = WorkflowTopicDescription.entries.toList()
    for (type in workflowTypes) {
      var prefix = getTopicName("", type)
      if (topic.startsWith(prefix)) return topic.removePrefix(prefix)

      prefix = getDlqTopicName("", type)!!
      if (topic.startsWith(prefix)) return topic.removePrefix(prefix)
    }

    return null
  }
}
