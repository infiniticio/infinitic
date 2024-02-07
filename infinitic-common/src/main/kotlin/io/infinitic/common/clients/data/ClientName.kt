/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.common.clients.data

import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.utils.JsonAble
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import java.lang.reflect.Method

@JvmInline
@Serializable
value class ClientName(private val name: String) : JsonAble {
  companion object {
    fun from(method: Method) = ClientName(method.declaringClass.name)
    fun from(emitterName: EmitterName) = ClientName(emitterName.toString())
  }

  override fun toString() = name

  override fun toJson() = JsonPrimitive(name)
}
