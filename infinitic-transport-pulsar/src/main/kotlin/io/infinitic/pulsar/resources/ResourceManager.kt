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

import io.infinitic.common.messages.Message
import io.infinitic.common.topics.Topic
import io.infinitic.pulsar.admin.PulsarInfiniticAdmin
import io.infinitic.pulsar.config.Pulsar
import io.infinitic.pulsar.config.policies.Policies

class ResourceManager(
  val admin: PulsarInfiniticAdmin,
    // tenant configuration
  val tenant: String,
  val allowedClusters: Set<String>?,
    // namespace configuration
  val namespace: String,
  val adminRoles: Set<String>?,
  val policies: Policies
) {

  /**
   * Full name of the current Pulsar namespace
   */
  val namespaceFullName = "$tenant/$namespace"

  /**
   * Full name of a topic
   */
  fun topicFullName(topic: String) = "persistent://$namespaceFullName/$topic"

  /** Set of topics for current tenant and namespace */
  val topicsFullName: Set<String>
    get() = admin.getTopicsSet(namespaceFullName).getOrThrow()

  /** Set of service's names for current tenant and namespace */
  val servicesName: Set<String>
    get() = topicsFullName.mapNotNull {
      getServiceNameFromTopicName(it.removePrefix(topicFullName("")))
    }.toSet()

  /** Set of workflow's names for current tenant and namespace */
  val workflowsName: Set<String>
    get() = topicsFullName.mapNotNull {
      getWorkflowNameFromTopicName(it.removePrefix(topicFullName("")))
    }.toSet()

  fun Topic<*>.fullName(entity: String?) = topicFullName(name(entity))
      .also {
        initTopicOnce(
            topic = it,
            isPartitioned = isPartitioned,
            isDelayed = isDelayed,
        )
      }

  fun Topic<*>.fullNameDLQ(entity: String) = topicFullName(nameDLQ(entity))
      .also {
        initTopicOnce(
            topic = it,
            isPartitioned = isPartitioned,
            isDelayed = isDelayed,
        )
      }

  fun <S : Message> Topic<S>.forMessage(message: S? = null) = fullName(message?.entity())

  /**
   * Delete a topic by name
   */
  fun deleteTopic(topic: String): Result<Unit> = admin.deleteTopic(topic)

  /**
   * Check if a topic exists, and create it if not
   * We skip this if the topic has already been initialized
   */
  fun initTopicOnce(
    topic: String,
    isPartitioned: Boolean,
    isDelayed: Boolean
  ): Result<Unit> {
    // initialize tenant once (do nothing on error)
    admin.initTenantOnce(tenant, allowedClusters, adminRoles)
    // initialize namespace once (do nothing on error)
    admin.initNamespaceOnce(namespaceFullName, policies)
    // initialize topic once  (do nothing on error)
    val ttl = when (isDelayed) {
      true -> policies.delayedTTLInSeconds
      false -> policies.messageTTLInSeconds
    }

    return admin.initTopicOnce(topic, isPartitioned, ttl).map { }
  }

  /**
   * Check if a Dead Letter Queue topic exists, and create it if not
   * We skip this if the topic has already been initialized
   */
  fun initDlqTopicOnce(
    topic: String?,
    isPartitioned: Boolean,
    isDelayed: Boolean
  ): Result<Unit?> = topic?.let { initTopicOnce(it, isPartitioned, isDelayed) }
    ?: Result.success(null)

  fun getConsumerName(name: String, topicDescription: TopicDescription): String = TODO()

  companion object {
    /** Create TopicManager from a Pulsar configuration instance */
    fun from(pulsar: Pulsar) = ResourceManager(
        PulsarInfiniticAdmin(pulsar.admin),
        pulsar.tenant,
        pulsar.allowedClusters,
        pulsar.namespace,
        pulsar.adminRoles,
        pulsar.policies,
    )
  }
}
