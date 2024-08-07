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
package io.infinitic.common.serDe.utils

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import io.infinitic.serDe.java.Json.mapper
import kotlin.reflect.full.superclasses

/**
 * Infers the JavaType of the given object.
 *
 * @return The inferred JavaType or null if the object is null.
 */
internal fun Any.inferJavaType(): JavaType {
  val typeFactory = mapper.typeFactory
  val rawType = this::class.java
  return when (this) {
    is Array<*> -> {
      // for Array, rawType contains elements type
      val typeArg = typeFactory.inferIteratorType(iterator())
      typeFactory.constructArrayType(typeArg)
    }

    is Collection<*> -> {
      val typeArg = typeFactory.inferIteratorType(iterator())
      typeFactory.constructParametricType(rawType, typeArg)
    }

    is Map<*, *> -> {
      val typeKey = typeFactory.inferIteratorType(keys.iterator())
      val typeVal = typeFactory.inferIteratorType(values.iterator())
      typeFactory.constructParametricType(rawType, typeKey, typeVal)
    }

    else -> typeFactory.constructType(this::class.java)
  }
}

private fun TypeFactory.inferIteratorType(iterator: Iterator<*>): JavaType {
  /**
   * Finds the common type among the given list of classes.
   *
   * @param types The list of classes to find the common type from.
   * @return The common type among the given classes.
   */
  fun findCommonType(types: List<Class<*>>): Class<*> {
    fun checkEmptyOrSingle(types: List<Class<*>>): Class<*>? {
      return when {
        types.isEmpty() -> Any::class.java
        types.size == 1 -> types.first()
        else -> null
      }
    }

    fun getAllSuperTypes(clazz: Class<*>): Set<Class<*>> {
      val superTypes = mutableSetOf<Class<*>>()
      superTypes.add(clazz)
      clazz.kotlin.superclasses.forEach {
        superTypes.addAll(getAllSuperTypes(it.java))
      }
      superTypes.addAll(clazz.interfaces)
      return superTypes
    }

    return checkEmptyOrSingle(types) ?: run {
      val allTypes = types.flatMap { getAllSuperTypes(it) }.toSet()
      allTypes.find { candidate ->
        candidate != Any::class.java && types.all { candidate.isAssignableFrom(it) }
      } ?: Any::class.java
    }
  }

  /**
   * Finds the common super type among a list of JavaTypes.
   *
   * @param types The list of JavaTypes to find the common super type from.
   * @return The common super type among the given JavaTypes.
   */
  fun commonSuperType(types: List<JavaType?>): JavaType {
    fun constructCommonType(nonNullTypes: List<JavaType>): JavaType {
      val classes = nonNullTypes.map { it.rawClass }
      val commonType = findCommonType(classes)
      val typeParameters = commonType.typeParameters
      if (typeParameters.isEmpty()) return constructType(commonType)

      val commonTypeArgs = typeParameters.indices.map { index ->
        val paramTypes = nonNullTypes.mapNotNull { it.containedTypeOrUnknown(index) }
        commonSuperType(paramTypes)
      }.toTypedArray()
      return constructParametricType(commonType, *commonTypeArgs)
    }

    val nonNullTypes = types.filterNotNull()

    return when {
      nonNullTypes.isEmpty() -> constructType(Any::class.java)
      nonNullTypes.size == 1 -> nonNullTypes.first()
      else -> constructCommonType(nonNullTypes)
    }
  }

  val elementTypes = iterator.asSequence().filterNotNull().map { it.inferJavaType() }.toList()

  return commonSuperType(elementTypes)
}
