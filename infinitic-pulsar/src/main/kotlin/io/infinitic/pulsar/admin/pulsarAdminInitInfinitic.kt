
/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.pulsar.admin

import io.infinitic.pulsar.PulsarInfiniticAdmin
import io.infinitic.pulsar.schemas.getPostSchemaPayload
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.apache.pulsar.common.policies.data.AutoTopicCreationOverride
import org.apache.pulsar.common.policies.data.Policies
import org.apache.pulsar.common.policies.data.RetentionPolicies
import org.apache.pulsar.common.policies.data.SchemaCompatibilityStrategy
import org.apache.pulsar.common.policies.data.TenantInfo
import org.apache.pulsar.common.policies.data.TopicType
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger(PulsarInfiniticAdmin::class.java.name)

suspend fun PulsarAdmin.setupInfinitic(tenant: String, namespace: String, allowedClusters: Set<String>?) {
    createTenant(this, tenant, getAllowedClusters(this, allowedClusters))

    createNamespace(this, tenant, namespace)
}

private suspend fun getAllowedClusters(admin: PulsarAdmin, allowedClusters: Set<String>? = null): Set<String> {
    // get all existing clusters
    val existingClusters: Set<String> = admin.clusters().clustersAsync.await().toSet()

    // if authorized clusters are provided, check that they exist
    allowedClusters?.map {
        if (! existingClusters.contains(it)) throw RuntimeException("Unknown cluster $it")
    }

    // if authorizedClusters is not provided, default to all clusters
    return allowedClusters ?: existingClusters
}

private suspend fun createTenant(admin: PulsarAdmin, tenant: String, allowedClusters: Set<String>) {
    // create Infinitic tenant info
    // if authorizedClusters is not provided, default is all clusters
    val tenantInfo = TenantInfo().apply {
        this.allowedClusters = allowedClusters
    }

    // get all existing tenant
    val tenants = admin.tenants().tenantsAsync.await()

    // create or update infinitic tenant
    if (!tenants.contains(tenant)) {
        logger.info { "Creating tenant $tenant with info $tenantInfo" }
        admin.tenants().createTenantAsync(tenant, tenantInfo).await()
    } else {
        logger.info { "Updating tenant $tenant with info $tenantInfo" }
        admin.tenants().updateTenantAsync(tenant, tenantInfo).await()
    }
}

private suspend fun createNamespace(admin: PulsarAdmin, tenant: String, namespace: String) {
    // get all existing namespaces
    val existingNamespaces = admin.namespaces().getNamespacesAsync(tenant).await()

    // create namespace if it does not exist
    val fullNamespace = getFullNamespace(tenant, namespace)

    if (!existingNamespaces.contains(fullNamespace)) {
        val policies = Policies().apply {

            // enable message deduplication
            deduplicationEnabled = true
            // all new topics (especially tasks and workflows) are partitioned
            autoTopicCreationOverride = AutoTopicCreationOverride(
                true,
                TopicType.PARTITIONED.toString(),
                1
            )
            // default retention : 7j || 1Gb - msg is kept 7 days after acknowledgement (up to 1 Gb)
            retention_policies = RetentionPolicies(
                60 * 24 * 7,
                1024
            )
            // default ttl : 14j - after 14 days a message is automatically acknowledged
            message_ttl_in_seconds = 3600 * 24 * 14
            // schema are mandatory for producers/consumers
            schema_validation_enforced = true
            // this allow topic auto creation for task / workflows
            is_allow_auto_update_schema = true
            // Changes allowed: add optional fields, delete fields
            schema_compatibility_strategy = SchemaCompatibilityStrategy.BACKWARD_TRANSITIVE
        }
        logger.info { "Creating namespace $fullNamespace with policies $policies" }
        admin.namespaces().createNamespaceAsync(fullNamespace, policies).await()
    }
}

private suspend fun createPartitionedTopic(admin: PulsarAdmin, topic: String) {
    // create topic as partitioned topic with one partition
    try {
        logger.info { "Creating partitioned topic $topic" }
        admin.topics().createPartitionedTopicAsync(topic, 1).await()
    } catch (e: PulsarAdminException.ConflictException) {
        logger.info { "Topic $topic already exist" }
        // the topic already exists
    }
}

private suspend fun <T : Any> setSchema(admin: PulsarAdmin, topic: String, klass: KClass<T>) {
    val schema = getPostSchemaPayload(klass)
    logger.info("Uploading topic {} with schema {}", topic, schema)
    admin.schemas().createSchemaAsync(topic, schema).await()
}

private fun getFullNamespace(tenantName: String, namespace: String) =
    "$tenantName/$namespace"
