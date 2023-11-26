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

import io.infinitic.pulsar.admin.PulsarInfiniticAdmin
import io.infinitic.pulsar.config.Pulsar
import io.infinitic.pulsar.config.policies.Policies
import java.util.concurrent.ConcurrentHashMap

class ResourceManager(
  val admin: PulsarInfiniticAdmin,
    // tenant configuration
  val allowedClusters: Set<String>?,
  val adminRoles: Set<String>?,
    // namespace configuration
  val policies: Policies,
    // topic naming
  val topicNamer: TopicNamer
) : TopicNamer by topicNamer {

  /** Set of topics for current tenant and namespace */
  val topicSet: Set<String>
    get() = admin.getTopicsSet(topicNamer.fullNameSpace).getOrThrow()

  /** Set of service's names for current tenant and namespace */
  val serviceSet: Set<String>
    get() = topicSet.mapNotNull { topicNamer.getServiceName(it) }.toSet()

  /** Set of workflow's names for current tenant and namespace */
  val workflowSet: Set<String>
    get() = topicSet.mapNotNull { topicNamer.getWorkflowName(it) }.toSet()

  /**
   * Check if the namer topic exists, and create it if not
   * We skip this if the topic has already been initialized successfully
   *
   */
  fun initNamer(): Result<String> =
      initTopic(
          topicNamer.getTopicName("global", GlobalTopicDescription.NAMER),
          GlobalTopicDescription.NAMER,
      )

  /**
   * Check if a topic exists, and create it if not
   * We skip this if the topic has already been initialized successfully
   */
  fun initTopic(name: String, topicDescription: TopicDescription): Result<String> = initTopic(
      topicNamer.getTopicName(name, topicDescription),
      topicDescription.isPartitioned,
      topicDescription.isDelayed,
  )


  /**
   * Check if a Dead Letter Queue topic exists, and create it if not
   * We skip this if the topic has already been initialized successfully
   */
  fun initDlqTopic(name: String, topicDescription: TopicDescription): Result<String?> =
      topicNamer.getDlqTopicName(name, topicDescription)
          ?.let {
            initTopic(
                it,
                topicDescription.isPartitioned,
                topicDescription.isDelayed,
            )
          }
        ?: Result.success(null)

  /**
   * Delete a topic by name
   */
  fun deleteTopic(topic: String): Result<Unit> = admin.deleteTopic(topic)

  private fun initTopic(topic: String, isPartitioned: Boolean, isDelayed: Boolean) =
      when (isInitialized(topic)) {
        true -> Result.success(topic)
        false -> admin.initTopic(
            topic,
            isPartitioned,
            when (isDelayed) {
              true -> policies.delayedTTLInSeconds
              false -> policies.messageTTLInSeconds
            },
        ).map { initializedTopics.add(topic); topic }
      }


  private fun isInitialized(topic: String): Boolean {
    val tenant = topicNamer.tenant
    if (!initializedTenants.contains(tenant)) {
      admin.initTenant(topicNamer.tenant, allowedClusters, adminRoles)
          .onSuccess { initializedTenants.add(tenant) }
    }
    val namespace = topicNamer.namespace
    if (!initializedNamespaces.contains(Pair(tenant, namespace))) {
      admin.initNamespace(topicNamer.fullNameSpace, policies)
          .onSuccess { initializedNamespaces.add(Pair(tenant, namespace)) }
    }
    return initializedTopics.contains(topic)
  }

  companion object {
    // thread-safe set of initialized topics (topic name includes tenant and namespace)
    private val initializedTopics = ConcurrentHashMap.newKeySet<String>()

    // thread-safe set of initialized tenants
    private val initializedTenants = ConcurrentHashMap.newKeySet<String>()

    // thread-safe set of initialized namespaces
    private val initializedNamespaces = ConcurrentHashMap.newKeySet<Pair<String, String>>()

    /** Create TopicManager from a Pulsar configuration instance */
    fun from(pulsar: Pulsar) = ResourceManager(
        PulsarInfiniticAdmin(pulsar.admin),
        pulsar.allowedClusters,
        pulsar.adminRoles,
        pulsar.policies,
        TopicNamerDefault(pulsar.tenant, pulsar.namespace),
    )
  }
}
