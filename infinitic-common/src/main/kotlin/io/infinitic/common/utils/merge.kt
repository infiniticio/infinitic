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

package io.infinitic.common.utils

import io.infinitic.common.exceptions.thisShouldNotHappen
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

@JvmName("mergeNullableLeft")
inline infix fun <reified T : Any> T?.merge(default: T?): T? {
  if (this == null) return default

  return this merge default
}

inline infix fun <reified T : Any> T.merge(default: T?): T {
  if (default == null) return this

  val constructor = T::class.primaryConstructor ?: thisShouldNotHappen()
  val params = constructor.parameters.associateBy({ it.name!! }, { it })
  val args = mutableMapOf<KParameter, Any?>()

  T::class.memberProperties.forEach { prop ->
    val parameter = params[prop.name]
    if (parameter != null) {
      args[parameter] = prop.get(this) ?: prop.get(default)
    }
  }

  return constructor.callBy(args)
}
