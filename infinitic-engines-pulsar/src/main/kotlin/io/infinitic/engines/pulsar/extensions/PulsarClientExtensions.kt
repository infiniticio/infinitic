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

package io.infinitic.engines.pulsar.extensions

import io.infinitic.common.monitoringGlobal.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoringPerName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.messages.TaskEngineEnvelope
import io.infinitic.common.workflows.messages.WorkflowEngineEnvelope
import io.infinitic.messaging.pulsar.Topic
import io.infinitic.messaging.pulsar.schemas.schemaDefinition
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionType

fun PulsarClient.newTaskEngineConsumer(): Consumer<TaskEngineEnvelope> =
    newConsumer(Schema.AVRO(schemaDefinition<TaskEngineEnvelope>()))
        .topic(Topic.TASK_ENGINE.get()) // FIXME: We are probably missing an abstraction somewhere to avoid going to the Topic class to properly get a consumer
        .subscriptionName("infinitic-task-engine") // FIXME: Should be in a constant somewhere
        .subscriptionType(SubscriptionType.Key_Shared)
        .subscribe()

fun PulsarClient.newMonitoringPerNameConsumer(): Consumer<MonitoringPerNameEnvelope> =
    newConsumer(Schema.AVRO(schemaDefinition<MonitoringPerNameEnvelope>()))
        .topic(Topic.MONITORING_PER_NAME.get()) // FIXME: We are probably missing an abstraction somewhere to avoid going to the Topic class to properly get a consumer
        .subscriptionName("infinitic-monitoring-per-name-engine") // FIXME: Should be in a constant somewhere
        .subscriptionType(SubscriptionType.Key_Shared)
        .subscribe()

fun PulsarClient.newMonitoringGlobalConsumer(): Consumer<MonitoringGlobalEnvelope> =
    newConsumer(Schema.AVRO(schemaDefinition<MonitoringGlobalEnvelope>()))
        .topic(Topic.MONITORING_GLOBAL.get()) // FIXME: We are probably missing an abstraction somewhere to avoid going to the Topic class to properly get a consumer
        .subscriptionName("infinitic-monitoring-global-engine") // FIXME: Should be in a constant somewhere
        .subscriptionType(SubscriptionType.Key_Shared)
        .subscribe()

fun PulsarClient.newWorkflowEngineConsumer(): Consumer<WorkflowEngineEnvelope> =
    newConsumer(Schema.AVRO(schemaDefinition<WorkflowEngineEnvelope>()))
        .topic(Topic.WORKFLOW_ENGINE.get()) // FIXME: We are probably missing an abstraction somewhere to avoid going to the Topic class to properly get a consumer
        .subscriptionName("infinitic-workflow-engine") // FIXME: Should be in a constant somewhere
        .subscriptionType(SubscriptionType.Key_Shared)
        .subscribe()
