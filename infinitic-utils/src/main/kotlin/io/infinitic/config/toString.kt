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
package io.infinitic.config

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

fun <T : Any> T.notNullPropertiesToString(interfaceClass: KClass<T>? = null): String {
  val interfaceProperties = interfaceClass?.declaredMemberProperties?.map { it.name }

  return this::class.memberProperties
      .filter { prop ->
        // keep only properties that are part of the interface class
        interfaceProperties?.contains(prop.name) ?: true
      }
      .mapNotNull { prop ->
        prop.isAccessible = true
        // display non null properties
        prop.call(this)?.let { "${prop.name}=$it" }
      }.joinToString()
}

