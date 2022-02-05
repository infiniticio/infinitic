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

package io.infinitic.pulsar.functions

import io.infinitic.cache.caffeine.Caffeine
import io.infinitic.cache.caffeine.CaffeineKeyValueCache
import io.infinitic.common.data.ClientName
import io.infinitic.common.storage.keyValue.CachedKeyValueStorage
import io.infinitic.common.tasks.engine.messages.TaskEngineEnvelope
import io.infinitic.pulsar.functions.storage.keyValueStorage
import io.infinitic.pulsar.transport.PulsarOutput
import io.infinitic.tasks.engine.TaskEngine
import io.infinitic.tasks.engine.storage.BinaryTaskStateStorage
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class TaskEnginePulsarFunction : Function<TaskEngineEnvelope, Void> {

    override fun process(envelope: TaskEngineEnvelope, context: Context?): Void? = runBlocking {
        val ctx = context ?: throw NullPointerException("Null Context received")

        try {
            getTaskEngine(ctx).handle(envelope.message())
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, envelope)
            throw e
        }

        null
    }

    internal fun getTaskEngine(context: Context): TaskEngine {
        val output = PulsarOutput.from(context)

        return TaskEngine(
            ClientName(""),
            BinaryTaskStateStorage(
                // context storage decorated with logging and a 1h cache
                CachedKeyValueStorage(
                    CaffeineKeyValueCache(Caffeine(expireAfterAccess = 3600)),
                    context.keyValueStorage()
                )
            ),
            output.sendToClient(),
            output.sendToTaskTagEngine(),
            output.sendToTaskEngineAfter(),
            output.sendToWorkflowEngine(),
            output.sendToTaskExecutors(),
            output.sendToMetricsPerName()
        )
    }
}
