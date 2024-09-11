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
package io.infinitic.pulsar.admin

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.pulsar.config.policies.PoliciesConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.apache.pulsar.common.partition.PartitionedTopicMetadata
import org.apache.pulsar.common.policies.data.PartitionedTopicStats
import org.apache.pulsar.common.policies.data.RetentionPolicies
import org.apache.pulsar.common.policies.data.TenantInfo
import org.apache.pulsar.common.policies.data.impl.AutoTopicCreationOverrideImpl
import org.apache.pulsar.common.policies.data.impl.DelayedDeliveryPoliciesImpl
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import org.apache.pulsar.common.policies.data.Policies as PulsarPolicies
import org.apache.pulsar.common.policies.data.TopicType as PulsarTopicType

@Suppress("MemberVisibilityCanBePrivate")
class PulsarInfiniticAdmin(
  pulsarAdmin: PulsarAdmin
) {
  private val logger = KotlinLogging.logger {}

  private val clusters = pulsarAdmin.clusters()
  private val topics = pulsarAdmin.topics()
  private val topicPolicies = pulsarAdmin.topicPolicies()
  private val tenants = pulsarAdmin.tenants()
  private val namespaces = pulsarAdmin.namespaces()

  private val topicsMutex = Mutex()
  private val namespacesMutex = Mutex()
  private val tenantsMutex = Mutex()

  /**
   * Get set of clusters' name
   *
   * Returns:
   * - Result.success(Set<String>)
   * - Result.failure(e) in case of error
   */
  suspend fun getClusters(): Result<Set<String>> = try {
    logger.debug { "Getting list of clusters." }
    val clusters = clusters.clustersAsync.await().toSet()
    logger.info { "List of clusters got ($clusters)." }
    Result.success(clusters)
  } catch (e: PulsarAdminException) {
    logger.warn(e) { "Unable to get clusters." }
    Result.failure(e)
  }

  /**
   * Get set of topics' name for current namespace
   *
   * Returns:
   *  - Result.success(Set<String>)
   *  - Result.failure(e) in case of error
   **/
  suspend fun getTopicsSet(fullNamespace: String): Result<Set<String>> =
      try {
        val topicSet = with(topics) {
          (getPartitionedTopicListAsync(fullNamespace).await() + getListAsync(fullNamespace).await()
              .filter {
                // filter out non-partitioned topic whose name ends like -partition-<number>
                !it.matches(Regex(".+-partition-[0-9]+$"))
              }).toSet()
        }

        Result.success(topicSet)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to get topics for namespace '$fullNamespace'" }
        Result.failure(e)
      }

  /**
   * Checks if a subscription has any consumer and ensures that the check is performed only once.
   *
   * @param topic The name of the topic.
   * @param isPartitioned Boolean value indicating if the topic is partitioned or not.
   * @param subscriptionName The name of the subscription.
   */
  suspend fun checkSubscriptionHasConsumerOnce(
    topic: String,
    isPartitioned: Boolean,
    subscriptionName: String
  ): Result<Boolean> = subscriptionCheckedForConsumers.getOrPut(Pair(topic, subscriptionName)) {
    checkSubscriptionHasConsumer(topic, isPartitioned, subscriptionName)
  }

  /**
   * Ensure once that tenant exists.
   *
   * Note we do not use a Mutex like for topics,
   * because it would trigger multiple creation of the same tenant in parallel
   *
   * Returns:
   *  - Result.success(TenantInfo) if tenant exists or has been created
   *  - Result.failure(e) in case of error
   **/
  suspend fun syncInitTenantOnce(
    tenant: String,
    allowedClusters: Set<String>?,
    adminRoles: Set<String>?
  ): Result<TenantInfo> {
    // First check if the tenant is already initialized, to avoid locking unnecessarily
    initializedTenants[tenant]?.let { return it }

    // Locking the section where we might do an expensive initialization
    return tenantsMutex.withLock {
      // Double-checked locking to ensure the tenant hasn't been initialized by another coroutine
      initializedTenants[tenant]?.let { return it }

      // Initialize the topic if it has not been initialized yet
      initTenantOnce(tenant, allowedClusters, adminRoles)
          .also { initializedTenants[tenant] = it }
    }
  }

  private suspend fun initTenantOnce(
    tenant: String,
    allowedClusters: Set<String>?,
    adminRoles: Set<String>?
  ): Result<TenantInfo> = try {
    // get tenant info or create it
    val tenantInfo = getTenantInfo(tenant).getOrThrow()
      ?: createTenant(tenant, allowedClusters, adminRoles).getOrThrow()

    checkTenantInfo(tenant, tenantInfo, allowedClusters, adminRoles)
    Result.success(tenantInfo)
  } catch (e: Exception) {
    logger.info(e) { "Unable to check/create tenant '$tenant'" }
    Result.failure(e)
  }


  /**
   * Ensure once that namespace exists.
   *
   * Note we do not use a Mutex like for topics,
   * because it would trigger multiple creation of the same tenant in parallel
   *
   * Returns:
   *  - Result.success(Policies) if tenant exists or has been created
   *  - Result.failure(e) in case of error
   **/
  suspend fun syncInitNamespaceOnce(
    fullNamespace: String,
    config: PoliciesConfig
  ): Result<PulsarPolicies> {
    // First check if the namespace is already initialized, to avoid locking unnecessarily
    initializedNamespaces[fullNamespace]?.let { return it }

    // Locking the section where we might do an expensive initialization
    return namespacesMutex.withLock {
      // Double-checked locking to ensure the namespace hasn't been initialized by another coroutine
      initializedNamespaces[fullNamespace]?.let { return it }

      // Initialize the topic if it has not been initialized yet
      initNamespaceOnce(fullNamespace, config)
          .also { initializedNamespaces[fullNamespace] = it }
    }
  }

  suspend fun initNamespaceOnce(
    fullNamespace: String,
    config: PoliciesConfig
  ): Result<PulsarPolicies> = try {
    // get Namespace policies or create it
    val policies = getNamespacePolicies(fullNamespace).getOrThrow()
      ?: createNamespace(fullNamespace, config).getOrThrow()

    checkNamespacePolicies(policies, config.getPulsarPolicies())
    Result.success(policies)
  } catch (e: Exception) {
    logger.info(e) { "Unable to check/create namespace '$fullNamespace'" }
    Result.failure(e)
  }

  /**
   * Ensure once that topic exists.
   *
   * As topics are created on the fly, it can happen that
   * the same topic is created twice simultaneously, e.g. when dispatching a task
   * with multiple new tags (there is one topic per tag).
   * That's why we manage ConflictException with retry and random delay
   *
   * Returns:
   *  - Result.success(Unit) if topic exists or has been created
   *  - Result.failure(e) in case of error
   **/
  suspend fun syncInitTopicOnce(
    topic: String,
    isPartitioned: Boolean,
    messageTTLPolicy: Int,
  ): Result<TopicInfo> {
    // First check if the topic is already initialized, to avoid locking unnecessarily
    initializedTopics[topic]?.let { return it }

    // Locking the section where we might do an expensive initialization
    return topicsMutex.withLock {
      // Double-checked locking to ensure the topic hasn't been initialized by another coroutine
      initializedTopics[topic]?.let { return it }

      // Initialize the topic if it has not been initialized yet
      initTopicOnce(topic, isPartitioned, messageTTLPolicy)
          .also { initializedTopics[topic] = it }
    }
  }

  private suspend fun initTopicOnce(
    topic: String,
    isPartitioned: Boolean,
    messageTTLPolicy: Int,
    retry: Int = 0
  ): Result<TopicInfo> = try {
    // get topic info or create it if it doesn't exist
    val topicInfo = getTopicInfo(topic).getOrThrow()
      ?: createTopic(topic, isPartitioned, messageTTLPolicy).getOrThrow()
    checkTopicInfo(topic, topicInfo, TopicInfo(isPartitioned, messageTTLPolicy))
    Result.success(topicInfo)
  } catch (e: PulsarAdminException.ConflictException) {
    if (retry >= 3) {
      logger.warn(e) { "Unable to check/create topic '$topic' after $retry retries." }
      Result.failure(e)
    } else {
      delay(Random.nextLong(100))
      initTopicOnce(topic, isPartitioned, messageTTLPolicy, retry + 1)
    }
  } catch (e: Exception) {
    logger.warn(e) { "Unable to check/create topic '$topic'" }
    Result.failure(e)
  }

  /**
   * Delete topic.
   *
   * Returns:
   * - Result.success(Unit) in case of success or already deleted topic
   * - Result.failure(e) in case of error
   */
  suspend fun deleteTopic(topic: String): Result<Unit?> = try {
    logger.debug { "Deleting topic $topic." }
    topics.deleteAsync(topic, true).await()
    logger.info { "Topic '$topic' deleted." }
    Result.success(Unit)
  } catch (e: PulsarAdminException.NotFoundException) {
    logger.debug { "Unable to delete topic '$topic' that does not exist." }
    Result.success(null)
  } catch (e: PulsarAdminException) {
    logger.warn(e) { "Unable to delete topic '$topic'." }
    Result.failure(e)
  }

  /**
   * Get stats for partitioned topic
   *
   * Returns:
   *  - Result.success(PartitionedTopicStats) if topic exists
   *  - Result.success(null) if topic does not exist
   *  - Result.failure(e) in case of error
   **/
  suspend fun getPartitionedTopicStats(topic: String): Result<PartitionedTopicStats?> {
    return try {
      logger.debug { "Topic '$topic': getting PartitionedStats." }
      val stats = topics.getPartitionedStatsAsync(topic, false, false, false, false).await()
      logger.info { "Topic '$topic': PartitionedStats retrieved ($stats)." }
      Result.success(stats)
    } catch (e: PulsarAdminException.NotFoundException) {
      logger.debug { "Topic '$topic': Unable to get PartitionedStats as topic does not exist." }
      Result.success(null)
    } catch (e: PulsarAdminException) {
      logger.warn(e) { "Unable to get stats for topic '$topic'." }
      Result.failure(e)
    }
  }

  suspend fun getTenantInfo(tenant: String): Result<TenantInfo?> =
      try {
        logger.debug { "Tenant '$tenant': Getting info." }
        val info = tenants.getTenantInfoAsync(tenant).await()
        logger.info { "Tenant '$tenant': info got ($info)." }
        Result.success(info)
      } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized to admin tenant '$tenant'." }
        Result.failure(e)
      } catch (e: PulsarAdminException.NotFoundException) {
        logger.debug { "Tenant '$tenant': unable to get info as tenant does not exist." }
        Result.success(null)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to get info for tenant '$tenant'." }
        Result.failure(e)
      }

  suspend fun createTenant(
    tenant: String,
    allowedClusters: Set<String>?,
    adminRoles: Set<String>?
  ): Result<TenantInfo> =
      try {
        logger.debug { "Creating tenant '$tenant'." }
        val tenantInfo = TenantInfo.builder()
            // if allowedClusters is null, we use all the current ones
            .allowedClusters(
                allowedClusters ?: getClusters().getOrElse { return Result.failure(it) },
            )
            .adminRoles(adminRoles)
            .build()
        tenants.createTenantAsync(tenant, tenantInfo).await()
        logger.info { "Tenant '$tenant' created with tenantInfo=$tenantInfo." }
        Result.success(tenantInfo)
      } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized to create tenant." }
        Result.failure(e)
      } catch (e: PulsarAdminException.ConflictException) {
        logger.warn { "Tenant '$tenant' already exists." }
        Result.failure(e)
      } catch (e: PulsarAdminException.PreconditionFailedException) {
        logger.warn { "Unable to create tenant: '$tenant' is an invalid tenant name." }
        Result.failure(e)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to create tenant '$tenant'." }
        Result.failure(e)
      }

  suspend fun createNamespace(
    fullNamespace: String,
    config: PoliciesConfig
  ): Result<PulsarPolicies> =
      try {
        logger.debug { "Creating namespace '$fullNamespace'." }
        val pulsarPolicies = config.getPulsarPolicies()
        namespaces.createNamespaceAsync(fullNamespace, pulsarPolicies).await()
        logger.info { "Namespace '$fullNamespace' created with policies $pulsarPolicies." }
        Result.success(pulsarPolicies)
      } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized to create namespace '$fullNamespace'." }
        Result.failure(e)
      } catch (e: PulsarAdminException.NotFoundException) {
        logger.debug { "Unable to create namespace '$fullNamespace' as tenant does not exist." }
        Result.failure(e)
      } catch (e: PulsarAdminException.ConflictException) {
        logger.warn { "Namespace '$fullNamespace' already exists." }
        Result.failure(e)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to create namespace '$fullNamespace'." }
        Result.failure(e)
      }

  suspend fun createTopic(
    topic: String,
    isPartitioned: Boolean,
    messageTTLPolicy: Int
  ): Result<TopicInfo> = try {
    logger.debug { "Creating topic $topic." }
    when (isPartitioned) {
      true -> topics.createPartitionedTopicAsync(topic, DEFAULT_NUM_PARTITIONS).await()
      false -> topics.createNonPartitionedTopicAsync(topic).await()
    }
    logger.info {
      "Topic '$topic' created " +
          if (isPartitioned) "with $DEFAULT_NUM_PARTITIONS partitions." else "without partition."
    }
    // set message TTL
    setTopicTTL(topic, messageTTLPolicy)
    // Note: to simplify the implementation, we do not check that message TTL has been rightfully set
    Result.success(TopicInfo(isPartitioned, messageTTLPolicy))
  } catch (e: PulsarAdminException.ConflictException) {
    logger.debug { "Unable to create topic '$topic' that already exists." }
    Result.failure(e)
  } catch (e: PulsarAdminException) {
    logger.warn(e) { "Unable to create topic '$topic'." }
    Result.failure(e)
  }

  suspend fun getNamespacePolicies(fullNamespace: String): Result<PulsarPolicies?> =
      try {
        logger.debug { "Getting namespace policies." }
        val pulsarPolicies = namespaces.getPoliciesAsync(fullNamespace).await()
        logger.info { "Namespace policies got ($pulsarPolicies)." }
        Result.success(pulsarPolicies)
      } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized to admin namespace '$fullNamespace'." }
        Result.failure(e)
      } catch (e: PulsarAdminException.NotFoundException) {
        Result.success(null)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to get policies of namespace '$fullNamespace'." }
        Result.failure(e)
      }

  fun setTopicTTL(topic: String, messageTTLSeconds: Int): Result<Int> = try {
    logger.debug { "Topic '$topic': setting messageTTLInSecond=$messageTTLSeconds." }
    topicPolicies.setMessageTTL(topic, messageTTLSeconds)
    logger.info { "Topic '$topic': messageTTLInSecond=$messageTTLSeconds set." }
    Result.success(messageTTLSeconds)
  } catch (e: PulsarAdminException) {
    logger.warn(e) { "Topic '$topic': Unable to set message TTL for topic." }
    Result.failure(e)
  }

  suspend fun getTopicInfo(topic: String): Result<TopicInfo?> {
    val ttl = getMessageTTL(topic).getOrElse { return Result.failure(it) }
    return when (ttl) {
      null -> Result.success(null)
      else -> {
        val metadata = getPartitionedTopicMetadata(topic).getOrElse { return Result.failure(it) }
        when (metadata) {
          null -> Result.success(TopicInfo(false, ttl))
          else -> Result.success(TopicInfo(metadata.partitions != 0, ttl))
        }
      }
    }
  }

  fun getMessageTTL(topic: String): Result<Int?> =
      try {
        logger.debug { "Topic '$topic': getting MessageTTL." }
        val ttl = topicPolicies.getMessageTTL(topic, true)
        logger.info { "Topic '$topic': MessageTTL retrieved ($ttl)." }
        Result.success(ttl)
      } catch (e: PulsarAdminException.NotFoundException) {
        logger.debug { "Topic '$topic': Unable to retrieve MessageTTL as topic does not exist." }
        Result.success(null)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to get message TTL for topic '$topic'." }
        Result.failure(e)
      }

  suspend fun getPartitionedTopicMetadata(topic: String): Result<PartitionedTopicMetadata?> {
    return try {
      logger.debug { "Topic '$topic': getting PartitionedTopicMetadata." }
      val metadata = topics.getPartitionedTopicMetadataAsync(topic).await()
      logger.info { "Topic '$topic': PartitionedTopicMetadata retrieved ($metadata)." }
      Result.success(metadata)
    } catch (e: PulsarAdminException.NotFoundException) {
      logger.debug { "Topic '$topic': Unable to retrieve PartitionedTopicMetadata as topic does not exist." }
      Result.success(null)
    } catch (e: PulsarAdminException) {
      logger.warn(e) { "Unable to get metadata for topic '$topic'." }
      Result.failure(e)
    }
  }

  private fun checkTenantInfo(
    tenant: String,
    tenantInfo: TenantInfo,
    allowedClusters: Set<String>?,
    adminRoles: Set<String>?
  ) {
    if (allowedClusters != null && allowedClusters != tenantInfo.allowedClusters) logger.warn {
      "Tenant '$tenant': allowedClusters policy (${tenantInfo.allowedClusters}) " +
          "is different from expected value ($allowedClusters)."
    }
    if (adminRoles != null && adminRoles != tenantInfo.adminRoles) logger.warn {
      "Tenant '$tenant': adminRoles policy (${tenantInfo.adminRoles}) " +
          "is different from expected value ($adminRoles)."
    }
  }


  private fun checkNamespacePolicies(policies: PulsarPolicies, expected: PulsarPolicies) {
    // check that policies are the same
    if (policies.schema_compatibility_strategy != expected.schema_compatibility_strategy) {
      logger.warn {
        "Namespace policy 'schema_compatibility_strategy' (${policies.schema_compatibility_strategy}) " +
            "is different from expected value (${expected.schema_compatibility_strategy})."
      }
    }
    if (policies.autoTopicCreationOverride != expected.autoTopicCreationOverride) {
      logger.warn {
        "Namespace policy 'autoTopicCreationOverride' (${policies.autoTopicCreationOverride}) " +
            "is different from expected value (${expected.autoTopicCreationOverride})."
      }
    }
    if (policies.schema_validation_enforced != expected.schema_validation_enforced) {
      logger.warn {
        "Namespace policy 'schema_validation_enforced' (${policies.schema_validation_enforced}) " +
            "is different from expected value (${expected.schema_validation_enforced})."
      }
    }
    if (policies.is_allow_auto_update_schema != expected.is_allow_auto_update_schema) {
      logger.warn {
        "Namespace policy 'is_allow_auto_update_schema' (${policies.is_allow_auto_update_schema}) " +
            "is different from expected value (${expected.is_allow_auto_update_schema})."
      }
    }
    if (policies.deduplicationEnabled != expected.deduplicationEnabled) {
      logger.warn {
        "Namespace policy 'deduplicationEnabled' (${policies.deduplicationEnabled}) " +
            "is different from expected value (${expected.deduplicationEnabled})."
      }
    }
    if (policies.retention_policies != expected.retention_policies) {
      logger.warn {
        "Namespace policy 'retention_policies policy (${policies.retention_policies}) " +
            "is different from expected value (${expected.retention_policies})."
      }
    }
    if (policies.message_ttl_in_seconds != expected.message_ttl_in_seconds) {
      logger.warn {
        "Namespace policy 'message_ttl_in_seconds policy (${policies.message_ttl_in_seconds}) " +
            "is different from expected value (${expected.message_ttl_in_seconds})."
      }
    }
    if (policies.delayed_delivery_policies != expected.delayed_delivery_policies) {
      logger.warn {
        "Namespace policy 'delayed_delivery_policies policy (${policies.delayed_delivery_policies}) " +
            "is different from expected value (${expected.delayed_delivery_policies})."
      }
    }
  }

  private fun checkTopicInfo(topic: String, topicInfo: TopicInfo, expected: TopicInfo) {

    if (topicInfo.messageTTLPolicy != expected.messageTTLPolicy) logger.warn {
      "Topic '$topic': messageTTLPolicy (${topicInfo.messageTTLPolicy}) " +
          "is different from expected (${expected.messageTTLPolicy})."
    }

    if (expected.isPartitioned && !topicInfo.isPartitioned) logger.warn {
      "Topic '$topic' is expected to be partitioned but is not."
    }

    if (!expected.isPartitioned && topicInfo.isPartitioned) logger.warn {
      "Topic '$topic' is expected to be non-partitioned but is."
    }
  }

  private fun PoliciesConfig.getPulsarPolicies() = PulsarPolicies().also {
    it.retention_policies = RetentionPolicies(retentionTimeMinutes, retentionSizeMB)
    it.message_ttl_in_seconds = messageTTLSeconds
    it.delayed_delivery_policies =
        DelayedDeliveryPoliciesImpl(delayedDeliveryTickTimeMillis, true)
    it.schema_compatibility_strategy = schemaCompatibilityStrategy
    it.autoTopicCreationOverride =
        AutoTopicCreationOverrideImpl(
            allowAutoTopicCreation,
            PulsarTopicType.PARTITIONED.toString(),
            3,
        )
    it.schema_validation_enforced = schemaValidationEnforced
    it.is_allow_auto_update_schema = isAllowAutoUpdateSchema
    it.deduplicationEnabled = deduplicationEnabled
  }

  data class TopicInfo(
    val isPartitioned: Boolean,
    val messageTTLPolicy: Int,
  )

  private suspend fun checkSubscriptionHasConsumer(
    topic: String,
    isPartitioned: Boolean,
    subscriptionName: String
  ): Result<Boolean> {
    return try {
      val hasConsumer = when (isPartitioned) {
        true -> topics.getPartitionedStatsAsync(topic, false)
        false -> topics.getStatsAsync(topic)
      }.await().subscriptions[subscriptionName]?.consumers?.isNotEmpty() ?: false

      if (!hasConsumer) logger.warn { "No consumer detected for subscription '$subscriptionName' on topic '$topic' " }

      Result.success(hasConsumer)
    } catch (e: PulsarAdminException) {
      logger.info(e) { "Unable to check presence of consumer for subscription '$subscriptionName' on topic '$topic' " }
      Result.failure(e)
    }
  }

  companion object {
    private const val DEFAULT_NUM_PARTITIONS = 3

    // set of initialized tenants
    private val initializedTenants = mutableMapOf<String, Result<TenantInfo>>()

    // set of initialized namespaces
    private val initializedNamespaces = mutableMapOf<String, Result<PulsarPolicies>>()

    // set of initialized topics (topic name includes tenant and namespace)
    private val initializedTopics = mutableMapOf<String, Result<TopicInfo>>()

    // thread-safe set of (topic, subscription name) that have already been checked for consumers
    private val subscriptionCheckedForConsumers =
        ConcurrentHashMap<Pair<String, String>, Result<Boolean>>()
  }
}
