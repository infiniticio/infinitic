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

package io.infinitic.pulsar.storage

import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.storage.InsertTaskEvent
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.common.workflows.engine.storage.InsertWorkflowEvent
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilder
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromClient
import io.infinitic.pulsar.messageBuilders.PulsarMessageBuilderFromFunction
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.functions.api.Context

class PulsarEventStorage(private val pulsarMessageBuilder: PulsarMessageBuilder) {
    companion object {
        /*
        Create a new PulsarTaskEventStorage from a Pulsar Client
         */
        fun from(client: PulsarClient) = PulsarEventStorage(PulsarMessageBuilderFromClient(client))

        /*
        Create a new PulsarTaskEventStorage from a Pulsar Function Context
         */
        fun from(context: Context) = PulsarEventStorage(PulsarMessageBuilderFromFunction(context))
    }

    /*
    No need to store task events as the task engine topic stores them already
     */
    val insertTaskEvent: InsertTaskEvent = { _: TaskEngineMessage -> }

    /*
    No need to store workflow events as the workflow engine topic stores them already
     */
    val insertWorkflowEvent: InsertWorkflowEvent = { _: WorkflowEngineMessage -> }
}
