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
import io.infinitic.common.tasks.data.TaskId
import java.lang.reflect.Constructor
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

/**
 * Represents a pair of methods: a single method and its corresponding batch method.
 *
 * @property single The single method that processes a single item.
 * @property batch The batch method that processes multiple items.
 * @property constructor The constructor used to create instances for the batch method's parameter, if applicable.
 */
data class BatchMethod(
  val single: Method,
  val batch: Method,
  val constructor: Constructor<*>?,
  val componentReturnType: Type?,
) {
  fun getArgs(args: Map<TaskId, List<*>>): Map<String, Any?> = when (constructor) {
    null -> args.map { (k, v) -> k.toString() to v.first() }
    else -> args.map { (k, v) -> k.toString() to constructor.newInstance(*v.toTypedArray()) }
  }.toMap()
}

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
fun Class<*>.getBatchMethods(): List<BatchMethod> =
    getAllMethods().filter {
      // get all batched methods
      it.findAnnotation(Batch::class.java) != null
    }.associateWith { batchMethod ->
      // for this batch method, find all single method with the same annotated name
      methods.filter { singleMethod ->
        batchMethod.isBatchedOf(singleMethod)
      }
    }.also {
      it.validateSingleMethodsAssociation()
      it.forEach { (batch, singles) ->
        when (singles.size) {
          0 -> throw Exception(
              "No single method found corresponding to the @Batch method $name:${batch.name}",
          )

          1 -> if (!batch.hasBatchReturnTypeOf(singles[0])) throw Exception(
              "The return type of the @Batch method $name:${batch.name} should be " +
                  "List<${singles[0].genericReturnType}>, but is ${batch.genericReturnType}",
          )

          else -> {
            // This is to cover a special case in Kotlin where the single method returns
            // Nothing ( fun myMethod() = thiswhouldNnothappen())
            singles.forEach { single ->
              if ((single.returnType != Void::class.java) && !batch.hasBatchReturnTypeOf(single))
                throw Exception(
                    "The return type of the @Batch method $name:${batch.name} should be " +
                        "List<${single.genericReturnType}>, but is ${batch.genericReturnType}",
                )
            }
          }
        }
      }
    }.map { (batch, singles) ->
      singles.map { single ->
        BatchMethod(
            single,
            batch,
            getBatchConstructor(single, batch),
            batch.genericReturnType.getMapType(),
        )
      }
    }.flatten()

private fun getBatchConstructor(single: Method, batch: Method): Constructor<*>? =
    batch.parameters[0].parameterizedType.getMapType()!!
        .getConstructor(single.parameters.map { it.parameterizedType })

/**
 * This ensures that the single methods have a unique and clear association with their corresponding batch method.
 */
private fun Map<Method, List<Method>>.validateSingleMethodsAssociation() {
  flatMap { (batch, singles) -> singles.map { single -> single to batch } }
      .groupBy({ it.first }, { it.second })
      .filter { it.value.size > 1 }
      .forEach { (single, batches) ->
        throw Exception(
            "Multiple @Batch methods (${batches.joinToString { it.name }}) found " +
                "for the single method ${single.declaringClass}::${single.name}",
        )
      }
}

private fun Method.isBatchedOf(method: Method): Boolean =
    (annotatedName == method.annotatedName) &&
        (method.findAnnotation(Batch::class.java) == null) &&
        hasBatchParameterTypesOf(method)

// Comparing the parameters
private fun Method.hasBatchParameterTypesOf(method: Method): Boolean {
  if (parameters.size != 1) throw Exception(
      "A @Batch method must have exactly one parameter that is a Map<String, T>. " +
          "The @Batch method ${declaringClass.name}::$name has ${parameters.size} parameters",
  )
  val type: Type = parameters[0].parameterizedType
  val elementType = type.getMapType()
    ?: throw Exception(
        "A @Batch method must have exactly one parameter that is a Map<String, T>. " +
            "But for the @Batch method ${declaringClass.name}::$name this type is $type",
    )
  val singleTypes = method.parameters.map { it.parameterizedType }
  return elementType.isAssignableFrom(singleTypes)
}

// Comparing the return value
private fun Method.hasBatchReturnTypeOf(method: Method): Boolean {
  if (genericReturnType.isVoid()) return method.genericReturnType.isVoid()

  val returnElementType = genericReturnType.getMapType()
    ?: throw Exception(
        "A @Batch method must have a return type that is a collection or an array. " +
            "But for the @Batch method ${declaringClass.name}::$name this type is $genericReturnType",
    )
  return returnElementType.isAssignableFrom(method.genericReturnType)
}

private fun Type.isObject(): Boolean = if (this is Class<*>) when {
  isPrimitive -> false
  Collection::class.java.isAssignableFrom(this) -> false
  isArray -> false
  else -> true
}
else false

private fun Type.getMapType(): Type? {
  return when {
    isStringMap() -> extractBoundType((this as ParameterizedType).actualTypeArguments[1])
    else -> null
  }
}

// Is type a List?
private fun Type.isStringMap() =
    (this is ParameterizedType) &&
        (rawType == Map::class.java) &&
        actualTypeArguments[0] == String::class.java

// Extracts the bound type for a given type. If the type is a `WildcardType` with upper bounds,
// it returns the first upper bound; otherwise, it returns the type itself.
private fun extractBoundType(type: Type) = when (type) {
  is WildcardType -> {
    val upperBounds = type.upperBounds
    if (upperBounds.isNotEmpty()) upperBounds.first() else type
  }

  else -> type
}

private fun Type.isVoid() = (primitiveToWrapperMap[this] ?: this) == java.lang.Void::class.java

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
    typeOf<Unit>().javaType to java.lang.Void::class.java,
)

private fun Type.isAssignableFrom(subType: Type): Boolean {
  return when {
    // Les deux types sont identiques
    ((primitiveToWrapperMap[this] ?: this) == (primitiveToWrapperMap[subType] ?: subType)) -> true

    // Les deux sont des Class
    this is Class<*> && subType is Class<*> -> this.isAssignableFrom(subType)

    // Les deux sont des ParameterizedType
    this is ParameterizedType && subType is ParameterizedType ->
      this.rawType.isAssignableFrom(subType.rawType)

    // Le this est ParameterizedType et le subType est Class
    this is ParameterizedType && subType is Class<*> ->
      this.rawType.isAssignableFrom(subType)

    // Le this est Class et le subType est ParameterizedType
    this is Class<*> && subType is ParameterizedType ->
      this.isAssignableFrom(subType.rawType)

    // Les deux sont des TypeVariable
    this is TypeVariable<*> && subType is TypeVariable<*> ->
      this.genericDeclaration == subType.genericDeclaration && this.name == subType.name

    // Les deux sont des GenericArrayType
    this is GenericArrayType && subType is GenericArrayType ->
      this.genericComponentType.isAssignableFrom(subType.genericComponentType)

    // Le this est GenericArrayType et le subType est un array Class
    this is GenericArrayType && subType is Class<*> && subType.isArray ->
      this.genericComponentType.isAssignableFrom(subType.componentType)

    // Le this est Class et représente un array, et le subType est GenericArrayType
    this is Class<*> && this.isArray && subType is GenericArrayType ->
      this.componentType.isAssignableFrom(subType.genericComponentType)

    // Le this est WildcardType (le joker)
    this is WildcardType -> {
      val upperBounds = this.upperBounds
      val lowerBounds = this.lowerBounds

      if (upperBounds.isNotEmpty() && upperBounds[0].isAssignableFrom(subType)) true
      else lowerBounds.isNotEmpty() && lowerBounds[0] == subType
    }

    else -> false
  }
}

private fun Type.isAssignableFrom(types: List<Type>): Boolean {
  if (isObject() && ((this as Class<*>).getConstructorWith(types) != null)) return true
  return when (types.size) {
    1 -> (this.isAssignableFrom(types.first()))
    else -> false
  }
}

private fun Type.getConstructor(types: List<Type>): Constructor<*>? = when (isObject()) {
  true -> (this as Class<*>).getConstructorWith(types)
  false -> null
}

private fun <S> Class<S>.getConstructorWith(types: List<Type>): Constructor<S>? {
  val constructors = declaredConstructors.filter { constructor ->
    (constructor.parameterTypes.size == types.size) &&
        constructor.parameters.map { it.parameterizedType }.zip(types).all { (type, otherType) ->
          type.isAssignableFrom(otherType)
        }
  }
  val size = constructors.size
  @Suppress("UNCHECKED_CAST")
  return when {
    size > 1 -> throw Exception("Class $name has $size constructors with the same types ${types.joinToString()}. This is unexpected.")
    size == 1 -> constructors.first() as Constructor<S>
    else -> null
  }
}
