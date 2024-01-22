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
package io.infinitic.common.workflows.data.channels

import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Criteria
import com.jayway.jsonpath.Filter
import com.jayway.jsonpath.Filter.filter
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import kotlinx.serialization.Serializable
import java.security.InvalidParameterException

@Serializable
@AvroNamespace("io.infinitic.workflows.data")
@AvroName("ChannelEventFilter")
data class ChannelFilter(val jsonPath: String, val filter: String? = null) {
  companion object {
    @JvmOverloads
    fun from(jsonPath: String?, criteria: Criteria? = null): ChannelFilter? =
        jsonPath?.let { ChannelFilter(it, criteria?.let { c -> filter(c).toString() }) }
          ?: if (criteria != null)
            throw InvalidParameterException("jsonPath can not be null if criteria is non null")
          else null

    init {
      Configuration.setDefaults(
          object : Configuration.Defaults {
            override fun jsonProvider() = JacksonJsonProvider()

            override fun mappingProvider() = JacksonMappingProvider()

            override fun options() = setOf(Option.ALWAYS_RETURN_LIST)
          },
      )
    }
  }

  /**
   * Evaluate if an event is filtered by the provided jsonPath true if this event should be caught
   * false either
   */
  fun check(event: SignalData): Boolean {
    // get Json of the provided event
    val json = event.serializedData.toJsonString()
    // is this json filtered by the provided jsonPath
    return when (filter) {
      null -> JsonPath.parse(json).read<List<Any>>(jsonPath)
      else -> JsonPath.parse(json).read(jsonPath, Filter.parse(filter))
    }.isNotEmpty()
  }
}
