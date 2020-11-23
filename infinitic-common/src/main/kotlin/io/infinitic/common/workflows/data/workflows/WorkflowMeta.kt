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

package io.infinitic.common.workflows.data.workflows

import io.infinitic.common.data.Meta
import io.infinitic.common.serDe.SerializedData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = WorkflowMetaSerializer::class)
data class WorkflowMeta(override val serialized: Map<String, SerializedData> = mapOf()) : Meta(serialized) {
    companion object {
        fun from(data: Map<String, Any?>) = WorkflowMeta(data.mapValues { SerializedData.from(it) })
    }
}

operator fun WorkflowMeta.plus(other: Pair<String, Any?>) =
    WorkflowMeta(this.serialized + Pair(other.first, SerializedData.from(other.second)))

operator fun WorkflowMeta.minus(other: String) =
    WorkflowMeta(this.serialized - other)

object WorkflowMetaSerializer : KSerializer<WorkflowMeta> {
    val ser = MapSerializer(String.serializer(), SerializedData.serializer())
    override val descriptor: SerialDescriptor = ser.descriptor
    override fun serialize(encoder: Encoder, value: WorkflowMeta) { ser.serialize(encoder, value.serialized) }
    override fun deserialize(decoder: Decoder) = WorkflowMeta(ser.deserialize(decoder))
}
