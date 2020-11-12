// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.engines.pulsar.functions

import io.infinitic.engines.workflows.engine.WorkflowEngine
import io.infinitic.engines.workflows.storage.WorkflowStateKeyValueStorage
import io.infinitic.common.workflows.messages.WorkflowEngineEnvelope
import io.infinitic.messaging.pulsar.extensions.keyValueStorage
import io.infinitic.messaging.pulsar.extensions.messageBuilder
import io.infinitic.messaging.pulsar.senders.getSendToTaskEngine
import io.infinitic.messaging.pulsar.senders.getSendToWorkflowEngine
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class WorkflowEnginePulsarFunction : Function<WorkflowEngineEnvelope, Void> {

    override fun process(envelope: WorkflowEngineEnvelope, context: Context?): Void? = runBlocking {
        val ctx = context ?: throw NullPointerException("Null Context received")

        try {
            getWorkflowEngine(ctx).handle(envelope.message())
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, envelope)
            throw e
        }

        null
    }

    internal fun getWorkflowEngine(context: Context) =
        WorkflowEngine(
            WorkflowStateKeyValueStorage(context.keyValueStorage()),
            getSendToWorkflowEngine(context.messageBuilder()),
            getSendToTaskEngine(context.messageBuilder())
        )
}
