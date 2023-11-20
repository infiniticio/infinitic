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
import io.infinitic.pulsar.config.Pulsar
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.apache.pulsar.common.policies.data.PartitionedTopicStats
import org.apache.pulsar.common.policies.data.Policies
import org.apache.pulsar.common.policies.data.RetentionPolicies
import org.apache.pulsar.common.policies.data.TenantInfo
import org.apache.pulsar.common.policies.data.TopicStats
import org.apache.pulsar.common.policies.data.impl.AutoTopicCreationOverrideImpl
import org.apache.pulsar.common.policies.data.impl.DelayedDeliveryPoliciesImpl
import org.apache.pulsar.common.policies.data.TopicType as PulsarTopicType

class PulsarInfiniticAdmin(
  val pulsarAdmin: PulsarAdmin,
  val pulsar: Pulsar,
) {
  private val clusters = pulsarAdmin.clusters()
  private val topics = pulsarAdmin.topics()
  private val topicPolicies = pulsarAdmin.topicPolicies()
  private val tenants = pulsarAdmin.tenants()
  private val namespaces = pulsarAdmin.namespaces()

  private val fullNamespace = "${pulsar.tenant}/${pulsar.namespace}"

  private val expectedPolicies by lazy {
    Policies().apply {
      retention_policies =
          RetentionPolicies(
              pulsar.policies.retentionTimeInMinutes,
              pulsar.policies.retentionSizeInMB,
          )
      message_ttl_in_seconds = pulsar.policies.messageTTLInSeconds
      delayed_delivery_policies =
          DelayedDeliveryPoliciesImpl(pulsar.policies.delayedDeliveryTickTimeMillis, true)
      schema_compatibility_strategy = pulsar.policies.schemaCompatibilityStrategy
      autoTopicCreationOverride =
          AutoTopicCreationOverrideImpl(
              pulsar.policies.allowAutoTopicCreation,
              PulsarTopicType.PARTITIONED.toString(),
              3,
          )
      schema_validation_enforced = pulsar.policies.schemaValidationEnforced
      is_allow_auto_update_schema = pulsar.policies.isAllowAutoUpdateSchema
      deduplicationEnabled = pulsar.policies.deduplicationEnabled
    }
  }

  private val tenantInfo by lazy {
    TenantInfo.builder()
        .allowedClusters(
            when (pulsar.allowedClusters) {
              // all cluster by default
              null -> clusters.clusters.toSet()
              else -> pulsar.allowedClusters
            },
        )
        .also { if (pulsar.adminRoles != null) it.adminRoles(pulsar.adminRoles) }
        .build()
  }

  /**
   * Ensure tenant and namespace exists.
   *
   * Returns:
   *  - Result.success(Unit) if tenant and namespace exist or has been created
   *  - Result.failure(e) in case of error
   */
  fun initTenantAndNamespace(): Result<Unit> {
    initTenant().getOrElse { return Result.failure(it) }
    initNamespace().getOrElse { return Result.failure(it) }

    return Result.success(Unit)
  }

  /**
   * Ensure tenant exists.
   *
   * Returns:
   *  - Result.success(TenantInfo) if tenant exists or has been created
   *  - Result.failure(e) in case of error
   **/
  fun initTenant(): Result<TenantInfo> {
    val info = getTenantInfo().getOrElse { return Result.failure(it) }
      ?: createTenant().getOrElse { return Result.failure(it) }

    return Result.success(info)
  }

  /**
   * Get Tenant Info.
   *
   * Returns:
   *  - Result.success(TenantInfo) if tenant exists
   *  - Result.success(null) if tenant does not exist
   *  - Result.failure(e) in case of error
   **/
  fun getTenantInfo(): Result<TenantInfo?> =
      try {
        val info = tenants.getTenantInfo(pulsar.tenant)
        Result.success(info)
      } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized to admin tenant '${pulsar.tenant}'" }
        Result.failure(e)
      } catch (e: PulsarAdminException.NotFoundException) {
        Result.success(null)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to get info for tenant '${pulsar.tenant}'" }
        Result.failure(e)
      }

  /**
   * Create tenant.
   *
   * Returns:
   * - Result.success(TenantInfo) in case of success or already existing tenant
   * - Result.failure(e) in case of error
   */
  fun createTenant(): Result<TenantInfo> =
      try {
        logger.info { "Creating tenant '${pulsar.tenant}'" }
        tenants.createTenant(pulsar.tenant, tenantInfo)
        Result.success(tenantInfo)
      } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized to admin namespace '$fullNamespace'" }
        Result.failure(e)
      } catch (e: PulsarAdminException.ConflictException) {
        logger.warn { "Tenant '${pulsar.tenant}' already exists" }
        Result.failure(e)
      } catch (e: PulsarAdminException.PreconditionFailedException) {
        logger.warn { "Unable to create tenant: '${pulsar.tenant}' is an invalid tenant name'" }
        Result.failure(e)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to create tenant '${pulsar.tenant}'" }
        Result.failure(e)
      }

  /**
   * Ensure namespace exists.
   *
   * Returns:
   *  - Result.success(Policies) if tenant exists or has been created
   *  - Result.failure(e) in case of error
   **/
  fun initNamespace(): Result<Policies> {

    val policies =
        getNamespacePolicies().getOrElse { return Result.failure(it) }
            ?.also { checkNamespacePolicies(it) }
          ?: createNamespace().getOrElse { return Result.failure(it) }

    return Result.success(policies)
  }


  /**
   * Get Policies for current namespace
   *
   * Returns:
   *  - Result.success(Policies) if namespace exists
   *  - Result.success(null) if namespace does not exist
   *  - Result.failure(e) in case of error
   **/
  fun getNamespacePolicies(): Result<Policies?> =
      try {
        val policies = namespaces.getPolicies(fullNamespace)
        Result.success(policies)
      } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized to admin namespace '$fullNamespace'" }
        Result.failure(e)
      } catch (e: PulsarAdminException.NotFoundException) {
        Result.success(null)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to get policies of namespace '$fullNamespace" }
        Result.failure(e)
      }

  /**
   * Create namespace
   *
   * Returns:
   * - Result.success(Policies) in case of success or already existing namespace
   * - Result.failure(e) in case of error
   */
  fun createNamespace(): Result<Policies> =
      try {
        logger.info { "Creating namespace $fullNamespace" }
        namespaces.createNamespace(fullNamespace, expectedPolicies)
        Result.success(expectedPolicies)
      } catch (e: PulsarAdminException.NotAuthorizedException) {
        logger.warn { "Not authorized to create a namespace" }
        Result.failure(e)
      } catch (e: PulsarAdminException.NotFoundException) {
        logger.warn {
          "Unable to create namespace '$fullNamespace' as tenant '${pulsar.tenant}' does not exist"
        }
        Result.failure(e)
      } catch (e: PulsarAdminException.ConflictException) {
        logger.warn { "Namespace '$fullNamespace' already exists" }
        Result.failure(e)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to create namespace '$fullNamespace'" }
        Result.failure(e)
      }

  /**
   * Get set of topics' name for current namespace
   *
   * Returns:
   *  - Result.success(Set<String>)
   *  - Result.failure(e) in case of error
   **/
  fun getTopicsSet(): Result<Set<String>> =
      try {
        val topicSet = with(topics) {
          (getPartitionedTopicList(fullNamespace) + getList(fullNamespace)).toSet()
        }
        Result.success(topicSet)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to get topics for namespace '$fullNamespace'" }
        Result.failure(e)
      }

  /**
   * Ensure topic exists.
   *
   * Returns:
   *  - Result.success(Unit) if topic exists or has been created
   *  - Result.failure(e) in case of error
   **/
  fun initTopic(topic: String, isPartitioned: Boolean, isDelayed: Boolean): Result<Unit> {
    getMessageTTL(topic).getOrElse { return Result.failure(it) }
        ?.also { checkMessageTTL(topic, it, isDelayed) }
      ?: createTopic(topic, isPartitioned, isDelayed).getOrElse { return Result.failure(it) }

    return Result.success(Unit)
  }

  /**
   * Get message TTL for topic
   *
   * Returns:
   *  - Result.success(Int) if topic exists
   *  - Result.success(null) if topic does not exist
   *  - Result.failure(e) in case of error
   **/
  fun getMessageTTL(topic: String): Result<Int?> =
      try {
        val ttl = topicPolicies.getMessageTTL(topic)
        Result.success(ttl)
      } catch (e: PulsarAdminException.NotFoundException) {
        Result.success(null)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to get message TTL for topic '$topic'" }
        Result.failure(e)
      }

  /**
   * Get stats for non-partitioned topic
   *
   * Returns:
   *  - Result.success(TopicStats) if topic exists
   *  - Result.success(null) if topic does not exist
   *  - Result.failure(e) in case of error
   **/
  fun getTopicStats(topic: String): Result<TopicStats?> {
    return try {
      val stats = topics.getStats(topic, false, false, false)
      Result.success(stats)
    } catch (e: PulsarAdminException.NotFoundException) {
      Result.success(null)
    } catch (e: PulsarAdminException) {
      logger.warn(e) { "Unable to get stats for topic '$topic'" }
      Result.failure(e)
    }
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
      val stats = topics.getPartitionedStats(topic, false, false, false, false)
      Result.success(stats)
    } catch (e: PulsarAdminException.NotFoundException) {
      Result.success(null)
    } catch (e: PulsarAdminException) {
      logger.warn(e) { "Unable to get stats for topic '$topic'" }
      Result.failure(e)
    }
  }

  /**
   * Create topic.
   *
   * Returns:
   * - Result.success(Unit) in case of success or already existing topic
   * - Result.failure(e) in case of error
   */
  fun createTopic(topic: String, isPartitioned: Boolean, isDelayed: Boolean): Result<Unit> =
      try {
        logger.info { "Creating topic $topic" }
        when (isPartitioned) {
          true -> topics.createPartitionedTopic(topic, 3)
          false -> topics.createNonPartitionedTopic(topic)
        }
        if (isDelayed) setTopicTTL(topic, pulsar.policies.delayedTTLInSeconds)
        else Result.success(Unit)
      } catch (e: PulsarAdminException.ConflictException) {
        logger.warn { "Already existing topic '$topic'" }
        Result.success(Unit)
      } catch (e: PulsarAdminException) {
        logger.warn(e) { "Unable to create topic '$topic'" }
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
    logger.info { "Deleting topic $topic" }
    topics.delete(topic, true)
    Result.success(Unit)
  } catch (e: PulsarAdminException.NotFoundException) {
    logger.warn { "Topic '$topic' does not exist" }
    Result.success(Unit)
  } catch (e: PulsarAdminException) {
    logger.warn(e) { "Unable to delete topic '$topic'" }
    Result.failure(e)
  }

  private fun setTopicTTL(topic: String, ttl: Int): Result<Unit> = try {
    topicPolicies.setMessageTTL(topic, ttl)
    Result.success(Unit)
  } catch (e: PulsarAdminException) {
    logger.warn(e) { "Unable to set message TTL for topic '$topic'" }
    Result.failure(e)
  }

  private fun checkMessageTTL(topic: String, ttl: Int, isDelayed: Boolean) {
    when (isDelayed) {
      true -> if (ttl != pulsar.policies.delayedTTLInSeconds) {
        logger.warn {
          "For topic $topic, messageTTLInSeconds policy is different from expected value: " +
              "$ttl != ${pulsar.policies.delayedTTLInSeconds}"
        }
      }

      false ->
        if (ttl != pulsar.policies.messageTTLInSeconds) {
          logger.warn {
            "For topic $topic, messageTTLInSeconds policy is different from expected value: " +
                "$ttl != ${pulsar.policies.messageTTLInSeconds}"
          }
        }
    }
  }

  private fun checkNamespacePolicies(policies: Policies) {
    // check that policies are the same
    if (policies.schema_compatibility_strategy != expectedPolicies.schema_compatibility_strategy) {
      logger.warn {
        "Namespace policy 'schema_compatibility_strategy' is different from expected value: " +
            "${policies.schema_compatibility_strategy} != ${expectedPolicies.schema_compatibility_strategy}"
      }
    }
    if (policies.autoTopicCreationOverride != expectedPolicies.autoTopicCreationOverride) {
      logger.warn {
        "Namespace policy 'autoTopicCreationOverride' is different from expected value: " +
            "${policies.autoTopicCreationOverride} != ${expectedPolicies.autoTopicCreationOverride}"
      }
    }
    if (policies.schema_validation_enforced != expectedPolicies.schema_validation_enforced) {
      logger.warn {
        "Namespace policy 'schema_validation_enforced' is different from expected value: " +
            "${policies.schema_validation_enforced} != ${expectedPolicies.schema_validation_enforced}"
      }
    }
    if (policies.is_allow_auto_update_schema != expectedPolicies.is_allow_auto_update_schema) {
      logger.warn {
        "Namespace policy 'is_allow_auto_update_schema' is different from expected value: " +
            "${policies.is_allow_auto_update_schema} != ${expectedPolicies.is_allow_auto_update_schema}"
      }
    }
    if (policies.deduplicationEnabled != expectedPolicies.deduplicationEnabled) {
      logger.warn {
        "Namespace policy 'deduplicationEnabled' is different from expected value: " +
            "${policies.deduplicationEnabled} != ${expectedPolicies.deduplicationEnabled}"
      }
    }
    if (policies.retention_policies != expectedPolicies.retention_policies) {
      logger.warn {
        "Namespace policy 'retention_policies policy is different from expected value: " +
            "${policies.retention_policies} != ${expectedPolicies.retention_policies}"
      }
    }
    if (policies.message_ttl_in_seconds != expectedPolicies.message_ttl_in_seconds) {
      logger.warn {
        "Namespace policy 'message_ttl_in_seconds policy is different from expected value: " +
            "${policies.message_ttl_in_seconds} != ${expectedPolicies.message_ttl_in_seconds}"
      }
    }
    if (policies.delayed_delivery_policies != expectedPolicies.delayed_delivery_policies) {
      logger.warn {
        "Namespace policy 'delayed_delivery_policies policy is different from expected value: " +
            "${policies.delayed_delivery_policies} != ${expectedPolicies.delayed_delivery_policies}"
      }
    }
  }

  companion object {
    internal val logger = KotlinLogging.logger {}

    /** Create InfiniticAdmin from file in resources directory */
    @JvmStatic
    fun fromResource(vararg resources: String): PulsarInfiniticAdmin {
      val config = AdminConfig.fromResource(*resources)
      return PulsarInfiniticAdmin(config.pulsar.admin, config.pulsar)
    }

    /** Create InfiniticAdmin from file in system file */
    @JvmStatic
    fun fromFile(vararg files: String): PulsarInfiniticAdmin {
      val config = AdminConfig.fromFile(*files)
      return PulsarInfiniticAdmin(config.pulsar.admin, config.pulsar)
    }
  }
}
