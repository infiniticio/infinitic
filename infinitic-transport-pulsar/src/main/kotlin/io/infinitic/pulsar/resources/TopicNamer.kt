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

interface TopicNamer {
  /**
   * Current Pulsar tenant
   */
  val tenant: String

  /**
   * Current Pulsar namespace
   */
  val namespace: String

  /**
   * Full name of the current Pulsar namespace
   */
  val fullNameSpace
    get() = "$tenant/$namespace"

  /**
   * Full name of a topic
   */
  fun fullNameTopic(topic: String) = "persistent://$fullNameSpace/$topic"

  /**
   * Return the producer name associated to the type [topicDescription] with name [name]
   */
  fun getProducerName(name: String, topicDescription: TopicDescription): String

  /**
   * Return the consumer name associated to the type [topicDescription] with name [name]
   */
  fun getConsumerName(name: String, topicDescription: TopicDescription): String

  /**
   * Return the full name of the topic associated to the type [topicDescription] with name [name]
   */
  fun getTopicName(name: String, topicDescription: TopicDescription): String

  /**
   * Return the full name of the DLQ topic associated to the type [topicDescription] with name [name]
   */
  fun getDlqTopicName(name: String, topicDescription: TopicDescription): String?

  /**
   * Return the service name associated to a topic
   * Return null if the topic is not a service topic
   */
  fun getServiceName(topic: String): String?

  /**
   * Return the workflow name associated to a topic
   * Return null if the topic is not a workflow topic
   */
  fun getWorkflowName(topic: String): String?
}

