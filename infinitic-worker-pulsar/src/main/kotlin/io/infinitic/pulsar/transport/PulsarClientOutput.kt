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

package io.infinitic.pulsar.transport

import io.infinitic.client.transport.ClientOutput
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.transport.SendToTaskEngine
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.transport.SendToWorkflowEngine
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilder
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromClient
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromFunction
import io.infinitic.pulsar.messageBuilders.sendPulsarMessage
import io.infinitic.pulsar.topics.TaskEngineCommandsTopic
import io.infinitic.pulsar.topics.WorkflowEngineCommandsTopic
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.functions.api.Context

class PulsarClientOutput(private val pulsarMessageBuilder: PulsarMessageBuilder) : ClientOutput {
    companion object {
        /*
        Create a new PulsarTransport from a Pulsar Client
         */
        fun from(client: PulsarClient) = PulsarClientOutput(PulsarMessageBuilderFromClient(client))

        /*
        Create a new PulsarTransport from a Pulsar Function Context
         */
        fun from(context: Context) = PulsarClientOutput(PulsarMessageBuilderFromFunction(context))
    }

    override val sendToTaskEngine: SendToTaskEngine = { message: TaskEngineMessage, after: Float ->
        pulsarMessageBuilder.sendPulsarMessage(
            TaskEngineCommandsTopic.name,
            TaskEngineEnvelope.from(message),
            "${message.taskId}",
            after
        )
    }

    override val sendToWorkflowEngine: SendToWorkflowEngine = { message: WorkflowEngineMessage, after: Float ->
        pulsarMessageBuilder.sendPulsarMessage(
            WorkflowEngineCommandsTopic.name,
            WorkflowEngineEnvelope.from(message),
            "${message.workflowId}",
            after
        )
    }
}
