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

package io.infinitic.pulsar.consumers

import io.infinitic.common.monitoring.global.messages.MonitoringGlobalEnvelope
import io.infinitic.common.monitoring.perName.messages.MonitoringPerNameEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromClient
import io.infinitic.pulsar.schemas.schemaDefinition
import io.infinitic.pulsar.topics.MonitoringGlobalTopic
import io.infinitic.pulsar.topics.MonitoringPerNameTopic
import io.infinitic.pulsar.topics.TaskEngineCommandsTopic
import io.infinitic.pulsar.topics.TaskEngineEventsTopic
import io.infinitic.pulsar.topics.TaskExecutorTopic
import io.infinitic.pulsar.topics.WorkflowEngineCommandsTopic
import io.infinitic.pulsar.topics.WorkflowEngineEventsTopic
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionType

fun PulsarClient.messageBuilder() = PulsarMessageBuilderFromClient(this)

fun PulsarClient.newTaskEngineConsumer(): Consumer<TaskEngineEnvelope> =
    newConsumer(Schema.AVRO(schemaDefinition<TaskEngineEnvelope>()))
        .topics(listOf(TaskEngineCommandsTopic.name, TaskEngineEventsTopic.name))
        .subscriptionName("task-engine-consumer") // FIXME: Should be in a constant somewhere
        .subscriptionType(SubscriptionType.Key_Shared)
        .subscribe()

fun PulsarClient.newWorkflowEngineConsumer(): Consumer<WorkflowEngineEnvelope> =
    newConsumer(Schema.AVRO(schemaDefinition<WorkflowEngineEnvelope>()))
        .topics(listOf(WorkflowEngineCommandsTopic.name, WorkflowEngineEventsTopic.name))
        .subscriptionName("workflow-engine-consumer") // FIXME: Should be in a constant somewhere
        .subscriptionType(SubscriptionType.Key_Shared)
        .subscribe()

fun PulsarClient.newMonitoringPerNameEngineConsumer(): Consumer<MonitoringPerNameEnvelope> =
    newConsumer(Schema.AVRO(schemaDefinition<MonitoringPerNameEnvelope>()))
        .topic(MonitoringPerNameTopic.name)
        .subscriptionName("monitoring-per-name-consumer") // FIXME: Should be in a constant somewhere
        .subscriptionType(SubscriptionType.Key_Shared)
        .subscribe()

fun PulsarClient.newMonitoringGlobalEngineConsumer(): Consumer<MonitoringGlobalEnvelope> =
    newConsumer(Schema.AVRO(schemaDefinition<MonitoringGlobalEnvelope>()))
        .topic(MonitoringGlobalTopic.name)
        .subscriptionName("monitoring-global-consumer") // FIXME: Should be in a constant somewhere
        .subscriptionType(SubscriptionType.Key_Shared)
        .subscribe()

fun PulsarClient.newTaskConsumer(name: String): Consumer<TaskExecutorEnvelope> =
    newConsumer(Schema.AVRO(schemaDefinition<TaskExecutorEnvelope>()))
        .topic(TaskExecutorTopic.name(name))
        .subscriptionName("task-executor-consumer-$name") // FIXME: Should be in a constant somewhere
        .subscriptionType(SubscriptionType.Shared)
        .subscribe()
