package io.infinitic.serDe.java

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import kotlin.reflect.full.superclasses

//private fun main() {
//  val obj: List<Map<Int, List<Pojo>>> =
//      listOf(mapOf(1 to listOf(Pojo2("a", "b"), Pojo1("a", 32, JType.TYPE_2))))
//
//  val mapper = ObjectMapper()
//  val javaType = mapper.typeFactory.inferType(obj)
//  val javaTypeStr = mapper.writeValueAsString(javaType).replace(
//      "java.util.Collections\$SingletonMap<", "java.util.LinkedHashMap<",
//  ).replace(
//      "java.util.Collections\$SingletonSet<", "java.util.LinkedHashSet<",
//  ).replace(
//      "java.util.Collections\$SingletonList<", "java.util.ArrayList<",
//  )
//  val javaType2 = mapper.readValue(javaTypeStr, JavaType::class.java)
//  println("javaType2=$javaType2")
//
//  val objStr = mapper.writeValueAsString(obj)
//  println("objStr=$objStr")
//
//  val obj2: Any? = mapper.readValue(objStr, javaType2)
//  println("obj2=$obj2")
//  println("obj1=obj2 ${obj == obj2}")
//}

/**
 * Infers the JavaType of the given object.
 *
 * @param any The object to infer the JavaType from.
 * @return The inferred JavaType or null if the object is null.
 */
private fun TypeFactory.inferType(any: Any?): JavaType? {
  return when (any) {
    null -> null
    is Array<*> -> {
      val rawType = any::class.java
      val typeArg = inferIteratorType(any.iterator())
      constructParametricType(rawType, typeArg)
    }

    is Collection<*> -> {
      val rawType = any::class.java
      val typeArg = inferIteratorType(any.iterator())
      constructParametricType(rawType, typeArg)
    }

    is Map<*, *> -> {
      val rawType = any::class.java
      val typeKey = inferIteratorType(any.keys.iterator())
      val typeVal = inferIteratorType(any.values.iterator())
      constructParametricType(rawType, typeKey, typeVal)
    }

    else -> constructType(any::class.java)
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

  val elementTypes = iterator.asSequence().map { inferType(it) }.toList()

  return commonSuperType(elementTypes)
}
