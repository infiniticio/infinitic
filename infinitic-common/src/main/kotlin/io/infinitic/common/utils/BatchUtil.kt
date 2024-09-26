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

import io.infinitic.annotations.Batch
import io.infinitic.common.exceptions.thisShouldNotHappen
import java.lang.reflect.Constructor
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/**
 * Retrieves a map associating single method with their batch methods for the class.
 *
 * The method scans through all methods in the class and identifies methods annotated with `@Batch`.
 * For each batch method found, it locates the corresponding single method that is not annotated with `@Batch`
 * but has the same annotated name and matches the parameter constraints defined by `isBatchedOf`.
 *
 * @return a map where the key is the batch method and the value is the corresponding single method.
 * @throws Exception if a corresponding single method is not found or multiple corresponding single methods are found.
 */
fun Class<*>.getBatchMethods(): Map<Method, Method> =
    methods.filter {
      // get all batched methods
      it.findAnnotation(Batch::class.java) != null
    }.associateBy { batchMethod ->
      // for this batch method, find all single method with the same annotated name
      methods.filter { singleMethod ->
        (batchMethod.annotatedName == singleMethod.annotatedName) &&
            (singleMethod.findAnnotation(Batch::class.java) == null) &&
            batchMethod.isBatchedOf(singleMethod)
      }.also { candidates ->
        // we should not have a batch method without an associated single method
        if (candidates.isEmpty()) throw Exception(
            "Corresponding single method not found for the @Batch method $name:${batchMethod.name}",
        )
        // we should not have a batch method with more than one associated single method
        if (candidates.size > 1) throw Exception(
            "Multiple Corresponding single methods (${candidates.joinToString { it.name }}) " +
                "found for the @Batch method $name:${batchMethod.name}",
        )
      }.first()
    }

private fun Method.isBatchedOf(method: Method): Boolean {
  if (parameters.size != 1) throw Exception(
      "A @Batch method must have exactly one parameter that is a collection or an array. " +
          "The @Batch method ${declaringClass.name}::$name has ${parameters.size} parameters",
  )
  val parameter = parameters[0]
  val type: Type = parameter.parameterizedType
  val elementType = type.getElementType()?.also { println("batch: elementType = ${it.typeName}") }
    ?: throw Exception(
        "A @Batch method must have exactly one parameter that is a collection or an array. " +
            "But for the @Batch method ${declaringClass.name}::$name this type is $type",
    )
  val singleTypes = method.parameters.map { it.parameterizedType }
  return elementType.isSameThan(singleTypes)
}

private fun Type.isArray() = (this is Class<*> && isArray) || (this is GenericArrayType)

private fun Type.isCollection(): Boolean {
  if (this !is ParameterizedType) return false
  return when (val rawType = this.rawType) {
    is Class<*> -> Collection::class.java.isAssignableFrom(rawType)
    else -> false
  }
}

private fun Type.isObject(): Boolean = if (this is Class<*>) when {
  isPrimitive -> false
  Collection::class.java.isAssignableFrom(this) -> false
  isArray -> false
  else -> true
}
else false

private fun Type.getElementType(): Type? {
  return when {
    isArray() -> {
      when (this) {
        is Class<*> -> this.componentType
        is GenericArrayType -> this.genericComponentType
        else -> thisShouldNotHappen()
      }
    }

    isCollection() -> extractBoundType((this as ParameterizedType).actualTypeArguments.first())

    else -> null
  }
}

// Extracts the bound type for a given type. If the type is a `WildcardType` with upper bounds,
// it returns the first upper bound; otherwise, it returns the type itself.
private fun extractBoundType(type: Type) = when (type) {
  is WildcardType -> {
    val upperBounds = type.upperBounds
    if (upperBounds.isNotEmpty()) upperBounds.first() else type
  }

  else -> type
}

// Map of primitive types and their wrapper
private val primitiveToWrapperMap = mapOf<Type, Class<*>>(
    java.lang.Byte.TYPE to java.lang.Byte::class.java,
    java.lang.Short.TYPE to java.lang.Short::class.java,
    java.lang.Integer.TYPE to java.lang.Integer::class.java,
    java.lang.Long.TYPE to java.lang.Long::class.java,
    java.lang.Float.TYPE to java.lang.Float::class.java,
    java.lang.Double.TYPE to java.lang.Double::class.java,
    java.lang.Character.TYPE to java.lang.Character::class.java,
    java.lang.Boolean.TYPE to java.lang.Boolean::class.java,
    java.lang.Void.TYPE to java.lang.Void::class.java,
)

private fun Type.isSameThan(type: Type) =
    (primitiveToWrapperMap[this] ?: this) == (primitiveToWrapperMap[type] ?: type)

private fun Type.isSameThan(types: List<Type>): Boolean {
  return when {
    isObject() -> (this as Class<*>).getConstructorWith(types) != null
    else -> when (types.size) {
      1 -> (this.isSameThan(types.first())).also { println("$this == ${types.first()} -> $it") }
      else -> false
    }
  }
}

private fun <S> Class<S>.getConstructorWith(types: List<Type>): Constructor<S>? {
  val constructors = constructors.filter { constructor ->
    (constructor.parameterTypes.size == types.size) &&
        constructor.parameters.map { it.parameterizedType }.zip(types).all { (type, otherType) ->
          type.isSameThan(otherType)
        }
  }
  val size = constructors.size
  return when {
    size > 1 -> throw Exception("Class $name has $size constructors with the same types ${types.joinToString()}. This is unexpected.")
    size == 1 -> constructors.first() as Constructor<S>
    else -> null
  }
}
