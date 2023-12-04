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
import io.infinitic.pulsar.config.policies.Policies
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.apache.pulsar.common.partition.PartitionedTopicMetadata
import org.apache.pulsar.common.policies.data.PartitionedTopicStats
import org.apache.pulsar.common.policies.data.RetentionPolicies
import org.apache.pulsar.common.policies.data.TenantInfo
import org.apache.pulsar.common.policies.data.impl.AutoTopicCreationOverrideImpl
import org.apache.pulsar.common.policies.data.impl.DelayedDeliveryPoliciesImpl
import java.util.concurrent.ConcurrentHashMap
import org.apache.pulsar.common.policies.data.Policies as PulsarPolicies
import org.apache.pulsar.common.policies.data.TopicType as PulsarTopicType

class PulsarInfiniticAdmin(
  pulsarAdmin: PulsarAdmin
) {
  private val logger = KotlinLogging.logger {}

  private val clusters = pulsarAdmin.clusters()
  private val topics = pulsarAdmin.topics()
  private val topicPolicies = pulsarAdmin.topicPolicies()
  private val tenants = pulsarAdmin.tenants()
  private val namespaces = pulsarAdmin.namespaces()

  private val DEFAUT_NUM_PARTITIONS = 3
  fun isTenantInitialized(tenant: String) =
      initializedTenants.containsKey(tenant)

  fun isNamespaceInitialized(fullNamespace: String) =
      initializedNamespaces.containsKey(fullNamespace)

  fun isTopicInitialized(topic: String) =
      initializedTopics.containsKey(topic)

  /**
   * Get set of clusters' name
   *
   * Returns:
   * - Result.success(Set<String>)
   * - Result.failure(e) in case of error
   */
  private fun getClusters(): Result<Set<String>> = try {
    logger.debug { "Getting list of clusters" }
    val clusters = clusters.clusters.toSet()
    logger.info { "List of clusters got ($clusters)" }
    Result.success(clusters)
  } catch (e: PulsarAdminException) {
    logger.warn(e) { "Unable to get clusters" }
    Result.failure(e)
  }

  /**
   * Get set of topics' name for current namespace
   *
   * Returns:
   *  - Result.success(Set<String>)
   *  - Result.failure(e) in case of error
   **/
  fun getTopicsSet(fullNamespace: String): Result<Set<String>> =
      try {
        val topicSet = with(topics) {
          (getPartitionedTopicList(fullNamespace) + getList(fullNamespace).filter {
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
   * Ensure tenant exists.
   *
   * Returns:
   *  - Result.success(TenantInfo) if tenant exists or has been created
   *  - Result.failure(e) in case of error
   **/
  fun initTenant(
    tenant: String,
    allowedClusters: Set<String>?,
    adminRoles: Set<String>?
  ): Result<TenantInfo> = try {
    initializedTenants.computeIfAbsent(tenant) {
      // get tenant info or create it
      getTenantInfo(tenant).getOrThrow()
        ?: createTenant(tenant, allowedClusters, adminRoles).getOrThrow()
    }.let {
      checkTenantInfo(tenant, it, allowedClusters, adminRoles)
      Result.success(it)
    }
  } catch (e: Exception) {
    Result.failure(e)
  }

  /**
   * Ensure namespace exists.
   *
   * Returns:
   *  - Result.success(Policies) if tenant exists or has been created
   *  - Result.failure(e) in case of error
   **/
  fun initNamespace(
    fullNamespace: String,
    config: Policies
  ): Result<PulsarPolicies> = try {
    initializedNamespaces.computeIfAbsent(fullNamespace) {
      // get Namespace policies or create it
      getNamespacePolicies(fullNamespace).getOrThrow()
        ?: createNamespace(fullNamespace, config).getOrThrow()
    }.let {
      checkNamespacePolicies(it, config.getPulsarPolicies())
      Result.success(it)
    }
  } catch (e: Exception) {
    Result.failure(e)
  }

  /**
   * Ensure topic exists.
   *
   * Returns:
   *  - Result.success(Unit) if topic exists or has been created
   *  - Result.failure(e) in case of error
   **/
  fun initTopic(
    topic: String,
    isPartitioned: Boolean,
    messageTTLPolicy: Int
  ): Result<TopicInfo> = try {
    initializedTopics.computeIfAbsent(topic) {
      // get Namespace policies or create it
      getTopicInfo(topic).getOrThrow()
        ?: createTopic(topic, isPartitioned, messageTTLPolicy).getOrThrow()
    }.let {
      checkTopicInfo(topic, it, TopicInfo(isPartitioned, messageTTLPolicy))
      Result.success(it)
    }
  } catch (e: Exception) {
    Result.failure(e)
  }

  /**
   * Delete topic.
   *
   * Returns:
   * - Result.success(Unit) in case of success or already deleted topic
   * - Result.failure(e) in case of error
   */
  fun deleteTopic(topic: String): Result<Unit> = try {
    logger.debug { "Deleting topic $topic" }
    topics.delete(topic, true)
    logger.info { "Topic '$topic' deleted" }
    Result.success(Unit)
  } catch (e: PulsarAdminException.NotFoundException) {
    logger.debug { "Unable to delete topic '$topic' that does not exist" }
    Result.success(Unit)
  } catch (e: PulsarAdminException) {
    logger.warn(e) { "Unable to delete topic '$topic'" }
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
  fun getPartitionedTopicStats(topic: String): Result<PartitionedTopicStats?> {
    return try {
      logger.debug { "Topic '$topic': getting PartitionedStats" }
      val stats = topics.getPartitionedStats(topic, false, false, false, false)
      logger.info { "Topic '$topic': PartitionedStats retrieved ($stats)" }
      Result.success(stats)
    } catch (e: PulsarAdminException.NotFoundException) {
      logger.debug { "Topic '$topic': Unable to get PartitionedStats as topic does not exist" }
      Result.success(null)
    } catch (e: PulsarAdminException) {
      logger.warn(e) { "Unable to get stats for topic '$topic'" }
      Result.failure(e)
    }
  }

  private fun getTenantInfo(tenant: String): Result<TenantInfo?> =
      try {
        logger.debug { "Tenant '$tenant': Getting info" }
        val info = tenants.getTenantInfo(tenant)
        logger.info { "Tenant '$tenant': info got ($info)" }
        Result.success(info)
      } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized to admin tenant '$tenant'" }
        Result.failure(e)
      } catch (e: PulsarAdminException.NotFoundException) {
        logger.debug { "Tenant '$tenant': unable to get info as tenant does not exist" }
        Result.success(null)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to get info for tenant '$tenant'" }
        Result.failure(e)
      }

  private fun createTenant(
    tenant: String,
    allowedClusters: Set<String>?,
    adminRoles: Set<String>?
  ): Result<TenantInfo> =
      try {
        logger.debug { "Creating tenant '$tenant'" }
        val tenantInfo = TenantInfo.builder()
            // if allowedClusters is null, we use all the current ones
            .allowedClusters(
                allowedClusters ?: getClusters().getOrElse { return Result.failure(it) },
            )
            .adminRoles(adminRoles)
            .build()
        tenants.createTenant(tenant, tenantInfo)
        logger.info { "Tenant '$tenant' created with tenantInfo=$tenantInfo" }
        Result.success(tenantInfo)
      } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized to create tenant" }
        Result.failure(e)
      } catch (e: PulsarAdminException.ConflictException) {
        logger.warn { "Tenant '$tenant' already exists" }
        Result.failure(e)
      } catch (e: PulsarAdminException.PreconditionFailedException) {
        logger.warn { "Unable to create tenant: '$tenant' is an invalid tenant name'" }
        Result.failure(e)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to create tenant '$tenant'" }
        Result.failure(e)
      }

  private fun createNamespace(fullNamespace: String, config: Policies): Result<PulsarPolicies> =
      try {
        logger.debug { "Creating namespace '$fullNamespace'" }
        val pulsarPolicies = config.getPulsarPolicies()
        namespaces.createNamespace(fullNamespace, pulsarPolicies)
        logger.info { "Namespace '$fullNamespace' created with policies $pulsarPolicies" }
        Result.success(pulsarPolicies)
      } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized to create namespace '$fullNamespace'" }
        Result.failure(e)
      } catch (e: PulsarAdminException.NotFoundException) {
        logger.debug { "Unable to create namespace '$fullNamespace' as tenant does not exist" }
        Result.failure(e)
      } catch (e: PulsarAdminException.ConflictException) {
        logger.warn { "Namespace '$fullNamespace' already exists" }
        Result.failure(e)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to create namespace '$fullNamespace'" }
        Result.failure(e)
      }

  private fun createTopic(
    topic: String,
    isPartitioned: Boolean,
    messageTTLPolicy: Int
  ): Result<TopicInfo> = try {
    logger.debug { "Creating topic $topic" }
    when (isPartitioned) {
      true -> topics.createPartitionedTopic(topic, DEFAUT_NUM_PARTITIONS)
      false -> topics.createNonPartitionedTopic(topic)
    }
    logger.info {
      "Topic '$topic' created " +
          if (isPartitioned) "with $DEFAUT_NUM_PARTITIONS partitions" else ""
    }
    // set message TTL
    setTopicTTL(topic, messageTTLPolicy)
    // Note: to simplify the implementation, we do not check that message TTL has been rightfully set
    Result.success(TopicInfo(isPartitioned, messageTTLPolicy))
  } catch (e: PulsarAdminException.ConflictException) {
    logger.debug { "Unable to create topic '$topic' that already exists" }
    Result.failure(e)
  } catch (e: PulsarAdminException) {
    logger.warn(e) { "Unable to create topic '$topic'" }
    Result.failure(e)
  }

  private fun getNamespacePolicies(fullNamespace: String): Result<PulsarPolicies?> =
      try {
        logger.debug { "Getting namespace policies" }
        val pulsarPolicies = namespaces.getPolicies(fullNamespace)
        logger.info { "Namespace policies got ($pulsarPolicies)" }
        Result.success(pulsarPolicies)
      } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized to admin namespace '$fullNamespace'" }
        Result.failure(e)
      } catch (e: PulsarAdminException.NotFoundException) {
        Result.success(null)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to get policies of namespace '$fullNamespace" }
        Result.failure(e)
      }

  private fun setTopicTTL(topic: String, messageTTLInSecond: Int): Result<Int> = try {
    logger.debug { "Topic '$topic': setting messageTTLInSecond=$messageTTLInSecond" }
    topicPolicies.setMessageTTL(topic, messageTTLInSecond)
    logger.info { "Topic '$topic': messageTTLInSecond=$messageTTLInSecond set" }
    Result.success(messageTTLInSecond)
  } catch (e: PulsarAdminException) {
    logger.warn(e) { "Topic '$topic': Unable to set message TTL for topic" }
    Result.failure(e)
  }

  private fun getTopicInfo(topic: String): Result<TopicInfo?> {
    val ttl = getMessageTTL(topic).getOrElse { return Result.failure(it) }
    return when (ttl) {
      null -> Result.success(null)
      else -> {
        val metadata = getPartitionedTopicMetadata(topic).getOrElse { return Result.failure(it) }
        when (metadata) {
          null -> Result.success(TopicInfo(false, ttl))
          else -> Result.success(TopicInfo(true, ttl))
        }
      }
    }
  }

  private fun getMessageTTL(topic: String): Result<Int?> =
      try {
        logger.debug { "Topic '$topic': getting MessageTTL" }
        val ttl = topicPolicies.getMessageTTL(topic)
        logger.info { "Topic '$topic': MessageTTL retrieved ($ttl)" }
        Result.success(ttl)
      } catch (e: PulsarAdminException.NotFoundException) {
        logger.debug { "Topic '$topic': Unable to retrieve MessageTTL as topic does not exist" }
        Result.success(null)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to get message TTL for topic '$topic'" }
        Result.failure(e)
      }

  private fun getPartitionedTopicMetadata(topic: String): Result<PartitionedTopicMetadata?> {
    return try {
      logger.debug { "Topic '$topic': getting PartitionedTopicMetadata" }
      val metadata = topics.getPartitionedTopicMetadata(topic)
      logger.info { "Topic '$topic': PartitionedTopicMetadata retrieved ($metadata)" }
      Result.success(metadata)
    } catch (e: PulsarAdminException.NotFoundException) {
      logger.debug { "Topic '$topic': Unable to retrieve PartitionedTopicMetadata as topic does not exist" }
      Result.success(null)
    } catch (e: PulsarAdminException) {
      logger.warn(e) { "Unable to get metadata for topic '$topic'" }
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
          "is different from expected value ($allowedClusters) "
    }
    if (adminRoles != null && adminRoles != tenantInfo.adminRoles) logger.warn {
      "Tenant '$tenant': adminRoles policy (${tenantInfo.adminRoles}) " +
          "is different from expected value ($adminRoles) "
    }
  }


  private fun checkNamespacePolicies(policies: PulsarPolicies, expected: PulsarPolicies) {
    // check that policies are the same
    if (policies.schema_compatibility_strategy != expected.schema_compatibility_strategy) {
      logger.warn {
        "Namespace policy 'schema_compatibility_strategy' (${policies.schema_compatibility_strategy}) " +
            "is different from expected value (${expected.schema_compatibility_strategy})"
      }
    }
    if (policies.autoTopicCreationOverride != expected.autoTopicCreationOverride) {
      logger.warn {
        "Namespace policy 'autoTopicCreationOverride' (${policies.autoTopicCreationOverride}) " +
            "is different from expected value (${expected.autoTopicCreationOverride})"
      }
    }
    if (policies.schema_validation_enforced != expected.schema_validation_enforced) {
      logger.warn {
        "Namespace policy 'schema_validation_enforced' (${policies.schema_validation_enforced}) " +
            "is different from expected value (${expected.schema_validation_enforced})"
      }
    }
    if (policies.is_allow_auto_update_schema != expected.is_allow_auto_update_schema) {
      logger.warn {
        "Namespace policy 'is_allow_auto_update_schema' (${policies.is_allow_auto_update_schema}) " +
            "is different from expected value (${expected.is_allow_auto_update_schema})"
      }
    }
    if (policies.deduplicationEnabled != expected.deduplicationEnabled) {
      logger.warn {
        "Namespace policy 'deduplicationEnabled' (${policies.deduplicationEnabled}) " +
            "is different from expected value (${expected.deduplicationEnabled})"
      }
    }
    if (policies.retention_policies != expected.retention_policies) {
      logger.warn {
        "Namespace policy 'retention_policies policy (${policies.retention_policies}) " +
            "is different from expected value (${expected.retention_policies})"
      }
    }
    if (policies.message_ttl_in_seconds != expected.message_ttl_in_seconds) {
      logger.warn {
        "Namespace policy 'message_ttl_in_seconds policy (${policies.message_ttl_in_seconds}) " +
            "is different from expected value (${expected.message_ttl_in_seconds})"
      }
    }
    if (policies.delayed_delivery_policies != expected.delayed_delivery_policies) {
      logger.warn {
        "Namespace policy 'delayed_delivery_policies policy (${policies.delayed_delivery_policies}) " +
            "is different from expected value (${expected.delayed_delivery_policies})"
      }
    }
  }

  private fun checkTopicInfo(topic: String, topicInfo: TopicInfo, expected: TopicInfo) {

    if (topicInfo.messageTTLPolicy != expected.messageTTLPolicy) logger.warn {
      "Topic '$topic': messageTTLPolicy (${topicInfo.messageTTLPolicy}) " +
          "is different from expected (${expected.messageTTLPolicy})"
    }

    if (expected.isPartitioned && !topicInfo.isPartitioned) logger.warn {
      "Topic '$topic' is expected to be partitioned but is not"
    }

    if (!expected.isPartitioned && topicInfo.isPartitioned) logger.warn {
      "Topic '$topic' is expected to be non-partitioned but it is"
    }
  }

  private fun Policies.getPulsarPolicies() = PulsarPolicies().also {
    it.retention_policies = RetentionPolicies(retentionTimeInMinutes, retentionSizeInMB)
    it.message_ttl_in_seconds = messageTTLInSeconds
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

  companion object {
    // thread-safe set of initialized tenants
    private val initializedTenants = ConcurrentHashMap<String, TenantInfo>()

    // thread-safe set of initialized namespaces
    private val initializedNamespaces = ConcurrentHashMap<String, PulsarPolicies>()

    // thread-safe set of initialized topics (topic name includes tenant and namespace)
    private val initializedTopics = ConcurrentHashMap<String, TopicInfo>()
  }
}
