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

package io.infinitic.engines.pulsar.functions

import io.infinitic.common.tasks.messages.TaskEngineEnvelope
import io.infinitic.messaging.pulsar.extensions.keyValueStorage
import io.infinitic.messaging.pulsar.extensions.messageBuilder
import io.infinitic.messaging.pulsar.senders.getSendToMonitoringPerName
import io.infinitic.messaging.pulsar.senders.getSendToTaskEngine
import io.infinitic.messaging.pulsar.senders.getSendToWorkers
import io.infinitic.messaging.pulsar.senders.getSendToWorkflowEngine
import io.infinitic.tasks.engine.storage.TaskStateKeyValueStorage
import io.infinitic.workflows.engine.taskEngineInWorkflowEngine
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

    internal fun getTaskEngine(context: Context) =
        taskEngineInWorkflowEngine(
            TaskStateKeyValueStorage(context.keyValueStorage()),
            getSendToWorkflowEngine(context.messageBuilder()),
            getSendToTaskEngine(context.messageBuilder()),
            getSendToMonitoringPerName(context.messageBuilder()),
            getSendToWorkers(context.messageBuilder())
        )
}
