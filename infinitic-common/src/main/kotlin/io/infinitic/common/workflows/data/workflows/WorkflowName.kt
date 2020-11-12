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

import io.infinitic.common.data.Name
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.lang.reflect.Method

@Serializable(with = WorkflowNameSerializer::class)
data class WorkflowName(override val name: String) : Name(name) {
    companion object {
        fun from(method: Method) = WorkflowName(method.declaringClass.name)
    }
}

object WorkflowNameSerializer : KSerializer<WorkflowName> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("WorkflowName", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: WorkflowName) { encoder.encodeString(value.name) }
    override fun deserialize(decoder: Decoder) = WorkflowName(decoder.decodeString())
}
