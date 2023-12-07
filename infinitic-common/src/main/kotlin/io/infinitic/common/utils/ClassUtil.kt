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

import io.infinitic.annotations.CheckMode
import io.infinitic.annotations.Name
import io.infinitic.annotations.Retry
import io.infinitic.annotations.Timeout
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.millis
import io.infinitic.workflows.WorkflowCheckMode
import io.mockk.every
import io.mockk.mockkClass
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.jvm.javaMethod

/**
 * Get an instance of a class by name
 */
fun String.getInstance(
  classNotFound: String = this.classNotFound,
  errorClass: String = this.errorClass,
  noEmptyConstructor: String = this.noEmptyConstructor,
  constructorError: String = this.constructorError,
  instanceError: String = this.instanceError
): Result<Any> = getClass(classNotFound, errorClass)
    .getOrElse { return Result.failure(it) }
    .getInstance(noEmptyConstructor, constructorError, instanceError)
    .getOrElse { return Result.failure(it) }
    .let { Result.success(it) }

/**
 * Get a class by name
 */
fun String.getClass(
  classNotFound: String = this.classNotFound,
  errorClass: String = this.errorClass,
): Result<Class<*>> = try {
  Result.success(Class.forName(this))
} catch (e: ClassNotFoundException) {
  Result.failure(IllegalArgumentException(classNotFound))
} catch (e: Exception) {
  Result.failure(IllegalArgumentException(errorClass, e))
}

/**
 * Check if a class can be an implementation of a service or workflow by name
 * This is used as a health check when registering a service or workflow in workers
 */
fun Class<*>.isImplementationOf(name: String): Boolean {
  var klass = this

  do {
    klass.interfaces.forEach {
      if (name == it.findName()) return true
    }
    // Look for the name on the superclass
    klass = klass.superclass ?: break

  } while (true)

  return false
}

/**
 * Return the "fullMethodName" used in workflow task to detect workflow changes
 * This must NOT change as it could trigger false positive in change detection
 */
fun Class<*>.getFullMethodName(method: Method): String =
    "${findAnnotation(Name::class.java)?.name ?: simpleName}::${method.findName()}"

/**
 * Return the name of a method, or its @Name annotation if any
 */
fun Method.findName(): String =
    // Use @Name annotation if any
    findAnnotation(Name::class.java)?.name
    // else use method name
      ?: name

/**
 * Return the name of a class, or its @Name annotation if any
 */
fun Class<*>.findName(): String =
    // Use @Name annotation if any
    findAnnotation(Name::class.java)?.name
    // else use class name
      ?: name

/**
 * Get the CheckMode of a method (if any)
 */
fun Method.getCheckMode(): WorkflowCheckMode? =
    // look for @CheckMode annotation on this method
    getAnnotation(CheckMode::class.java)?.mode
    // else look for @CheckMode annotation on the class
      ?: declaringClass::class.java.getAnnotation(CheckMode::class.java)?.mode

/**
 * Get the WithTimeout instance of a method (if any)
 */
fun Method.getWithRetry(): Result<WithRetry?> =
    findWithRetryClass()?.getInstance()?.getOrElse { return Result.failure(it) }
        ?.let { Result.success(it) }
      ?: Result.success(null)

/**
 * Get the WithTimeout instance of a method (if any)
 * This method MUST be call with a klass that is not an interface
 */
@Suppress("UNCHECKED_CAST")
fun Method.getWithTimeout(): Result<WithTimeout?> =
    (findWithTimeoutClassByAnnotation()
      ?: when (WithTimeout::class.java.isAssignableFrom(declaringClass)) {
        true -> declaringClass as Class<out WithTimeout>
        false -> null
      })?.getInstance() ?: Result.success(null)


/**
 * Get the WithTimeout instance of a method (if any)
 */
fun <T : Any> Method.getMillisDuration(klass: Class<T>): Result<MillisDuration?> {
  val millis: Long? = (getTimeoutInMillisByAnnotation() ?: klass.getTimeoutInMillisByInterface())
      ?.getOrElse { return Result.failure(it) }

  return Result.success(millis?.let { MillisDuration(it) })
}

/**
 * Return the timeout of a method
 */
internal fun Method.getTimeoutInMillisByAnnotation(): Result<Long?>? =
    findWithTimeoutClassByAnnotation()
        ?.getInstance()
        ?.getOrElse { return Result.failure(it) }
        ?.millis

internal fun <T : Any> Class<T>.getTimeoutInMillisByInterface(): Result<Long?>? {
  // if this is not a WithTimeout interface, return null
  if (!hasMethodImplemented(WithTimeout::getTimeoutInSeconds.javaMethod!!))
    return null

  // this is not an interface, we can build an instance and call the method
  if (!isInterface) getInstance()
      .getOrElse { return Result.failure(it) }
      .let { return (it as WithTimeout).millis }

  // if 'this' is an interface, we use a mock to call the method
  // as building a mock is expensive, we cache it
  return try {
    Result.success(
        TimeoutInInterfaces.timeouts.getOrPut(name) {
          when (val mock = mock(this)) {
            is WithTimeout -> mock.millis.getOrThrow()
            else -> thisShouldNotHappen()
          }
        },
    )
  } catch (e: Exception) {
    Result.failure(e)
  }
}

fun <T : Any> mock(klass: Class<T>): T {
  val mock = mockkClass(klass.kotlin)
  when (mock is WithTimeout) {
    true -> every { mock.getTimeoutInSeconds() } answers { callOriginal() }
    false -> Unit
  }
  return mock
}

private val String.classNotFound: String
  get() = "Class '$this' not found"

private val String.errorClass: String
  get() = "Can not access class '$this'"

private val String.noEmptyConstructor: String
  get() = "Class '$this' must have an empty constructor"

private val String.constructorError: String
  get() = "Can not access class '$this' constructor"

private val String.instanceError: String
  get() = "Error during class '$this' instantiation"

internal fun <T> Class<T>.getInstance(
  noEmptyConstructor: String = (this.name).noEmptyConstructor,
  constructorError: String = (this.name).constructorError,
  instanceError: String = (this.name).instanceError,
): Result<T> = when (this.isInterface) {
  true -> Result.failure(IllegalArgumentException("Class '$this' is an interface"))
  false -> getEmptyConstructor(noEmptyConstructor, constructorError)
      .getOrElse { return Result.failure(it) }
      .getInstance(instanceError)
      .getOrElse { return Result.failure(it) }
      .let { Result.success(it) }
}

internal fun <T> Class<T>.getEmptyConstructor(
  noEmptyConstructor: String,
  constructorError: String
): Result<Constructor<T>> = try {
  Result.success(getDeclaredConstructor())
} catch (e: NoSuchMethodException) {
  Result.failure(IllegalArgumentException(noEmptyConstructor))
} catch (e: Exception) {
  Result.failure(IllegalArgumentException(constructorError, e))
}

internal fun <T> Constructor<T>.getInstance(instanceError: String): Result<T> = try {
  Result.success(newInstance())
} catch (e: Exception) {
  Result.failure(IllegalArgumentException(instanceError, e))
}

internal fun Method.findWithTimeoutClassByAnnotation(): Class<out WithTimeout>? =
    // look for @Timeout annotation on this method
    findAnnotation(Timeout::class.java)?.with?.java
    // else look for @Timeout annotation at class level
      ?: declaringClass.findAnnotation(Timeout::class.java)?.with?.java

@Suppress("UNCHECKED_CAST")
internal fun Class<*>.findWithRetryClass(): Class<out WithRetry>? =
    // look for @Timeout annotation on this class
    findAnnotation(Retry::class.java)?.with?.java
    // else look for a WithRetry interface, with an implemented getSecondsBeforeRetry method
      ?: when (hasMethodImplemented(WithRetry::getSecondsBeforeRetry.javaMethod!!)) {
        true -> this as Class<out WithRetry>
        false -> null
      }

internal fun Method.findWithRetryClass(): Class<out WithRetry>? =
    // look for @Retry annotation on this method
    findAnnotation(Retry::class.java)?.with?.java
    // else look at the class level
      ?: declaringClass.findWithRetryClass()

// search for an annotation on a method, in the class, its interfaces, or its parent
internal fun <T : Annotation, S : Class<out T>> Method.findAnnotation(
  annotation: S,
): T? {
  var method = this
  var klass = declaringClass

  do {
    // Look for the annotation on the method
    method.getAnnotation(annotation)?.also { return it }

    // Look for the annotation on all interfaces
    klass.interfaces.forEach { interfac ->
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
internal fun <T : Annotation> Class<*>.findAnnotation(annotation: Class<out T>): T? {
  var klass = this

  do {
    // Look for the annotation on the class
    klass.getAnnotation(annotation)?.also { return it }

    // Look for the annotation on the interfaces
    klass.interfaces.forEach { interfce ->
      interfce.findAnnotation(annotation)?.also { return it }
    }

    // if not, inspect the superclass
    klass = klass.superclass ?: break

  } while (true)

  return null
}

// search for an interface and its implemented method on a class
internal fun Class<*>.hasMethodImplemented(method: Method): Boolean {
  require(method.declaringClass.isInterface) { "Class '${method.declaringClass}' is not an interface" }

  if (!method.declaringClass.isAssignableFrom(this)) return false

  if (!isInterface) return true

  return try {
    getDeclaredMethod(method.name, *(method.parameterTypes)).isDefault
  } catch (e: NoSuchMethodException) {
    false
  }
}

private object TimeoutInInterfaces {
  val timeouts = ConcurrentHashMap<String, Long?>()
}
