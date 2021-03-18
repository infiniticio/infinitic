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

package io.infinitic.common.workflows.data.channels

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ChannelEventFilterSerializer::class)
data class ChannelEventFilter(val jsonPath: String) {
    override fun toString() = jsonPath

    companion object {
        init {
            Configuration.setDefaults(object : Configuration.Defaults {
                override fun jsonProvider() = JacksonJsonProvider()
                override fun mappingProvider() = JacksonMappingProvider()
                override fun options() = setOf(Option.ALWAYS_RETURN_LIST)
            })
        }
    }

    /**
     * Evaluate if an event is filtered by the provided jsonPath
     * true if this event should be caught
     * false either
     */
    fun validate(event: ChannelEvent): Boolean {
        // get Json of the provided event
        val json = event.serializedData.getJson()
        // is this json filtered by the provided jsonPath
        return JsonPath.parse(json).read<List<Any>>(jsonPath).isNotEmpty()
    }
}

object ChannelEventFilterSerializer : KSerializer<ChannelEventFilter> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ChannelEventFilter", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ChannelEventFilter) { encoder.encodeString(value.jsonPath) }
    override fun deserialize(decoder: Decoder) = ChannelEventFilter(decoder.decodeString())
}
