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

package io.infinitic.events.methods

import io.infinitic.events.data.Data
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents a collection of method arguments.
 *
 * This class implements the [Collection] interface and delegates all collection operations to the underlying [args] list.
 *
 * @param args The list of [Data] objects representing the method arguments.
 */
@Serializable(with = MethodArgsSerializer::class)
data class MethodArgs(
  val args: List<Data> = listOf()
) : Collection<Data> by args

/**
 * Serializer class for MethodArgs.
 */
object MethodArgsSerializer : KSerializer<MethodArgs> {
  override val descriptor: SerialDescriptor = ListSerializer(Data.serializer()).descriptor

  override fun serialize(encoder: Encoder, value: MethodArgs) {
    ListSerializer(Data.serializer()).serialize(encoder, value.args.toList())
  }

  override fun deserialize(decoder: Decoder) =
      MethodArgs(ListSerializer(Data.serializer()).deserialize(decoder).toList())
}