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

package io.infinitic.common.messages

import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.tags.messages.TaskTagEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.tags.messages.WorkflowTagEnvelope
import org.apache.avro.Schema
import kotlin.reflect.KClass

interface Envelope<T> {
    fun message(): T
}

@Suppress("UNCHECKED_CAST")
fun <T : Envelope<*>> KClass<T>.fromByteArray(bytes: ByteArray, schema: Schema) = when (this) {
    ClientEnvelope::class -> ClientEnvelope.fromByteArray(bytes, schema)
    TaskTagEnvelope::class -> TaskTagEnvelope.fromByteArray(bytes, schema)
    TaskExecutorEnvelope::class -> TaskExecutorEnvelope.fromByteArray(bytes, schema)
    WorkflowEngineEnvelope::class -> WorkflowEngineEnvelope.fromByteArray(bytes, schema)
    WorkflowTagEnvelope::class -> WorkflowTagEnvelope.fromByteArray(bytes, schema)
    else -> thisShouldNotHappen("applying fromByteArray() on $qualifiedName")
} as T

fun <T : Envelope<*>> KClass<T>.schema() = when (this) {
    ClientEnvelope::class -> ClientEnvelope.schema
    TaskTagEnvelope::class -> TaskTagEnvelope.schema
    TaskExecutorEnvelope::class -> TaskExecutorEnvelope.schema
    WorkflowEngineEnvelope::class -> WorkflowEngineEnvelope.schema
    WorkflowTagEnvelope::class -> WorkflowTagEnvelope.schema
    else -> thisShouldNotHappen("applying schema() on $qualifiedName")
}

fun <T : Envelope<*>> T.toByteArray() = when (this) {
    is ClientEnvelope -> this.toByteArray()
    is TaskTagEnvelope -> this.toByteArray()
    is TaskExecutorEnvelope -> this.toByteArray()
    is WorkflowEngineEnvelope -> this.toByteArray()
    is WorkflowTagEnvelope -> this.toByteArray()
    else -> thisShouldNotHappen("applying fromByteArray() on $this")
}
