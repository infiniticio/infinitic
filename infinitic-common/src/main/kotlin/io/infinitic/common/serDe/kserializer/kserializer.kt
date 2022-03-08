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

package io.infinitic.common.serDe.kserializer

import io.infinitic.common.clients.messages.ClientEnvelope
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.engines.messages.TaskEngineEnvelope
import io.infinitic.common.tasks.executors.messages.TaskExecutorEnvelope
import io.infinitic.common.tasks.tags.messages.TaskTagEnvelope
import io.infinitic.common.workflows.engine.messages.WorkflowEngineEnvelope
import io.infinitic.common.workflows.tags.messages.WorkflowTagEnvelope
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun <T : Any> kserializer(klass: KClass<T>) = when (klass) {
    ClientEnvelope::class -> ClientEnvelope.serializer()
    TaskTagEnvelope::class -> TaskTagEnvelope.serializer()
    TaskEngineEnvelope::class -> TaskEngineEnvelope.serializer()
    TaskExecutorEnvelope::class -> TaskExecutorEnvelope.serializer()
    WorkflowEngineEnvelope::class -> WorkflowEngineEnvelope.serializer()
    WorkflowTagEnvelope::class -> WorkflowTagEnvelope.serializer()
    else -> thisShouldNotHappen("applying kserializer with ${klass.qualifiedName}")
} as KSerializer <T>
