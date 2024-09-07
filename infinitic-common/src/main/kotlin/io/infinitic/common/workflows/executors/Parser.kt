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
package io.infinitic.common.workflows.executors

import com.fasterxml.jackson.annotation.JsonView
import io.github.oshai.kotlinlogging.KLogger
import io.infinitic.annotations.Ignore
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.workflows.Channel
import io.infinitic.workflows.Workflow
import org.slf4j.Logger
import java.lang.reflect.Proxy
import java.security.InvalidParameterException
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaField

fun <T : Any> setPropertiesToObject(obj: T, values: Map<PropertyName, PropertyValue>) {
  val properties = obj::class.memberProperties
  values.forEach { (name, value) ->
    properties.find { it.name == name.name }?.let { setProperty(obj, it, value) }
      ?: thisShouldNotHappen(
          "Trying to set unknown property ${obj::class.java.name}:${name.name}",
      )
  }
}

fun <T : Any> getPropertiesFromObject(
  obj: T,
  filter: (p: Pair<KProperty1<out T, *>, Any?>) -> Boolean = { true }
): Map<PropertyName, PropertyValue> =
    obj::class
        .memberProperties
        .map { p: KProperty1<out T, *> -> Pair(p, getProperty(obj, p)) }
        .filter { filter(it) }
        .associateBy(
            { PropertyName(it.first.name) },
            {
              PropertyValue.from(
                  it.second,
                  it.first.javaField!!.genericType,
              )
            },
        )

fun Workflow.setProperties(
  propertiesHashValue: Map<PropertyHash, PropertyValue>,
  propertiesNameHash: Map<PropertyName, PropertyHash>
) {
  val properties = propertiesNameHash.mapValues {
    propertiesHashValue[it.value]
      ?: thisShouldNotHappen("unknown hash ${it.value} in $propertiesHashValue")
  }

  setPropertiesToObject(this, properties)
}

fun Workflow.getProperties() =
    getPropertiesFromObject(this) {
      // excludes Channels
      !it.first.returnType.isSubtypeOf(Channel::class.starProjectedType) &&
          // excludes Proxies (tasks and workflows) and null
          !(it.second?.let { Proxy.isProxyClass(it::class.java) } ?: true) &&
          // exclude SLF4J loggers
          !it.first.returnType.isSubtypeOf(Logger::class.createType()) &&
          // exclude KotlinLogging loggers
          !it.first.returnType.isSubtypeOf(KLogger::class.createType()) &&
          // exclude Ignore annotation
          !it.first.hasAnnotation<Ignore>()
    }

private val KProperty1<*, *>.jsonViewClass
  get(): Class<*>? {
    val jsonViewAnnotation = findAnnotations(JsonView::class)
        .also {
          if (it.size > 1)
            throw InvalidParameterException("Property '$name' should not have more than one @JsonView annotation")
        }
        .firstOrNull()
    return jsonViewAnnotation?.value
        ?.also {
          if (it.size != 1)
            throw InvalidParameterException("The annotation @JsonView on property '$name' must have one parameter")
        }
        ?.firstOrNull()?.java
  }

private fun <T : Any> getProperty(obj: T, kProperty: KProperty1<out T, *>): Any? =
    kProperty.javaField?.let {
      val errorMsg = "Property ${obj::class.java.name}:${it.name} is not readable"

      try {
        it.isAccessible = true
      } catch (e: SecurityException) {
        throw RuntimeException("$errorMsg (can not set accessible)")
      }

      it.get(obj)
    }

private fun <T : Any> setProperty(
  obj: T,
  kProperty: KProperty1<out T, *>,
  propertyValue: PropertyValue
) {
  kProperty.javaField?.apply {
    val errorMsg = "Property ${obj::class.java.name}:$name can not be set"

    try {
      isAccessible = true
    } catch (e: SecurityException) {
      throw RuntimeException("$errorMsg (not accessible)", e)
    }
    val value = propertyValue.value(genericType, kProperty.jsonViewClass)

    set(obj, value)
  }
}
