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

package io.infinitic.worker.executor.pulsar.functions

import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.messaging.pulsar.extensions.messageBuilder
import io.infinitic.messaging.pulsar.senders.getSendToTaskEngine
import io.infinitic.tasks.executor.TaskExecutor
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

open class WorkerPulsarFunction : Function<TaskExecutorEnvelope, Void> {

    override fun process(envelope: TaskExecutorEnvelope, context: Context?): Void? = runBlocking {
        val ctx = context ?: throw NullPointerException("Null Context received")

        try {
            getWorker(context).handle(envelope.message())
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, envelope)
            throw e
        }

        return@runBlocking null
    }

    fun getWorker(context: Context): TaskExecutor {
        return TaskExecutor(getSendToTaskEngine(context.messageBuilder()))
    }
}
