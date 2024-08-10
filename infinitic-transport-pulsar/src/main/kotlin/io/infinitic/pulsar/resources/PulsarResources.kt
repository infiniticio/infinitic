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
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.isDelayed
import io.infinitic.pulsar.admin.PulsarInfiniticAdmin
import io.infinitic.pulsar.config.PulsarConfig
import io.infinitic.pulsar.config.policies.PoliciesConfig

@Suppress("MemberVisibilityCanBePrivate")
class PulsarResources(
  val admin: PulsarInfiniticAdmin,
    // tenant configuration
  val tenant: String,
  val allowedClusters: Set<String>?,
    // namespace configuration
  val namespace: String,
  val adminRoles: Set<String>?,
  val policies: PoliciesConfig
) {

  /**
   * Full name of the current Pulsar namespace
   */
  private val namespaceFullName = "$tenant/$namespace"

  /**
   * Full name of a topic
   */
  fun topicFullName(topic: String) = "persistent://$namespaceFullName/$topic"

  /** Set of topics for current tenant and namespace */
  suspend fun getTopicsFullName(): Set<String> = admin.getTopicsSet(namespaceFullName).getOrThrow()

  /** Set of service's names for current tenant and namespace */
  suspend fun getServicesName(): Set<String> = getTopicsFullName().mapNotNull {
    getServiceNameFromTopicName(it.removePrefix(topicFullName("")))
  }.toSet()

  /** Set of workflow's names for current tenant and namespace */
  suspend fun getWorkflowsName(): Set<String> = getTopicsFullName().mapNotNull {
    getWorkflowNameFromTopicName(it.removePrefix(topicFullName("")))
  }.toSet()

  /**
   * @param entity The optional entity name (service name or workflow name).
   * @return The full name of the topic with the optional entity name.
   */
  fun Topic<*>.fullName(entity: String?) = topicFullName(name(entity))

  /**
   * @param entity The optional entity name (service name or workflow name).
   * @return The full name of the topic with the optional entity name.
   */
  fun Topic<*>.fullNameDLQ(entity: String) = topicFullName(nameDLQ(entity))

  /**
   * @return the full name of a topic for an entity
   *
   * If [init] is true, ensure that the topic exists by calling initTopicOnce
   */
  suspend fun <S : Message> Topic<S>.forEntity(
    entity: String?,
    init: Boolean,
    checkConsumer: Boolean
  ): String = fullName(entity).also {
    if (init) initTopicOnce(
        topic = it,
        isPartitioned = isPartitioned,
        isDelayed = isDelayed,
    )

    if (checkConsumer) admin.checkSubscriptionHasConsumerOnce(
        it,
        isPartitioned,
        MainSubscription(this).defaultName,
    )
  }

  /**
   * @return the full name of a DLQ topic for an entity
   *
   * If [init] is true, ensure that the topic exists by calling initTopicOnce
   */
  suspend fun <S : Message> Topic<S>.forEntityDLQ(entity: String, init: Boolean): String =
      fullNameDLQ(entity).also {
        if (init) initTopicOnce(
            topic = it,
            isPartitioned = isPartitioned,
            isDelayed = isDelayed,
        )
      }

  /**
   * Delete a topic by name
   */
  suspend fun deleteTopic(topic: String): Result<Unit> = admin.deleteTopic(topic)

  /**
   * Check if a topic exists, and create it if not
   * We skip this if the topic has already been initialized
   */
  suspend fun initTopicOnce(
    topic: String,
    isPartitioned: Boolean,
    isDelayed: Boolean,
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
  suspend fun initDlqTopicOnce(
    topic: String?,
    isPartitioned: Boolean,
    isDelayed: Boolean,
  ): Result<Unit?> = topic?.let { initTopicOnce(it, isPartitioned, isDelayed) }
    ?: Result.success(null)

  companion object {
    /** Create TopicManager from a Pulsar configuration instance */
    fun from(pulsarConfig: PulsarConfig) = PulsarResources(
        PulsarInfiniticAdmin(pulsarConfig.admin),
        pulsarConfig.tenant,
        pulsarConfig.allowedClusters,
        pulsarConfig.namespace,
        pulsarConfig.adminRoles,
        pulsarConfig.policies,
    )
  }
}
