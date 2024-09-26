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

import com.fasterxml.jackson.annotation.JsonView
import io.infinitic.annotations.CheckMode
import io.infinitic.annotations.Delegated
import io.infinitic.annotations.Name
import io.infinitic.annotations.Retry
import io.infinitic.annotations.Timeout
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.exceptions.tasks.NoMethodFoundWithParameterCountException
import io.infinitic.exceptions.tasks.NoMethodFoundWithParameterTypesException
import io.infinitic.exceptions.tasks.TooManyMethodsFoundWithParameterCountException
import io.infinitic.exceptions.tasks.TooManyMethodsFoundWithParameterTypesException
import io.infinitic.tasks.WithRetry
import io.infinitic.tasks.WithTimeout
import io.infinitic.tasks.millis
import io.infinitic.workflows.WorkflowCheckMode
import io.mockk.every
import io.mockk.mockkClass
import org.jetbrains.annotations.TestOnly
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.security.InvalidParameterException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.jvm.javaMethod

private val cachesList
  get() = listOf(
      methodsCache,
      methodNameCache,
      classSimpleNameCache,
      classNameCache,
      workflowCheckModeCache,
      methodWithRetryCache,
      methodWithTimeoutCache,
      methodIsDelegatedCache,
      methodTimeoutCache,
      classTimeoutCache,
      methodJsonViewClassCache,
      methodParameterJsonViewClassCache,
  )

@get:TestOnly
val maxCachesSize get() = cachesList.maxOf { it.keys.size }

internal val methodsCache = ConcurrentHashMap<String, Method>()

fun Class<*>.getMethodPerNameAndParameters(
  methodName: String,
  parameterTypes: List<String>?,
  parametersCount: Int
): Method = methodsCache.getOrPut(getCacheKey(this, methodName, parameterTypes, parametersCount)) {
  when (parameterTypes) {
    null -> getMethodPerAnnotationAndParametersCount(methodName, parametersCount)
      ?: getMethodPerNameAndParameterCount(methodName, parametersCount)
      ?: throw NoMethodFoundWithParameterCountException(name, methodName, parametersCount)

    else -> getMethodPerAnnotationAndParameterTypes(methodName, parameterTypes)
      ?: getMethodPerNameAndParameterTypes(methodName, parameterTypes)
      ?: throw NoMethodFoundWithParameterTypesException(name, methodName, parameterTypes)
  }
}

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
      if (name == it.annotatedName) return true
    }
    // Look for the name on the superclass
    klass = klass.superclass ?: break

  } while (true)

  return false
}

/**
 * Return the name of a class, or its @Name annotation if any
 */
internal val classNameCache = ConcurrentHashMap<Class<*>, String>()

val Class<*>.annotatedName: String
  get() = classNameCache.getOrPut(this) {
    // Use @Name annotation if any
    findAnnotation(Name::class.java)?.name
    // else use class name
      ?: name
  }

/**
 * Get the CheckMode of a method (if any)
 */

internal val workflowCheckModeCache = ConcurrentHashMap<Method, Optional<WorkflowCheckMode>>()

val Method.checkMode: WorkflowCheckMode?
  get() = workflowCheckModeCache.getOrPut(this) {
    when (val mode = findCheckMode()) {
      null -> Optional.empty()
      else -> Optional.of(mode)
    }
  }.getOrNull()

/**
 * Return the "fullMethodName" used in workflow task to detect workflow changes
 * This must NOT change as it could trigger false positive in change detection
 */
fun Class<*>.getFullMethodName(method: Method) =
    "${findAnnotatedSimpleName()}::${method.annotatedName}"

/**
 * Return the name of a method, or its @Name annotation if any
 */
internal val methodNameCache = ConcurrentHashMap<Method, String>()

val Method.annotatedName: String
  get() = methodNameCache.getOrPut(this) {
    // Use @Name annotation if any
    findAnnotation(Name::class.java)?.name
    // else use method name
      ?: name
  }


/**
 * Get the WithRetry instance of a method (if any)
 */
internal val methodWithRetryCache = ConcurrentHashMap<Method, Result<WithRetry?>>()

val Method.withRetry: Result<WithRetry?>
  get() = methodWithRetryCache.getOrPut(this) {
    findWithRetryClass()?.getInstance()?.getOrElse { return Result.failure(it) }
        ?.let { Result.success(it) }
      ?: Result.success(null)
  }

/**
 * Get the WithTimeout instance of a method (if any)
 * This method MUST be call with a klass that is not an interface
 */
internal val methodWithTimeoutCache = ConcurrentHashMap<Method, Result<WithTimeout?>>()

@Suppress("UNCHECKED_CAST")
val Method.withTimeout: Result<WithTimeout?>
  get() = methodWithTimeoutCache.getOrPut(this) {
    (findWithTimeoutClassByAnnotation()
      ?: when (WithTimeout::class.java.isAssignableFrom(declaringClass)) {
        true -> declaringClass as Class<out WithTimeout>
        false -> null
      })?.getInstance() ?: Result.success(null)
  }

/**
 * Returns true if this method has the [Delegated] annotation
 */
internal val methodIsDelegatedCache = ConcurrentHashMap<Method, Boolean>()

val Method.isDelegated: Boolean
  get() = methodIsDelegatedCache.getOrPut(this) {
    findAnnotation(Delegated::class.java) != null
  }

/**
 * Get the WithTimeout instance of a method (if any)
 */
fun <T : Any> Method.getMillisDuration(klass: Class<T>): Result<MillisDuration?> {
  val millis: Long? = (timeoutInMillis ?: klass.timeoutInMillis)
      ?.getOrElse { return Result.failure(it) }

  return Result.success(millis?.let { MillisDuration(it) })
}

private fun getCacheKey(
  klass: Class<*>,
  methodName: String,
  parameterTypes: List<String>?,
  parametersCount: Int
) = "${klass.name}-$methodName-${parameterTypes?.joinToString("-")}-$parametersCount"

private fun Class<*>.getMethodPerAnnotationAndParameterTypes(
  methodName: String,
  parameterTypes: List<String>
): Method? {
  var clazz = this

  do {
    // has current class a method with @Name annotation and right parameters?
    val methods = clazz.methods.filter { method ->
      method.isAccessible = true
      method.isAnnotationPresent(Name::class.java) &&
          method.getDeclaredAnnotation(Name::class.java).name == methodName &&
          method.parameterTypes.map { it.name } == parameterTypes
    }
    when (methods.size) {
      0 -> Unit
      1 -> return methods[0]
      else -> throw TooManyMethodsFoundWithParameterTypesException(name, methodName, parameterTypes)
    }

    // has any of the interfaces a method with @Name annotation and right parameters?
    clazz.interfaces.forEach { interfaze ->
      interfaze.getMethodPerAnnotationAndParameterTypes(methodName, parameterTypes)?.also {
        return it
      }
    }

    // if not, inspect the superclass
    clazz = clazz.superclass ?: break
  } while ("java.lang.Object" != clazz.canonicalName)

  return null
}

private fun Class<*>.getMethodPerNameAndParameterTypes(
  methodName: String,
  parameterTypes: List<String>
): Method? = try {
  getMethod(methodName, *(parameterTypes.map { classForName(it) }.toTypedArray()))
} catch (e: NoSuchMethodException) {
  null
}

private fun Class<*>.getMethodPerAnnotationAndParametersCount(
  methodName: String,
  parameterCount: Int
): Method? {
  var klass = this

  do {
    // has current class a method with correct @Name annotation and right count of parameters?
    val methods = klass.methods.filter { method ->
      method.isAccessible = true
      method.isAnnotationPresent(Name::class.java) &&
          method.getDeclaredAnnotation(Name::class.java).name == methodName &&
          method.parameterTypes.size == parameterCount
    }

    when (methods.size) {
      0 -> Unit
      1 -> return methods[0]
      else -> throw TooManyMethodsFoundWithParameterCountException(
          this.name,
          methodName,
          parameterCount,
      )
    }

    // has any of the interfaces a method with @Name annotation and right count of parameters?
    klass.interfaces.forEach { `interface` ->
      `interface`.getMethodPerAnnotationAndParametersCount(methodName, parameterCount)?.also {
        return it
      }
    }

    // if not, inspect the superclass
    klass = klass.superclass ?: break
  } while ("java.lang.Object" != klass.canonicalName)

  return null
}

private fun Class<*>.getMethodPerNameAndParameterCount(
  methodName: String,
  parameterCount: Int
): Method? {
  val methods = methods.filter { method ->
    method.isAccessible = true
    method.name == methodName && method.parameterCount == parameterCount
  }

  return when (methods.size) {
    0 -> null
    1 -> methods[0]
    else -> throw TooManyMethodsFoundWithParameterCountException(name, methodName, parameterCount)
  }
}

private fun classForName(name: String): Class<out Any> =
    when (name) {
      "long" -> Long::class.java
      "int" -> Int::class.java
      "short" -> Short::class.java
      "byte" -> Byte::class.java
      "double" -> Double::class.java
      "float" -> Float::class.java
      "char" -> Char::class.java
      "boolean" -> Boolean::class.java
      else -> Class.forName(name)
    }

/**
 * Return the simple name of a class, or its @Name annotation if any
 */
internal val classSimpleNameCache = ConcurrentHashMap<Class<*>, String>()

private fun Class<*>.findAnnotatedSimpleName(): String = classSimpleNameCache.getOrPut(this) {
  // Use @Name annotation if any
  findAnnotation(Name::class.java)?.name
  // else use class name
    ?: simpleName
}


private fun Method.findCheckMode(): WorkflowCheckMode? =
    // look for @CheckMode annotation on this method
    getAnnotation(CheckMode::class.java)?.mode
    // else look for @CheckMode annotation on the class
      ?: declaringClass::class.java.getAnnotation(CheckMode::class.java)?.mode


/**
 * Returns the timeout of a method as defined by its @Timeout annotation
 */
internal val methodTimeoutCache = ConcurrentHashMap<Method, Optional<Result<Long?>>>()

private val Method.timeoutInMillis: Result<Long?>?
  get() = methodTimeoutCache.getOrPut(this) {
    when (val result = findTimeoutInMillis()) {
      null -> Optional.empty()
      else -> Optional.of(result)
    }
  }.getOrNull()

private fun Method.findTimeoutInMillis(): Result<Long?>? =
    findWithTimeoutClassByAnnotation()
        ?.getInstance()
        ?.getOrElse { return Result.failure(it) }
        ?.millis

/**
 * Returns the timeout of a class as defined by the @Timeout annotation
 */

internal val classTimeoutCache = ConcurrentHashMap<Class<*>, Optional<Result<Long?>>>()

internal val <T : Any> Class<T>.timeoutInMillis: Result<Long?>?
  get() = classTimeoutCache.getOrPut(this) {
    when (val timeout = findTimeoutInMillis()) {
      null -> Optional.empty()
      else -> Optional.of(timeout)
    }
  }.getOrNull()

private fun <T : Any> Class<T>.findTimeoutInMillis(): Result<Long?>? {
  // if this is not a WithTimeout interface, return null
  if (!hasMethodImplemented(WithTimeout::getTimeoutSeconds.javaMethod!!))
    return null

  // this is not an interface, we can build an instance and call the method
  if (!isInterface) getInstance()
      .getOrElse { return Result.failure(it) }
      .let { return (it as WithTimeout).millis }

  // if 'this' is an interface, we use a mock to call the method
  // as building a mock is expensive, we cache it
  return try {
    Result.success(
        when (val mock = mock(this)) {
          is WithTimeout -> mock.millis.getOrThrow()
          else -> thisShouldNotHappen()
        },
    )
  } catch (e: Exception) {
    Result.failure(e)
  }
}

private fun <T : Any> mock(klass: Class<T>): T {
  val mock = mockkClass(klass.kotlin)
  when (mock is WithTimeout) {
    true -> every { mock.getTimeoutSeconds() } answers { callOriginal() }
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

fun <T> Class<T>.getInstance(
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

private fun Method.findWithTimeoutClassByAnnotation(): Class<out WithTimeout>? =
    // look for @Timeout annotation on this method
    findAnnotation(Timeout::class.java)?.with?.java
    // else look for @Timeout annotation at class level
      ?: declaringClass.findAnnotation(Timeout::class.java)?.with?.java

@Suppress("UNCHECKED_CAST")
private fun Class<*>.findWithRetryClass(): Class<out WithRetry>? =
    // look for @Timeout annotation on this class
    findAnnotation(Retry::class.java)?.with?.java
    // else look for a WithRetry interface, with an implemented getSecondsBeforeRetry method
      ?: when (hasMethodImplemented(WithRetry::getSecondsBeforeRetry.javaMethod!!)) {
        true -> this as Class<out WithRetry>
        false -> null
      }

private fun Method.findWithRetryClass(): Class<out WithRetry>? =
    // look for @Retry annotation on this method
    findAnnotation(Retry::class.java)?.with?.java
    // else look at the class level
      ?: declaringClass.findWithRetryClass()


internal val methodJsonViewClassCache = ConcurrentHashMap<Method, Optional<Class<*>>>()

val Method.jsonViewClass: Class<*>?
  get() = methodJsonViewClassCache.getOrPut(this) {
    when (val klass = findJsonViewClass()) {
      null -> Optional.empty()
      else -> Optional.of(klass)
    }
  }.getOrNull()

private fun Method.findJsonViewClass(): Class<*>? =
    // look for @JsonView annotation on this class
    findAnnotation(JsonView::class.java)?.value?.let {
      when (it.size) {
        0 -> null
        1 -> it[0].java
        else -> throw InvalidParameterException(
            "The annotation @JsonView on method ${declaringClass.simpleName}::$name " +
                " must not have more than one parameter",
        )
      }
    }

internal fun Method.getParameterType(index: Int): Type = parameters[index].parameterizedType

internal val methodParameterJsonViewClassCache =
    ConcurrentHashMap<Pair<Method, Int>, Optional<Class<*>>>()

internal fun Method.getParameterJsonViewClass(index: Int): Class<*>? =
    methodParameterJsonViewClassCache.getOrPut(Pair(this, index)) {
      when (val klass = findParameterJsonViewClass(index)) {
        null -> Optional.empty()
        else -> Optional.of(klass)
      }
    }.getOrNull()

private fun Method.findParameterJsonViewClass(index: Int): Class<*>? =
    findParameterAnnotation(JsonView::class.java, index)?.value?.let {
      when (it.size) {
        0 -> null
        1 -> it[0].java
        else -> throw InvalidParameterException(
            "The annotation @JsonView on parameter $index of " +
                "${declaringClass.simpleName}::${name} must not have more than one parameter",
        )
      }
    }

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
    klass.interfaces.forEach { `interface` ->
      try {
        `interface`.getMethod(name, *parameterTypes).also { it.isAccessible = true }
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

// search for an annotation on a Method's parameter, its interfaces, or its parent
internal fun <T : Annotation, S : Class<out T>> Method.findParameterAnnotation(
  annotation: S,
  parameterIndex: Int
): T? {
  var method = this
  var klass = declaringClass

  do {
    // Look for the annotation on the parameter of current method
    method.parameters[parameterIndex].getAnnotation(annotation)?.also { return it }

    // Look for the annotation on all interfaces
    klass.interfaces.forEach { `interface` ->
      try {
        `interface`.getMethod(name, *parameterTypes).also { it.isAccessible = true }
      } catch (e: Exception) {
        null
      }?.findParameterAnnotation(annotation, parameterIndex)?.also { return it }
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
    klass.interfaces.forEach { `interface` ->
      `interface`.findAnnotation(annotation)?.also { return it }
    }

    // if not, inspect the superclass
    klass = klass.superclass ?: break

  } while (true)

  return null
}


// search for an interface and its implemented method on a class
private fun Class<*>.hasMethodImplemented(method: Method): Boolean {
  require(method.declaringClass.isInterface) { "Class '${method.declaringClass}' is not an interface" }

  if (!method.declaringClass.isAssignableFrom(this)) return false

  if (!isInterface) return true

  return try {
    getDeclaredMethod(method.name, *(method.parameterTypes)).isDefault
  } catch (e: NoSuchMethodException) {
    false
  }
}
