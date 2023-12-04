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

import io.infinitic.annotations.Name
import io.infinitic.annotations.Timeout
import io.infinitic.tasks.WithTimeout
import java.lang.reflect.Constructor
import java.lang.reflect.Method

fun String.getClass(
  classNotFound: String = "Class '$this' not found",
  errorClass: String = "Can not access class '$this'"
): Class<*> =
    try {
      Class.forName(this)
    } catch (e: ClassNotFoundException) {
      throwError(classNotFound)
    } catch (e: Exception) {
      throwError(errorClass, e)
    }

fun <T : Any> Class<T>.getEmptyConstructor(
  noEmptyConstructor: String = "Class '$name' must have an empty constructor",
  constructorError: String = "Can not access constructor of class '$name'"
): Constructor<T> =
    try {
      getDeclaredConstructor()
    } catch (e: NoSuchMethodException) {
      throwError(noEmptyConstructor)
    } catch (e: Exception) {
      throwError(constructorError, e)
    }

fun <T : Any> Constructor<T>.getInstance(
  instanceError: String = "Error during instantiation of class '$name'"
): T =
    try {
      newInstance()
    } catch (e: Exception) {
      throwError(instanceError, e)
    }

// check if a class can be an implementation of a service or workflow by name
// This is used as a health check when registering a service or workflow in workers
fun Class<*>.isImplementationOf(name: String): Boolean {
  var klass = this

  do {
    klass.interfaces.forEach {
      if (name == it.getAnnotatedName()) return true
    }

    // Look for the name on the superclass
    klass = klass.superclass ?: break

  } while (true)

  return false
}

// Return the "fullMethodName" used in workflow task to detect workflow changes
// This must NOT change as it could trigger false positive in change detection
fun Class<*>.getFullMethodName(method: Method): String =
    "${findNameAnnotation() ?: simpleName}::${method.getAnnotatedName()}"

// Return the name of a method, or its @Name annotation if any
fun Method.getAnnotatedName(): String = findNameAnnotation() ?: name

// Return the name of a class, or its @Name annotation if any
fun Class<*>.getAnnotatedName(): String = findNameAnnotation() ?: name

// search for a @Timeout annotation on this method or its parents' methods
// Interfaces are EXCLUDED, as this annotation has a different meaning on interfaces
fun Method.findTimeoutAnnotation(): Class<out WithTimeout>? =
    findAnnotation(Timeout::class.java, false)?.with?.java

// search for a @Timeout annotation on this class
// Interfaces are EXCLUDED, as this annotation has a different meaning on interfaces
fun Class<*>.findTimeoutAnnotation(): Class<out WithTimeout>? =
    findAnnotation(Timeout::class.java, false)?.with?.java

// search for a @Name annotation on this method,
// Interfaces are included in the search
internal fun Method.findNameAnnotation(): String? = findAnnotation(Name::class.java)?.name


// search for a @Name annotation on this class,
// Interfaces are included in the search
internal fun Class<*>.findNameAnnotation(): String? = findAnnotation(Name::class.java)?.name

// search for an annotation on a method, in the class, its interfaces, or its parent
internal fun <T : Annotation, S : Class<out T>> Method.findAnnotation(
  annotation: S,
  withInterfaces: Boolean = true
): T? {
  var method = this
  var klass = declaringClass

  // if not
  do {
    // Look for the annotation on the method
    method.getAnnotation(annotation)?.also { return it }

    // Look for the annotation on all interfaces
    if (withInterfaces) klass.interfaces.forEach { interfac ->
      try {
        interfac.getMethod(name, *parameterTypes).also { it.isAccessible = true }
      } catch (e: Exception) {
        null
      }?.findAnnotation(annotation)?.also { return it }
    }

    // Look for the annotation on the superclass
    klass = klass.superclass ?: break

    method = try {
      klass.getMethod(name, *parameterTypes).also { it.isAccessible = true }
    } catch (e: Exception) {
      break
    }

  } while (true)

  return null
}

// search for an annotation on a class, its interfaces, or its parent
internal fun <T : Annotation> Class<*>.findAnnotation(
  annotation: Class<out T>,
  withInterfaces: Boolean = true
): T? {
  var klass = this

  do {
    // Look for the annotation on the class
    klass.getAnnotation(annotation)?.also { return it }

    // Look for the annotation on the interfaces
    if (withInterfaces) klass.interfaces.forEach { interfac ->
      interfac.findAnnotation(annotation)?.also { return it }
    }

    // if not, inspect the superclass
    klass = klass.superclass ?: break

  } while (true)

  return null
}

private fun throwError(msg: String, e: Exception? = null): Nothing {
  throw IllegalArgumentException(msg, e)
}
