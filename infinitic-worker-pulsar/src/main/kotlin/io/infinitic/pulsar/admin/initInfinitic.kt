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

import io.infinitic.common.monitoring.global.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.pulsar.schemas.getPostSchemaPayload
import io.infinitic.pulsar.topics.MonitoringGlobalTopic
import io.infinitic.pulsar.topics.MonitoringPerNameTopic
import io.infinitic.pulsar.topics.TaskEngineCommandsTopic
import io.infinitic.pulsar.topics.TaskEngineEventsTopic
import io.infinitic.pulsar.topics.WorkflowEngineCommandsTopic
import io.infinitic.pulsar.topics.WorkflowEngineEventsTopic
import org.apache.pulsar.client.admin.PulsarAdmin
import org.apache.pulsar.client.admin.PulsarAdminException
import org.apache.pulsar.common.policies.data.TenantInfo
import kotlin.reflect.KClass

fun PulsarAdmin.initInfinitic(tenant: String, namespace: String, allowedClusters: Set<String>? = null) {
    createTenant(this, tenant, getAllowedClusters(this, allowedClusters))

    createNamespace(this, tenant, namespace)

    createPartitionedTopic(this, tenant, namespace, WorkflowEngineCommandsTopic.name)
    createPartitionedTopic(this, tenant, namespace, WorkflowEngineEventsTopic.name)
    createPartitionedTopic(this, tenant, namespace, TaskEngineCommandsTopic.name)
    createPartitionedTopic(this, tenant, namespace, TaskEngineEventsTopic.name)
    createPartitionedTopic(this, tenant, namespace, MonitoringPerNameTopic.name)
    createPartitionedTopic(this, tenant, namespace, MonitoringGlobalTopic.name)

    setSchema(this, tenant, namespace, WorkflowEngineCommandsTopic.name, WorkflowEngineEnvelope::class)
    setSchema(this, tenant, namespace, WorkflowEngineEventsTopic.name, WorkflowEngineEnvelope::class)
    setSchema(this, tenant, namespace, TaskEngineCommandsTopic.name, TaskEngineEnvelope::class)
    setSchema(this, tenant, namespace, TaskEngineEventsTopic.name, TaskEngineEnvelope::class)
    setSchema(this, tenant, namespace, MonitoringPerNameTopic.name, MonitoringPerNameEnvelope::class)
    setSchema(this, tenant, namespace, MonitoringGlobalTopic.name, MonitoringGlobalEnvelope::class)
}

private fun getAllowedClusters(admin: PulsarAdmin, allowedClusters: Set<String>? = null): Set<String> {
    // get all existing clusters
    val existingClusters: Set<String> = admin.clusters().clusters.toSet()

    // if authorized clusters are provided, check that they exist
    allowedClusters?.map {
        if (! existingClusters.contains(it)) throw RuntimeException("Unknown cluster $it")
    }

    // if authorizedClusters is not provided, default to all clusters
    return allowedClusters ?: existingClusters
}

private fun createTenant(admin: PulsarAdmin, tenant: String, allowedClusters: Set<String>) {
    // create Infinitic tenant info
    // if authorizedClusters is not provided, default is all clusters
    val tenantInfo = TenantInfo().apply {
        this.allowedClusters = allowedClusters
    }

    // get all existing tenant
    val tenants: List<String> = admin.tenants().tenants

    // create or update infinitic tenant
    if (!tenants.contains(tenant)) {
        admin.tenants().createTenant(tenant, tenantInfo)
    } else {
        admin.tenants().updateTenant(tenant, tenantInfo)
    }
}

private fun createNamespace(admin: PulsarAdmin, tenant: String, namespace: String) {
    // get all existing namespaces
    val existingNamespaces = admin.namespaces().getNamespaces(tenant)

    // create namespace if it does not exist
    val fullNamespace = getFullNamespace(tenant, namespace)

    if (!existingNamespaces.contains(fullNamespace)) {
        admin.namespaces().createNamespace(fullNamespace)
    }
}

private fun createPartitionedTopic(admin: PulsarAdmin, tenant: String, namespace: String, topic: String) {
    // create topic as partitioned topic with one partition
    val topicFullName = getPersistentTopicFullName(tenant, namespace, topic)

    try {
        admin.topics().createPartitionedTopic(topicFullName, 1)
    } catch (e: PulsarAdminException.ConflictException) {
        // the topic already exist
    }
}

private fun <T : Any> setSchema(admin: PulsarAdmin, tenant: String, namespace: String, topic: String, klass: KClass<T>) {
    admin.schemas().createSchema(
        getPersistentTopicFullName(tenant, namespace, topic),
        getPostSchemaPayload(klass)
    )
}

private fun getFullNamespace(tenantName: String, namespace: String) =
    "$tenantName/$namespace"

private fun getPersistentTopicFullName(tenantName: String, namespace: String, topic: String) =
    "persistent://$tenantName/$namespace/$topic"
