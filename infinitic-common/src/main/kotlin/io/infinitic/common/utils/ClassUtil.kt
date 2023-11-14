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

import java.lang.reflect.Constructor

fun String.getClass(
  classNotFound: String = "Class \"$this\" not found",
  errorClass: String = "Can not access class \"$this\""
): Class<*> =
    try {
      Class.forName(this)
    } catch (e: ClassNotFoundException) {
      throwError(classNotFound)
    } catch (e: Exception) {
      throwError(errorClass, e)
    }

fun <T : Any> Class<T>.getEmptyConstructor(
  noEmptyConstructor: String = "Class \"$name\" must have an empty constructor",
  constructorError: String = "Can not access constructor of class \"$name\""
): Constructor<T> =
    try {
      getDeclaredConstructor()
    } catch (e: NoSuchMethodException) {
      throwError(noEmptyConstructor)
    } catch (e: Exception) {
      throwError(constructorError, e)
    }

fun <T : Any> Constructor<T>.getInstance(
  instanceError: String = "Error during instantiation of class \"$name\""
): T =
    try {
      newInstance()
    } catch (e: Exception) {
      throwError(instanceError, e)
    }

private fun throwError(msg: String, e: Exception? = null): Nothing {
  throw IllegalArgumentException(msg, e)
}
