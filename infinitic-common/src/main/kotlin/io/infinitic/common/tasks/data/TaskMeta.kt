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

package io.infinitic.common.tasks.data

import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.tasks.data.bases.Meta
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = TaskMetaSerializer::class)
data class TaskMeta(override val serialized: Map<String, SerializedData> = mapOf()) : Meta(serialized) {
    companion object {
        fun from(data: Map<String, Any?>) = TaskMeta(data.mapValues { SerializedData.from(it.value) })
    }
}

operator fun TaskMeta.plus(other: Pair<String, Any?>) =
    TaskMeta(this.serialized + Pair(other.first, SerializedData.from(other.second)))

operator fun TaskMeta.minus(other: String) =
    TaskMeta(this.serialized - other)

object TaskMetaSerializer : KSerializer<TaskMeta> {
    val ser = MapSerializer(String.serializer(), SerializedData.serializer())
    override val descriptor: SerialDescriptor = ser.descriptor
    override fun serialize(encoder: Encoder, value: TaskMeta) { ser.serialize(encoder, value.serialized) }
    override fun deserialize(decoder: Decoder) = TaskMeta(ser.deserialize(decoder))
}
