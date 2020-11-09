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

package io.infinitic.engine.pulsar.workflowManager.functions

import io.infinitic.messaging.api.dispatcher.AvroDispatcher
import io.infinitic.messaging.pulsar.PulsarTransport
import io.infinitic.storage.pulsar.PulsarFunctionStorage
import io.infinitic.common.workflows.avro.AvroConverter
import io.infinitic.engine.workflows.engine.WorkflowEngine
import io.infinitic.engine.workflows.storages.AvroKeyValueWorkflowStateStorage
import io.infinitic.avro.workflowManager.messages.envelopes.AvroEnvelopeForWorkflowEngine
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class WorkflowEnginePulsarFunction : Function<AvroEnvelopeForWorkflowEngine, Void> {

    override fun process(input: AvroEnvelopeForWorkflowEngine, context: Context?): Void? = runBlocking {
        val ctx = context ?: throw NullPointerException("Null Context received")

        try {
            val message = AvroConverter.fromWorkflowEngine(input)

            getWorkflowEngine(context).handle(message)
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        return@runBlocking null
    }

    internal fun getWorkflowEngine(context: Context): WorkflowEngine {
        val storage = AvroKeyValueWorkflowStateStorage(PulsarFunctionStorage(context))
        val dispatcher = AvroDispatcher(PulsarTransport.forPulsarFunctionContext(context))

        return WorkflowEngine(storage, dispatcher)
    }
}
